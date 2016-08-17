package com.stock.bean;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by Administrator on 2016/8/13.
 */
public class BKInfo {
    public String name;
    public String code;
    public ArrayList<BKInfoDatePoint> datas = new ArrayList<>();
    public int score;
    public BKInfoDatePoint current;
    public static class BKInfoDatePoint{
        public int date;
        public double currentValue;
        public double zhangdiefu;
    }
    @Override
    public String toString() {
        return "name:"+name+",code:"+code;
    }
}
