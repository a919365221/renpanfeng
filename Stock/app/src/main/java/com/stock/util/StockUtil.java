package com.stock.util;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.stock.bean.SZindex;
import com.stock.bean.StockInfo;
import com.stock.bean.StockOneDay;




/**
 * Created by 907703 on 2016/3/12.
 */
public class StockUtil {

    //http://hqdigi2.eastmoney.com/EM_Quote2010NumericApplication/CompatiblePage.aspx?Type=ZT&jsName=js_fav&fav=000545 单只股票信息查询
    private static final String TAG = StockUtil.class.getSimpleName();
    //String path = "http://hqdigi2.eastmoney.com/EM_Quote2010NumericApplication/index.aspx?type=s&sortType=C&sortRule=-1&pageSize=2000&page=1&jsName=quote_123&style=20";
    //String path = "http://nufm.dfcfw.com/EM_Finance2014NumericApplication/JS.aspx?type=CT&cmd=C._A&sty=FCOIATA&sortType=C&sortRule=-1&page=1&pageSize=4000&js=var%20quote_123={rank:[(x)],pages:(pc)}&token=7bc05d0d4c3c22ef9fca8c2a912d779c&jsName=quote_123";
    private int targetCount = 7;
    // http://money.finance.sina.com.cn/corp/go.php/vMS_MarketHistory/stockid/002371.phtml?year=2016&jidu=1
    String stockPath = "http://money.finance.sina.com.cn/corp/go.php/vMS_MarketHistory/stockid/%s.phtml?year=%d&jidu=%d";

    //查询大盘的指数
    String dapanPath = "http://web.ifzq.gtimg.cn/appstock/app/fqkline/get?_var=kline_dayqfq&param=sz399001,day,,,%d,qfq";

    String geGuPath = "http://api.finance.ifeng.com/akdaily/?code=%s&type=last";
    String infoPath = "http://news.gtimg.cn/notice_more.php?q=%s&page=1";
    private HashMap<Integer,Integer> bodong = new HashMap();
    Handler handler;
    private ExecutorService executorService;
    private boolean prepared = false;
    int jidu = 0;//当前季度
    int year = 0;//当前年份
    int days = 10;
    private String selectDate;
    public SimpleCache<String,ArrayList<StockOneDay>> cache ;
    public ArrayList<StockInfo> stocksQush = new ArrayList<StockInfo>();
    public ArrayList<StockInfo> stocksSuoliang = new ArrayList<StockInfo>();
    private String confPath;
    //private String[] excudeKeys   = {"减持","质押","暂停上市","增发","*ST","退市","解禁","重组","预亏","上市流通","限售股","大宗交易"};
    private String[] excudeKeys   = {"暂停上市","退市","预亏","上市流通","限售股","*ST","增发","大宗交易","期权","持股计划"};

