package com.ouc.tcp.config;

import com.ouc.tcp.config.SYS_INI;
import com.ouc.tcp.tool.TCP_TOOL;
import java.net.InetAddress;

public class Constant {
    public static InetAddress LocalAddr = TCP_TOOL.getLocalIpAddr();
    public static int ServerPort = Integer.parseInt(SYS_INI.getIniKey("servPort"));
    public static int LocalSenderPort = Integer.parseInt(SYS_INI.getIniKey("localSenderPort"));
    public static int LocalReceiverPort = Integer.parseInt(SYS_INI.getIniKey("localReceiverPort"));
}

