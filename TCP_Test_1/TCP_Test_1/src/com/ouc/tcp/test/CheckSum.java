package com.ouc.tcp.test;

import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

public class CheckSum {
	
	//计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段
	public static short computeChkSum(TCP_PACKET tcpPack) {
		int checkSum = 0;
		
		//获取TCP首部
		TCP_HEADER header = tcpPack.getTcpH();
		
		//将序号(seq)加入校验和计算
		//seq是32位整数，分成高16位和低16位分别相加
		checkSum += (header.getTh_seq() >> 16) & 0xFFFF;
		checkSum += header.getTh_seq() & 0xFFFF;
		
		//将确认号(ack)加入校验和计算
		//ack也是32位整数，同样分成高16位和低16位
		checkSum += (header.getTh_ack() >> 16) & 0xFFFF;
		checkSum += header.getTh_ack() & 0xFFFF;
		
		//将校验和字段(sum)本身加入计算
		//sum是16位短整数
		checkSum += header.getTh_sum() & 0xFFFF;
		
		//将数据字段加入校验和计算
		int[] data = tcpPack.getTcpS().getData();
		if(data != null) {
			for(int i = 0; i < data.length; i++) {
				//每个int数据分成高16位和低16位
				checkSum += (data[i] >> 16) & 0xFFFF;
				checkSum += data[i] & 0xFFFF;
			}
		}
		
		//处理进位：将高16位的进位加到低16位上
		//这是标准的Internet校验和算法
		while((checkSum >> 16) != 0) {
			checkSum = (checkSum >> 16) + (checkSum & 0xFFFF);
		}
		
		//返回校验和（取反后的低16位）
		return (short)(~checkSum);
	}
	
}
