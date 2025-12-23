/***************************2.2: RDT 2.2实现*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {
	
	private TCP_PACKET ackPack;
	//记录上一个正确接收的包序号
	private int lastCorrectSeq = 0;
		
	//构造函数
	public TCP_Receiver() {
		super();
		super.initTCP_Receiver(this);
	}

	@Override
	//接收到数据报：检查校验和，设置回复的ACK报文段
	public void rdt_recv(TCP_PACKET recvPack) {
		//检查校验码
		if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			//校验和正确
			int recvSeq = recvPack.getTcpH().getTh_seq();
			
			//检查序号是否是期待的（按序到达）
			if(recvSeq == lastCorrectSeq + 1 || lastCorrectSeq == 0) {
				//是期待的包，接收并确认
				lastCorrectSeq = recvSeq;
				
				//生成ACK报文段（确认号为当前包序号）
				tcpH.setTh_ack(recvSeq);
				ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
				tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
				reply(ackPack);
				
				//将接收到的正确有序的数据插入data队列，准备交付
				dataQueue.add(recvPack.getTcpS().getData());
				
				System.out.println("接收正确，序号: " + recvSeq);
			} else {
				//不是期待的包（可能是重复包），发送对上一个正确包的ACK
				tcpH.setTh_ack(lastCorrectSeq);
				ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
				tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
				reply(ackPack);
				
				System.out.println("收到重复包，序号: " + recvSeq + "，期待: " + (lastCorrectSeq + 1));
			}
		} else {
			//校验和错误，发送对上一个正确包的ACK
			System.out.println("校验和错误！");
			System.out.println("计算值: " + CheckSum.computeChkSum(recvPack));
			System.out.println("接收值: " + recvPack.getTcpH().getTh_sum());
			
			//发送对上一个正确接收包的确认
			tcpH.setTh_ack(lastCorrectSeq);
			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
			reply(ackPack);
		}
		
		System.out.println();
		
		//交付数据（每20组数据交付一次）
		if(dataQueue.size() == 20) 
			deliver_data();	
	}

	@Override
	//交付数据（将数据写入文件）；不需要修改
	public void deliver_data() {
		//检查dataQueue，将数据写入文件
		File fw = new File("recvData.txt");
		BufferedWriter writer;
		
		try {
			writer = new BufferedWriter(new FileWriter(fw, true));
			
			//循环检查data队列中是否有新交付数据
			while(!dataQueue.isEmpty()) {
				int[] data = dataQueue.poll();
				
				//将数据写入文件
				for(int i = 0; i < data.length; i++) {
					writer.write(data[i] + "\n");
				}
				
				writer.flush();		//清空输出缓存
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	//回复ACK报文段
	public void reply(TCP_PACKET replyPack) {
		//设置错误控制标志
		tcpH.setTh_eflag((byte)0);	//eFlag=0，信道无错误
				
		//发送数据报
		client.send(replyPack);
	}
	
}
