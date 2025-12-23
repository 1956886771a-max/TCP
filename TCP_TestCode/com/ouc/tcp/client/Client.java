package com.ouc.tcp.client;

import com.ouc.tcp.config.Constant;
import com.ouc.tcp.message.MSG_STREAM;
import com.ouc.tcp.message.TCP_PACKET;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Client {
    private InetAddress ipAddr = Constant.LocalAddr;
    private int localPort;
    public DatagramSocket socket;
    private DatagramPacket sendPacket;
    public DatagramPacket receivePacket;
    private final int ECHOMAX = 65600;

    public Client(int localPort) {
        this.localPort = localPort;
        try {
            this.socket = new DatagramSocket(this.localPort, this.ipAddr);
            this.receivePacket = new DatagramPacket(new byte[65600], 65600);
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void send(TCP_PACKET tcpPack) {
        MSG_STREAM msgStr = new MSG_STREAM(tcpPack);
        byte[] packStr = msgStr.getPacket_byteStream();
        try {
            InetAddress destinAddr = Constant.LocalAddr;
            int destinPort = Constant.ServerPort;
            this.sendPacket = new DatagramPacket(packStr, packStr.length, destinAddr, destinPort);
            this.socket.send(this.sendPacket);
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printLocalSocketAddress() {
        String socketAddr = this.socket.getLocalSocketAddress().toString();
        System.out.println(socketAddr);
    }
}

