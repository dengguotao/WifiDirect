package com.ckt.io.wifidirect.fragment;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.adapter.MyListViewAdapter;
import com.ckt.io.wifidirect.utils.AudioUtils;
import com.ckt.io.wifidirect.utils.Song;

import java.util.ArrayList;


/**
 * Created by ckt on 2/29/16.
 */
public class MusicFragment extends Fragment implements View.OnClickListener {
    private ArrayList<Song> songList;
    private ArrayList<String> nameList;
    private ArrayList<Drawable> iconList;
    private ListView listView;
    private TextView refresh;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.music_layout, container, false);
        nameList = new ArrayList<>();
        iconList = new ArrayList<>();
        listView = (ListView) view.findViewById(R.id.id_adapter_list_view);
        refresh = (TextView) view.findViewById(R.id.id_music_refresh);
        initData();
        refresh.setOnClickListener(this);
        return view;
    }

    public void initData() {
        MyListViewAdapter adapter;
        if (songList != null) {
            songList.clear();
            nameList.clear();
            iconList.clear();
        }
        songList = AudioUtils.getAllSongs(getActivity());
        for (Song song : songList) {
            nameList.add(song.getTitle());
            iconList.add(new BitmapDrawable(BitmapFactory.decodeResource(getResources(), R.drawable.icon)));
        }
        adapter = new MyListViewAdapter(getActivity(), nameList, iconList);
        listView.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_music_refresh:
                MediaScannerConnection.scanFile(getActivity(), new String[]{Environment
                        .getExternalStorageDirectory().getAbsolutePath()}, null, null);
                initData();
                Log.i("Test", "OnClick");
                break;
            default:
                break;
        }
    }
}
