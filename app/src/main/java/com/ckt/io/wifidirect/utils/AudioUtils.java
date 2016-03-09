package com.ckt.io.wifidirect.utils;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;

public class AudioUtils {
    public static ArrayList<Song> getAllSongs(Context context) {
        ArrayList<Song> songs;
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.DATA},
                MediaStore.Audio.Media.MIME_TYPE + "=? or " + MediaStore.Audio.Media.MIME_TYPE + "=?",
                new String[]{"audio/mpeg", "audio/x-ms-wma"}, null
        );
        songs = new ArrayList<>();
        if (cursor.moveToFirst()) {
            Song song;
            do {
                song = new Song();
                song.setFileName(cursor.getString(1));
                song.setTitle(cursor.getString(2));
                song.setDuration(cursor.getInt(3));
                song.setSinger(cursor.getString(4));
                song.setAlbum(cursor.getString(5));
                if (cursor.getString(6) != null) {
                    float size = cursor.getInt(6) / 1024f / 1024f;
                    song.setSize((size + "").substring(0, 4) + "M");
                } else {
                    song.setSize("未知");
                }
                if (cursor.getString(7) != null) {
                    song.setFileUrl(cursor.getString(7));
                }
                songs.add(song);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return songs;
    }
}
