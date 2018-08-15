package com.easy.transfer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.easy.transfer.utils.LogUtils;

import java.io.File;

/**
 * Created by admin on 2016/8/10.
 */

public class TransferFileInfo {

    private static final String TAG = "TransferFileInfo";

    public int id;
    public String path;
    public String name;
    public long length = 0;
    public int state;
    public long transferedLength = 0;
    public int direction;
    public String transferMac;
    public double speed;

    private ContentResolver mContentResolver;
    private Uri uri;

    public TransferFileInfo(int id, String path, String name, long length, int state,
                            long transferLength, int direction, String transferMac,
                            ContentResolver mContentResolver) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.length = length;
        this.state = state;
        this.transferedLength = transferLength;
        this.direction = direction;
        this.transferMac = transferMac;
        this.mContentResolver = mContentResolver;
    }

    public TransferFileInfo(Cursor cursor, ContentResolver contentResolver) {
        id = cursor.getInt(cursor.getColumnIndex(Constants.InstanceColumns.ID));
        if (id > 0) {
            uri = ContentUris.withAppendedId(Constants.InstanceColumns.CONTENT_URI, id);
        }
        name = cursor.getString(cursor.getColumnIndex(Constants.InstanceColumns.NAME));
        path = cursor.getString(cursor.getColumnIndex(Constants.InstanceColumns.PATH));
        if (path != null) {
            File file = new File(path);
            if (file.exists()) {
                //length = file.get
                length = file.length();
            }
        }
        state = cursor.getInt(cursor.getColumnIndex(Constants.InstanceColumns.STATE));
        transferedLength = cursor.getLong(cursor.getColumnIndex(Constants.InstanceColumns.TRANSFER_LENGTH));
        direction = cursor.getInt(cursor.getColumnIndex(Constants.InstanceColumns.TRANSFER_DIRECTION));
        transferMac = cursor.getString(cursor.getColumnIndex(Constants.InstanceColumns.TRANSFER_MAC));
        mContentResolver = contentResolver;
    }

    public boolean updateState(int state) {
        if (uri == null) {
            LogUtils.e(TAG, "update state failed, reson: uri is null");
            return false;
        }
        if (this.state == state) {
            return false;
        }
        this.state = state;
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.InstanceColumns.STATE, state);
        mContentResolver.update(uri, contentValues, null, null);
        return true;
    }

    public boolean updateTransferSize() {
        if (uri == null) {
            LogUtils.e(TAG, "update state failed, reson: uri is null");
            return false;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.InstanceColumns.TRANSFER_LENGTH, transferedLength);
        mContentResolver.update(uri, contentValues, null, null);
        return true;
    }

    public boolean insert() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Constants.InstanceColumns.PATH, path);
        contentValues.put(Constants.InstanceColumns.NAME, name);
        contentValues.put(Constants.InstanceColumns.LENGTH, length);
        contentValues.put(Constants.InstanceColumns.TRANSFER_MAC, transferMac);
        contentValues.put(Constants.InstanceColumns.TRANSFER_DIRECTION, direction);
        contentValues.put(Constants.InstanceColumns.STATE, state);
        contentValues.put(Constants.InstanceColumns.TRANSFER_LENGTH, transferedLength);
        uri = mContentResolver.insert(Constants.InstanceColumns.CONTENT_URI, contentValues);
        if (uri == null) return false;
        id = (int) ContentUris.parseId(uri);
        return true;
    }

    @Override
    public String toString() {
        return "TransferFileInfo{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", length=" + length +
                ", state=" + state +
                ", transferedLength=" + transferedLength +
                ", direction=" + direction +
                ", transferMac='" + transferMac + '\'' +
                ", speed=" + speed +
                ", uri=" + uri +
                '}';
    }
}
