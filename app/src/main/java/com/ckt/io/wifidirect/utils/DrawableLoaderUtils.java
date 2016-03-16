package com.ckt.io.wifidirect.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.util.Log;

import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by admin on 2016/3/15.
 */
public class DrawableLoaderUtils {
    private OnLoadFinishedListener listener;
    private static HashMap<String, Object> data;
    private LoadTask loadTask;//用来在后台加载图片

    private DrawableLoaderUtils(){}

    public static DrawableLoaderUtils getInstance(OnLoadFinishedListener listener) {
        DrawableLoaderUtils drawableLoader = new DrawableLoaderUtils();
        drawableLoader.listener = listener;
        return drawableLoader;
    }

    public Object get(String key) {
        return data.get(key);
    }

    public void load(Context context, String path) {
        if(data == null) {
            data = new HashMap<>();
        }
        if(data.keySet().contains(path)) {
            LogUtils.i(WifiP2pHelper.TAG, "DrawableLoaderUtils-->load() warning:"+path+" has been loaded before");
            return;
        }
        if(loadTask == null || !loadTask.isLoadiing) {//后台任务没有运行,加入loadlist后启动后台任务
            loadTask = new LoadTask(context);
            loadTask.loadList.add(path);
            loadTask.execute();
        }else {//后台正在运行,加入loadlist即可
            loadTask.loadList.add(path);
        }
    }

    private class LoadTask extends AsyncTask<String, Object, Boolean> {
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
                    obj = AudioUtils.getMusicBitpMap(path);
                }else if(s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".bmp") || s.endsWith(".gif") || s.endsWith(".png")) {//图片文件
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 10;
                    Bitmap bitmap = BitmapFactory.decodeFile(path, options);
                    obj = ThumbnailUtils.extractThumbnail(bitmap, 80,
                            80);
                    bitmap.recycle();
                }else if(s.endsWith(".3gp") || s.endsWith(".mp4") || s.endsWith(".rmvb")) { //视频文件
                    obj = new BitmapDrawable(GetVideoThumbnail.getVideoThumbnailTool(path));
                }

                data.put(path, obj);

                publishProgress(path, obj);
            }
            isLoadiing = false;
            return null;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            if(listener!=null) {
                listener.onLoadOneFinished((String) values[0], values[1], loadList.size() == 0);
            }
        }
    }

    //判断文件是否需要加载图片
    public static boolean isNeedToLoadDrawable(String path) {
        String s = path.toLowerCase();
        if(s.endsWith(".apk") || s.endsWith(".jepg") || s.endsWith(".jpg") || s.endsWith(".mp3")
                || s.endsWith(".bmp") ||s.endsWith(".gif") || s.endsWith(".png")
                || s.endsWith(".mp4") ||s.endsWith(".rmvb") ||s.endsWith(".3gp")) {//需要加载图片的一些文件
            return true;
        }
        return false;
    }

    public static void release() {
        data = null;
    }

    public interface OnLoadFinishedListener {
        public abstract void onLoadOneFinished(String path, Object obj, boolean isAllFinished);
    }
}
