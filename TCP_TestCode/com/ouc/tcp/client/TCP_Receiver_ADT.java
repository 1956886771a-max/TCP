package com.ouc.tcp.client;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.client.ListenPacket;
import com.ouc.tcp.config.Constant;
import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.message.TCP_SEGMENT;
import com.ouc.tcp.test.TCP_Receiver;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class TCP_Receiver_ADT {
    protected final Client client;
    protected int localPort = Constant.LocalReceiverPort;
    protected int destinPort;
    protected TCP_HEADER tcpH;
    protected TCP_SEGMENT tcpS;
    protected Queue<int[]> dataQueue;
    protected HashMap<Integer, int[]> recvBuffer;

    public TCP_Receiver_ADT() {
        this.client = new Client(this.localPort);
        System.out.print("Receiver socket address: ");
        this.client.printLocalSocketAddress();
        System.out.println("** TCP_Receiver: Waiting for arriving packets...\n");
        this.destinPort = Constant.LocalSenderPort;
        this.dataQueue = new LinkedBlockingQueue<int[]>();
        this.recvBuffer = new HashMap();
        try {
            FileOutputStream dataFile = new FileOutputStream("recvData.txt");
            dataFile.write(new String("").getBytes());
            dataFile.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initTCP_Receiver(TCP_Receiver tcpReceiver) {
        new ListenPacket(this.client, tcpReceiver).start();
        this.tcpH = new TCP_HEADER((short)this.localPort, (short)this.destinPort, 1, 1, (byte)6, "010000", (short)1000, (short)0, (short)1024, (byte)0, (byte)7);
        this.tcpS = new TCP_SEGMENT();
    }

    public abstract void rdt_recv(TCP_PACKET var1);

    public abstract void deliver_data();

    public abstract void reply(TCP_PACKET var1);
}

