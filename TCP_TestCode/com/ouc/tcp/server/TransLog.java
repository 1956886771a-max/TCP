package com.ouc.tcp.server;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class TransLog {
    private SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS z");
    private long sysTimeMillis;
    private ArrayList<String> transitTime = new ArrayList();
    private ArrayList<Integer> packType = new ArrayList();
    private ArrayList<Integer> transStatus = new ArrayList();
    private ArrayList<Integer> seq = new ArrayList();
    private ArrayList<Integer> ack = new ArrayList();
    private ArrayList<Boolean> checkResponse = new ArrayList();
    private ArrayList<Boolean> retransFlag = new ArrayList();

    public void addLog(int packType, int transStatus, int seq, int ack) {
        this.sysTimeMillis = System.currentTimeMillis();
        this.transitTime.add(this.timeFormat.format(this.sysTimeMillis));
        int logIndex = packType == 0 ? this.seq.lastIndexOf(seq) : this.ack.lastIndexOf(ack);
        if (logIndex != -1) {
            if (this.packType.get(logIndex) == packType && !this.checkResponse.get(logIndex).booleanValue()) {
                this.retransFlag.add(true);
            } else {
                this.retransFlag.add(false);
            }
        } else {
            this.retransFlag.add(false);
        }
        this.packType.add(packType);
        this.transStatus.add(transStatus);
        this.seq.add(seq);
        this.ack.add(ack);
        if (packType == 1 && transStatus == 0) {
            this.checkResponse.add(true);
        } else {
            this.checkResponse.add(false);
        }
    }

    public void checkDataACK(int responseAck) {
        int i = this.seq.size() - 1;
        while (i >= 0) {
            if (this.packType.get(i) == 0 && this.seq.get(i) == responseAck && !this.checkResponse.get(i).booleanValue() && this.transStatus.get(i) == 0) {
                this.checkResponse.set(i, true);
                break;
            }
            --i;
        }
    }

    public int getTotalPackNum() {
        return this.seq.size();
    }

    public int[] countStatus() {
        int[] status = new int[4];
        Arrays.fill(status, 0);
        int i = 0;
        while (i < this.transStatus.size()) {
            int n = this.transStatus.get(i);
            status[n] = status[n] + 1;
            ++i;
        }
        return status;
    }

    public double getTransSucRatio() {
        double sucN = 0.0;
        int i = 0;
        while (i < this.seq.size()) {
            if (this.transStatus.get(i) == 0 && this.checkResponse.get(i).booleanValue()) {
                sucN += 1.0;
            }
            ++i;
        }
        double sucRatio = sucN / (double)this.seq.size();
        return sucRatio;
    }

    public String toString() {
        StringBuilder logList = new StringBuilder();
        String statusStr = "";
        String responseStr = "";
        int i = 0;
        while (i < this.seq.size()) {
            logList.append("\t");
            logList.append(this.transitTime.get(i));
            logList.append("\t");
            if (this.retransFlag.get(i).booleanValue()) {
                logList.append("*Re: ");
            }
            if (this.packType.get(i) == 0) {
                logList.append("DATA_seq: ");
                logList.append(this.seq.get(i));
                responseStr = this.checkResponse.get(i).booleanValue() ? "ACKed" : "NO_ACK";
            } else {
                logList.append("ACK_ack: ");
                logList.append(this.ack.get(i));
            }
            switch (this.transStatus.get(i)) {
                case 0: {
                    statusStr = "";
                    break;
                }
                case 1: {
                    statusStr = "WRONG";
                    break;
                }
                case 2: {
                    statusStr = "LOSS";
                    break;
                }
                case 3: {
                    statusStr = "DELAY";
                }
            }
            logList.append("\t");
            logList.append(statusStr);
            if (this.packType.get(i) == 0) {
                logList.append("\t");
                logList.append(responseStr);
            }
            logList.append("\n");
            ++i;
        }
        return logList.toString();
    }
}

