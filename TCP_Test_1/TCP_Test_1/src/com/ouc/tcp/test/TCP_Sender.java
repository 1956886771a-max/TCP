package com.ouc.tcp.test;

import java.util.LinkedList;
import java.util.TimerTask;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;

public class TCP_Sender_Reno extends TCP_Sender_ADT {
	
	private LinkedList<TCP_PACKET> sendWindow = new LinkedList<TCP_PACKET>();
	private int windowBase = 1;
	private int nextSeqNum = 1;
	private UDT_Timer timer;
	
	//拥塞控制
	private int cwnd = 1;
	private int ssthresh = 16;
	private int ackedInRound = 0;
	private int lastCwnd = 0;
	private int dupAckCount = 0;
	private int lastAck = 0;
	
	//构造函数
	public TCP_Sender_Reno() {
		super();
		super.initTCP_Sender(this);
	}
	
	@Override
	//可靠发送：滑动窗口+Reno(慢开始/拥塞避免/快重传/快恢复)
	public void rdt_send(int dataIndex, int[] appData) {
		
		//窗口按cwnd控制
		while(nextSeqNum >= windowBase + cwnd) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		//生成TCP数据报
		int seq = dataIndex * appData.length + 1;
		tcpH.setTh_seq(seq);
		tcpS.setData(appData);
		TCP_PACKET tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);
		tcpH.setTh_sum(CheckSum_Reno.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);
		
		//发送
		udt_send(tcpPack);
		
		//入窗口
		try {
			sendWindow.add(tcpPack.clone());
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		
		//窗口首包启动计时器
		if(windowBase == seq) {
			startTimer();
		}
		
		nextSeqNum++;
	}
	
	private void startTimer() {
		cancelTimer();
		timer = new UDT_Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				onTimeout();
			}
		}, 3000);
	}
	
	private void cancelTimer() {
		if(timer != null) {
			timer.cancel();
			timer = null;
		}
	}
	
	private void onTimeout() {
		System.out.println("Reno timeout: cwnd=" + cwnd + ", ssthresh=" + ssthresh);
		ssthresh = Math.max(cwnd / 2, 2);
		cwnd = 1;
		dupAckCount = 0;
		lastAck = 0;
		ackedInRound = 0;
		lastCwnd = 0;
		//重传窗口内所有未确认包
		for(TCP_PACKET packet : sendWindow) {
			udt_send(packet);
		}
		startTimer();
	}
	
	@Override
	//不可靠发送
	public void udt_send(TCP_PACKET stcpPack) {
		tcpH.setTh_eflag((byte)7);
		client.send(stcpPack);
	}
	
	@Override
	//等待ACK：处理重复ACK、快重传/快恢复、拥塞避免
	public void waitACK() {
		if(!ackQueue.isEmpty()) {
			int ackNum = ackQueue.poll();
			
			//重复ACK检测
			if(ackNum == lastAck) {
				dupAckCount++;
				//3个重复ACK -> 快重传+快恢复
				if(dupAckCount == 3) {
					onFastRetransmit();
				} else if(dupAckCount > 3) {
					//快恢复阶段，收到更多重复ACK，cwnd临时膨胀
					cwnd++;
				}
				return;
			} else {
				//新ACK
				dupAckCount = 0;
				lastAck = ackNum;
			}
			
			//记录当前轮cwnd
			if(lastCwnd == 0) {
				lastCwnd = cwnd;
			}
			
			//累计确认移除窗口
			int removed = 0;
			while(!sendWindow.isEmpty() && sendWindow.peek().getTcpH().getTh_seq() <= ackNum) {
				sendWindow.poll();
				windowBase = ackNum + 1;
				removed++;
				ackedInRound++;
			}
			
			//拥塞控制：慢开始/拥塞避免
			if(removed > 0) {
				if(cwnd < ssthresh) {
					cwnd += removed; //慢开始
				} else {
					if(ackedInRound >= lastCwnd) {
						cwnd++; //拥塞避免按轮次+1
						ackedInRound = 0;
						lastCwnd = cwnd;
					}
				}
			}
			
			//计时器处理
			if(!sendWindow.isEmpty()) {
				startTimer();
			} else {
				cancelTimer();
				lastCwnd = 0;
				ackedInRound = 0;
			}
		}
	}
	
	private void onFastRetransmit() {
		System.out.println("Reno fast retransmit");
		//快重传：重传窗口首未确认包
		if(!sendWindow.isEmpty()) {
			udt_send(sendWindow.peek());
		}
		//快恢复
		ssthresh = Math.max(cwnd / 2, 2);
		cwnd = ssthresh + 3; //收到3个重复ACK
		ackedInRound = 0;
		lastCwnd = cwnd;
		//计时器保持运行（重启以跟踪窗口左沿）
		startTimer();
	}

	@Override
	//接收ACK报文
	public void recv(TCP_PACKET recvPack) {
		if(CheckSum_Reno.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			ackQueue.add(recvPack.getTcpH().getTh_ack());
			waitACK();
		}
	}
	
}

