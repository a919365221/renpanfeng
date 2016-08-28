package com.stock.util;

import android.os.Handler;
import android.os.Message;

import com.stock.bean.CDGStock;

import com.stock.bean.StockInfo;
import com.stock.bean.StockOneDay;
import com.stock.bean.StockOneMonth;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2016/8/21.
 */
public class CDGUtil {
    private Handler handler;
    public HashMap<String,CDGStock> allStocks;
    private ExecutorService executorService;
    private String TAG = "CDGUtil";
    private String monthPath = "http://api.finance.ifeng.com/akmonthly/?code=%s&type=last";
    private int months = 10;
    public CDGUtil(Handler handler){
        this.handler = handler;
        allStocks = new HashMap<>();
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
                        CDGStock cdgStock = new CDGStock();
                        String []tmp = lineStock.split(",");
                        cdgStock.stockName = tmp[2];
                        cdgStock.stockId = tmp[1];
                        allStocks.put(tmp[1],cdgStock);
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
            ArrayList<StockOneMonth> datas =  readStock2(stockId);
            //分析k线
            allStocks.get(stockId).stockOneMonths =datas;
            boolean pass = analyzeOneStock(datas,stockId);
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

    private boolean analyzeOneStock(ArrayList<StockOneMonth> datas, String stock){
        if(datas == null){
            LogUtil.i(TAG,"股票："+stock+"，月线数据异常");
            return false;
        }else{
            return chaodieguGraph(datas,stock);
        }


    }
    private boolean chaodieguGraph(ArrayList<StockOneMonth> datas,String stock){
        int m = 0;
        //找到该股最高价，最低价，当前价
        double max = 0;
        double min = 1000;
        double current = 0;
        for(StockOneMonth stockOneMonth:datas){
            if(stockOneMonth.zuigaojia>max){
                max = stockOneMonth.zuigaojia;
            }
            if(stockOneMonth.zuidijia<min){
                min = stockOneMonth.zuidijia;
            }
        }

        StockOneMonth thisMonth = datas.get(datas.size()-1);
        current = thisMonth.shoupanjia;
        LogUtil.i(TAG,"股票"+stock+",最高价:"+max+",最低价:"+min+"当前价:"+current);
        double off = ((current -min)/min);
        LogUtil.i(TAG,"股票离最低价偏离:"+off);
        if(current >= min && off < 0.15){
            allStocks.get(stock).stockInfo = new StockInfo();
            StockInfo tmp = allStocks.get(stock).stockInfo;
            tmp.stockId = stock;
            getStockInfo(tmp);
            LogUtil.i(TAG,"股票"+stock+",最高价:"+max+",最低价:"+min+"当前价:"+current);
            if(tmp.price<20 && tmp.liutonggu<6&& tmp.pb< 4 && tmp.pb>0  && tmp.pe>0){
                getStockSanhubi(tmp);
                if(tmp.sanhubi>2.9){
                    return true;
                }else{
                    return false;
                }

            }
            return false;
        }else{
            return false;
        }

    }
    private ArrayList<StockOneMonth> readStock2(String stockId)
    {
        String jsonData = "";
        if (stockId.charAt(0) == '3')
            return null;
        ArrayList<StockOneMonth>  results = new ArrayList<>(); //(ArrayList)this.cache.get(stockId);

        String urlPath;
        if (true)
        {
            if (stockId.charAt(0) == '6'){
                urlPath = "sh"+stockId;
            }else{
                urlPath = "sz"+stockId;
            }
            jsonData = Tools.browseUrl(String.format(this.monthPath, urlPath));
            if(jsonData == null){
                LogUtil.i(TAG, "查询股票::" + stockId+"读网络数据超时");
                return null;
            }
            try
            {

                JSONArray localJSONArray1 = (JSONArray)new JSONObject(jsonData).get("record");
                int i = localJSONArray1.length();

                if(i<this.months){
                    return null;
                }


                for(int j = 0; j<this.months;j++){
                    JSONArray localJSONArray2 = localJSONArray1.getJSONArray(j + (i - this.months));
                    StockOneMonth localStockOneMonth = new StockOneMonth();
                    localStockOneMonth.date = localJSONArray2.getString(0);
                    localStockOneMonth.kaipanjia = Float.valueOf(localJSONArray2.getString(1)).floatValue();
                    localStockOneMonth.shoupanjia = Float.valueOf(localJSONArray2.getString(3)).floatValue();
                    localStockOneMonth.zuigaojia = Float.valueOf(localJSONArray2.getString(2)).floatValue();
                    localStockOneMonth.zuidijia = Float.valueOf(localJSONArray2.getString(4)).floatValue();
                    localStockOneMonth.jiaoyiliang = Float.valueOf(localJSONArray2.getString(5)).floatValue();
                    results.add(localStockOneMonth);
                }
            }
            catch (Exception localException)
            {
                LogUtil.i(TAG, "查询股票::" + stockId+"readStock2异常");
                localException.printStackTrace();
                return null;
            }
            //this.cache.put(stockId, results);
        }

        return results;
    }
    //<td>股东人数（户）</td><td>17212</td><td>12990</td>
    Pattern sanhubiPattern = Pattern.compile("<td>股东人数（户）</td><td>(.+?)</td>");
    public void getStockSanhubi(StockInfo stockInfo){
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
                stockInfo.sanhubi = stockInfo.zongguben/(iGudongshu*1.0/10000);
                LogUtil.i(TAG,"股票："+stockInfo.stockId+",总股本/股东人数"+stockInfo.sanhubi);
                break;
            }
        } catch (Exception e) {

        LogUtil.i(TAG,"getStockSanhubi null:"+stockInfo.stockId );

        e.printStackTrace();
    }
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

                CDGStock cdgStock = allStocks.get(stockInfo.stockId);
                StockOneMonth stockOneMonth = cdgStock.stockOneMonths.get(cdgStock.stockOneMonths.size()-1);
                stockInfo.price  = stockOneMonth.shoupanjia;
                float jingzichan =  Float.parseFloat(mm[2]);
                price = stockInfo.price;
                LogUtil.i(TAG,"股票："+stockInfo.stockId+",股价:"+price+",净资产:"+jingzichan+",流通股"+stockInfo.liutonggu+"亿"+",总股本"+stockInfo.zongguben);
                stockInfo.pb = price/jingzichan;
                stockInfo.pb = Float.parseFloat(sjl);
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
