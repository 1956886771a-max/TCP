package com.ouc.tcp.client;

import com.ouc.tcp.client.Client;
import com.ouc.tcp.message.TCP_PACKET;
import java.util.TimerTask;

public class UDT_RetransTask
extends TimerTask {
    private Client senderClient;
    private TCP_PACKET reTransPacket;

    public UDT_RetransTask(Client client, TCP_PACKET packet) {
        this.senderClient = client;
        this.reTransPacket = packet;
    }

    @Override
    public void run() {
        this.senderClient.send(this.reTransPacket);
    }
}

