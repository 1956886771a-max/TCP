package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.TimerTask;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {
	
	private TCP_PACKET ackPack;
	//记录上一个正确接收的包序号
	private int lastCorrectSeq = 0;
	//累计确认计时器：正常按序到达时，延迟500ms再发送累计ACK
	private UDT_Timer ackTimer;
		
	//构造函数
	public TCP_Receiver() {
		super();
		super.initTCP_Receiver(this);
	}

	@Override
	//接收到数据报：检查校验和，设置回复的ACK报文段
	public void rdt_recv(TCP_PACKET recvPack) {
		//检查校验码
		if(CheckSum.computeChkSum(recvPack) == 0) {
			//校验和正确
			int recvSeq = recvPack.getTcpH().getTh_seq();
			
			//检查序号是否是期待的（按序到达）
			if(recvSeq == lastCorrectSeq + 1 || lastCorrectSeq == 0) {
				//是期待的包，接收并确认
				lastCorrectSeq = recvSeq;
				
				//正常传输时，使用累积确认，延迟500ms
				//500ms内如果又收到新包，就取消旧计时器，重启新计时器
				scheduleDelayedCumulativeAck(recvPack.getSourceAddr());
				
				//将接收到的正确有序的数据插入data队列，准备交付
				dataQueue.add(recvPack.getTcpS().getData());
				
				System.out.println("接收正确，序号: " + recvSeq);
			} else {
				//不是期待的包（可能是重复包），发送对上一个正确包的ACK
				//收到失序/重复包时，立即发送重复ACK，不延迟
				//并取消延迟ACK计时器，避免后续延迟ACK产生多余重复ACK
				cancelAckTimer();
				sendImmediateCumulativeAck(recvPack.getSourceAddr());
				
				System.out.println("收到重复包，序号: " + recvSeq + "，期待: " + (lastCorrectSeq + 1));
			}
		} else {
			//校验和错误，发送对上一个正确包的ACK
			System.out.println("校验和错误！");
			System.out.println("计算值: " + CheckSum.computeChkSum(recvPack));
			System.out.println("接收值: " + recvPack.getTcpH().getTh_sum());
			
			//发送对上一个正确接收包的确认
			//校验和错误时也不延迟，立即回累计ACK，并取消延迟计时器
			cancelAckTimer();
			sendImmediateCumulativeAck(recvPack.getSourceAddr());
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

	//正常传输时，使用累积确认，延迟500ms
	//500ms内如果又收到新包，就取消旧计时器，重启新计时器
	private void scheduleDelayedCumulativeAck(final InetAddress sourceAddr) {
		cancelAckTimer();
		ackTimer = new UDT_Timer();
		ackTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				//500ms后发送累积ACK（确认号=lastCorrectSeq）
				sendImmediateCumulativeAck(sourceAddr);
			}
		}, 500);
	}

	//收到失序包时，立即发送重复ACK，不延迟（确认号=lastCorrectSeq）
	private void sendImmediateCumulativeAck(InetAddress sourceAddr) {
		tcpH.setTh_ack(lastCorrectSeq);
		ackPack = new TCP_PACKET(tcpH, tcpS, sourceAddr);
		tcpH.setTh_sum((short)0);
		tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
		reply(ackPack);
	}

	private void cancelAckTimer() {
		if(ackTimer != null) {
			ackTimer.cancel();
			ackTimer = null;
		}
	}
	
}
