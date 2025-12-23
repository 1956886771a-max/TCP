package com.ouc.tcp.server;

import com.ouc.tcp.message.TCP_PACKET;
import com.ouc.tcp.server.Server;
import java.util.Random;

public class ForwardDelay
extends Thread {
    private TCP_PACKET delayPack;

    public ForwardDelay(TCP_PACKET delayPack) {
        this.delayPack = delayPack;
    }

    @Override
    public void run() {
        Random timeRand = new Random();
        long delay = timeRand.nextInt(20001) + 20000;
        try {
            Thread.sleep(delay);
            Server.forwardMessage(this.delayPack);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

