package com.ckt.io.wifidirect.fragment;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ckt.io.wifidirect.R;


/**
 * Created by ckt on 2/29/16.
 */
public class MovieFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.movie_layout, container, false);
        return view;
    }
}
