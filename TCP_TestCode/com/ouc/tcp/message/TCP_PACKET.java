package com.ouc.tcp.message;

import com.ouc.tcp.config.Constant;
import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_SEGMENT;
import java.io.Serializable;
import java.net.InetAddress;

public class TCP_PACKET
implements Serializable,
Cloneable {
    private TCP_HEADER tcpH;
    private TCP_SEGMENT tcpS;
    private InetAddress sourceAddr;
    private InetAddress destinAddr;
    private int tansFlag;

    public TCP_PACKET() {
        this.tcpH = new TCP_HEADER();
        this.tcpS = new TCP_SEGMENT();
        this.sourceAddr = this.destinAddr = Constant.LocalAddr;
        this.tansFlag = 0;
    }

    public TCP_PACKET(TCP_HEADER tcpH, TCP_SEGMENT tcpS, InetAddress destinAddr) {
        this.tcpH = tcpH;
        this.tcpS = tcpS;
        this.sourceAddr = Constant.LocalAddr;
        this.destinAddr = destinAddr;
        this.tansFlag = 0;
    }

    public TCP_PACKET clone() throws CloneNotSupportedException {
        TCP_PACKET tcpPack_Copy = (TCP_PACKET)super.clone();
        tcpPack_Copy.tcpH = this.tcpH.clone();
        tcpPack_Copy.tcpS = this.tcpS.clone();
        return tcpPack_Copy;
    }

    public TCP_HEADER getTcpH() {
        return this.tcpH;
    }

    public TCP_SEGMENT getTcpS() {
        return this.tcpS;
    }

    public InetAddress getSourceAddr() {
        return this.sourceAddr;
    }

    public InetAddress getDestinAddr() {
        return this.destinAddr;
    }

    public void setTcpH(TCP_HEADER tcpH) {
        this.tcpH = tcpH;
    }

    public void setTcpS(TCP_SEGMENT tcpS) {
        this.tcpS = tcpS;
    }

    public void setSourceAddr(InetAddress sourceAddr) {
        this.sourceAddr = sourceAddr;
    }

    public void setDestinAddr(InetAddress destinAddr) {
        this.destinAddr = destinAddr;
    }

    public int getTansFlag() {
        return this.tansFlag;
    }

    public void setTansFlag(int tansFlag) {
        this.tansFlag = tansFlag;
    }

    public int getTCP_Length() {
        return this.tcpH.getTh_Length() + this.tcpS.getDataLengthInByte();
    }
}

