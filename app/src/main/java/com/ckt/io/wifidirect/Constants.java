package com.ckt.io.wifidirect;

import android.net.Uri;

/**
 * Created by admin on 2016/3/14.
 */
public class Constants {
    public static final String AUTHORITY = "com.ckt.io.wifidirect";
    public final static String DATABASE_NAME = "wifi_direct.db";
    public final static String TABLE_NAME = "wifi_direct";

    public static class InstanceColumns{
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/wifi_direct");
        public static final String ID = "_id";
        public static final String NAME = "_name";
        public static final String LENGTH = "_length";
        public static final String TRANSFER_LENGTH = "_tlength";
        public static final String STATE = "_state";
        public static final String TRANSFER_DIRECTION = "_direction";
    }
}
