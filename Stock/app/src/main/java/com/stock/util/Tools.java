package com.stock.util;

import com.stock.bean.StockInfo;
import com.stock.bean.StockOneDay;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2016/8/21.
 */
public class Tools {

    private static String TAG = "Tools";
    public static String browseUrl(String urlStr) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        String data = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setReadTimeout(5000);
            if(conn.getResponseCode() == 200) {
                InputStream inputStream = conn.getInputStream();
                int count = 0;
                while ((count = inputStream.read(buffer)) > 0) {
                    baos.write(buffer, 0, count);
                }
                inputStream.close();
                data = new String(baos.toByteArray(), "gb2312");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }
    public static String browseUrl2(String urlStr) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        String data = null;
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setReadTimeout(5000);
            if(conn.getResponseCode() == 200) {
                InputStream inputStream = conn.getInputStream();
                int count = 0;
                while ((count = inputStream.read(buffer)) > 0) {
                    baos.write(buffer, 0, count);
                }
                inputStream.close();
                data = new String(baos.toByteArray(), "utf-8");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }
    //<td>股东人数（户）</td><td>17212</td><td>12990</td>
    static Pattern sanhubiPattern = Pattern.compile("<td>股东人数（户）</td><td>(.+?)</td>");
    public static void getStockSanhubi(StockInfo stockInfo){
        String path = "http://eq.10jqka.com.cn/newf10/f10JgccGdrs.php?code=%s";
        String urlStr = String.format(path,stockInfo.stockId);
        try {
            String data = Tools.browseUrl2(urlStr);
            //LogUtil.i(TAG,"股票：renpanfeng"+stockInfo.stockId+data.length());
            Matcher matcher = sanhubiPattern.matcher(data);
            boolean isFind = matcher.find();
            //LogUtil.i(TAG,"股票："+stockInfo.stockId+",xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
            while(isFind){

                String gudongshu = matcher.group(1);
                int iGudongshu = Integer.parseInt(gudongshu);
                LogUtil.i(TAG,"股票："+stockInfo.stockId+",股东人数"+iGudongshu);
                stockInfo.sanhubi = stockInfo.liutonggu/(iGudongshu*1.0/10000);
                stockInfo.gudongrenshu = iGudongshu;
                LogUtil.i(TAG,"股票："+stockInfo.stockId+",总股本/股东人数"+stockInfo.sanhubi);
                break;
            }
        } catch (Exception e) {

            LogUtil.i(TAG,"getStockSanhubi null:"+stockInfo.stockId );

            e.printStackTrace();
        }
    }
}
