package com.ouc.tcp.tool;

import com.ouc.tcp.message.TCP_PACKET;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class TCP_TOOL {
    public static final int MAXSEQ = Integer.MAX_VALUE;

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static InetAddress getLocalIpAddr() {
        try {
            InetAddress address;
            Enumeration<NetworkInterface> interfaceList = NetworkInterface.getNetworkInterfaces();
            if (interfaceList == null) {
                return null;
            }
            block2: while (true) {
                if (!interfaceList.hasMoreElements()) {
                    return null;
                }
                NetworkInterface iface = interfaceList.nextElement();
                Enumeration<InetAddress> addrList = iface.getInetAddresses();
                do {
                    if (!addrList.hasMoreElements()) continue block2;
                } while (!((address = addrList.nextElement()) instanceof Inet4Address) || address.isLoopbackAddress());
                break;
            }
            return address;
        }
        catch (SocketException se) {
            System.out.println("Error to get network interface: " + se.getMessage());
        }
        return null;
    }

    public static TCP_PACKET getTCP_Packet(DatagramPacket packet) {
        TCP_PACKET tcpPack = null;
        byte[] packetByte = packet.getData();
        ByteArrayInputStream bAIStream = new ByteArrayInputStream(packetByte);
        try {
            ObjectInputStream oIStream = new ObjectInputStream(bAIStream);
            tcpPack = (TCP_PACKET)oIStream.readObject();
            oIStream.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return tcpPack;
    }

    public static int judgePacketType(TCP_PACKET tcpPack) {
        if (tcpPack.getTcpH().getTh_flags_SYN() && !tcpPack.getTcpH().getTh_flags_ACK()) {
            return 0;
        }
        if (tcpPack.getTcpH().getTh_flags_SYN() && tcpPack.getTcpH().getTh_flags_ACK()) {
            return 1;
        }
        if (tcpPack.getTcpH().getTh_flags_ACK() && !tcpPack.getTcpH().getTh_flags_SYN()) {
            if (tcpPack.getTcpS().getData().length > 0) {
                return 2;
            }
            return 3;
        }
        return -1;
    }
}

