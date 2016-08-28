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
}
