package com.ouc.tcp.test;

import java.util.LinkedList;
import java.util.TimerTask;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;

public class TCP_Sender_Tahoe extends TCP_Sender_ADT {
	
	private LinkedList<TCP_PACKET> sendWindow = new LinkedList<TCP_PACKET>();
	private int windowBase = 1;
	private int nextSeqNum = 1;
	private UDT_Timer timer;
	
	//拥塞控制
	private int cwnd = 1;
	private int ssthresh = 16;
	private int ackedInRound = 0;
	private int lastCwnd = 0;
	
	//构造函数
	public TCP_Sender_Tahoe() {
		super();
		super.initTCP_Sender(this);
	}
	
	@Override
	//可靠发送：滑动窗口+累计ACK+单计时器+Tahoe拥塞控制
	public void rdt_send(int dataIndex, int[] appData) {
		
		//窗口满则等待（按cwnd控制）
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
		tcpH.setTh_sum(CheckSum_Tahoe.computeChkSum(tcpPack));
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
				//超时：执行Tahoe回退并重传窗口内未确认包
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
		System.out.println("Tahoe timeout: cwnd=" + cwnd + ", ssthresh=" + ssthresh);
		ssthresh = Math.max(cwnd / 2, 2);
		cwnd = 1;
		ackedInRound = 0;
		lastCwnd = 0;
		//重传窗口内所有未确认包
		for(TCP_PACKET packet : sendWindow) {
			udt_send(packet);
		}
		//重启计时器
		startTimer();
	}
	
	@Override
	//不可靠发送：设置错误标志
	public void udt_send(TCP_PACKET stcpPack) {
		tcpH.setTh_eflag((byte)7);
		client.send(stcpPack);
	}
	
	@Override
	//等待ACK：累计确认，前移窗口并调整cwnd
	public void waitACK() {
		if(!ackQueue.isEmpty()) {
			int ackNum = ackQueue.poll();
			
			//记录当前轮开始的cwnd
			if(lastCwnd == 0) {
				lastCwnd = cwnd;
			}
			
			//累计确认：移除已确认的包
			int removed = 0;
			while(!sendWindow.isEmpty() && sendWindow.peek().getTcpH().getTh_seq() <= ackNum) {
				sendWindow.poll();
				windowBase = ackNum + 1;
				removed++;
				ackedInRound++;
			}
			
			//拥塞控制调整
			if(removed > 0) {
				if(cwnd < ssthresh) {
					//慢开始：每个ACK cwnd+1
					cwnd += removed;
				} else {
					//拥塞避免：每轮 cwnd+1
					if(ackedInRound >= lastCwnd) {
						cwnd++;
						ackedInRound = 0;
						lastCwnd = cwnd;
					}
				}
			}
			
			//根据窗口是否为空处理计时器
			if(!sendWindow.isEmpty()) {
				startTimer();
			} else {
				cancelTimer();
				lastCwnd = 0;
				ackedInRound = 0;
			}
		}
	}

	@Override
	//接收ACK报文
	public void recv(TCP_PACKET recvPack) {
		if(CheckSum_Tahoe.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			ackQueue.add(recvPack.getTcpH().getTh_ack());
			waitACK();
		}
	}
	
}
