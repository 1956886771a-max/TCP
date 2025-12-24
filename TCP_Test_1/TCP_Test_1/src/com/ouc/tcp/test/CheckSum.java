package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

public class CheckSum_GBN {
	
	//计算TCP报文段校验和：校验TCP首部的seq、ack、sum以及数据字段
	public static short computeChkSum(TCP_PACKET tcpPack) {
		int checkSum = 0;
		
		TCP_HEADER header = tcpPack.getTcpH();
		
		//序号seq
		checkSum += (header.getTh_seq() >> 16) & 0xFFFF;
		checkSum += header.getTh_seq() & 0xFFFF;
		
		//确认号ack
		checkSum += (header.getTh_ack() >> 16) & 0xFFFF;
		checkSum += header.getTh_ack() & 0xFFFF;
		
		//校验和字段本身
		checkSum += header.getTh_sum() & 0xFFFF;
		
		//数据字段
		int[] data = tcpPack.getTcpS().getData();
		if(data != null) {
			for(int i = 0; i < data.length; i++) {
				checkSum += (data[i] >> 16) & 0xFFFF;
				checkSum += data[i] & 0xFFFF;
			}
		}
		
		//进位处理
		while((checkSum >> 16) != 0) {
			checkSum = (checkSum >> 16) + (checkSum & 0xFFFF);
		}
		
		//取反
		return (short)(~checkSum);
	}
}

