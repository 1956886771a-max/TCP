package com.ouc.tcp.test;

import java.util.LinkedList;
import java.util.TimerTask;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;

public class TCP_Sender_GBN extends TCP_Sender_ADT {
	
	private static final int WINDOW_SIZE = 10;
	private LinkedList<TCP_PACKET> sendWindow = new LinkedList<TCP_PACKET>();
	private int windowBase = 1;
	private int nextSeqNum = 1;
	private UDT_Timer timer;
	
	//构造函数
	public TCP_Sender_GBN() {
		super();
		super.initTCP_Sender(this);
	}
	
	@Override
	//可靠发送：滑动窗口+单计时器（超时重传窗口内所有未确认包）
	public void rdt_send(int dataIndex, int[] appData) {
		
		//窗口满则等待
		while(nextSeqNum >= windowBase + WINDOW_SIZE) {
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
		tcpH.setTh_sum(CheckSum_GBN.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);
		
		//发送
		udt_send(tcpPack);
		
		//入窗口
		try {
			sendWindow.add(tcpPack.clone());
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		
		//如是窗口第一个包，启动计时器
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
				//超时：重传窗口内所有未确认包
				for(TCP_PACKET packet : sendWindow) {
					udt_send(packet);
				}
				//重启计时器
				startTimer();
			}
		}, 3000);
	}
	
	private void cancelTimer() {
		if(timer != null) {
			timer.cancel();
			timer = null;
		}
	}
	
	@Override
	//不可靠发送：设置错误标志
	public void udt_send(TCP_PACKET stcpPack) {
		//允许综合错误测试
		tcpH.setTh_eflag((byte)7);
		client.send(stcpPack);
	}
	
	@Override
	//等待ACK：在recv里调用
	public void waitACK() {
		if(!ackQueue.isEmpty()) {
			int ackNum = ackQueue.poll();
			//累计确认：ackNum 表示已正确收到的最后序号
			if(ackNum >= windowBase) {
				//移除窗口中已确认的包
				while(!sendWindow.isEmpty() && sendWindow.peek().getTcpH().getTh_seq() <= ackNum) {
					sendWindow.poll();
					windowBase = ackNum + 1;
				}
				
				//窗口非空，重启计时器；否则取消
				if(!sendWindow.isEmpty()) {
					startTimer();
				} else {
					cancelTimer();
				}
			}
		}
	}

	@Override
	//接收ACK报文
	public void recv(TCP_PACKET recvPack) {
		if(CheckSum_GBN.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			ackQueue.add(recvPack.getTcpH().getTh_ack());
			waitACK();
		}
	}
	
}

