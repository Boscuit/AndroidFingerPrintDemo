package com.createchance.fingerprintdemo;
import java.util.HashMap;

public class SampleGattAttributes {
    //attribute 是一个哈希表来的 将UUID与 服务的名称联系起来了
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String BROADCAST_DATA = "0000ffe1-0000-1000-8000-00805f9b34fb";
    //public static String DATA_BROADCAST_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000ffe0-0000-1000-8000-00805f9b34fb", "Data Broadcast Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put(BROADCAST_DATA,"Broadcast Data");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
    }

    //这里是通过UUID 从哈希表中获取 服务名 如果失败的话 就返回默认的名字。
    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
