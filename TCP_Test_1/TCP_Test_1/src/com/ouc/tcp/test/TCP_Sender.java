package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;
import java.util.LinkedList;
import java.util.TimerTask;

public class TCP_Sender extends TCP_Sender_ADT {
	
	//发送窗口：存储已发送但未确认的数据包（动态大小）
	private LinkedList<TCP_PACKET> sendWindow;
	//窗口左边界：最小未确认序号
	private int windowBase = 1;
	//下一个要发送的包序号（按包递增：1,2,3...，需与接收方expectedSeq语义一致）
	private int nextSeqNum = 1;
	//超时计时器（仅使用1个）
	private UDT_Timer timer;
	
	//拥塞控制变量
	//拥塞窗口大小（初始为1）
	private int cwnd = 1;
	//慢开始门限（初始设为较大值）
	private int ssthresh = 32;
	//当前轮次已确认的包数量
	private int ackedInRound = 0;
	//上一轮的cwnd值
	private int lastCwnd = 0;
	
	//快重传相关变量
	//上一次收到的ACK序号
	private int lastAck = 0;
	//重复ACK计数
	private int dupAckCount = 0;

	//Reno快恢复状态
	private boolean inFastRecovery = false;
	//进入快恢复时“已发送的最大序号”（用于判断何时退出快恢复）
	private int recover = 0;
	
	//构造函数
	public TCP_Sender() {
		super();
		super.initTCP_Sender(this);
		sendWindow = new LinkedList<TCP_PACKET>();
	}
	
	@Override
	//可靠发送（应用层调用）：封装应用层数据，产生TCP数据报
	public void rdt_send(int dataIndex, int[] appData) {
		
		//窗口满时等待（使用拥塞窗口控制）
		while(nextSeqNum >= windowBase + cwnd) {
			//窗口已满，等待ACK释放窗口空间
			try {
				Thread.sleep(1);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		//生成TCP数据报
		//序号按“包”递增（1,2,3...），否则会与接收方(lastCorrectSeq+1)的期待语义冲突，导致重复ACK风暴
		int seq = nextSeqNum;
		tcpH.setTh_seq(seq);
		tcpS.setData(appData);
		TCP_PACKET tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);
		
		//计算校验和
		tcpH.setTh_sum((short)0);
		tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);
		
		//发送数据包
		udt_send(tcpPack);
		System.out.println("发送包: " + seq + ", cwnd=" + cwnd + ", ssthresh=" + ssthresh);
		
		//将数据包加入发送窗口
		try {
			sendWindow.add(tcpPack.clone());
		} catch(CloneNotSupportedException e) {
			e.printStackTrace();
		}
		
		//如果这是窗口中的第一个包，启动计时器
		if(windowBase == nextSeqNum) {
			startTimer();
		}
		
		nextSeqNum++;
	}
	
	@Override
	//不可靠发送：将打包好的TCP数据报通过不可靠传输信道发送
	public void udt_send(TCP_PACKET stcpPack) {
		//设置错误控制标志
		//eflag控制信道错误类型：0无错误 1只出错 2只丢包 3只延迟 4出错/丢包 5出错/延迟 6丢包/延迟 7全部
		tcpH.setTh_eflag((byte)7);
		
		//发送数据报
		client.send(stcpPack);
	}
	
	@Override
	//等待ACK报文
	public void waitACK() {
		//处理接收到的ACK
		//此方法会在recv方法中被调用
	}

