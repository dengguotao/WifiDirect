package com.ckt.io.wifidirect.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pManager;
import android.support.v4.app.Fragment;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;


import com.ckt.io.wifidirect.MainActivity;
import com.ckt.io.wifidirect.R;
import com.ckt.io.wifidirect.adapter.MyFragmentAdapter;
import com.ckt.io.wifidirect.myViews.SendFileListPopWin;
import com.ckt.io.wifidirect.p2p.WifiP2pHelper;
import com.ckt.io.wifidirect.utils.SdcardUtils;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by ckt on 2/29/16.
 */
public class ContentFragment extends Fragment implements View.OnClickListener, MainActivity.OnSendFileListChangeListener{
    private ViewGroup mButtomFunViewGroup;
    private ViewGroup mButtomFun_fileNum;
    private ViewGroup mButtomFun_send;
    private ViewGroup mButtomFun_clear;
    private TextView txt_sendFileNum;
    private ImageView img_connect; //the right conner "send"
    private ViewPager mPager;
    private ArrayList<Fragment> fragmentArrayList;
    private ImageView image;
    private int viewPagerTitleIds [] = {R.id.id_application_title, R.id.id_music_title, R.id.id_movie_title, R.id.id_file_title};
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
        txt_sendFileNum = (TextView) view.findViewById(R.id.txt_fileNum);
        mButtomFunViewGroup = (ViewGroup) view.findViewById(R.id.lin_send_fun);
        mButtomFun_fileNum = (ViewGroup) view.findViewById(R.id.lin_fun_fileNum);
        mButtomFun_send = (ViewGroup) view.findViewById(R.id.lin_fun_sendFile);
        mButtomFun_clear = (ViewGroup) view.findViewById(R.id.lin_fun_clear);
        mButtomFun_fileNum.setOnClickListener(this);
        mButtomFun_send.setOnClickListener(this);
        mButtomFun_clear.setOnClickListener(this);
        img_connect = (ImageView) view.findViewById(R.id.img_connect);
        img_connect.setOnClickListener(this);
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

