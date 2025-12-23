package com.ouc.tcp.client;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.test.TCP_Sender;
import com.ouc.tcp.tool.TCP_TOOL;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class ListenACK
extends Thread {
    private Client listenedClinet;
    private TCP_Sender tcpSender;
    private long sysTimeMillis;
    private SimpleDateFormat receiveTime;

    public ListenACK(Client client, TCP_Sender sender) {
        this.listenedClinet = client;
        this.tcpSender = sender;
        this.receiveTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS z");
    }

    @Override
    public void run() {
        while (true) {
            try {
                this.listenedClinet.socket.receive(this.listenedClinet.receivePacket);
                this.sysTimeMillis = System.currentTimeMillis();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            TCP_PACKET recvPack = TCP_TOOL.getTCP_Packet(this.listenedClinet.receivePacket);
            int[] recvData = recvPack.getTcpS().getData();
            System.out.println("-> " + this.receiveTime.format(this.sysTimeMillis));
            System.out.println("** TCP_Sender");
            System.out.println("   Receive packet from: [" + recvPack.getSourceAddr().getHostAddress() + ":" + recvPack.getTcpH().getTh_sport() + "]");
            System.out.print("   Packet data:");
            int i = 0;
            while (i < recvData.length) {
                System.out.print(" " + recvData[i]);
                ++i;
            }
            System.out.println();
            if (TCP_TOOL.judgePacketType(recvPack) != 3) continue;
            System.out.println("   PACKET_TYPE: ACK_" + recvPack.getTcpH().getTh_ack());
            this.tcpSender.recv(recvPack);
        }
    }
}