	@Override
	//接收到ACK报文：检查校验和，处理确认
	public void recv(TCP_PACKET recvPack) {
		//检查ACK报文的校验和
		if(CheckSum.computeChkSum(recvPack) == 0) {
			int ackNum = recvPack.getTcpH().getTh_ack();
			System.out.println("接收到ACK: " + ackNum);

			//如果发送窗口已空，说明当前没有“未确认”的在途数据包。
			//此时收到的ACK多为信道延迟导致的“迟到ACK/重复ACK”（常见于eflag包含延迟）。
			//为了让log更清爽、也避免发送结束后误触发快重传，直接忽略。
			if(sendWindow.isEmpty()) {
				System.out.println("窗口为空，忽略该ACK（可能为延迟/重复ACK）");
				System.out.println();
				return;
			}

			//ACK必须是“累计确认”：ACK=x 表示 <=x 的包都已按序收到（与TCP_Receiver语义一致）
			
			//检查是否是重复ACK
			if(ackNum == lastAck) {
				//重复ACK
				dupAckCount++;
				System.out.println("收到重复ACK（第" + dupAckCount + "次）");
				
				//收到3个重复ACK，执行快重传 + 进入快恢复
				if(dupAckCount == 3) {
					System.out.println("=== 快重传 ===");
					
					//找到需要重传的包（窗口中第一个未确认的包）
					if(!sendWindow.isEmpty()) {
						TCP_PACKET retransPacket = sendWindow.peek();
						System.out.println("快重传包: " + retransPacket.getTcpH().getTh_seq());
						udt_send(retransPacket);
						
						//快恢复：调整拥塞窗口（Reno）
						System.out.println("=== 快恢复 ===");
						System.out.println("旧 cwnd=" + cwnd + ", ssthresh=" + ssthresh);
						
						//设置ssthresh为cwnd的一半
						ssthresh = Math.max(cwnd / 2, 2);
						//cwnd设置为ssthresh + 3（因为已收到3个重复ACK）
						cwnd = ssthresh + 3;
						inFastRecovery = true;
						//记录进入快恢复时已发送的最大序号，用于判断何时退出快恢复
						recover = nextSeqNum - 1;
						
						System.out.println("新 cwnd=" + cwnd + ", ssthresh=" + ssthresh);
						
						//重置计数
						ackedInRound = 0;
						lastCwnd = cwnd;

						//重启计时器以跟踪窗口左沿
						startTimer();
					}
				} else if(dupAckCount > 3) {
					//快恢复阶段继续收到重复ACK：cwnd临时增加（窗口“膨胀”）
					if(inFastRecovery) {
						cwnd++;
						System.out.println("拥塞窗口膨胀: cwnd=" + cwnd);
					}
				}
				
				//重复ACK不需要处理窗口移动
				return;
			}
			
			//新的ACK（不是重复ACK）
			if(ackNum > lastAck) {
				//重置重复ACK计数
				dupAckCount = 0;

				//如果处于快恢复，收到“新ACK”表示丢失段已被修复：退出快恢复
				//在经典Reno中：当ACK推进到 >= recover 时退出；这里用该条件避免重复ACK导致cwnd无限膨胀
				if(inFastRecovery) {
					if(ackNum >= recover) {
						inFastRecovery = false;
						cwnd = ssthresh; //退出快恢复后cwnd回到ssthresh（进入拥塞避免）
						ackedInRound = 0;
						lastCwnd = cwnd;
						System.out.println("退出快恢复: cwnd=" + cwnd + ", ssthresh=" + ssthresh + ", recover=" + recover);
					} else {
						//“部分ACK”：简单实现下直接按正常累计确认处理（不做NewReno的额外重传）
						System.out.println("快恢复中收到部分ACK: " + ackNum + " < recover=" + recover);
					}
				}

				lastAck = ackNum;
				
				//记录本轮开始时的cwnd
				if(lastCwnd == 0) {
					lastCwnd = cwnd;
				}
				
				//处理累积确认
				//移除所有序号小于等于ackNum的包
				int ackedPackets = 0;
				while(!sendWindow.isEmpty() && sendWindow.peek().getTcpH().getTh_seq() <= ackNum) {
					TCP_PACKET removedPack = sendWindow.poll();
					windowBase = removedPack.getTcpH().getTh_seq() + 1; //窗口左沿右移到下一个未确认包
					ackedPackets++;
					ackedInRound++;
					System.out.println("确认并移除包: " + removedPack.getTcpH().getTh_seq());
				}
				
				//拥塞控制：根据当前状态调整cwnd
				if(ackedPackets > 0) {
					//快恢复退出后（或非快恢复）才做常规慢开始/拥塞避免增长
					if(!inFastRecovery && cwnd < ssthresh) {
						//慢开始阶段：每收到一个ACK，cwnd增加1
						//由于累积确认，一次可能确认多个包
						cwnd += ackedPackets;
						System.out.println("慢开始: cwnd=" + cwnd + " (增加" + ackedPackets + ")");
						
						//检查是否达到门限
						if(cwnd >= ssthresh) {
							System.out.println("到达ssthresh，进入拥塞避免阶段");
							lastCwnd = cwnd;
							ackedInRound = 0;
						}
					} else if(!inFastRecovery) {
						//拥塞避免阶段：每个RTT（一轮），cwnd增加1
						//当确认数达到当前cwnd时，表示一轮结束
						if(ackedInRound >= lastCwnd) {
							cwnd++;
							System.out.println("拥塞避免: cwnd=" + cwnd + " (一轮完成)");
							ackedInRound = 0;
							lastCwnd = cwnd;
						} else {
							System.out.println("拥塞避免: 当前轮次已确认 " + ackedInRound + "/" + lastCwnd);
						}
					}
				}
			}

			//计时器：始终跟踪“窗口左沿”未确认包；窗口为空则取消
			if(!sendWindow.isEmpty()) {
				startTimer();
			} else {
				if(timer != null) {
					timer.cancel();
					timer = null;
				}
				lastCwnd = 0;
				ackedInRound = 0;
			}
			
			System.out.println();
		} else {
			//ACK校验和错误，丢弃
			System.out.println("ACK校验和错误，丢弃");
			System.out.println();
		}
	}
	
	private void startTimer() {
		if(timer != null) {
			timer.cancel();
		}
		timer = new UDT_Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				onTimeout();
			}
		}, 3000);
	}
	
	private void onTimeout() {
		//超时！执行拥塞控制（回到慢开始）
		System.out.println("=== 超时重传 ===");
		System.out.println("旧 cwnd=" + cwnd + ", ssthresh=" + ssthresh);
		
		//更新慢开始门限为当前cwnd的一半（但至少为2）
		ssthresh = Math.max(cwnd / 2, 2);
		//cwnd重置为1（慢开始）
		cwnd = 1;
		//重置所有计数
		ackedInRound = 0;
        lastCwnd = 0;
        dupAckCount = 0;
        lastAck = 0;
		inFastRecovery = false;
		recover = 0;
		
		System.out.println("新 cwnd=" + cwnd + ", ssthresh=" + ssthresh);
		
		//重传窗口内所有未确认的包
		System.out.println("重传窗口内所有包（共" + sendWindow.size() + "个）");
		for(TCP_PACKET packet : sendWindow) {
			udt_send(packet);
		}
		
		//如果仍有未确认包，重启计时器
		if(!sendWindow.isEmpty()) {
			startTimer();
		}
	}
	
}
