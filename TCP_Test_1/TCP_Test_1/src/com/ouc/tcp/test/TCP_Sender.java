package com.ouc.tcp.test;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.TreeMap;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;

public class TCP_Sender_SR extends TCP_Sender_ADT {
	
	private static final int WINDOW_SIZE = 10;
	private TreeMap<Integer, TCP_PACKET> sendWindow = new TreeMap<Integer, TCP_PACKET>();
	private Map<Integer, UDT_Timer> timerMap = new HashMap<Integer, UDT_Timer>();
	private int windowBase = 1;
	private int nextSeqNum = 1;
	
	//构造函数
	public TCP_Sender_SR() {
		super();
		super.initTCP_Sender(this);
	}
	
	@Override
	//可靠发送：滑动窗口 + 每个包独立计时器
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
		tcpH.setTh_sum(CheckSum_SR.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);
		
		//发送
		udt_send(tcpPack);
		
		//入窗口并启动独立计时器
		try {
			sendWindow.put(seq, tcpPack.clone());
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		startTimer(seq);
		
		nextSeqNum++;
	}
	
	private void startTimer(final int seq) {
		cancelTimer(seq);
		UDT_Timer t = new UDT_Timer();
		timerMap.put(seq, t);
		t.schedule(new TimerTask() {
			@Override
			public void run() {
				//重传该序号的包
				TCP_PACKET p = sendWindow.get(seq);
				if(p != null) {
					udt_send(p);
					startTimer(seq);
				}
			}
		}, 3000);
	}
	
	private void cancelTimer(int seq) {
		UDT_Timer t = timerMap.remove(seq);
		if(t != null) {
			t.cancel();
		}
	}
	
	private void updateWindowBase() {
		if(sendWindow.isEmpty()) {
			windowBase = nextSeqNum;
		} else {
			windowBase = sendWindow.firstKey();
		}
	}
	
	@Override
	//不可靠发送：设置错误标志
	public void udt_send(TCP_PACKET stcpPack) {
		//综合错误测试
		tcpH.setTh_eflag((byte)7);
		client.send(stcpPack);
	}
	
	@Override
	//等待ACK：在recv里调用
	public void waitACK() {
		if(!ackQueue.isEmpty()) {
			int ackNum = ackQueue.poll();
			//收到该序号的ACK，则移除并取消计时器
			if(sendWindow.containsKey(ackNum)) {
				sendWindow.remove(ackNum);
				cancelTimer(ackNum);
				updateWindowBase();
			}
		}
	}

	@Override
	//接收ACK报文
	public void recv(TCP_PACKET recvPack) {
		if(CheckSum_SR.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			ackQueue.add(recvPack.getTcpH().getTh_ack());
			waitACK();
		}
	}
	
}
