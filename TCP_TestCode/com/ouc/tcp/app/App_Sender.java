package com.ouc.tcp.app;

import com.ouc.tcp.test.TCP_Sender;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

public class App_Sender {
    private static Key key;
    private static final int dataGroupSize = 100;
    private static int[] appData;
    private static TCP_Sender tcpSender;

    static {
        appData = new int[100];
        tcpSender = new TCP_Sender();
    }

    public static void main(String[] args) {
        System.out.println("** TCP_Sender: Press enter key to start data transmission...");
        try {
            System.in.read();
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
        App_Sender.getKey("OUCnet2012$#@!");
        File fr = new File("ENCDA.tcp");
        String encStr = "";
        String dataStr = "";
        int dataNum = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fr));
            while ((encStr = reader.readLine()) != null) {
                dataStr = App_Sender.getDesString(encStr);
                App_Sender.appData[dataNum++ % 100] = Integer.parseInt(dataStr);
                if (dataNum % 100 != 0) continue;
                tcpSender.rdt_send((dataNum - 1) / 100, appData);
                Thread.sleep(10L);
            }
            reader.close();
            System.out.println("\n**************** TCP_Sender: Data sending ends. ****************\n");
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void getKey(String strKey) {
        try {
            KeyGenerator _generator = KeyGenerator.getInstance("DES");
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(strKey.getBytes());
            _generator.init(secureRandom);
            key = _generator.generateKey();
            _generator = null;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getDesString(String strMi) {
        Base64.Decoder base64De = Base64.getDecoder();
        byte[] byteMing = null;
        byte[] byteMi = null;
        String strMing = "";
        try {
            try {
                byteMi = base64De.decode(strMi);
                byteMing = App_Sender.getDesCode(byteMi);
                strMing = new String(byteMing, "UTF8");
            }
            catch (Exception e) {
                e.printStackTrace();
                base64De = null;
                byteMing = null;
                byteMi = null;
            }
        }
        finally {
            base64De = null;
            byteMing = null;
            byteMi = null;
        }
        return strMing;
    }

    private static byte[] getDesCode(byte[] byteD) {
        Cipher cipher;
        byte[] byteFina = null;
        try {
            try {
                cipher = Cipher.getInstance("DES");
                cipher.init(2, key);
                byteFina = cipher.doFinal(byteD);
            }
            catch (Exception e) {
                e.printStackTrace();
                Object cipher2 = null;
            }
        }
        finally {
            cipher = null;
        }
        return byteFina;
    }
}

