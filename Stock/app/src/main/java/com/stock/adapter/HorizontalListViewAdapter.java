package com.stock.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.stock.bean.BKInfo;
import com.stock.bean.BKInfoUI;
import com.stock.view.VerticalTextView;

import java.util.ArrayList;

public class HorizontalListViewAdapter extends BaseAdapter{

    private ArrayList<BKInfoUI> datas;
    private Context mContext;
    private LayoutInflater mInflater;
    Bitmap iconBitmap;
    private int selectIndex = -1;

    public HorizontalListViewAdapter(Context context, ArrayList<BKInfoUI> datas){
        this.mContext = context;
        this.datas = datas;
        mInflater=(LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);//LayoutInflater.from(mContext);
    }
    @Override
    public int getCount() {
        return datas.size();
    }
    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {


        if(convertView==null){

            VerticalTextView verticalTextView = new VerticalTextView(mContext);
            convertView = verticalTextView;

        }else{

        }
        VerticalTextView v = (VerticalTextView)convertView;
        BKInfoUI bkInfoUI = datas.get(position);
        v.text = bkInfoUI.name;
        v.len = bkInfoUI.score;
        v.max = bkInfoUI.max;
        v.invalidate();
        return convertView;
    }
}