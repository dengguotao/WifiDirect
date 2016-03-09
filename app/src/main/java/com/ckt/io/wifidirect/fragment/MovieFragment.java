package com.ckt.io.wifidirect.fragment;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.adapter.MyGridViewAdapterMovie;
import com.ckt.io.wifidirect.utils.GetVideoThumbnail;
import com.ckt.io.wifidirect.utils.Movie;
import com.ckt.io.wifidirect.utils.MovieUtils;

import java.util.ArrayList;


/**
 * Created by ckt on 2/29/16.
 */
public class MovieFragment extends Fragment {
    private ArrayList<Movie> movieList;
    private ArrayList<String> nameList;
    private ArrayList<Drawable> iconList;
    private ArrayList<Boolean> checkBoxList;
    private GridView gridView;
    MyGridViewAdapterMovie adapterMovie;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.movie_layout, container, false);
        movieList = new ArrayList<>();
        nameList = new ArrayList<>();
        iconList = new ArrayList<>();
        checkBoxList = new ArrayList<>();

        gridView = (GridView) view.findViewById(R.id.id_movie_grid_view);
        movieList = MovieUtils.getAllMovies(getActivity());

        for (Movie movie : movieList) {
            nameList.add(movie.getTitle());
            iconList.add(new BitmapDrawable(GetVideoThumbnail.getVideoThumbnailTool(movie.getFileUrl())));
            checkBoxList.add(false);
        }
        adapterMovie = new MyGridViewAdapterMovie(getActivity(), nameList, iconList, checkBoxList);
        gridView.setAdapter(adapterMovie);
        return view;
    }
}
