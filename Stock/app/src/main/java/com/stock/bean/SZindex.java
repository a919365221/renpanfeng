package com.stock.bean;

import android.text.format.DateFormat;

import java.text.SimpleDateFormat;

/**
 * Created by 909060 on 16-6-22.
 */
public class SZindex {


    public String date;
    public float bodyLenth;
    public float kaipan;
    public float shoupan;

    @Override
    public String toString() {
        return "date:"+date+",bodyLenth:"+bodyLenth+",kaipan:"+kaipan+",shoupan:"+shoupan;
    }
    public void setData(String date,float bodyLenth,float kaipan,float shoupan){
        this.date = date;
        this.bodyLenth = bodyLenth;
        this.kaipan = kaipan;
        this.shoupan = shoupan;
    }
}
