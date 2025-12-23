package com.ouc.tcp.message;

import com.ouc.tcp.config.Constant;
import java.io.Serializable;

public class TCP_HEADER
implements Serializable,
Cloneable {
    private short th_sport;
    private short th_dport;
    private int th_seq;
    private int th_ack;
    private byte th_doff;
    private byte th_flags;
    private short th_win;
    private short th_sum;
    private short th_urp;
    private short th_mss;
    private byte th_sackFlag;
    private byte th_eflag;
    private int[] th_sack_borders;

    public TCP_HEADER() {
        this.th_sport = (short)Constant.LocalSenderPort;
        this.th_dport = (short)Constant.LocalReceiverPort;
        this.th_seq = 1;
        this.th_ack = 1;
        this.th_doff = (byte)6;
        this.th_flags = Byte.parseByte("010000", 2);
        this.th_win = (short)1000;
        this.th_sum = 0;
        this.th_urp = 0;
        this.th_mss = (short)1024;
        this.th_sackFlag = 0;
        this.th_sack_borders = new int[0];
        this.th_eflag = 0;
    }

    public TCP_HEADER(short th_sport, short th_dport, int th_seq, int th_ack, byte th_doff, String flags, short th_win, short th_urp, short th_mss, byte th_sackNum, byte th_eflag) {
        this.th_sport = th_sport;
        this.th_dport = th_dport;
        this.th_seq = th_seq;
        this.th_ack = th_ack;
        this.th_doff = (byte)(th_doff * 16);
        this.th_flags = Byte.parseByte(flags, 2);
        this.th_win = th_win;
        this.th_sum = 0;
        this.th_urp = th_urp;
        this.th_mss = th_mss;
        if (th_sackNum >= 0) {
            this.th_sackFlag = th_sackNum;
            this.th_sack_borders = new int[th_sackNum * 2];
        } else {
            this.th_sackFlag = 0;
            this.th_sack_borders = new int[0];
        }
        this.th_eflag = th_eflag;
    }

    public TCP_HEADER clone() throws CloneNotSupportedException {
        TCP_HEADER tcpH_Copy = (TCP_HEADER)super.clone();
        tcpH_Copy.th_sack_borders = (int[])this.th_sack_borders.clone();
        return tcpH_Copy;
    }

    public void setTh_sport(short th_sport) {
        this.th_sport = th_sport;
    }

    public void setTh_dport(short th_dport) {
        this.th_dport = th_dport;
    }

    public void setTh_seq(int th_seq) {
        this.th_seq = th_seq;
    }

    public void setTh_ack(int th_ack) {
        this.th_ack = th_ack;
    }

    public void setTh_doff(byte th_doff) {
        this.th_doff = (byte)(th_doff * 16);
    }

    public void setTh_flags(String flags) {
        this.th_flags = Byte.parseByte(flags, 2);
    }

    public void setTh_win(short th_win) {
        this.th_win = th_win;
    }

    public void setTh_sum(short th_sum) {
        this.th_sum = th_sum;
    }

    public void setTh_urp(short th_urp) {
        this.th_urp = th_urp;
    }

    public void setTh_mss(short th_mss) {
        this.th_mss = th_mss;
    }

    public void setTh_sackFlag(byte th_sackNum) {
        if (th_sackNum >= 0) {
            this.th_sackFlag = th_sackNum;
            this.th_sack_borders = new int[th_sackNum * 2];
        } else {
            this.th_sackFlag = 0;
            this.th_sack_borders = new int[0];
        }
    }

    public void setTh_eflag(byte th_eflag) {
        this.th_eflag = th_eflag;
    }

    public void setTh_sack_border(int k, int Lk, int Rk) {
        if (k >= 0 && 2 * k <= this.th_sack_borders.length) {
            this.th_sack_borders[2 * k] = Lk;
            this.th_sack_borders[2 * k + 1] = Rk;
        }
    }

    public short getTh_sport() {
        return this.th_sport;
    }

    public short getTh_dport() {
        return this.th_dport;
    }

    public int getTh_seq() {
        return this.th_seq;
    }

    public int getTh_ack() {
        return this.th_ack;
    }

    public byte getTh_doff() {
        return (byte)(this.th_doff / 16);
    }

    public byte getTh_flags() {
        return this.th_flags;
    }

    public boolean getTh_flags_URG() {
        return this.checkFlags(0);
    }

    public boolean getTh_flags_ACK() {
        return this.checkFlags(1);
    }

    public boolean getTh_flags_PSH() {
        return this.checkFlags(2);
    }

    public boolean getTh_flags_RST() {
        return this.checkFlags(3);
    }

    public boolean getTh_flags_SYN() {
        return this.checkFlags(4);
    }

    public boolean getTh_flags_FIN() {
        return this.checkFlags(5);
    }

    private boolean checkFlags(int flagIndex) {
        char[] flags = "000000".toCharArray();
        char[] subFlags = Integer.toBinaryString(this.th_flags).toCharArray();
        int k = flags.length - subFlags.length;
        while (k < flags.length) {
            flags[k] = subFlags[k - (flags.length - subFlags.length)];
            ++k;
        }
        return flags[flagIndex] == '1';
    }

    public short getTh_win() {
        return this.th_win;
    }

    public short getTh_sum() {
        return this.th_sum;
    }

    public short getTh_urp() {
        return this.th_urp;
    }

    public short getTh_mss() {
        return this.th_mss;
    }

    public byte getTh_sackFlag() {
        return this.th_sackFlag;
    }

    public boolean checkSACK() {
        return this.th_sackFlag > 0;
    }

    public byte getTh_eflag() {
        return this.th_eflag;
    }

    public int[] getTh_sack_borders() {
        return this.th_sack_borders;
    }

    public int getTh_Length() {
        return 24 + this.th_sack_borders.length * 4;
    }
}

