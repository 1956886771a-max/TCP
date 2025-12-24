package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.message.*;

public class TCP_Sender_RDT20 extends TCP_Sender_ADT {
	
	private TCP_PACKET tcpPack;//待发送的TCP数据报
	private volatile int flag = 0;
	
	//构造函数
	public TCP_Sender_RDT20() {
		super();
		super.initTCP_Sender(this);
	}
	
	@Override
	//可靠发送：封装应用层数据，产生TCP数据报
	public void rdt_send(int dataIndex, int[] appData) {
		
		//生成TCP数据报（设置序号和数据/校验和)
		tcpH.setTh_seq(dataIndex * appData.length + 1);//包序号设置为字节流号
		tcpS.setData(appData);
		tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);
		tcpH.setTh_sum(CheckSum_RDT20.computeChkSum(tcpPack));
		tcpPack.setTcpH(tcpH);
		
		//发送TCP数据报
		udt_send(tcpPack);
		flag = 0;
		
		//等待ACK报文（停止等待）
		while(flag == 0);
	}
	
	@Override
	//不可靠发送：仅需修改错误标志
	public void udt_send(TCP_PACKET stcpPack) {
		//设置错误控制标志
		tcpH.setTh_eflag((byte)0);
		//发送数据报
		client.send(stcpPack);
	}
	
	@Override
	//等待ACK
	public void waitACK() {
		if(!ackQueue.isEmpty()) {
			int currentAck = ackQueue.poll();
			if(currentAck == tcpPack.getTcpH().getTh_seq()) {
				flag = 1;
			} else {
				//重传当前包
				udt_send(tcpPack);
				flag = 0;
			}
		}
	}

	@Override
	//接收到ACK报文：检查校验和，将确认号插入ack队列
	public void recv(TCP_PACKET recvPack) {
		if(CheckSum_RDT20.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {
			ackQueue.add(recvPack.getTcpH().getTh_ack());
			waitACK();
		}
	}
	
}
