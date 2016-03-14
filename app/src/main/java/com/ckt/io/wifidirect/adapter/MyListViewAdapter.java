package com.ckt.io.wifidirect.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.ckt.io.wifidirect.R;

import java.util.ArrayList;

/**
 * Created by admin on 2016/3/8.
 */
public class MyListViewAdapter extends BaseAdapter {
    private ArrayList<String> mNameList;
    private ArrayList<String> mPathList;
    private ArrayList<Drawable> mIconList;
    private ArrayList<Boolean> mCheckBoxList;
    private LayoutInflater inflater;
    private Context mContext;

    public MyListViewAdapter() {
        super();
    }

    public MyListViewAdapter(Context context, ArrayList<String> nameList, ArrayList<Drawable> iconList, ArrayList<Boolean> checkBoxList) {
        super();
        mNameList = nameList;
        mIconList = iconList;
        mContext = context;
        mCheckBoxList = checkBoxList;
        inflater = LayoutInflater.from(mContext);
    }

    public ArrayList<Boolean> getmCheckBoxList() {
        return this.mCheckBoxList;
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
            convertView = inflater.inflate(R.layout.listview_item, null);
            viewTag = new ItemViewTag((ImageView) convertView.findViewById(R.id.list_item_Image),
                    (TextView) convertView.findViewById(R.id.list_item_Title), (CheckBox) convertView.findViewById(R.id.list_item_checkbox));
            convertView.setTag(viewTag);
        } else {
            viewTag = (ItemViewTag) convertView.getTag();
        }
        viewTag.mName.setText(mNameList.get(position));
        viewTag.mIcon.setImageDrawable(mIconList.get(position));
        viewTag.mCheckBox.setChecked(mCheckBoxList.get(position));
        viewTag.mCheckBox.setVisibility(mCheckBoxList.get(position) ? View.VISIBLE : View.GONE);
        return convertView;
    }

    public class ItemViewTag {
        public ImageView mIcon;
        public TextView mName;
        public CheckBox mCheckBox;

        public ItemViewTag(ImageView icon, TextView name, CheckBox checkBox) {
            mName = name;
            mIcon = icon;
            mCheckBox = checkBox;
        }
    }
}
