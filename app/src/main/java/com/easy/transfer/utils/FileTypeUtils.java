package com.easy.transfer.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.easy.transfer.R;

import java.io.File;
import java.text.DecimalFormat;

public class FileTypeUtils {

    public static boolean isApk(String path) {
        return path.toLowerCase().endsWith(".apk");
    }
    public static boolean isMusic(String path) {
        String s = path.toLowerCase();
        return s.endsWith(".mp3") || s.endsWith(".m4a") || s.endsWith(".mid")
                || s.endsWith(".ogg") || s.endsWith(".wav") || s.endsWith(".xmf");
    }
    public static boolean isPhoto(String path) {
        String s = path.toLowerCase();
        return s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".bmp") || s.endsWith(".gif") || s.endsWith(".png");
    }
    public static boolean isMovie(String path) {
        String s = path.toLowerCase();
        return s.endsWith(".3pg") || s.endsWith(".mp4") || s.endsWith(".rmvb");
    }


    public static String getTypeString(Context context, String path) {
        String ret;
        if(isApk(path)) {
            ret = context.getResources().getString(R.string.type_application);
        }else if(isMusic(path)) {
            ret = context.getResources().getString(R.string.type_music);
        }else if(isPhoto(path)) {
            ret = context.getResources().getString(R.string.type_photo);
        }else if(isMovie(path)) {
            ret = context.getResources().getString(R.string.type_movie);
        }else {
            ret = context.getResources().getString(R.string.type_other);
        }
        return ret;
    }

    public static int getDefaultFileIcon(String path, int defaultId) {
        String s = path.toLowerCase();
        int id = defaultId;
        if(isApk(path)) {
            id = R.drawable.apk_icon;
        }else if(isMusic(path)) {
            id = R.drawable.music_icon;
        }else if(isPhoto(path)) {
            id = R.drawable.photo_icon;
        }else if(isMovie(path)) {
            id = R.drawable.film_icon;
        }
        return id;
    }


    public static int getDefaultFileIcon(String path) {
        return getDefaultFileIcon(path, R.drawable.file_icon);
    }


    public static boolean isNeedToLoadDrawable(String path) {
        String s = path.toLowerCase();

        return isApk(path) || isMusic(path) || isPhoto(path) || isMovie(path);
    }


    public static String getMIMEType(String path)
    {

        String type = "*/*";
        if(isMusic(path)) {
            type = "audio";
        }else if(isMovie(path)) {
            type = "video";
        }else if(isPhoto(path)) {
            type = "image";
        }else if(isApk(path)) {
            /* android.permission.INSTALL_PACKAGES */
            type = "application/vnd.android.package-archive";
        }else {
            type="*";
        }

        if(!isApk(path)) {
            type += "/*";
        }
        return type;
    }

    public static void openFile(Context context, String path) {
        Intent it = new Intent(Intent.ACTION_VIEW);
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        it.setDataAndType(Uri.fromFile(new File(path)), getMIMEType(path));
        context.startActivity(it);
    }

    public static String getFileSize(String path) {
        long size = new File(path).length();
        DecimalFormat df = new DecimalFormat("0.0");
        return df.format(size / 1024.0 / 1024.0) + "M";
    }
}
