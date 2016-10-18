package com.stock;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.stock.R;
import com.stock.bean.StockWuDang;
import com.stock.util.CDGUtil;
import com.stock.util.WuDangUtil;

import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by Administrator on 2016/8/21.
 */
public class WuDangActivity extends Activity{

    String TAG = "ChaoDieActivity";
    TextView count;
    TextView process;
    TextView remainCount;
    TextView status;
    private int stockCount;
    ProgressBar progressBar;
    private ListView results;
    ArrayAdapter resultArrayAdapter;
    private ArrayList<String> datas = new ArrayList<>();
    WuDangUtil wuDangUtil;
    DecimalFormat df4 = new DecimalFormat("###.##");
    TextView stockName,liutonggu,zongguben,price,s5,s4,s3,s2,s1,b1,b2,b3,b4,b5,liutongshizhi,zongshizhi,shijinglv,gudongrenshu;
    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                    String stock = String.format("%06d", msg.arg1);
                    process.setText("已分析:"+stock);
                    //int remain = Integer.parseInt(count.getText().toString());
                    //count.setText(String.valueOf(remain));
                    stockCount--;
                    progressBar.setProgress(stockCount);
                    remainCount.setText("剩余："+stockCount);

                    if(msg.arg2 == 1){
                        //LogUtil.i(TAG,"向UI抛出股票:"+stock);
                        //datas.add(wuDangUtil.allStocks.get(stock).stockName);
                        datas.add(stock);
                        count.setText("挂单压力小："+datas.size());
                        resultArrayAdapter.notifyDataSetChanged();
                    }


                    if(stockCount<=0){
                        status.setText("完成");
                    }
                        break;
                case 1:
                    stockCount = msg.arg1;
                    progressBar.setMax(msg.arg1);
                    count.setText("股票数量："+msg.arg1 + "");
                    break;
            }
            super.handleMessage(msg);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        wuDangUtil = new WuDangUtil(handler);
        setContentView(R.layout.activity_wudang);
        count = (TextView) findViewById(R.id.count);
        count.setText("0");
        process = (TextView) findViewById(R.id.process);
        process.setText("0");
        status = (TextView) findViewById(R.id.status);
        status.setText("正在读取五档盘口数据");
        remainCount = (TextView) findViewById(R.id.remainCount);
        results = (ListView) findViewById(R.id.results);
        //  http://quote.eastmoney.com/sz002615.html
        progressBar = (ProgressBar) findViewById(R.id.progress_horizontal);

        resultArrayAdapter = new ArrayAdapter(this, R.layout.basic_text_view, datas);
        results.setAdapter(resultArrayAdapter);

        stockName = (TextView)findViewById(R.id.stockName);
        liutonggu = (TextView)findViewById(R.id.liutonggu);
        zongguben = (TextView)findViewById(R.id.zongguben);
        price = (TextView)findViewById(R.id.price);

        s5 = (TextView)findViewById(R.id.s5);
        s4 = (TextView)findViewById(R.id.s4);
        s3 = (TextView)findViewById(R.id.s3);
        s2 = (TextView)findViewById(R.id.s2);
        s1 = (TextView)findViewById(R.id.s1);
        b1 = (TextView)findViewById(R.id.b1);
        b2 = (TextView)findViewById(R.id.b2);
        b3 = (TextView)findViewById(R.id.b3);
        b4 = (TextView)findViewById(R.id.b4);
        b5 = (TextView)findViewById(R.id.b5);

        liutongshizhi = (TextView)findViewById(R.id.liutongshizhi);
        zongshizhi = (TextView)findViewById(R.id.zongshizhi);
        shijinglv = (TextView)findViewById(R.id.shijinglv);
        gudongrenshu = (TextView)findViewById(R.id.gudongrenshu);
        new Thread(){
            @Override
            public void run() {
                wuDangUtil.start();
            }
        }.start();

        results.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String stockId = datas.get(position);
                StockWuDang stockWuDang = wuDangUtil.allStocks.get(stockId);
                updateInfo(stockWuDang);
            }
        });
        super.onCreate(savedInstanceState);
    }
    private void updateInfo(StockWuDang stockWuDang){
        stockName.setText(stockWuDang.stockName);
        liutonggu.setText("流通股:"+stockWuDang.stockInfo.liutonggu+"亿");
        zongguben.setText("总股本:"+stockWuDang.stockInfo.zongguben+"亿");
        price.setText("价格："+stockWuDang.stockInfo.price);
        s5.setText("卖5 "+df4.format(stockWuDang.s10[0])+" "+stockWuDang.cs10[0]/100);
        s4.setText("卖4 "+df4.format(stockWuDang.s10[1])+" "+stockWuDang.cs10[1]/100);
        s3.setText("卖3 "+df4.format(stockWuDang.s10[2])+" "+stockWuDang.cs10[2]/100);
        s2.setText("卖2 "+df4.format(stockWuDang.s10[3])+" "+stockWuDang.cs10[3]/100);
        s1.setText("卖1 "+df4.format(stockWuDang.s10[4])+" "+stockWuDang.cs10[4]/100);
        b1.setText("买1 "+df4.format(stockWuDang.s10[5])+" "+stockWuDang.cs10[5]/100);
        b2.setText("买2 "+df4.format(stockWuDang.s10[6])+" "+stockWuDang.cs10[6]/100);
        b3.setText("买3 "+df4.format(stockWuDang.s10[7])+" "+stockWuDang.cs10[7]/100);
        b4.setText("买4 "+df4.format(stockWuDang.s10[8])+" "+stockWuDang.cs10[8]/100);
        b5.setText("买5 "+df4.format(stockWuDang.s10[9])+" "+stockWuDang.cs10[9]/100);

        liutongshizhi.setText("流通市值:"+stockWuDang.stockInfo.liutonggu*stockWuDang.stockInfo.price+"亿");
        zongshizhi.setText("总市值:"+stockWuDang.stockInfo.zongguben*stockWuDang.stockInfo.price+"亿");
        shijinglv.setText("市净率:"+stockWuDang.stockInfo.pb);
        gudongrenshu.setText("股东人数:"+stockWuDang.stockInfo.gudongrenshu);
    }
}
