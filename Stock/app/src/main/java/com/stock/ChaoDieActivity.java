package com.stock;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.stock.util.CDGUtil;
import com.stock.util.LogUtil;

import java.util.ArrayList;

/**
 * Created by Administrator on 2016/8/21.
 */
public class ChaoDieActivity extends Activity{

    String TAG = "ChaoDieActivity";
    TextView count;
    TextView process;
    TextView remainCount;
    TextView successCount;
    TextView status;
    private int stockCount;
    ProgressBar progressBar;
    private ListView results;
    ArrayAdapter resultArrayAdapter;
    private ArrayList<String> datas = new ArrayList<>();
    CDGUtil cdgUtil;
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
                        datas.add(cdgUtil.allStocks.get(stock).stockName);
                        successCount.setText("上涨趋势："+datas.size());
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

        cdgUtil = new CDGUtil(handler);
        setContentView(R.layout.activity_main);
        count = (TextView) findViewById(R.id.count);
        count.setText("0");
        process = (TextView) findViewById(R.id.process);
        process.setText("0");
        status = (TextView) findViewById(R.id.status);
        status.setText("正在查找超跌股票");
        remainCount = (TextView) findViewById(R.id.remainCount);
        results = (ListView) findViewById(R.id.results);
        //  http://quote.eastmoney.com/sz002615.html
        progressBar = (ProgressBar) findViewById(R.id.progress_horizontal);
        successCount = (TextView) findViewById(R.id.successCount);
        resultArrayAdapter = new ArrayAdapter(this, R.layout.basic_text_view, datas);
        results.setAdapter(resultArrayAdapter);
        new Thread(){
            @Override
            public void run() {
                cdgUtil.start();
            }
        }.start();
        super.onCreate(savedInstanceState);
    }
}
