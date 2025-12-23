package com.ouc.tcp.app;

import com.ouc.tcp.app.App_Sender;
import com.ouc.tcp.app.RunServer;
import com.ouc.tcp.test.TCP_Receiver;

public class SystemStart {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("OUC TCP Protocol Teaching Experimental System (V1.0)\n");
        new RunServer().start();
        Thread.sleep(200L);
        new TCP_Receiver();
        Thread.sleep(200L);
        App_Sender.main(null);
    }
}

