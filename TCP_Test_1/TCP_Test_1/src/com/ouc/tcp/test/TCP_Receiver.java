package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;

public class TCP_Receiver_GBN extends TCP_Receiver_ADT {
	
	private TCP_PACKET ackPack;
	private int expectedSeq = 1;
	
	//构造函数
	public TCP_Receiver_GBN() {
		super();
		super.initTCP_Receiver(this);
	}

	@Override
	//接收到数据报：按GBN逻辑处理
	public void rdt_recv(TCP_PACKET recvPack) {
		//校验
		if(CheckSum_GBN.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			int recvSeq = recvPack.getTcpH().getTh_seq();
			
			if(recvSeq == expectedSeq) {
				//按序，接收并前移窗口
				dataQueue.add(recvPack.getTcpS().getData());
				expectedSeq++;
				
				//发送累计ACK（最后正确接收的序号）
				sendAck(expectedSeq - 1, recvPack.getSourceAddr());
			} else if(recvSeq > expectedSeq) {
				//失序，丢弃并重发上次ACK
				sendAck(expectedSeq - 1, recvPack.getSourceAddr());
			} else {
				//重复包，重发上次ACK
				sendAck(expectedSeq - 1, recvPack.getSourceAddr());
			}
			
		} else {
			//校验错误，重发上次ACK
			sendAck(expectedSeq - 1, recvPack.getSourceAddr());
		}
		
		//交付数据（每20组数据交付一次）
		if(dataQueue.size() == 20) 
			deliver_data();	
	}
	
	private void sendAck(int ackSeq, java.net.InetAddress addr) {
		tcpH.setTh_ack(ackSeq);
		ackPack = new TCP_PACKET(tcpH, tcpS, addr);
		tcpH.setTh_sum(CheckSum_GBN.computeChkSum(ackPack));
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
		//设置错误控制标志
		tcpH.setTh_eflag((byte)7);//允许信道综合错误测试
		//发送数据报
		client.send(replyPack);
	}
	
}

