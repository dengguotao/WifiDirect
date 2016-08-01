package com.ckt.io.wifidirect.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.SoftReference;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by admin on 2016/3/14.
 */
public class DataTypeUtils {
    public static byte[] intToBytes2(int n) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (n >> (24 - i * 8));
        }
        return b;
    }

    public static int byteToInt2(byte[] b) {
        int mask = 0xff;
        int temp = 0;
        int n = 0;
        for (int i = 0; i < b.length; i++) {
            n <<= 8;
            temp = b[i] & mask;
            n |= temp;
        }
        return n;
    }

    public static String format(double num) {
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(num);
    }

    public static String toJsonStr(HashMap<String, String> map) {
        JSONObject obj = new JSONObject();
        for(String str : map.keySet()) {
            try {
                obj.put(str, map.get(str));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return obj.toString();
    }

    public static HashMap<String, String> toHashmap(String json) {
        HashMap<String, String> map = new HashMap<>();
        JSONObject object = null;
        try {
            object = new JSONObject(json);

            Iterator<String> it =object.keys();
            while (it.hasNext()) {
                String key = it.next();
                map.put(key, object.getString(key));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }
}
