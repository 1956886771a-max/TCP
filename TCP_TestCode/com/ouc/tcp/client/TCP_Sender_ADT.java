package com.ouc.tcp.client;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.ListenACK;
import com.ouc.tcp.config.Constant;
import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.message.TCP_SEGMENT;
import com.ouc.tcp.test.TCP_Sender;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class TCP_Sender_ADT {
    public final Client client;
    protected int localPort = Constant.LocalSenderPort;
    protected InetAddress destinAddr;
    protected int destinPort;
    protected TCP_HEADER tcpH;
    protected TCP_SEGMENT tcpS;
    public ArrayList<TCP_PACKET> sendBuffer;
    public Queue<Integer> ackQueue;

    public TCP_Sender_ADT() {
        this.client = new Client(this.localPort);
        System.out.print("Sender socket address: ");
        this.client.printLocalSocketAddress();
        this.destinAddr = Constant.LocalAddr;
        this.destinPort = Constant.LocalReceiverPort;
        this.sendBuffer = new ArrayList();
        this.ackQueue = new LinkedBlockingQueue<Integer>();
    }

    public void initTCP_Sender(TCP_Sender tcpSender) {
        new ListenACK(this.client, tcpSender).start();
        this.tcpH = new TCP_HEADER((short)this.localPort, (short)this.destinPort, 1, 1, (byte)6, "010000", (short)1000, (short)0, (short)1024, (byte)0, (byte)7);
        this.tcpS = new TCP_SEGMENT();
    }

    public abstract void rdt_send(int var1, int[] var2);

    public abstract void udt_send(TCP_PACKET var1);

    public abstract void recv(TCP_PACKET var1);

    public abstract void waitACK();
}

