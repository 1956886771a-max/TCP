package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;

public class TCP_Receiver_RDT22 extends TCP_Receiver_ADT {
	
	private TCP_PACKET ackPack;//回复的ACK/NACK报文段
	
	//构造函数
	public TCP_Receiver_RDT22() {
		super();
		super.initTCP_Receiver(this);
	}

	@Override
	//接收到数据报：检查校验和，设置回复的ACK或NACK报文段
	public void rdt_recv(TCP_PACKET recvPack) {
		//检查校验码
		if(CheckSum_RDT22.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			//校验正确，发送ACK
			tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());
			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
			tcpH.setTh_sum(CheckSum_RDT22.computeChkSum(ackPack));
			reply(ackPack);
			
			//缓存正确数据
			dataQueue.add(recvPack.getTcpS().getData());
		} else {
			//校验错误，发送NACK（ack=-1）
			tcpH.setTh_ack(-1);
			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
			tcpH.setTh_sum(CheckSum_RDT22.computeChkSum(ackPack));
			reply(ackPack);
		}
		
		//交付数据（每20组数据交付一次）
		if(dataQueue.size() == 20) 
			deliver_data();	
	}

	@Override
	//交付数据（将数据写入文件）
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
	//回复ACK/NACK报文段
	public void reply(TCP_PACKET replyPack) {
		//设置错误控制标志
		tcpH.setTh_eflag((byte)0);//信道无错误
		//发送数据报
		client.send(replyPack);
	}
	
}

