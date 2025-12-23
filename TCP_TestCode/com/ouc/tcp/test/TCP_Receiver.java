package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.TCP_PACKET;

public class TCP_Receiver extends TCP_Receiver_ADT {

    public TCP_Receiver() {
        super();
        this.initTCP_Receiver(this);
    }

    @Override
    public void rdt_recv(TCP_PACKET packet) {
        // Implement reliable data transfer receive
        // For now, just process the packet
        // Extract data and put into dataQueue
    }

    @Override
    public void deliver_data() {
        // Deliver data to application
        // For example, write to file or process
    }

    @Override
    public void reply(TCP_PACKET packet) {
        // Send reply packet, e.g., ACK
        this.client.send(packet);
    }
}