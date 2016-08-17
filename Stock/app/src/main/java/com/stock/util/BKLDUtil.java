package com.stock.util;

import android.os.Handler;
import android.os.Message;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.stock.bean.BKInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Administrator on 2016/8/12.
 */
public class BKLDUtil {
    public ArrayList<BKInfo> bkInfos;
    OkHttpClient mOkHttpClient;
    int requestCount;
    private String TAG = BKLDUtil.class.getSimpleName();
    String bkcodePath = "http://q.10jqka.com.cn/interface/stock/gn/zdf/desc/%d/quote/quote";
    String bkData = "http://d.10jqka.com.cn/v2/line/bk_%s/01/last.js";
    public int days = 7;
    public String status;
    private Handler handler;
    private  boolean working  = false;
    public BKLDUtil(Handler handler){
        this.handler = handler;
         mOkHttpClient = new OkHttpClient();
         bkInfos = new ArrayList<>();
         requestCount = 0;
    }
    public void stop(){

    }
    public void  start(){
        working = true;
        status = "开始获取板块代码";
        requestCount = 0;
        handler.sendEmptyMessage(1);
        Callback callback = new Callback()
        {
            @Override
            public void onFailure(Request request, IOException e)
            {
                synchronized (mOkHttpClient){
                    requestCount--;
                    if(requestCount==0){
                        for(BKInfo bkInfo:bkInfos){
                            LogUtil.i(TAG,bkInfo.toString());
                        }
                    }
                }
            }

            @Override
            public void onResponse(final Response response) throws IOException
            {
                String htmlStr =  response.body().string();
                JSONObject jsonObject = null;
                ArrayList<BKInfo> tmp = new ArrayList<>();
                try {
                    jsonObject = new JSONObject(htmlStr);
                    JSONArray datas = (JSONArray)jsonObject.get("data");
                    for(int i=0;i<datas.length();i++){
                        JSONObject record = (JSONObject) datas.get(i);
                        Object platename = (Object)record.get("platename");
                        String platecode = (String)record.get("platecode");
                        String zdf = (String)record.get("zdf");
                        String dateStr = (String)record.get("rtime");
                        String  [] yyyymmdd = dateStr.substring(0,10).split("-");
                        String vv = (String)record.get("zxj");
                        BKInfo bkInfo = new BKInfo();
                        bkInfo.name = platename.toString();
                        bkInfo.code = platecode;
                        bkInfo.current = new BKInfo.BKInfoDatePoint();
                        bkInfo.current.zhangdiefu = Double.parseDouble(zdf)/100;
                        bkInfo.current.date = Integer.parseInt(yyyymmdd[0])*10000+Integer.parseInt(yyyymmdd[1])*100+Integer.parseInt(yyyymmdd[2]);
                        bkInfo.current.currentValue = Double.parseDouble(vv);
                        tmp.add(bkInfo);
                    }
                    synchronized (mOkHttpClient){
                        //bkInfos.addAll();
                        bkInfos.addAll(tmp);
                        requestCount--;

                        if(requestCount==0){
                            /*for(BKInfo bkInfo:bkInfos){
                                LogUtil.i(TAG,bkInfo.toString());
                            }*/

                            LogUtil.i(TAG,"板块数量:"+bkInfos.size());
                            selectBKData();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        requestCount = 4;
        for(int i=1;i<=requestCount;i++){
            Request request = new Request.Builder()
                    .url(String.format(bkcodePath,i)).build();
            mOkHttpClient.newCall(request).enqueue(callback);
        }

    }


    class MyCallback implements Callback
    {
        BKInfo bkInfo;
        MyCallback(BKInfo bkInfo){
            this.bkInfo = bkInfo;
        }
        @Override
        public void onFailure(Request request, IOException e)
        {
            synchronized (BKLDUtil.class){
                requestCount--;
            }
        }

        @Override
        public void onResponse(final Response response) throws IOException
        {
            String content = response.body().string();
            int m = content.indexOf("(");
            int n = content.lastIndexOf(")");
            content = content.substring(m+1,n);
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(content);
                bkInfo.name = (String)jsonObject.get("name");
                String data = (String)jsonObject.get("data");
                //20160119,935.700,979.696,933.826,976.885,788420300,7989725700.000,; 这是yy
                String[] dateR = data.split(";");
                double lastValue = 0;
                for(int j = 0;j<dateR.length;j++){

                    BKInfo.BKInfoDatePoint xx = new BKInfo.BKInfoDatePoint();
                    String yy = dateR[j];
                    String[] details = yy.split(",");
                    xx.date = Integer.parseInt(details[0]);
                    xx.currentValue = Double.parseDouble(details[4]);
                    if(j == 0){
                        xx.zhangdiefu = 0;
                        lastValue = xx.currentValue;
                    }else{
                        xx.zhangdiefu = (xx.currentValue-lastValue)/lastValue;
                        lastValue = xx.currentValue;
                    }
                    //xx.zhangdiefu
                    bkInfo.datas.add(xx);
                }
                if(bkInfo.datas.size()>1){
                    BKInfo.BKInfoDatePoint last = bkInfo.datas.get(bkInfo.datas.size()-1);
                    if(last.date != bkInfo.current.date ){
                        bkInfo.datas.add(bkInfo.current);
                    }
                }
                LogUtil.i(TAG,"板块名称:"+bkInfo.name+"板块代码:"+bkInfo.code);
                LogUtil.i(TAG,"板块数据开始:");
                for(BKInfo.BKInfoDatePoint bkInfoDatePoint : bkInfo.datas){
                    LogUtil.i(TAG,"日期:"+bkInfoDatePoint.date+",点数:"+bkInfoDatePoint.currentValue+",涨跌幅:"+bkInfoDatePoint.zhangdiefu);
                }
                LogUtil.i(TAG,"板块数据结束:");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            synchronized (BKLDUtil.class){
                requestCount--;
                Message message = handler.obtainMessage(5);
                message.arg1 = requestCount;
                handler.sendMessage(message);
                if(requestCount == 0){

                    LogUtil.i(TAG,"板块数据下载结束！！！！！！！！！！！！！");
                    analyseBK();
                }
            }
        }
    };
    private void selectBKData(){
        status = "开始获取板块数据";
        handler.sendEmptyMessage(2);
        requestCount = bkInfos.size();
        for(BKInfo bkInfo:bkInfos){
            MyCallback myCallback = new MyCallback(bkInfo);
            Request request = new Request.Builder()
                    .url(String.format(bkData,bkInfo.code)).build();
            mOkHttpClient.newCall(request).enqueue(myCallback);
        }
    }
    class MyComparator implements Comparator {
        int day;
        MyComparator(int day){
            this.day = day;
        }
        @Override
        public int compare(Object lhs, Object rhs) {
            BKInfo left = (BKInfo)lhs;
            BKInfo right = (BKInfo)rhs;
            int size1 = left.datas.size();
            int size2 = right.datas.size();
            BKInfo.BKInfoDatePoint l = left.datas.get(size1 - days+day);
            BKInfo.BKInfoDatePoint r = right.datas.get(size2 - days+day);
            if(l.zhangdiefu>r.zhangdiefu){
                return 1;
            }else{
                return -1;
            }
        }
    }
    /*
    设计思路：
    1、统计板块数量 以此作为统计天数 假如有n个板块，就是n天。
    2、n天里第一天榜首给予评分n分依次排序给予n-1分
        第二天榜首给予加n分，次排序给予n-1分
            最后一天再评分。
    3、统计得分，得分最高为热门板块
    得分居中是回调板块
    得分最低是潜力板块。
    * */
    private void analyseBK(){
        status = "开始分析板块数据";
        handler.sendEmptyMessage(3);
        ArrayList<BKInfo> ruWei = new ArrayList<>();
        for(BKInfo bkInfo:bkInfos){
            int size = bkInfo.datas.size();
            if(size<=days || bkInfo.name.equals("昨日涨停表现")){
                LogUtil.i(TAG,"板块:"+bkInfo.name+"是"+days+"内新出现的板块");
                bkInfo.score = 0;
            }else{
                ruWei.add(bkInfo);
                bkInfo.score = 0;
            }
        }
        LogUtil.i(TAG,"参与统计的板块数量:"+ruWei.size()+"，以它作为总分");
        /*int allScore = ruWei.size();
        for(int j=0;j<days;j++){
            for(int x=0;x<allScore;x++){
                BKInfo bkInfo = ruWei.get(x);
                int size = bkInfo.datas.size();
                //50天数据后30天,50-30
               LogUtil.i(TAG,"板块:"+bkInfo.name+",date:"+bkInfo.datas.get(size - days+j).date+"涨跌幅:"+bkInfo.datas.get(size - days+j).zhangdiefu);
            }
        }*/
        //升序排列，第一个得分最低
       /*int allScore = ruWei.size();
        for(int j=0;j<days;j++){
            Collections.sort(ruWei, new MyComparator(j));
            for(int x=0;x<allScore;x++){
                BKInfo bkInfo = ruWei.get(x);
                bkInfo.score = bkInfo.score +x + 1;
            }
        }*/


        int allScore = ruWei.size();
        for(int x = 0; x < allScore; x++){
            BKInfo bkInfo = ruWei.get(x);
            double scoreTmp = 0;
            int size = bkInfo.datas.size();
            for(int j=0;j<days;j++){
                scoreTmp = scoreTmp + bkInfo.datas.get(size - days+j).zhangdiefu;
                LogUtil.i(TAG,bkInfo.name+",scoreTmp:"+scoreTmp);
            }
            bkInfo.score = (int)Math.round(scoreTmp*10000);
        }

        Collections.sort(bkInfos, new Comparator<BKInfo>() {
            @Override
            public int compare(BKInfo lhs, BKInfo rhs) {
                if(lhs.score>rhs.score){
                    return 1;
                }else {
                    return -1;
                }
            }
        });
        LogUtil.i(TAG,"评分结果:");
        for(BKInfo bkInfo:bkInfos){
            LogUtil.i(TAG,"板块"+bkInfo.name+",得分:"+bkInfo.score);
        }
        status = "统计结束";
        handler.sendEmptyMessage(4);
        working = false;
    }
    public void updateDays(int days){
        if(working){
            return;

        }else{
            this.days = days;
            analyseBK();
        }
    }
}
