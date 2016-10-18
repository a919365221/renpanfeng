package com.stock.bean;

import java.io.Serializable;

import com.stock.util.LogUtil;

/**
 * Created by 907703 on 2016/3/28.
 */
public class StockOneDay implements Serializable{
    public String date;
    public float jiaoyiliang;
    public float kaipanjia;
    public float shoupanjia;
    public float zuidijia;
    public float zuigaojia;
    public float huanshoulv;


    public String toString()
    {
        return "日期:" + this.date + ",开盘价:" + this.kaipanjia + ",收盘价:" + this.shoupanjia + ",最高价:" + this.zuigaojia + ",最低价:" + this.zuidijia + ",交易量:" + this.jiaoyiliang+",换手率:"+huanshoulv;
    }
}
