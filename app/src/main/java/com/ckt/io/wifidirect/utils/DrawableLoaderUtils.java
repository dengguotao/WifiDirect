package com.ckt.io.wifidirect.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;

import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by admin on 2016/3/15.
 */
public class DrawableLoaderUtils {
    private static DrawableLoaderUtils drawableLoader;
    private OnLoadFinishedListener listener;
    private HashMap<String, Object> data = new HashMap<>();
    private LoadTask loadTask;

    public static DrawableLoaderUtils getInstance(OnLoadFinishedListener listener) {
        if(drawableLoader == null) {
            drawableLoader = new DrawableLoaderUtils();
        }
        drawableLoader.listener = listener;
        return drawableLoader;
    }

    public Object get(String key) {
        return drawableLoader.data.get(key);
    }

    public void load(Context context, String path) {
        if(loadTask == null || !loadTask.isLoadiing) {//后台任务没有运行,加入loadlist后启动后台任务
            loadTask = new LoadTask(context);
            loadTask.loadList.add(path);
            loadTask.execute();
        }else {//后台正在运行,加入loadlist即可
            loadTask.loadList.add(path);
        }
    }

    private class LoadTask extends AsyncTask<String, Integer, Boolean> {
        private Context context;
        private ArrayList<String> loadList = new ArrayList<>();
        private boolean isLoadiing = false;
        public LoadTask(Context context) {
            this.context = context;
        }
        @Override
        protected Boolean doInBackground(String... params) {
            isLoadiing = true;
            while (loadList.size() != 0) {
                String path = loadList.get(0);
                loadList.remove(0);
                LogUtils.i(WifiP2pHelper.TAG, "DrawableLoaderUtils-->load pic of "+path);
                String s = path.toLowerCase();
                Object obj = null;
                if(s.endsWith(".apk")) { //apk文件
                    obj = ApkUtils.getApkIcon(context, path);
                }else if(s.endsWith(".mp3")) { //音乐文件

                }else if(s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".bmp") || s.endsWith(".gif")) {//图片文件
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 10;
                    Bitmap bitmap = BitmapFactory.decodeFile(path, options);
                    obj = ThumbnailUtils.extractThumbnail(bitmap, 80,
                            80);
                    bitmap.recycle();
                }
                data.put(path, obj);
                if(listener!=null) {
                    listener.onLoadOneFinished(path, obj);
                }
            }
            isLoadiing = false;
            return null;
        }
    }

    public static boolean isNeedToLoadDrawable(String path) {
        String s = path.toLowerCase();
        if(s.endsWith(".apk") || s.endsWith(".jepg") || s.endsWith(".jpg")
                || s.endsWith(".bmp") ||s.endsWith(".gif")) {//需要加载图片的一些文件
            return true;
        }
        return false;
    }

   public interface OnLoadFinishedListener {
        public abstract void onLoadOneFinished(String path, Object obj);
    }
}
