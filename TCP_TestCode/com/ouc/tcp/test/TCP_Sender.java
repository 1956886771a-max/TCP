package com.ouc.tcp.test;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.message.TCP_PACKET;

public class TCP_Sender extends TCP_Sender_ADT {

    public TCP_Sender() {
        super();
        this.initTCP_Sender(this);
    }

    @Override
    public void rdt_send(int seqNum, int[] data) {
        // Implement reliable data transfer send
        // For now, just send the data
        for (int i = 0; i < data.length; i++) {
            TCP_PACKET packet = new TCP_PACKET();
            // Set packet data
            this.udt_send(packet);
        }
    }

    @Override
    public void udt_send(TCP_PACKET packet) {
        // Implement unreliable data transfer send
        this.client.send(packet);
    }

    @Override
    public void recv(TCP_PACKET packet) {
        // Handle received packet
        // For example, check if it's an ACK
    }

    @Override
    public void waitACK() {
        // Wait for ACK
        // Implementation depends on the protocol
    }
}