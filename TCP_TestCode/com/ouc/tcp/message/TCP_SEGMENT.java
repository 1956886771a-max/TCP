package com.ouc.tcp.message;

import java.io.Serializable;

public class TCP_SEGMENT
implements Serializable,
Cloneable {
    private int[] tcpData;

    public TCP_SEGMENT() {
        this.tcpData = new int[0];
    }

    public TCP_SEGMENT(int l, int r) {
        if (l == r) {
            this.tcpData = new int[0];
        } else {
            this.tcpData = new int[Math.abs(r - l + 1)];
            if (l > r) {
                int temp = l;
                l = r;
                r = temp;
            }
            int i = 0;
            while (i < this.tcpData.length) {
                this.tcpData[i] = l + i;
                ++i;
            }
        }
    }

    public TCP_SEGMENT(int[] data) {
        this.tcpData = (int[])data.clone();
    }

    public TCP_SEGMENT clone() throws CloneNotSupportedException {
        return new TCP_SEGMENT((int[])this.tcpData.clone());
    }

    public int[] getData() {
        return this.tcpData;
    }

    public void setData(int l, int r) {
        if (l == r) {
            this.tcpData = new int[0];
        } else {
            this.tcpData = new int[Math.abs(r - l + 1)];
            if (l > r) {
                int temp = l;
                l = r;
                r = temp;
            }
            int i = 0;
            while (i < this.tcpData.length) {
                this.tcpData[i] = l + i;
                ++i;
            }
        }
    }

    public void setData(int[] data) {
        this.tcpData = (int[])data.clone();
    }

    public void setDataByIndex(int index, int data) {
        this.tcpData[index] = data;
    }

    public int getDataLengthInByte() {
        return this.tcpData.length * 4;
    }
}

