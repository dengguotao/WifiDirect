package com.ckt.io.wifidirect.fragment;

import android.annotation.TargetApi;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.adapter.MyGridViewAdapter;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;
import com.ckt.io.wifidirect.utils.DrawableLoaderUtils;
import com.ckt.io.wifidirect.utils.GetVideoThumbnail;
import com.ckt.io.wifidirect.utils.Movie;
import com.ckt.io.wifidirect.utils.MovieUtils;

import java.util.ArrayList;


/**
 * Created by ckt on 2/29/16.
 */
public class MovieFragment extends Fragment implements View.OnClickListener,
        AdapterView.OnItemClickListener,
        MainActivity.OnSendFileListChangeListener,
        DrawableLoaderUtils.OnLoadFinishedListener {
    private ArrayList<Movie> movieList = new ArrayList<>();
    private ArrayList<String> nameList = new ArrayList<>();
    private ArrayList<String> mPathList = new ArrayList<>();
    private ArrayList<Object> iconList = new ArrayList<>();
    private ArrayList<Boolean> checkBoxList = new ArrayList<>();
    private GridView gridView;
    private TextView refresh;
    private MyGridViewAdapter adapterMovie;
    private DrawableLoaderUtils drawableLoaderUtils;

    //用来还原gridview的位置
    private int gridViewState_pos = 0;

    public static final int LOAD_DATA_FINISHED = 0;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOAD_DATA_FINISHED:
                    gridView.setAdapter(adapterMovie);
                    gridView.setSelection(gridViewState_pos);
                    if (drawableLoaderUtils != null) {
                        for (int i = 0; i < nameList.size(); i++) {
                            //启动异步任务加载图片,加载完成一个图片后会调用onLoadOneFinished
                            drawableLoaderUtils.load(getContext(), mPathList.get(i));
                        }
                    }
                    ((BaseAdapter)(gridView.getAdapter())).notifyDataSetChanged();
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(WifiP2pHelper.TAG, "MovieFragment-->onCreateView");
        View view = inflater.inflate(R.layout.movie_layout, container, false);
        refresh = (TextView) view.findViewById(R.id.id_movie_refresh);
        gridView = (GridView) view.findViewById(R.id.id_movie_grid_view);
        refresh.setOnClickListener(this);
        if (adapterMovie == null) { //第一次加载view
            drawableLoaderUtils = DrawableLoaderUtils.getInstance(this);//获取图片加载器的实例
            loadData();
        }else {
            handler.sendEmptyMessage(LOAD_DATA_FINISHED);
        }
        gridView.setOnItemClickListener(this);
        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) { //stop
                    gridView.setTag(true);
                    ((BaseAdapter) gridView.getAdapter()).notifyDataSetChanged();
                } else { //scrolling
                    gridView.setTag(false);
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });
        return view;
    }

    public void loadData() {
        new Thread() {
            @Override
            public void run() {
                movieList = MovieUtils.getAllMovies(getActivity());
                for (Movie movie : movieList) {
                    nameList.add(movie.getTitle());
//                    iconList.add(new BitmapDrawable(GetVideoThumbnail.getVideoThumbnailTool(movie.getFileUrl())));
                    iconList.add(R.drawable.film_icon);
                    checkBoxList.add(false);
                    mPathList.add(movie.getFileUrl());
                }
                adapterMovie = new MyGridViewAdapter(getActivity(), nameList, iconList, checkBoxList, 100);
                handler.sendEmptyMessage(LOAD_DATA_FINISHED);
            }
        }.start();

    }

    @Override
    public void onPause() {
        super.onPause();
        gridViewState_pos = gridView.getFirstVisiblePosition();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_movie_refresh:
                MediaScannerConnection.scanFile(getActivity(), new String[]{Environment
                        .getExternalStorageDirectory().getAbsolutePath()}, null, null);
                if (movieList != null) {
                    movieList.clear();
                    nameList.clear();
                    iconList.clear();
                }
                movieList = MovieUtils.getAllMovies(getActivity());
                for (Movie movie : movieList) {
                    nameList.add(movie.getTitle());
                    iconList.add(new BitmapDrawable(GetVideoThumbnail.getVideoThumbnailTool(movie.getFileUrl())));
                }
                adapterMovie.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        checkBoxList = adapterMovie.getmCheckBoxList();
        checkBoxList.set(position, !checkBoxList.get(position));
        MainActivity activity = (MainActivity) getActivity();
        if (checkBoxList.get(position)) {//checked--->add to sendfile-list
            activity.addFileToSendFileList(mPathList.get(position));
        } else {//unchecked--->remove from sendfile-list
            activity.removeFileFromSendFileList(mPathList.get(position));
        }
        adapterMovie.notifyDataSetChanged();
    }

    @Override
    public void onSendFileListChange(ArrayList<String> sendFiles, int num) {
        if (adapterMovie != null) {
            ArrayList<Boolean> checkList = adapterMovie.getmCheckBoxList();
            String data = sendFiles.toString();
            Log.d(WifiP2pHelper.TAG, data);
            for (int i = 0; i < checkList.size(); i++) {
                String temp = mPathList.get(i);
                if (data.contains(temp)) {
                    checkList.set(i, true);
                } else {
                    checkList.set(i, false);
                }
            }
            adapterMovie.notifyDataSetChanged();
        }
    }

    //加载完path文件的图片
    @Override
    public void onLoadOneFinished(String path, Object obj, boolean isAllFinished) {
        int index = mPathList.indexOf(path);
        if (index >= 0) {
            iconList.set(index, obj);
        }
        if (gridView.getTag() == null || !(boolean) gridView.getTag()) { //gridview没有滑动
            ((BaseAdapter) (gridView.getAdapter())).notifyDataSetChanged();
        }
    }
}
