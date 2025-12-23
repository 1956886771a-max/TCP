/***************************TCP滑动窗口实现
**************************** Feng Hong; 2015-12-09*/

package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;
import java.util.LinkedList;

public class TCP_Sender extends TCP_Sender_ADT {
	
	//滑动窗口大小
	private static final int WINDOW_SIZE = 10;
	//发送窗口：存储已发送但未确认的数据包
	private LinkedList<TCP_PACKET> sendWindow;
	//窗口左边界：最小未确认序号
	private int windowBase = 1;
	//下一个要发送的序号
	private int nextSeqNum = 1;
	//超时计时器（仅使用1个）
	private UDT_Timer timer;
	
	//构造函数
	public TCP_Sender() {
		super();
		super.initTCP_Sender(this);
		sendWindow = new LinkedList<TCP_PACKET>();
	}
	
	@Override
	//可靠发送（应用层调用）：封装应用层数据，产生TCP数据报
	public void rdt_send(int dataIndex, int[] appData) {
		
		//窗口满时等待（流量控制）
		while(nextSeqNum >= windowBase + WINDOW_SIZE) {
			//窗口已满，等待ACK释放窗口空间
			try {
				Thread.sleep(1);
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		//生成TCP数据报
		//包序号设置为字节流号
		int seq = dataIndex * appData.length + 1;
		tcpH.setTh_seq(seq);
		tcpS.setData(appData);
		TCP_PACKET tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);
		
		//计算校验和
		tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);
		
		//发送数据包
		udt_send(tcpPack);
		
		//将数据包加入发送窗口
		try {
			sendWindow.add(tcpPack.clone());
		} catch(CloneNotSupportedException e) {
			e.printStackTrace();
		}
		
		//如果这是窗口中的第一个包，启动计时器
		if(windowBase == nextSeqNum) {
			if(timer != null) {
				timer.cancel();
			}
			timer = new UDT_Timer();
			//超时重传任务：重传窗口中所有未确认的包
			timer.schedule(new UDT_RetransTask(client, null) {
				@Override
				public void run() {
					//超时，重传窗口内所有包
					System.out.println("超时！重传窗口内所有包");
					for(TCP_PACKET packet : sendWindow) {
						udt_send(packet);
					}
					//重启计时器
					if(timer != null) {
						timer.cancel();
					}
					timer = new UDT_Timer();
					timer.schedule(this, 3000);
				}
			}, 3000);
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
		if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			int ackNum = recvPack.getTcpH().getTh_ack();
			System.out.println("接收到ACK: " + ackNum);
			
			//处理累积确认
			//移除所有序号小于等于ackNum的包
			while(!sendWindow.isEmpty() && sendWindow.peek().getTcpH().getTh_seq() <= ackNum) {
				TCP_PACKET removedPack = sendWindow.poll();
				windowBase = removedPack.getTcpH().getTh_seq() + 1;
				System.out.println("确认并移除包: " + removedPack.getTcpH().getTh_seq());
			}
			
			//如果窗口中还有未确认的包，重启计时器
			if(!sendWindow.isEmpty()) {
				if(timer != null) {
					timer.cancel();
				}
				timer = new UDT_Timer();
				timer.schedule(new UDT_RetransTask(client, null) {
					@Override
					public void run() {
						//超时，重传窗口内所有包
						System.out.println("超时！重传窗口内所有包");
						for(TCP_PACKET packet : sendWindow) {
							udt_send(packet);
						}
						//重启计时器
						if(timer != null) {
							timer.cancel();
						}
						timer = new UDT_Timer();
						timer.schedule(this, 3000);
					}
				}, 3000);
			} else {
				//窗口为空，取消计时器
				if(timer != null) {
					timer.cancel();
				}
			}
			
			System.out.println();
		} else {
			//ACK校验和错误，丢弃
			System.out.println("ACK校验和错误，丢弃");
			System.out.println();
		}
	}
	
}
