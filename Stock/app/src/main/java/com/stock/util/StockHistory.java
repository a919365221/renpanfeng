package com.stock.util;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by 907703 on 2016/3/17.
 */
public class StockHistory {
    public StockHistory(String date,ArrayList<String> datas) {
        this.date = date;
        this.data = datas;
    }

    public String date;
    public ArrayList<String> data = new ArrayList<>();
}
