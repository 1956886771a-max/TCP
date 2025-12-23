package com.ouc.tcp.client;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.test.TCP_Receiver;
import com.ouc.tcp.tool.TCP_TOOL;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class ListenPacket
extends Thread {
    private Client listenedClinet;
    private TCP_Receiver tcpReceiver;
    private long sysTimeMillis;
    private SimpleDateFormat receiveTime;

    public ListenPacket(Client client, TCP_Receiver receiver) {
        this.listenedClinet = client;
        this.tcpReceiver = receiver;
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
            System.out.println("** TCP_Receiver");
            System.out.println("   Receive packet from: [" + recvPack.getSourceAddr().getHostAddress() + ":" + recvPack.getTcpH().getTh_sport() + "]");
            System.out.print("   Packet data:");
            int i = 0;
            while (i < recvData.length) {
                System.out.print(" " + recvData[i]);
                ++i;
            }
            System.out.println();
            if (TCP_TOOL.judgePacketType(recvPack) != 2) continue;
            System.out.println("   PACKET_TYPE: DATA_SEQ_" + recvPack.getTcpH().getTh_seq());
            this.tcpReceiver.rdt_recv(recvPack);
        }
    }
}

