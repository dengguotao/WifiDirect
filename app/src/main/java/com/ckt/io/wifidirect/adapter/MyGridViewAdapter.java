package com.ckt.io.wifidirect.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ckt.io.wifidirect.R;

import java.util.ArrayList;

/**
 * Created by admin on 2016/3/8.
 */
public class MyGridViewAdapter extends BaseAdapter {
    private ArrayList<String> mNameList = new ArrayList<>();
    private ArrayList<Drawable> mIconList = new ArrayList<>();
    private LayoutInflater mInflater;
    private Context mContext;

    public MyGridViewAdapter(Context context, ArrayList<String> nameList, ArrayList<Drawable> iconList) {
        mNameList = nameList;
        mIconList = iconList;
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mNameList.size();
    }

    @Override
    public Object getItem(int position) {
        return mNameList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ItemViewTag viewTag;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.gridview_item, null);
            viewTag = new ItemViewTag((ImageView) convertView.findViewById(R.id.id_grid_view_icon),
                    (TextView) convertView.findViewById(R.id.id_grid_view_name));
            convertView.setTag(viewTag);
        } else {
            viewTag = (ItemViewTag) convertView.getTag();
        }
        viewTag.mName.setText(mNameList.get(position));
        viewTag.mIcon.setImageDrawable(mIconList.get(position));
        return convertView;
    }

    public class ItemViewTag {
        protected ImageView mIcon;
        protected TextView mName;

        public ItemViewTag(ImageView icon, TextView name) {
            mName = name;
            mIcon = icon;
        }
    }
}
