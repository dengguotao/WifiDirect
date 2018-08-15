package com.easy.transfer.utils;

import android.annotation.TargetApi;
import android.net.LinkAddress;
import android.net.wifi.p2p.WifiP2pGroup;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;

/**
 * Created by admin on 2016/8/8.
 */
public class NetworksUtils {
    public static final String TAG = "NetworkUtils";

    /*
    * This method can only use by the p2p client instead of p2p groupowner(becase the owner new use dhcp to get the ip from the gateway)
    * return : the ip addr of the p2p client or null
    * */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static InetAddress getPeerIp(WifiP2pGroup group) {
        InetAddress ret = null;
        try {
            Class cls = Class.forName("android.net.NetworkUtils");
            for(Method m : cls.getMethods()) {
                LogUtils.d(TAG, "mmmmm---->"+m.getName());
            }
            Class dhcpResultClass = Class.forName("android.net.DhcpResults");
            /*Method method = cls.getMethod("getDhcpResults", String.class, dhcpResultClass);
            Object obj = dhcpResultClass.newInstance();
            method.invoke(null, group.getInterface(), obj);
            LogUtils.d(TAG, "DhcpResult--->" + obj.toString());
            Field linkAddrField =  dhcpResultClass.getField("ipAddress");
            LinkAddress linkAddress = (LinkAddress) linkAddrField.get(obj);
            ret = linkAddress.getAddress();*/
            Method method = cls.getMethod("runDhcpRenew", String.class, dhcpResultClass);
            Object obj = dhcpResultClass.newInstance();
            method.invoke(null, group.getInterface(), obj);
            LogUtils.d(TAG, "DhcpResult--->" + obj.toString());
            Field linkAddrField =  dhcpResultClass.getField("ipAddress");
            LinkAddress linkAddress = (LinkAddress) linkAddrField.get(obj);
            ret = linkAddress.getAddress();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String getPeerIP(String peerMac) {
        Log.d(TAG, "getPeerIP():  peerMac= " + peerMac);

        String ip = null;

        /* Try ARP table lookup */
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            if (br != null) {
                while ((line = br.readLine()) != null) {
                    Log.d(TAG, "line in /proc/net/arp is " + line);
                    String[] splitted = null;
                    if (line != null) {
                        splitted = line.split(" +");
                    }

                    // consider it as a match if 5 out of 6 bytes of the mac
                    // address match
                    // ARP output is in the format
                    // <IP address> <HW type> <Flags> <HW address> <Mask Device>

                    if (splitted != null && splitted.length >= 4) {
                        String[] peerMacBytes = peerMac.split(":");
                        String[] arpMacBytes = splitted[3].split(":");

                        if (arpMacBytes == null) {
                            continue;
                        }

                        int matchCount = 0;
                        for (int i = 0; i < arpMacBytes.length; i++) {
                            if (peerMacBytes[i]
                                    .equalsIgnoreCase(arpMacBytes[i])) {
                                matchCount++;
                            }
                        }

                        if (matchCount >= 5) {
                            ip = splitted[0];
                            // Perfect match!
                            if (matchCount == 6) {
                                // Perfect match!
                                return ip;
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "Unable to open /proc/net/arp");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ip;
    }
}
