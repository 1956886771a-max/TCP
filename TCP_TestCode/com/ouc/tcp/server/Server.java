package com.ouc.tcp.server;

import com.ouc.tcp.config.Constant;
import com.ouc.tcp.message.MSG_STREAM;
import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.server.ForwardDelay;
import com.ouc.tcp.server.TransLog;
import com.ouc.tcp.server.WriteLogFile;
import com.ouc.tcp.tool.TCP_TOOL;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Server {
    private InetAddress ipAddr = Constant.LocalAddr;
    private int servPort = Constant.ServerPort;
    private static DatagramSocket socket;
    private static DatagramPacket newPacket;
    private static DatagramPacket forwardPacket;
    private final int ECHOMAX = 65600;
    private static ArrayList<String> clientHost;
    private static ArrayList<TransLog> transLog;

    public Server() {
        try {
            socket = new DatagramSocket(this.servPort, this.ipAddr);
            newPacket = new DatagramPacket(new byte[65600], 65600);
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
        clientHost = new ArrayList();
        transLog = new ArrayList();
    }

    public static void forwardMessage(TCP_PACKET tcpPack) {
        MSG_STREAM msgStr = new MSG_STREAM(tcpPack);
        byte[] packStr = msgStr.getPacket_byteStream();
        InetAddress destinAddr = tcpPack.getDestinAddr();
        short destinPort = tcpPack.getTcpH().getTh_dport();
        forwardPacket = new DatagramPacket(packStr, packStr.length, destinAddr, destinPort);
        try {
            socket.send(forwardPacket);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void printServerSocketAddress() {
        String socketAddr = socket.getLocalSocketAddress().toString();
        System.out.println(socketAddr);
    }

    private static void listenNewPacket() {
        Random errorRand = new Random();
        int eFlag = 0;
        int eType = 0;
        Timer timer = new Timer();
        TimerTask writeLog = new TimerTask(){

            @Override
            public void run() {
                new WriteLogFile(clientHost, transLog).start();
            }
        };
        timer.schedule(writeLog, 5000L, 5000L);
        while (true) {
            try {
                while (true) {
                    socket.receive(newPacket);
                    TCP_PACKET tcpPack = TCP_TOOL.getTCP_Packet(newPacket);
                    if (TCP_TOOL.judgePacketType(tcpPack) < 2) {
                        Server.forwardMessage(tcpPack);
                        continue;
                    }
                    eFlag = errorRand.nextInt(100);
                    if (eFlag == 1) {
                        switch (tcpPack.getTcpH().getTh_eflag()) {
                            case 0: {
                                eType = 0;
                                break;
                            }
                            case 1: {
                                eType = 1;
                                break;
                            }
                            case 2: {
                                eType = 2;
                                break;
                            }
                            case 3: {
                                eType = 3;
                                break;
                            }
                            case 4: {
                                eType = errorRand.nextInt(2) + 1;
                                break;
                            }
                            case 5: {
                                eType = errorRand.nextInt(2) + 1;
                                if (eType != 2) break;
                                ++eType;
                                break;
                            }
                            case 6: {
                                eType = errorRand.nextInt(2) + 2;
                                break;
                            }
                            default: {
                                eType = errorRand.nextInt(3) + 1;
                            }
                        }
                        if (eType == 0) {
                            Server.forwardMessage(tcpPack);
                        } else {
                            Server.processTransErr(tcpPack, eType);
                        }
                        Server.makeLog(tcpPack, eType);
                        continue;
                    }
                    Server.forwardMessage(tcpPack);
                    Server.makeLog(tcpPack, 0);
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private static String getClientHost(TCP_PACKET tcpPack, int clientFlag) {
        if (clientFlag == 0) {
            return String.valueOf(tcpPack.getSourceAddr().getHostAddress()) + ":" + tcpPack.getTcpH().getTh_sport();
        }
        return String.valueOf(tcpPack.getDestinAddr().getHostAddress()) + ":" + tcpPack.getTcpH().getTh_dport();
    }

    private static void processTransErr(TCP_PACKET tcpPack, int eType) {
        Random errorSet = new Random();
        switch (eType) {
            case 1: {
                if (tcpPack.getTcpS().getData().length > 0) {
                    int dataLen = tcpPack.getTcpS().getData().length;
                    int eNum = dataLen / 100;
                    if (eNum < 1) {
                        eNum = 1;
                    }
                    int eOffset = 0;
                    int i = 0;
                    while (i < eNum) {
                        eOffset = errorSet.nextInt(dataLen);
                        tcpPack.getTcpS().setDataByIndex(eOffset, -(errorSet.nextInt(Integer.MAX_VALUE) + 1));
                        ++i;
                    }
                } else {
                    tcpPack.getTcpH().setTh_ack(-(errorSet.nextInt(Integer.MAX_VALUE) + 1));
                }
                Server.forwardMessage(tcpPack);
                break;
            }
            case 2: {
                break;
            }
            case 3: {
                new ForwardDelay(tcpPack).start();
            }
        }
    }

    private static void makeLog(TCP_PACKET tcpPack, int transFlag) {
        String sHost = Server.getClientHost(tcpPack, 0);
        String dHost = Server.getClientHost(tcpPack, 1);
        if (!clientHost.contains(sHost)) {
            clientHost.add(sHost);
            transLog.add(new TransLog());
        }
        int packType = TCP_TOOL.judgePacketType(tcpPack) == 2 ? 0 : 1;
        int sHostIndex = clientHost.indexOf(sHost);
        transLog.get(sHostIndex).addLog(packType, transFlag, tcpPack.getTcpH().getTh_seq(), tcpPack.getTcpH().getTh_ack());
        if (packType == 1 && transFlag == 0 && clientHost.contains(dHost)) {
            int dHostIndex = clientHost.indexOf(dHost);
            transLog.get(dHostIndex).checkDataACK(tcpPack.getTcpH().getTh_ack());
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        System.out.print("Server socket address: ");
        server.printServerSocketAddress();
        System.out.println("** Server: Listening channel...\n");
        Server.listenNewPacket();
    }
}

