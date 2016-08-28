package com.stock.bean;

/**
 * Created by Administrator on 2016/8/21.
 */
public class StockOneMonth {
    public String date;
    public float jiaoyiliang;
    public float kaipanjia;
    public float shoupanjia;
    public float zuidijia;
    public float zuigaojia;


    public String toString()
    {
        return "日期:" + this.date + ",开盘价:" + this.kaipanjia + ",收盘价:" + this.shoupanjia + ",最高价:" + this.zuigaojia + ",最低价:" + this.zuidijia + ",交易量:" + this.jiaoyiliang;
    }
}
