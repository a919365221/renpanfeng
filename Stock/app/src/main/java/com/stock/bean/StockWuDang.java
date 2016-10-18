package com.stock.bean;

/**
 * Created by Administrator on 2016/10/18.
 */
public class StockWuDang {

    public float[] s10 = new float[10];//卖5 卖4  。。卖5
    public float[] cs10 = new float[10]; //挂单量
    public StockInfo stockInfo = new StockInfo();
    public String stockName;
    public boolean dataValid = false;
}
