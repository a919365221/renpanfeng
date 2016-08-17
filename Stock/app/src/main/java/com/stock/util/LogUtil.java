package com.stock.util;

import android.util.Log;

/**
 * Created by 907703 on 2016/3/26.
 */
public class LogUtil {

    public static boolean LogON = true;
    public static final String TAG = "stockapp";
    public static void i(String Tag,String msg){
       if(LogON){
           Log.i(TAG, "[" + Tag + "] " + msg);
       }
    }

}