    public HashMap<String,String>  stockName;
    private float zhangditing = 0.09f;
    private Object lock = new Object();
    public boolean working  = false;
    private List<StockComment> commentsData = new ArrayList<StockComment>();
    int graph = 0;
    ArrayList<SZindex> SZIndexData;
    public StockUtil(){

    }
    public StockUtil(String path){
        this.confPath   = path;
    }
    public Runnable work = new Runnable() {
        @Override
        public   void run() {

            StockUtil stockUtil = StockUtil.this;
            stockUtil.work();

            try {
                LogUtil.i(TAG, "wait before:");
                synchronized (lock){
                    handler.sendEmptyMessage(5);
                    lock.wait();
                }

                LogUtil.i(TAG, "wait end:");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                executorService.shutdownNow();
                try {
                    executorService.awaitTermination(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Message msg = handler.obtainMessage(4);
                handler.sendMessage(msg);
            }
            working = false;
        }
    };
    //判斷是否是目標股票
    public boolean isTargetStock(String stock){
        return false;
    }
    //分析这些股票，纳入记忆库
    //分析这只股票近期涨跌停 数 跌停与涨停相差 不超过5来排除补跌。 相加得到波动数量作为HashCode的key，值就为这个数的出现次数
    public void analyzeStock(){

        if(stockName==null){
            LogUtil.i(TAG, "stockName null:");
            return;
        }
        LogUtil.i(TAG, "查询股票数量:" + stockName.size());

        /*allStocks.add("002740");
        allStocks.add("000635");
        allStocks.add("002751");
        allStocks.add("002367");
        allStocks.add("002742");
        allStocks = new ArrayList<>();

        allStocks.add("600176");
        allStocks.add("600388");
        allStocks.add("601155");*/

        //allStocks = new ArrayList<>();
        //allStocks.add("002786");
        Message message= handler.obtainMessage(1);
        message.arg1 = stockName.size();
        handler.sendMessage(message);
        /*for(String stock:allStocks){
            LogUtil.i(TAG,"stock:"+stock);
        }

        */
        /*
        *
        */
        int ii;

        Iterator<String> stockids = stockName.keySet().iterator();
        while(stockids.hasNext()){
            String stock = stockids.next();
            executorService.execute(new StockRunable(stock));
        }

    }

    private ArrayList<StockOneDay> readStock(String stock){
        if(stock.charAt(0)=='3'){
            return  null;
        }

        ArrayList<StockOneDay> records = cache.get(stock);
        if(records == null){
            Elements elements = collectStockData(stock, days);
             if(elements.size()<days){
                LogUtil.i(TAG,"here goes?????");
                return null;
            }
            records = putToCatch(stock,elements);
        }
        return records;
    }

    private ArrayList<StockOneDay> readStock2(String stockId)
    {
        String jsonData = "";
        if (stockId.charAt(0) == '3')
            return null;
        ArrayList<StockOneDay>  results = (ArrayList)this.cache.get(stockId);

        String urlPath;
        if (results == null)
        {
            results = new ArrayList<StockOneDay>();
            if (stockId.charAt(0) == '6'){
                urlPath = "sh"+stockId;
            }else{
                urlPath = "sz"+stockId;
            }
            jsonData =Tools.browseUrl(String.format(this.geGuPath, urlPath));
            if(jsonData == null){
                LogUtil.i(TAG, "查询股票::" + stockId+"读网络数据超时");
                return null;
            }
            try
            {

                JSONArray localJSONArray1 = (JSONArray)new JSONObject(jsonData).get("record");
                int i = localJSONArray1.length();

                if(i<this.days){
                    return null;
                }


                for(int j = 0; j<this.days;j++){
                    JSONArray localJSONArray2 = localJSONArray1.getJSONArray(j + (i - this.days));
                    StockOneDay localStockOneDay = new StockOneDay();
                    localStockOneDay.date = localJSONArray2.getString(0);
                    localStockOneDay.kaipanjia = Float.valueOf(localJSONArray2.getString(1)).floatValue();
                    localStockOneDay.shoupanjia = Float.valueOf(localJSONArray2.getString(3)).floatValue();
                    localStockOneDay.zuigaojia = Float.valueOf(localJSONArray2.getString(2)).floatValue();
                    localStockOneDay.zuidijia = Float.valueOf(localJSONArray2.getString(4)).floatValue();
                    localStockOneDay.jiaoyiliang = Float.valueOf(localJSONArray2.getString(5)).floatValue();
                    results.add(localStockOneDay);
                }
            }
            catch (Exception localException)
            {
                LogUtil.i(TAG, "查询股票::" + stockId+"readStock2异常");
                localException.printStackTrace();
                return null;
            }
            this.cache.put(stockId, results);
        }

            return results;
    }
    private class StockRunable implements Runnable{

        private String stockId;

        public StockRunable(String stockId) {
            this.stockId = stockId;
        }

        @Override
        public void run() {
            boolean pass = false;
            LogUtil.i(TAG, "分析：" + stockId);

            //获取数据
            ArrayList datas = readStock2(stockId);
            int[] flags = new int[]{0,0};//0 huangjinkeng 1 suoliang
            if(datas == null){
                pass = false;
            }else{

                pass = analyzeOneStock(datas,flags,stockId);
            }

            /*try {
                //Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            int messageId = (int)Thread.currentThread().getId()%10;
            Message message= handler.obtainMessage(10+messageId);
            LogUtil.i(TAG,"发送message"+message);
            message.arg1 = Integer.valueOf(stockId);
            if(pass){
                if(flags[0]==1){
                    message.arg2 = 1;
                }
                if(flags[1]==1){
                    message.arg2 = 2;
                }
                if(flags[1]==1&& flags[0]==1){
                    message.arg2 = 3;
                }


            }else{
                message.arg2 = 0;
            }
            handler.sendMessage(message);
        }
    }
    /*收集指定天数的数据*/
    private Elements collectStockData(String stock,int days){
        Elements all = new Elements();
        Elements now = getStockData(stock, year, jidu);
        Elements lastJidu = null;
        if(now==null){
            LogUtil.i(TAG,"股票"+stock+"有异常");
            //Message message= handler.obtainMessage(2);
            //message.arg1 = Integer.valueOf(stock);
            //handler.sendMessage(message);
            return all;
        }
        //LogUtil.i(TAG,"当前季度统计天数"+now.size());
        if(now.size()<days){
            int yearT = 0;
            int jiduT = 0;
            if(jidu==1){
                jiduT = 4;
                yearT = year-1;
            }else{
                yearT = year;
                jiduT = jidu-1;
            }
            try{
                lastJidu = getStockData(stock,yearT,jiduT);
                int i = 0;
                int j = lastJidu.size();
                int need = (days-now.size());
                for(;i<need && j>0;i++){
                    now.add(lastJidu.get(i));
                    j--;
                }
                all.addAll(now);
            }catch (Exception e){
                //LogUtil.i(TAG,"stock是新股或者停牌股");
                Message message= handler.obtainMessage(2);
                message.arg1 = Integer.valueOf(stock);
                handler.sendMessage(message);
            }

        }else{
            int i = 0;
            for(i=0;i<days;i++){
                all.add(now.get(i));
            }
        }
        //LogUtil.i(TAG,"all stock"+stock+"数量"+all.size());
        return all;
    }
    private boolean getSZIndexData(){

        String urlDapanPath = String.format(dapanPath, days);
        String data = Tools.browseUrl(urlDapanPath);
        String jsonData = data.substring("kline_dayqfq=".length());
        SZIndexData= new ArrayList<>();
        try{
            JSONObject root = new JSONObject(jsonData);
            JSONObject root_data = (JSONObject)root.get("data");
            JSONObject sz39900 = (JSONObject)root_data.get("sz399001");
            JSONArray dayArray = (JSONArray)sz39900.get("day");

            for(int i= 0;i<dayArray.length();i++){
                SZindex sZindex = new SZindex();
                JSONArray dayData =(JSONArray) dayArray.get(i);
                String date = dayData.getString(0);
                String kaipan = dayData.getString(1);
                String shoupan = dayData.getString(2);
                String zuigao = dayData.getString(3);
                String zuidi = dayData.getString(4);
                String chengjiaoliang = dayData.getString(5);
                float bodylenth = (Float.parseFloat(shoupan)-Float.parseFloat(kaipan))/Float.parseFloat(kaipan);
                sZindex.setData(date,bodylenth,Float.parseFloat(kaipan),Float.parseFloat(shoupan));
                SZIndexData.add(sZindex);
            }
        }catch (JSONException e) {

            e.printStackTrace();
            return false;
        }
        for(SZindex record:SZIndexData){
            LogUtil.i(TAG,record.toString());
        }
        return true;
    }
    private Elements getStockData(String stock,int year,int jidu){

        BufferedReader in = null;
        HttpURLConnection conn = null;
        Elements trs = null;
        String urlStr = String.format(stockPath,stock,year,jidu);
        StringBuilder sb = new StringBuilder();
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String result = null;
            while ((result = in.readLine()) != null) {
                sb.append(result);
            }
            Document doc = Jsoup.parse(sb.toString());
            Element content = doc.getElementById("FundHoldSharesTable");
            if (content != null) {
                Element tableContent = content.getElementsByTag("tbody").get(0);
                trs = tableContent.getElementsByTag("tr");
                trs.remove(0);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            conn.disconnect();
        }
        return trs;
    }

    private ArrayList<StockOneDay> putToCatch(String stock,Elements all){
        ArrayList<StockOneDay> records = new ArrayList<>();
        int trCount = all.size();
        for(int i=0;i<trCount;i++){
            StockOneDay oneDay = new StockOneDay();
            Elements tds = all.get(i).getElementsByTag("td");
            //String date = tds.get(0).getElementsByTag("a").get(0).text();
            oneDay.shoupanjia = Float.valueOf(tds.get(3).getElementsByTag("div").get(0).text()).floatValue();
            oneDay.zuidijia = Float.valueOf(tds.get(4).getElementsByTag("div").get(0).text()).floatValue();
            oneDay.jiaoyiliang = Long.valueOf(tds.get(5).getElementsByTag("div").get(0).text());
            records.add(oneDay);
        }
        //if(!TextUtils.isEmpty(selectDate)){
            cache.put(stock,records);
        //}
        return records;

    }
    private boolean quShiGraph(ArrayList<StockOneDay> datas,String stock){
        int cishu = 0;
        float rise = 0;
        boolean pass = false;

        int trCount = datas.size();
        for(int i=0;i<trCount;i++){
            StockOneDay oneDay = datas.get(i);

            rise = (oneDay.shoupanjia-oneDay.kaipanjia)/oneDay.kaipanjia;
            if(rise>0.0f){
                cishu++;
            }
        }
        if(cishu>=(days-days/3)){
            pass = true;
            LogUtil.i(TAG,"股票："+stock+",具有连续上涨趋势");
        }
        return pass;
    }
    private boolean zhangtingHuitiaoGraph(ArrayList<StockOneDay> datas,String stock){

        float rise = 0;
        boolean pass = false;

        int trCount = datas.size();
        float yesterDayPrice = 0;
        int j = 10000;
        float kaipajia = 0;//涨停那天在开盘价
        for(int i=0;i<trCount;i++){
            StockOneDay oneDay = datas.get(i);

            if(i==0){
                //第一天收盘价作为开始价格
                yesterDayPrice = oneDay.shoupanjia;
            }
            if(i!=0){
                //每天在涨跌幅
                rise = (oneDay.shoupanjia-yesterDayPrice)/yesterDayPrice;
                yesterDayPrice = oneDay.shoupanjia;

                if(rise>0.095f){
                    j = i;
                    kaipajia = oneDay.kaipanjia;
                }

            }
            if((i==(trCount-1) &&i>j && (oneDay.shoupanjia<kaipajia))){
                LogUtil.i(TAG,"股票："+stock+",具有上涨回调图形");
                pass = true;
            }

        }

        return pass;
    }
    private boolean hengpanGraph(ArrayList<StockOneDay> datas,String stock){

        float rise = 0;
        boolean pass = false;

        int trCount = datas.size();
        float yesterDayPrice = 0;
        int count = 0;
        float leiji = 0;
        for(int i=0;i<trCount;i++){
            StockOneDay oneDay = datas.get(i);

            if(i==0){
                //第一天收盘价作为开始价格
                yesterDayPrice = oneDay.shoupanjia;
            }
            if(i!=0){
                //每天在涨跌幅
                rise = (oneDay.shoupanjia-yesterDayPrice)/yesterDayPrice;
                yesterDayPrice = oneDay.shoupanjia;
                if(rise<0.01 && rise>-0.01){
                    count++;
                }
                leiji = leiji+rise;
            }
        }
        if(count>=(days-days/3) && (leiji<0.02 && leiji>-0.02)){
            LogUtil.i(TAG,"股票："+stock+",具有横盘图形");
            pass =true;
        }
        return pass;
    }
    private boolean huitiaoGraph(ArrayList<StockOneDay> datas,String stock){
        boolean pass = false;
        int upCishu = 0;
        int downCishu = 0;
        float fengPrice = 0.0f;
        float fudu = 0.0f;
        float rise = 0;
        float currentPrice = 0;
        float yesterDayPrice = 0;
        int trCount = datas.size();

        for(int i=0;i<trCount;i++){
            StockOneDay oneDay = datas.get(i);

            rise = (oneDay.shoupanjia-oneDay.kaipanjia)/oneDay.shoupanjia;
            if(i==trCount-1){

                currentPrice = oneDay.shoupanjia;

            }
            if(i==trCount-2){
                yesterDayPrice = oneDay.shoupanjia;
            }


            if(rise>0.0f){
                upCishu++;
            }else{
                downCishu++;
            }
            if ((i == (days/2-1))||(i == (days/2)||(i == (days/2+1)))) {
                fengPrice = Math.max(fengPrice, oneDay.shoupanjia);
            }
            /*if ((i == (days/2-1))) {

                LogUtil.i(TAG,"股票："+stock+"峰price:"+oneDay.shoupanjia);
            }
            if ((i == (days/2))) {
                LogUtil.i(TAG,"股票："+stock+"峰price:"+oneDay.shoupanjia);
            }

            if ((i == (days/2+1))) {
                LogUtil.i(TAG,"股票："+stock+"峰price:"+oneDay.shoupanjia);
            }*/
            if(i==(days-1)){
                fudu = (fengPrice - currentPrice)/currentPrice;
            }

        }
        //坑股 5天上涨 5天下跌 最后一天不能跌停
        LogUtil.i(TAG,"股票："+stock+",rise"+rise);
        if(upCishu == (days/2) && downCishu == (days/2) && fudu>0.05f && rise > -0.07f && rise<0.04){
            LogUtil.i(TAG,"股票："+stock+",具有黄金坑特征");

            pass = true;
        }
        LogUtil.i(TAG,"股票："+stock+"当前价格："+currentPrice+"，中间最高价格:"+fengPrice);
        LogUtil.i(TAG,"股票："+stock+"上涨次数"+upCishu+",下降次数"+downCishu+",当前价格离最高价格偏离:"+fudu+","+"股票："+stock+"当前价格："+currentPrice+"，中间最高价格:"+fengPrice);
        return pass;
    }

    private boolean suoliangGraph(ArrayList<StockOneDay> datas,String stock){
        float currentJiaoyiliang = 0;
        float jiaoyiliang = 0;
        float rise = 0;
        boolean pass = false;

        int trCount = datas.size();
        float currentPrice = 0;
        float yesterDayPrice = 0;
        for(int i=0;i<trCount;i++){
            StockOneDay oneDay = datas.get(i);


            if(i==0){
                //第一天收盘价作为开始价格
                jiaoyiliang = oneDay.jiaoyiliang;
            }
            if(i==trCount-1){
                currentJiaoyiliang = oneDay.jiaoyiliang;
                currentPrice = oneDay.shoupanjia;
                rise = (currentPrice-yesterDayPrice)/yesterDayPrice;
           }
            if(i==trCount-2){
                yesterDayPrice = oneDay.shoupanjia;
            }
            if(i!=0){

                float jiaoyiliang2 = oneDay.jiaoyiliang;

                if(jiaoyiliang2<jiaoyiliang){
                    jiaoyiliang = jiaoyiliang2;
                }
            }
        }
        LogUtil.i(TAG,"股票当天涨跌幅："+stock+",rise"+rise);
        LogUtil.i(TAG,"股票："+stock+"当前成交量"+currentJiaoyiliang+",近期最小成交量"+jiaoyiliang+"所占比例:"+(currentJiaoyiliang)*1.0/jiaoyiliang);

        //今天缩量到 接近于近期最小成交量  非涨跌停的缩量 底部缩量
        if((currentJiaoyiliang)==jiaoyiliang && !(rise>0.05 || rise<-0.05) ){
            LogUtil.i(TAG,"股票："+stock+"当前成交量"+currentJiaoyiliang+",近期最小成交量"+jiaoyiliang+"超出比例:"+(currentJiaoyiliang-jiaoyiliang)*1.0/jiaoyiliang);

            pass = true;
        }
        return pass;
    }
    private boolean analyzeOneStock(ArrayList<StockOneDay> datas,int[] flags,String stock){


        /*LogUtil.i(TAG,"打印数据开始______________________");
        int j = 0;
        for(;j<datas.size();j++){
            LogUtil.i(TAG,"j="+j+","+datas.get(j).toString());
        }
        LogUtil.i(TAG,"打印数据结束______________________");*/
        boolean pass = false;

        ArrayList<StockOneDay> results =datas;

        LogUtil.i(TAG,"股票："+stock+"统计天数"+results.size());
        String szDate = SZIndexData.get(0).date;
        String geGuDate = results.get(0).date;
        LogUtil.i(TAG,"股票"+stock+"比较日期 深圳指数日期:"+szDate+",个股日期:"+geGuDate);
        if (szDate.equals(geGuDate)) {

        }else{
            return false;
        }

        if(graph == 0){
            pass = quShiGraph(datas,stock);
            if(pass){

                flags[0] = 1;
            }
        }
        if(graph == 1){
            pass = zhangtingHuitiaoGraph(datas,stock);
            if(pass){

                flags[0] = 1;
            }
        }
        if(graph == 2){
            pass = hengpanGraph(datas,stock);
            if(pass){

                flags[0] = 1;
            }
        }

        if(graph == 3){
            pass = suoliangGraph(datas,stock);
            if(pass){

                flags[0] = 1;
            }
        }
        boolean pass2  = huitiaoGraph(datas,stock);
        if(pass2){
            pass= pass2;
            flags[1] = 1;
        }




        return pass;
    }
    private void selectStockData(String stock){

    }
    //对记忆库数据进行汇总计算
    public ArrayList statisticsStock(){
        ArrayList<String> resultStocks = new ArrayList();
        return resultStocks;
    }
    //http://hqdigi2.eastmoney.com/EM_Quote2010NumericApplication/index.aspx?type=s&sortType=C&sortRule=-1&pageSize=20&page=76&jsName=quote_123&style=20&token=44c9d251add88e27b65ed86506f6e5da&_g=0.9510625265538692

    //http://hqdigi2.eastmoney.com/EM_Quote2010NumericApplication/index.aspx?type=s&sortType=C&sortRule=-1&pageSize=2000&page=1&jsName=quote_123&style=20

    public HashMap<String,String> getAllStocks(){
        if(stockName == null){
            stockName = new HashMap<String,String>();
        }
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
                        //stockName.put(lineStock.substring(start, start + 6),)
                        String []tmp = lineStock.split(",");
                        stockName.put(tmp[1],tmp[2]);
                        //llStocks.add(lineStock.substring(start, start + 6));
                        LogUtil.i(TAG, "xxxx"+lineStock);
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

        return stockName;
    }
    public   void work(){
        if(!prepared){
            prepare();
        }
         getAllStocks();

        getSZIndexData();
        analyzeStock();
        //ArrayList<String> resultStocks = statisticsStock();

    }

    public boolean prepare(){
        URL url = null;//取得资源对象
        URLConnection uc = null;


        commentsData = Collections.synchronizedList(commentsData);
        /*try {
            url = new URL("http://www.bjtime.cn");
            uc = url.openConnection();//生成连接对象
            uc.connect(); //发出连接

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        long ld=uc.getDate(); //取得网站日期时间

        Date date=new Date(ld); //转换为标准时间对象
        */
        Date date = null;
        /*SntpClient client = new SntpClient();
        if (client.requestTime("cn.pool.ntp.org", 30000)) {
            long now = client.getNtpTime() + System.nanoTime() / 1000
                    - client.getNtpTimeReference();
            date = new Date(now);
        }
        */
        date = new Date();
            //分别取得时间中的小时，分钟和秒，并输出
        year = date.getYear()+1900;

        jidu = (date.getMonth())/3 + 1;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dateStr = sdf.format(date);
        Message message = handler.obtainMessage(3);
        Bundle bundle = new Bundle();
        bundle.putString("date",dateStr);
        selectDate = dateStr;
        message.setData(bundle);
        handler.sendMessage(message);
        //LogUtil.i(TAG,"当前是第"+jidu+"季度");
        LogUtil.i(TAG,(date.getYear()+1900)+"年"+(date.getMonth()+1)+"月"+date.getDate()+"日");
        //LogUtil.i(TAG, date.getHours() + "时" + date.getMinutes() + "分" + date.getSeconds() + "秒");

        cache = SimpleCache.getCache(new File(confPath,selectDate),"",new StockOneDay());
        prepared = true;
        return false;
    }
    public void deleteCache(){
        SimpleCache.deleteCache(new File(confPath,selectDate));
    }
    public void init(Handler handler){
        this.handler = handler;
    }

    public void stop(){
        LogUtil.i(TAG, "stop()");
        synchronized (lock){
            lock.notifyAll();
            LogUtil.i(TAG, "stop() notifyAll");
        }

    }
    public void start(){
        if(working){
            return;
        }
        working = true;
        int threads = Runtime.getRuntime().availableProcessors();
        LogUtil.i(TAG,"threads:"+threads);
        executorService = Executors.newFixedThreadPool(threads*2);
        
        Thread thread = new Thread(work);
        thread.start();
    }
    /*主力出货并非像投资者所想的那样，都集中顶部区域来完成，而是多在上升途中就完成了大量仓位的出货动作。*/
    public void setSelectMode(int mode){
        switch (mode){
            case 0:
                zhangditing = 0.09f;
                break;
            case 1:
                zhangditing = 0.07f;
                break;
            case 2:
                zhangditing = 0.05f;
                break;
        }
    }
    public void setGraph(int graph){
        this.graph = graph;
    }
    public int  getCommentCount(String stock) {
        if (stock.charAt(0) == '3') {
            return 0;
        }



        String requestPattern= "http://guba.eastmoney.com/list,%s.html";

        //String requestPattern= "http://www.baidu.com";//
        String path = String.format(requestPattern,stock);
        BufferedReader in = null;
        String content = "";
        int count = 0;
        LogUtil.i(TAG,"stock:path="+path);
        LogUtil.i(TAG,"stock:selectDate="+selectDate);
        ArrayList<String> results = new ArrayList<>();
        Pattern urlPattern = Pattern.compile("http:////guba.eastmoney.com//news," + stock + ",/d+.html.+title=.+>(.+?)<//a>.+<span class=\"l6\">(.+?)<span class=\"l5\">(.+?)<//span>");



        try {

            URL url = new URL("http://guba.eastmoney.com/list,002336.html");
            InetAddress ia = InetAddress.getByName("guba.eastmoney.com/list,002336.html");
            LogUtil.i(TAG,"address="+ia.getHostAddress());
            if(false) {

                in = new BufferedReader(new InputStreamReader( url.openStream(),"utf-8"));

                String line;
                while ((line = in.readLine())!=null){
                    content += line;
                }
                Matcher matcher = urlPattern.matcher(content);
                boolean isFind = matcher.find();
                while(isFind){
                    String comment = matcher.group(1);
                    String firstDate = matcher.group(2);
                    String updateDate = matcher.group(3);
                    isFind = matcher.find();
                    LogUtil.i(TAG,"stock:comment="+comment);
                    LogUtil.i(TAG,"stock:firstDate="+firstDate);
                    LogUtil.i(TAG,"stock:updateDate="+updateDate);

                }
            }



        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            }catch (Exception e){

            }
        }

        return count;
    }
    public void saveCache(){
        File file = new File(confPath,selectDate);
        if(file.exists()){
            file.delete();
        }
        SimpleCache.writeCache(cache,file);
    }

    public static String convertUnicode(String ori) {
        char aChar;
        int len = ori.length();
        StringBuffer outBuffer = new StringBuffer(len);
        for (int x = 0; x < len; ) {
            aChar = ori.charAt(x++);
            if (aChar == '\\') {
                aChar = ori.charAt(x++);
                if (aChar == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = ori.charAt(x++);
                        switch (aChar) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "Malformed   \\uxxxx   encoding.");
                        }
                    }
                    outBuffer.append((char) value);
                } else {
                    if (aChar == 't')
                        aChar = '\t';
                    else if (aChar == 'r')
                        aChar = '\r';
                    else if (aChar == 'n')
                        aChar = '\n';
                    else if (aChar == 'f')
                        aChar = '\f';
                    outBuffer.append(aChar);
                }
            } else
                outBuffer.append(aChar);

        }
        return outBuffer.toString();
    }
    private boolean isStockPass(StockInfo stockInfo){


        stockInfo.info = "";

        String urlPath;
        if (stockInfo.stockId.charAt(0) == '6'){
            urlPath = "sh"+stockInfo.stockId;
        }else{
            urlPath = "sz"+stockInfo.stockId;
        }
        String urlData = String.format(infoPath,urlPath);

        String data = Tools.browseUrl(urlData);





        if (data !=null ) {
            data = convertUnicode(data);
            LogUtil.i(TAG,"股票:"+stockInfo.stockId+",data.length:" +data.length());
            for(String key: excudeKeys){
                if(data.contains(key)){

                    stockInfo.info += key+" ";
                }
            }
            //LogUtil.i(TAG,"股票:"+stockInfo.stockId+",data:" +data);
        }else{
            LogUtil.i(TAG,"股票:"+stockInfo.stockId+",data:" +data);
        }
        if("".equals(stockInfo.info) && data !=null){
            LogUtil.i(TAG,"股票:"+stockInfo.stockId+",没有问题关键字");
            return  true;
        }
        return false;
    }
    class MyThread2 extends Thread{
        int type ;
        @Override
        public void run() {
            ArrayList<StockInfo> stocks = null;
            if (type == 0) {
                stocks = stocksQush;
            }else{
                stocks = stocksSuoliang;
            }
            Iterator<StockInfo> sListIterator = stocks.iterator();
            while(sListIterator.hasNext()){
                StockInfo stockInfo = sListIterator.next();
                if(isStockPass(stockInfo)){

                }else{
                    LogUtil.i(TAG,"股票:"+stockInfo.stockId+"有问题关键字"+stockInfo.info);
                    sListIterator.remove();
                }
                if(type == 0){
                    Message message = handler.obtainMessage(20);
                    handler.sendMessage(message);
                }else{
                    Message message = handler.obtainMessage(21);
                    handler.sendMessage(message);
                }
            }
            if(type == 0){
                Message message = handler.obtainMessage(6);
                handler.sendMessage(message);
            }else{
                Message message = handler.obtainMessage(7);
                handler.sendMessage(message);
            }
        }
    }
  public void filterStockByKey( int type){


      MyThread2 m = new MyThread2();
      m.type = type;
        m.start();


    }
    //http://stockpage.10jqka.com.cn/realHead_v2.html#hs_601919
    class MyThread extends Thread{
        public int type;
        @Override
        public void run() {
            {
                ArrayList<StockInfo> stocks = null;
                if (type == 0) {
                    stocks = stocksQush;
                }else{
                    stocks = stocksSuoliang;
                }
                for(StockInfo stockInfo:stocks) {

                    getStockInfo(stockInfo);
                    if(type == 0){
                        Message message = handler.obtainMessage(20);
                        handler.sendMessage(message);
                    }else{
                        Message message = handler.obtainMessage(21);
                        handler.sendMessage(message);
                    }
                }
                Iterator<StockInfo> sListIterator = stocks.iterator();
                while(sListIterator.hasNext()) {
                    StockInfo stockInfo = sListIterator.next();
                    if (stockInfo.pe>50  || stockInfo.pe<1.0 || stockInfo.pb<1.0 || stockInfo.pb>10 || stockInfo.price>40 || stockInfo.price < 5
                            || (stockInfo.liutonggu * stockInfo.price > 400)) {
                        sListIterator.remove();
                    }

                }

                Collections.sort(stocks, new Comparator<StockInfo>() {
                    @Override
                    public int compare(StockInfo lhs, StockInfo rhs) {
                        if((lhs.price*lhs.liutonggu)<(rhs.price*rhs.liutonggu)){
                            return -1;
                        }else if((lhs.price*lhs.liutonggu)>(rhs.price*rhs.liutonggu)){
                            return 1;
                        }
                        return 0;
                    }
                });

                if(type == 0){
                    Message message = handler.obtainMessage(8);
                    handler.sendMessage(message);
                }else{
                    Message message = handler.obtainMessage(9);
                    handler.sendMessage(message);
                }
            }
        }
    }
    public void sortStocksByPB(final int type){
        MyThread m = new MyThread();
        m.type = type;
        m.start();
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
                //float price = Float.parseFloat(xj);
                float price = 0.0f;
                ArrayList<StockOneDay> allDays = this.cache.get(stockInfo.stockId);
                price = (allDays.get(-1 + allDays.size())).shoupanjia;
                stockInfo.price  = price;
                float jingzichan =  Float.parseFloat(mm[2]);
                LogUtil.i(TAG,"股票："+stockInfo.stockId+",股价:"+price+",净资产:"+jingzichan+",流通股"+stockInfo.liutonggu+"亿");
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
