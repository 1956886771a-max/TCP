package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;

public class TCP_Receiver_Tahoe extends TCP_Receiver_ADT {
	
	private TCP_PACKET ackPack;
	private int expectedSeq = 1;
	private TreeMap<Integer, int[]> recvWindow = new TreeMap<Integer, int[]>();
	private static final int WINDOW_SIZE = 10;
	
	//构造函数
	public TCP_Receiver_Tahoe() {
		super();
		super.initTCP_Receiver(this);
	}

	@Override
	//接收到数据报：累计确认，缓存失序
	public void rdt_recv(TCP_PACKET recvPack) {
		if(CheckSum_Tahoe.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			int seq = recvPack.getTcpH().getTh_seq();
			
			if(seq >= expectedSeq && seq < expectedSeq + WINDOW_SIZE) {
				if(!recvWindow.containsKey(seq)) {
					recvWindow.put(seq, recvPack.getTcpS().getData());
				}
				while(recvWindow.containsKey(expectedSeq)) {
					dataQueue.add(recvWindow.get(expectedSeq));
					recvWindow.remove(expectedSeq);
					expectedSeq++;
				}
			}
			
			//累计ACK：最后连续收到的序号
			sendAck(expectedSeq - 1, recvPack.getSourceAddr());
			
		} else {
			//校验错，重发最近累计ACK
			sendAck(expectedSeq - 1, recvPack.getSourceAddr());
		}
		
		//交付数据（每20组数据交付一次）
		if(dataQueue.size() >= 20) 
			deliver_data();	
	}
	
	private void sendAck(int ackSeq, java.net.InetAddress addr) {
		tcpH.setTh_ack(ackSeq);
		ackPack = new TCP_PACKET(tcpH, tcpS, addr);
		tcpH.setTh_sum(CheckSum_Tahoe.computeChkSum(ackPack));
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
		tcpH.setTh_eflag((byte)7);
		client.send(replyPack);
	}
	
}

