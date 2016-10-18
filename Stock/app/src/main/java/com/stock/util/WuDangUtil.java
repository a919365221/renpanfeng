package com.stock.util;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.stock.bean.CDGStock;
import com.stock.bean.StockInfo;
import com.stock.bean.StockWuDang;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2016/8/21.
 */
public class WuDangUtil {
    private Handler handler;
    public HashMap<String,StockWuDang> allStocks;
    private ExecutorService executorService;
    private String TAG = "WuDangUtil";
    private String wuDangPath = "http://d.10jqka.com.cn/v2/fiverange/hs_%s/last.js";
    private int months = 10;
    private HashMap<String,Integer> hashMap = new HashMap();
    public WuDangUtil(Handler handler){
        this.handler = handler;
        allStocks = new HashMap<>();
        hashMap.put("24",5);
        hashMap.put("25",5);

        hashMap.put("26",6);
        hashMap.put("27",6);

        hashMap.put("28",7);
        hashMap.put("29",7);

        hashMap.put("150",8);
        hashMap.put("151",8);

        hashMap.put("154",9);
        hashMap.put("155",9);

        hashMap.put("30",4);
        hashMap.put("31",4);

        hashMap.put("32",3);
        hashMap.put("33",3);

        hashMap.put("34",2);
        hashMap.put("35",2);

        hashMap.put("152",1);
        hashMap.put("153",1);

        hashMap.put("156",0);
        hashMap.put("157",0);

    }
    public void start(){
        getAllStocks();
        //CDGStock cdgStock = new CDGStock();
        //cdgStock.stockName = "建研集团";
        //allStocks.put("002398",cdgStock);
        Message message= handler.obtainMessage(1);
        message.arg1 = allStocks.size();
        handler.sendMessage(message);
        int threads = Runtime.getRuntime().availableProcessors();
        LogUtil.i(TAG,"threads:"+threads);
        executorService = Executors.newFixedThreadPool(threads*2);
        Iterator<String> stockids = allStocks.keySet().iterator();
        while(stockids.hasNext()){
            String stock = stockids.next();
            executorService.execute(new StockRunable(stock));
        }

    }
    public void stop(){
        executorService.shutdown();
    }
    public boolean getAllStocks(){

        //ArrayList<String> allStocks = new ArrayList();
        byte[] buffer = new byte[1024];

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            URL url = new URL(PathInfo.stockCountPath);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            if(conn.getResponseCode() == 200){
                InputStream inputStream = conn.getInputStream();
                int count = 0;
                while((count = inputStream.read(buffer))>0){
                    baos.write(buffer,0,count);
                }
                inputStream.close();
                String data = new String(baos.toByteArray(), "utf-8");
                String jsonData = data.substring(data.indexOf("{"));
                try {
                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONArray jsonArray = (JSONArray)jsonObject.get("rank");
                    for(int i= 0;i<jsonArray.length();i++){
                        String lineStock = jsonArray.getString(i);
                        int start  = lineStock.indexOf(",")+1;
                        StockWuDang stockWuDang = new StockWuDang();
                        String []tmp = lineStock.split(",");
                        stockWuDang.stockName = tmp[2];
                        stockWuDang.stockInfo.stockId = tmp[1];
                        allStocks.put(tmp[1],stockWuDang);
                        //llStocks.add(lineStock.substring(start, start + 6));

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    private class StockRunable implements Runnable{

        private String stockId;

        public StockRunable(String stockId) {
            this.stockId = stockId;
        }

        @Override
        public void run() {
           //获取数据
            StockWuDang datas =  readStock2(stockId);
            boolean pass = false;
            //分析k线
            if(datas.dataValid == false){
                LogUtil.i(TAG,"股票"+stockId+"数据无效");

            }else{
               pass = analyzeOneStock(datas,stockId);
            }

            int messageId = (int)Thread.currentThread().getId()%10;
            Message message= handler.obtainMessage(10+messageId);
            LogUtil.i(TAG,"发送message"+message);
            message.arg1 = Integer.valueOf(stockId);
            if(pass){
                message.arg2 = 1;
            }else{
                message.arg2 = 0;
            }
            handler.sendMessage(message);
        }
    }

    private boolean analyzeOneStock(StockWuDang datas, String stock){
        if(datas == null){
            LogUtil.i(TAG,"股票："+stock+"，五档数据异常");
            return false;
        }else{
            return wuDangGraph(datas,stock);
        }


    }
    private boolean wuDangGraph(StockWuDang datas,String stock){
        int m = 0;

        StockInfo tmp = datas.stockInfo;
        getStockInfo(tmp);

        if((tmp.liutonggu*tmp.price<70)&& tmp.pb>0  && tmp.pe>0 && tmp.pb<7){// tmp.price<20 && tmp.liutonggu<6&& tmp.pb< 4 && tmp.pb>0  && tmp.pe>0
            Tools.getStockSanhubi(tmp);
            /*if(tmp.sanhubi>=1.5){
                return true;
            }else{
                return false;
            }*/

            int allguadanshuliang = 0;
            for(int i=0;i<datas.cs10.length;i++){
                allguadanshuliang += datas.cs10[i];
            }
            allguadanshuliang  = allguadanshuliang/100;
            LogUtil.i(TAG,"股票"+stock+"10档共有"+allguadanshuliang+"手挂单");
            //10挡股数除以10000.比如10000手，会得到1.猜测6亿流通股有1万手挂单的话，这是垃圾股。
            //认为 6亿流通股 <= 0.2挂单.
            if(allguadanshuliang == 0){
                return false;
            }
            float o  = allguadanshuliang/(10000*1.0f);
            LogUtil.i(TAG,"股票比："+tmp.liutonggu/o);
            if(tmp.liutonggu/o>40){//6亿流通股 平均时刻每当小于200手挂单。
                return true;
            }
        }
        return false;


    }
    private StockWuDang readStock2(String stockId)
    {
        StockWuDang stockWuDang = allStocks.get(stockId);
        String jsonData = "";
        if (stockId.charAt(0) == '3')
            return stockWuDang;
        if (true)
        {
            jsonData = Tools.browseUrl(String.format(this.wuDangPath, stockId));
            if(jsonData == null){
                LogUtil.i(TAG, "查询股票::" + stockId+"读网络数据超时");
                return stockWuDang;
            }
            int bg = jsonData.indexOf("({")+1;
            int end = jsonData.lastIndexOf("})")+1;
            jsonData = jsonData.substring(bg,end);
            try
            {

                JSONObject local1 = (JSONObject)new JSONObject(jsonData).get("items");

                Iterator<Map.Entry<String,Integer>> iter = hashMap.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String,Integer> entry =  iter.next();
                    String key = entry.getKey();
                    int val = entry.getValue();
                    float res = 0;
                    String m = local1.getString(key);
                    if(TextUtils.isEmpty(m)){
                        res = 0;
                    }else{
                        res = Float.parseFloat(m);
                    }
                    int tmp = Integer.parseInt(key);
                    if(tmp%2==0){
                        stockWuDang.s10[val] = res;
                    }else{
                        stockWuDang.cs10[val] = res;
                    }
                }

                String p = local1.getString("6");
                if (TextUtils.isEmpty(p)) {
                    stockWuDang.stockInfo.price = 0;
                }else{
                    stockWuDang.stockInfo.price = Float.parseFloat(p);
                }


                /*Iterator<String> keys = local1.keys();

                String key;
                int i=0;

                if(keys.hasNext()){
                    i++;
                    key = keys.next();
                    if(i==21){
                        stockWuDang.stockInfo.price = Float.parseFloat(local1.getString(key));
                    }else{
                        if(i%2==0){
                            //量 i 2 4
                            String liang = local1.getString(key);
                            if(TextUtils.isEmpty(liang)){
                                stockWuDang.cs10[i/2-1] = 0;
                            }else{
                                stockWuDang.cs10[i/2-1] = Integer.parseInt(liang);
                            }

                        }else{
                            //价 i 1 2
                            String jia = local1.getString(key);
                            if(TextUtils.isEmpty(jia)){
                                stockWuDang.s10[i/2] = 0;
                            }else{
                                stockWuDang.s10[i/2] = Float.parseFloat(jia);
                            }

                        }
                    }
                }
                */

                stockWuDang.dataValid = true;

            }
            catch (Exception localException)
            {
                LogUtil.i(TAG, "查询股票::" + stockId+"readStock2异常");
                localException.printStackTrace();
                return stockWuDang;
            }
            //this.cache.put(stockId, results);
        }

        return stockWuDang;
    }

    public void getStockInfo(StockInfo stockInfo){

        //String path3 ="http://stockpage.10jqka.com.cn/spService/%s/Header/realHeader";
        // String urlStr3 = String.format(path3,stockInfo.stockId);

        //String path = "http://stockpage.10jqka.com.cn/spService/%s/Header/realHeader";
        String path = "http://d.10jqka.com.cn/v2/realhead/hs_%s/last.js";
        String path2 = "http://stockpage.10jqka.com.cn/%s/";
        String urlStr = String.format(path,stockInfo.stockId);
        String urlStr2 = String.format(path2,stockInfo.stockId);
        JSONObject jsonObject = null;
        try {
            String data = Tools.browseUrl(urlStr);

            data = data.substring(data.indexOf("_last(")+6,data.length()-1);
            LogUtil.i(TAG,"data--:"+data);
            jsonObject = new JSONObject(data);
            jsonObject = (JSONObject)jsonObject.get("items");
            String sjl = (String)jsonObject.get("592920");
            String syl = (String)jsonObject.get("2034120");
            //String xj = (String)jsonObject.get("xj");
            //String syl = (String)jsonObject.get("syl");
            LogUtil.i(TAG,"股票："+stockInfo.stockId);
            String others = Tools.browseUrl(urlStr2);
            //<div style=\"display:none\" id=\"indexBasicData\">(.+?)</div>
            Pattern urlPattern = Pattern.compile("<div style=\"display:none\" id=\"indexBasicData\">(.+?)</div>");

            LogUtil.i(TAG,"股票："+stockInfo.stockId+",others:len"+others.length());
            LogUtil.i(TAG,"股票："+stockInfo.stockId+",others:indexBasicData"+others.indexOf("indexBasicData"));
            Matcher matcher = urlPattern.matcher(others);
            boolean isFind = matcher.find();
            while(isFind){
                LogUtil.i(TAG,"股票："+stockInfo.stockId+",isFind"+isFind);
                String indexBasicData = matcher.group(1);//总股本 流通股  净资产
                LogUtil.i(TAG,"股票："+stockInfo.stockId+",indexBasicData:"+indexBasicData);
                indexBasicData = indexBasicData.replace('|',',');
                String[] mm = indexBasicData.split(",");
                LogUtil.i(TAG,"股票："+stockInfo.stockId+",mm:"+mm+",mm.len:"+mm.length);

                stockInfo.liutonggu = Float.parseFloat(mm[1]);
                stockInfo.zongguben = Float.parseFloat(mm[0]);
                //float price = Float.parseFloat(xj);
                float price = 0.0f;
                float jingzichan =  Float.parseFloat(mm[2]);
                price = stockInfo.price;
                LogUtil.i(TAG,"股票："+stockInfo.stockId+",股价:"+price+",净资产:"+jingzichan+",流通股"+stockInfo.liutonggu+"亿"+",总股本"+stockInfo.zongguben);
                stockInfo.pb = price/jingzichan;
                if(TextUtils.isEmpty(sjl)){
                    stockInfo.pb = -1;
                }else{
                    stockInfo.pb = Float.parseFloat(sjl);
                }

                break;
            }
            if(syl.equals("")){
                stockInfo.pe = 0;
            }else{
                stockInfo.pe = Float.parseFloat(syl);
            }

            LogUtil.i(TAG,"股票："+stockInfo.stockId+",市盈率:"+stockInfo.pe);
            LogUtil.i(TAG,"股票："+stockInfo.stockId+",市净率:"+stockInfo.pb);
        } catch (Exception e) {

            LogUtil.i(TAG,"getStockInfo null:"+stockInfo.stockId );

            e.printStackTrace();
        }

    }
}
