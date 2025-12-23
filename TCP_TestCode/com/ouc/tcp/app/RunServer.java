package com.ouc.tcp.app;

import com.ouc.tcp.server.Server;

public class RunServer
extends Thread {
    @Override
    public void run() {
        Server.main(null);
    }
}

