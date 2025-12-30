package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;

public class TCP_Receiver_SR extends TCP_Receiver_ADT {
	
	private TCP_PACKET ackPack;
	private static final int WINDOW_SIZE = 10;
	private int rcvBase = 1;
	private Map<Integer, int[]> recvBuffer = new HashMap<Integer, int[]>();
	
	//构造函数
	public TCP_Receiver_SR() {
		super();
		super.initTCP_Receiver(this);
	}

	@Override
	//接收到数据报：SR 接收窗口，缓存失序，逐包ACK
	public void rdt_recv(TCP_PACKET recvPack) {
		if(CheckSum_SR.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			int seq = recvPack.getTcpH().getTh_seq();
			
			//判断是否在接收窗口内
			if(seq >= rcvBase && seq < rcvBase + WINDOW_SIZE) {
				//缓存（或按序直接进入交付队列）
				if(!recvBuffer.containsKey(seq)) {
					recvBuffer.put(seq, recvPack.getTcpS().getData());
				}
				
				//对该包发送ACK（逐包确认）
				sendAck(seq, recvPack.getSourceAddr());
				
				//如果是窗口基序号，尝试前移窗口并交付连续数据
				while(recvBuffer.containsKey(rcvBase)) {
					dataQueue.add(recvBuffer.get(rcvBase));
					recvBuffer.remove(rcvBase);
					rcvBase++;
				}
			} else if(seq < rcvBase) {
				//已接收过，重发该序号ACK
				sendAck(seq, recvPack.getSourceAddr());
			} else {
				//超出窗口上沿，丢弃，不ACK
			}
			
		} else {
			//校验和错误，丢弃，不ACK
		}
		
		//交付数据（每20组数据交付一次）
		if(dataQueue.size() >= 20) 
			deliver_data();	
	}
	
	private void sendAck(int ackSeq, java.net.InetAddress addr) {
		tcpH.setTh_ack(ackSeq);
		ackPack = new TCP_PACKET(tcpH, tcpS, addr);
		tcpH.setTh_sum(CheckSum_SR.computeChkSum(ackPack));
		ackPack.setTcpH(tcpH);
		reply(ackPack);
	}

	@Override
	//交付数据（写入文件）
	public void deliver_data() {
		File fw = new File("recvData.txt");
		BufferedWriter writer;
		
		try {
			writer = new BufferedWriter(new FileWriter(fw, true));
			
			while(!dataQueue.isEmpty()) {
				int[] data = dataQueue.poll();
				for(int i = 0; i < data.length; i++) {
					writer.write(data[i] + "\n");
				}
				writer.flush();
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	//回复ACK报文段
	public void reply(TCP_PACKET replyPack) {
		//信道错误类型允许综合测试
		tcpH.setTh_eflag((byte)7);
		client.send(replyPack);
	}
	
}

