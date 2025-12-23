package com.ouc.tcp.message;

import com.ouc.tcp.message.TCP_PACKET;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class MSG_STREAM {
    private TCP_PACKET tcpPack;
    private byte[] packet_byteStream;

    public MSG_STREAM(TCP_PACKET tcpPack) {
        this.tcpPack = tcpPack;
        this.convert2ByteStream();
    }

    public byte[] getPacket_byteStream() {
        return this.packet_byteStream;
    }

    private void convert2ByteStream() {
        ByteArrayOutputStream bAOStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oOStream = new ObjectOutputStream(bAOStream);
            oOStream.writeObject(this.tcpPack);
            this.packet_byteStream = bAOStream.toByteArray();
            oOStream.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}