        DisplayMetrics dm = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenW = dm.widthPixels;
        offset = 0;
        ViewGroup.LayoutParams lp = image.getLayoutParams();
        lp.width = screenW/viewPagerTitleIds.length;
        bmpW = lp.width;
        image.setLayoutParams(lp);
        /*Matrix matrix = new Matrix();
        matrix.preTranslate(offset, 0);
        image.setImageMatrix(matrix);*/
    }

    public void InitViewPager() {
        mPager = (ViewPager) view.findViewById(R.id.id_view_pager);
        fragmentArrayList = new ArrayList<>();
        ApplicationFragment applicationFragment = new ApplicationFragment();
        MusicFragment musicFragment = new MusicFragment();
        MovieFragment movieFragment = new MovieFragment();
        mFileFragment = new FileExplorerFragment();
        mDeviceChooseFragment = new DeviceChooseFragment();

        fragmentArrayList.add(applicationFragment);
        fragmentArrayList.add(musicFragment);
        fragmentArrayList.add(movieFragment);
        fragmentArrayList.add(mFileFragment);

        MainActivity activity = (MainActivity) getActivity();
        mPager.setAdapter(new MyFragmentAdapter(activity.getSupportFragmentManager(), fragmentArrayList));
        mPager.setCurrentItem(0);
        mPager.setOnPageChangeListener(new MyOnPageChangeListener());
    }

    public void toggleButtomFun() {
        if(this.mButtomFunViewGroup.getVisibility() == View.VISIBLE) {
            toggleButtomFun(false);
        }else {
            toggleButtomFun(true);
        }
    }

    public void toggleButtomFun(boolean isShow) {
        if(isShow) {//show
            if(this.mButtomFunViewGroup.getVisibility() != View.VISIBLE) {
                mButtomFunViewGroup.setVisibility(View.VISIBLE);
                TranslateAnimation animation = new TranslateAnimation(0, 0, mButtomFunViewGroup.getHeight(), 0);
                animation.setDuration(200);
                animation.setInterpolator(new AccelerateInterpolator());
                mButtomFunViewGroup.startAnimation(animation);
                ScaleAnimation scaleAnimation = new ScaleAnimation(1, 0, 1, 0, img_connect.getWidth()/2, img_connect.getHeight()/2);
                scaleAnimation.setDuration(200);
                scaleAnimation.setInterpolator(new AccelerateInterpolator());
                img_connect.startAnimation(scaleAnimation);
                img_connect.setVisibility(View.GONE);
            }
        }else {//hide
            if(this.mButtomFunViewGroup.getVisibility() == View.VISIBLE) {

                TranslateAnimation animation = new TranslateAnimation(0, 0, 0, mButtomFunViewGroup.getHeight());
                animation.setDuration(200);
                animation.setInterpolator(new AccelerateInterpolator());
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mButtomFunViewGroup.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                mButtomFunViewGroup.startAnimation(animation);
                img_connect.setVisibility(View.VISIBLE);
                ScaleAnimation scaleAnimation = new ScaleAnimation(0, 1, 0, 1, img_connect.getWidth()/2, img_connect.getHeight()/2);
                scaleAnimation.setDuration(200);
                scaleAnimation.setInterpolator(new AccelerateInterpolator());
                img_connect.startAnimation(scaleAnimation);

            }
        }
    }

    @Override
    public void onClick(View v) {
        final MainActivity activity = (MainActivity) getActivity();
        switch (v.getId()) {
            case R.id.img_connect:
                activity.getDeviceConnectDialog().show();
                break;
            case R.id.lin_fun_fileNum: //click the fileNum view--->show a popwin to list all selected files to send
                new SendFileListPopWin(false, activity.getResources().getString(R.string.delete_sendfilelist_popwin_title), activity, activity.getSendFiles(), new SendFileListPopWin.IOnOkListener() {
                    @Override
                    public void onOk(ArrayList<String> seleckedList, boolean isAdd) {
                        activity.removeFileFromSendFileList(seleckedList);
                    }
                }).showAtLocation(this.image, Gravity.CENTER, 0, 0);
                break;
            case R.id.lin_fun_sendFile: //send the selected files
                WifiP2pHelper wifiP2pHelper = activity.getWifiP2pHelper();
                if(wifiP2pHelper.isConnected()) {
                    Log.d(WifiP2pHelper.TAG, "trying send files");
                    ArrayList<File> list = new ArrayList<>();
                    for(int i=0; i<activity.getSendFiles().size(); i++) {
                        list.add(new File(activity.getSendFiles().get(i)));
                    }
                    wifiP2pHelper.sendFiles(list);
                    activity.clearSendFileList();
                }else {
                    activity.getDeviceConnectDialog().show();
                }
                break;
            case R.id.lin_fun_clear: //clear all sendFile list
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.clear_sendfilelist_alert_title)
                        .setMessage(R.string.clear_sendfilelist_alert_content)
                        .setPositiveButton(R.string.clear_sendfilelist_alert_button_sure, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.clearSendFileList();
                            }
                        })
                        .setNegativeButton(R.string.clear_sendfilelist_alert_button_cancel, null)
                        .show();
                break;
        }
    }

    //the others has changed the sendFile-List, do update views here
    //this listen was seted in MainActivity
    @Override
    public void onSendFileListChange(ArrayList<String> sendFiles, int num) {
        txt_sendFileNum.setText(""+num);
        if(num == 0) {
            toggleButtomFun(false);
        }else {
            toggleButtomFun(true);
        }
        for(int i=0; i<fragmentArrayList.size(); i++) {
            Object obj = fragmentArrayList.get(i);
            if(obj instanceof MainActivity.OnSendFileListChangeListener) {
                ((MainActivity.OnSendFileListChangeListener)obj).onSendFileListChange(sendFiles, num);
            }
        }
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

    public boolean isNowFileExplorerFragment() {
        return fragmentArrayList.get(mPager.getCurrentItem()) instanceof FileExplorerFragment;
    }

    public FileExplorerFragment getFileExplorerFragment() {
        return mFileFragment;
    }
}
