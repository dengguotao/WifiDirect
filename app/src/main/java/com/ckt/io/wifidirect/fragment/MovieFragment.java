package com.ckt.io.wifidirect.fragment;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.adapter.MyGridViewAdapterMovie;
import com.ckt.io.wifidirect.utils.GetVideoThumbnail;
import com.ckt.io.wifidirect.utils.Movie;
import com.ckt.io.wifidirect.utils.MovieUtils;

import java.util.ArrayList;


/**
 * Created by ckt on 2/29/16.
 */
public class MovieFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemClickListener {
    private ArrayList<Movie> movieList;
    private ArrayList<String> nameList;
    private ArrayList<String> mPathList;
    private ArrayList<Drawable> iconList;
    private ArrayList<Boolean> checkBoxList;
    private GridView gridView;
    private TextView refresh;
    MyGridViewAdapterMovie adapterMovie;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.movie_layout, container, false);
        movieList = new ArrayList<>();
        nameList = new ArrayList<>();
        iconList = new ArrayList<>();
        mPathList = new ArrayList<>();
        checkBoxList = new ArrayList<>();
        refresh = (TextView) view.findViewById(R.id.id_movie_refresh);
        gridView = (GridView) view.findViewById(R.id.id_movie_grid_view);
        movieList = MovieUtils.getAllMovies(getActivity());

        refresh.setOnClickListener(this);
        for (Movie movie : movieList) {
            nameList.add(movie.getTitle());
            iconList.add(new BitmapDrawable(GetVideoThumbnail.getVideoThumbnailTool(movie.getFileUrl())));
            checkBoxList.add(false);
            mPathList.add(movie.getFileUrl());
        }
        adapterMovie = new MyGridViewAdapterMovie(getActivity(), nameList, iconList, checkBoxList);
        gridView.setAdapter(adapterMovie);
        gridView.setOnItemClickListener(this);
        return view;
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
        if(checkBoxList.get(position)) {//checked--->add to sendfile-list
            activity.addFileToSendFileList(mPathList.get(position));
        }else {//unchecked--->remove from sendfile-list
            activity.removeFileFromSendFileList(mPathList.get(position));
        }
        adapterMovie.notifyDataSetChanged();
    }
}
