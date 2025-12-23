package com.ouc.tcp.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class SYS_INI {
    private static Properties ini = null;
    static File file = new File("Config.ini");

    static {
        try {
            ini = new Properties();
            ini.load(new FileInputStream(file));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getIniKey(String key) {
        if (!ini.containsKey(key)) {
            return "";
        }
        return ini.get(key).toString();
    }

    public static void setIniKey(String key, String value) {
        if (!ini.containsKey(key)) {
            return;
        }
        ini.put(key, value);
    }
}

