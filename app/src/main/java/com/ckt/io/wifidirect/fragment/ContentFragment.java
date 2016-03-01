package com.ckt.io.wifidirect.fragment;

import android.net.wifi.p2p.WifiP2pManager;
import android.support.v4.app.Fragment;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;


import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.adapter.MyFragmentAdapter;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;
import com.ckt.io.wifidirect.utils.SdcardUtils;

import java.util.ArrayList;

/**
 * Created by ckt on 2/29/16.
 */
public class ContentFragment extends Fragment {
    private ViewPager mPager;
    private ArrayList<Fragment> fragmentArrayList;
    private ImageView image;
    private int viewPagerTitleIds [] = {R.id.id_device_title, R.id.id_application_title, R.id.id_music_title, R.id.id_movie_title, R.id.id_file_title};
    private TextView view_one, view_two, view_three, view_four, view_five;
    private int currIndex;
    private int bmpW;
    private int offset;
    private View view;

    private FileExplorerFragment mFileFragment;



    private DeviceChooseFragment mDeviceChooseFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.content_layout, container, false);
        InitImage();
        InitViewPager();
        InitTextView();
        return view;
    }

    public void InitTextView() {
        for(int i=0; i<viewPagerTitleIds.length; i++) {
            TextView textView = (TextView) view.findViewById(viewPagerTitleIds[i]);
            textView.setOnClickListener(new TextListener(i));
        }
    }

    public void InitImage() {
        image = (ImageView) view.findViewById(R.id.id_cursor);
        bmpW = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher).getWidth();
        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenW = dm.widthPixels;
        offset = (screenW / viewPagerTitleIds.length - bmpW) / 2;
        image.setPadding(offset, 0, 0, 0);

        Matrix matrix = new Matrix();
        matrix.preTranslate(offset, 0);
        image.setImageMatrix(matrix);
    }

    public void InitViewPager() {
        mPager = (ViewPager) view.findViewById(R.id.id_view_pager);
        fragmentArrayList = new ArrayList<>();
        ApplicationFragment applicationFragment = new ApplicationFragment();
        MusicFragment musicFragment = new MusicFragment();
        MovieFragment movieFragment = new MovieFragment();
        mFileFragment = new FileExplorerFragment(SdcardUtils.getInnerSDcardFile(getActivity()));
        mDeviceChooseFragment = new DeviceChooseFragment(((MainActivity)getActivity()).getWifiP2pHelper());

        fragmentArrayList.add(mDeviceChooseFragment);
        fragmentArrayList.add(applicationFragment);
        fragmentArrayList.add(musicFragment);
        fragmentArrayList.add(movieFragment);
        fragmentArrayList.add(mFileFragment);


        MainActivity activity = (MainActivity) getActivity();
        mPager.setAdapter(new MyFragmentAdapter(activity.getSupportFragmentManager(), fragmentArrayList));
        mPager.setCurrentItem(0);
        mPager.setOnPageChangeListener(new MyOnPageChangeListener());
    }

    public class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {
        private int one;
        public MyOnPageChangeListener() {
            one = offset * 2 + bmpW;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            Animation animation = new TranslateAnimation(currIndex * one, position * one, 0, 0);
            currIndex = position;
            animation.setFillAfter(true);
            animation.setDuration(200);
            image.startAnimation(animation);
            int i = currIndex + 1;
            Log.d(WifiP2pHelper.TAG, "onPageSelected()"+position+"  "+one);
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    public class TextListener implements View.OnClickListener {
        private int index = 0;

        public TextListener(int i) {
            index = i;
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            mPager.setCurrentItem(index);
        }
    }


    //setter and getter
    public DeviceChooseFragment getmDeviceChooseFragment() {
        return mDeviceChooseFragment;
    }

    public FileExplorerFragment getFileExplorerFragment() {
        return mFileFragment;
    }
}
