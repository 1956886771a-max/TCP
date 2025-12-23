package com.ouc.tcp.server;

import com.ouc.tcp.server.TransLog;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;

public class WriteLogFile
extends Thread {
    private ArrayList<String> clientHost;
    private ArrayList<TransLog> transLog;

    public WriteLogFile(ArrayList<String> host, ArrayList<TransLog> log) {
        this.clientHost = host;
        this.transLog = log;
    }

    @Override
    public void run() {
        File fw = new File("Log.txt");
        try {
            BufferedWriter output = new BufferedWriter(new FileWriter(fw, false));
            output.write("CLIENT HOST\tTOTAL\tSUC_RATIO\tNORMAL\tWRONG\tLOSS\tDELAY\n");
            int i = 0;
            while (i < this.clientHost.size()) {
                output.write(this.clientHost.get(i));
                output.write("\t" + this.transLog.get(i).getTotalPackNum());
                output.write("\t" + WriteLogFile.formatPercent(this.transLog.get(i).getTransSucRatio(), 2));
                int[] transStatus = this.transLog.get(i).countStatus();
                int j = 0;
                while (j < transStatus.length) {
                    output.write("\t" + transStatus[j]);
                    ++j;
                }
                output.write("\n" + this.transLog.get(i).toString() + "\n");
                ++i;
            }
            output.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String formatPercent(double k, int fDigit) {
        NumberFormat nf = NumberFormat.getPercentInstance();
        nf.setMinimumFractionDigits(fDigit);
        return nf.format(k);
    }
}

