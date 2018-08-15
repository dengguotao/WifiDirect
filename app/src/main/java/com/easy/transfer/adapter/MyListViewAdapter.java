package com.easy.transfer.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.easy.transfer.R;
import com.easy.transfer.utils.FileResLoaderUtils;
import com.easy.transfer.utils.FileTypeUtils;
import com.easy.transfer.utils.SdcardUtils;

import java.io.File;
import java.util.ArrayList;

public class MyListViewAdapter extends BaseAdapter{
    private ArrayList<Boolean> mCheckBoxList = new ArrayList<>();
    private ArrayList<String> list = new ArrayList<>();
    private File innerSdFile, externalSDFile;
    private Context context;
    private ArrayList<String> titleList;
    public MyListViewAdapter(Context context, ArrayList<String> fileList) {
        this.context = context;
        if(fileList != null) {
            list = fileList;
            for(int i=0; i<list.size(); i++) {
                mCheckBoxList.add(false);
            }
        }
        innerSdFile = SdcardUtils.getInnerSDcardFile(context);
        externalSDFile = SdcardUtils.getExternalSDcardFile(context);
    }
    public MyListViewAdapter(Context context, ArrayList<String> fileList, ArrayList<String> titleList) {
        this(context, fileList);
        this.titleList = titleList;
    }
    public void setData(ArrayList<String> list, ArrayList<String> titleList, ArrayList<Boolean> checkList) {
        this.list = list;
        this.mCheckBoxList = checkList;
        this.titleList = titleList;
        notifyDataSetChanged();
    }
    public boolean isChecked(int pos) {
        if(pos<0 || pos >= mCheckBoxList.size()) {
            return false;
        }
        return mCheckBoxList.get(pos);
    }
    public void clearChecked() {
        for(int i=0; i<mCheckBoxList.size(); i++) {
            mCheckBoxList.set(i, false);
        }
    }
    public void toggleChecked(int pos) {
        mCheckBoxList.set(pos, !mCheckBoxList.get(pos));
    }

    class ViewHolder {
        ImageView img_icon;
        TextView txt_title;
        TextView txt_info;
        CheckBox checkBox;
    }
    public ArrayList<Boolean> getmCheckBoxList() {
        return  this.mCheckBoxList;
    }
    public ArrayList<String> getList() {
        return this.list;
    }
    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return this.list == null ? 0 : this.list.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return this.list == null ? null :list.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.fragment_file_explorer_listview_item, null);
            ViewHolder holder = new ViewHolder();
            holder.img_icon = (ImageView) convertView
                    .findViewById(R.id.img_icon);
            holder.txt_title = (TextView) convertView
                    .findViewById(R.id.txt_title);
            holder.txt_info = (TextView) convertView
                    .findViewById(R.id.txt_info);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.list_item_checkbox);
            convertView.setTag(holder);
        }
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        String path = list.get(position);
        File tempFile = new File(path);
        if(tempFile.isDirectory()) {
            viewHolder.img_icon.setImageResource(R.drawable.folder_icon);

            if(innerSdFile != null) {
                if(tempFile.getPath().equals(innerSdFile.getPath())) {
                    viewHolder.img_icon.setImageResource(R.drawable.sdcard_icon);
                }
            }
            if(externalSDFile != null) {
                if(tempFile.getPath().equals(externalSDFile.getPath())) {
                    viewHolder.img_icon.setImageResource(R.drawable.sdcard_icon);
                }
            }
        }else {
            if(FileTypeUtils.isNeedToLoadDrawable(tempFile.getPath())) {
                Object object = FileResLoaderUtils.getPic(tempFile.getPath());
                if(object instanceof Drawable) {
                    viewHolder.img_icon.setImageDrawable((Drawable) object);
                }else if(object instanceof Bitmap) {
                    viewHolder.img_icon.setImageBitmap((Bitmap) object);
                }else {
                    int icon_id = FileTypeUtils.getDefaultFileIcon(tempFile.getPath());
                    viewHolder.img_icon.setImageResource(icon_id);
                }
            }else {
                viewHolder.img_icon.setImageResource(R.drawable.file_icon);
            }
        }


        if(titleList!=null) {
            viewHolder.txt_title.setText(titleList.get(position));
        }else {
            viewHolder.txt_title.setText(tempFile.getName());
        }

        if(mCheckBoxList.get(position)) {
            viewHolder.checkBox.setVisibility(View.VISIBLE);
            viewHolder.checkBox.setChecked(true);
        }else {//unchecked
            viewHolder.checkBox.setVisibility(View.GONE);
            viewHolder.checkBox.setChecked(false);
        }

        return convertView;
    }
}