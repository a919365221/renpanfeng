package com.stock.bean;

/**
 * Created by 907703 on 2016/4/28.
 */
public class StockInfo {
    public String stockId;
    public float pb;
    public float pe;
    public float zongguben;
    public float liutonggu;
    public float price;
    public String info;
    public double sanhubi;
    public boolean pass;
    public int gudongrenshu;


    @Override
    public boolean equals(Object o) {
        StockInfo t=null;
        if(o instanceof  StockInfo){
            t = (StockInfo)o;
        }
        if(t!=null){
            return t.stockId.equals(stockId);
        }else{
            return false;
        }

    }
}
