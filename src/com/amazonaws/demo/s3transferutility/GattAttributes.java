package com.amazonaws.demo.s3transferutility;

import java.net.PortUnreachableException;
import java.util.HashMap;
/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    public static String CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    public static String RECEIVE= "01366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static String SEND = "05366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static String X_CHAR = "02366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static String Y_CHAR = "03366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static String Z_CHAR = "04366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static String PROC_PITCH_CHAR = "06366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static String PROC_ROLL_CHAR = "07366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    //public static String Y_CHAR = "00002902-0000-1000-8000-00805f9b34fb";
    //public static String Z_CHAR = "00002902-0000-1000-8000-00805f9b34fb";
    static {
        // Sample Services.
        attributes.put(SEND, "PROC Service (send) ");
        attributes.put(RECEIVE, "ACC Service (receive)");
        // Sample Characteristics.
        attributes.put(X_CHAR, "ACC_X");
        attributes.put(Y_CHAR, "ACC_Y");
        attributes.put(Z_CHAR, "ACC_Z");
        attributes.put(PROC_PITCH_CHAR, "PROC_PITCH");
        attributes.put(PROC_ROLL_CHAR, "PROC_ROLL");
        //attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }
    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
