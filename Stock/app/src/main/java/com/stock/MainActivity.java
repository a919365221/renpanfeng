package com.stock;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.os.PersistableBundle;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import com.stock.bean.StockInfo;
import com.stock.util.LogUtil;
import com.stock.util.StockUtil;

public class MainActivity extends Activity implements View.OnClickListener{
    TextView count;
    TextView process;
    TextView remainCount;
    TextView successCount;
    TextView status;

    ProgressBar progressBar,progressBarQushi,progressBarSuoliang;
    private ListView results;
    private ListView resultsSpecial;
    private int stockCount;
    ArrayAdapter resultArrayAdapter,resultsSpecialArrayAdapter;
    private ArrayList<String> datas,datasSpecial,datas_flag;

    TextView resultsSpecialCount;
    int saveRecountCountLimit = 10;
    private String date;
    private int currentMode = 0;
    private boolean needToWork = false;
    ProgressDialog pd ;
    private static  final String TAG = MainActivity.class.getSimpleName();
    private boolean utilThreadIsWait = false;
    StockUtil stockUtil ;
            Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
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
                    if(msg.arg2 == 2|| msg.arg2 == 3){
                        if(msg.arg2 == 3){
                            datas_flag.add(stockUtil.stockName.get(stock));
                        }
                        StockInfo stockInfo = new StockInfo();
                        stockInfo.stockId = stock;
                        stockUtil.stocksSuoliang.add(stockInfo);
                        datasSpecial.add(stockUtil.stockName.get(stock));
                        resultsSpecialArrayAdapter.notifyDataSetChanged();
                        resultsSpecialCount.setText("黄金坑股:"+datasSpecial.size());
                    }
                    if(msg.arg2 == 1 || msg.arg2 == 3){
                        //上涨趋势
                        StockInfo stockInfo = new StockInfo();
                        stockInfo.stockId = stock;
                        stockUtil.stocksQush.add(stockInfo);
                        datas.add(stockUtil.stockName.get(stock));
                        successCount.setText("上涨趋势："+datas.size());
                        resultArrayAdapter.notifyDataSetChanged();
                    }


                    if(stockCount<=0){
                        status.setText("");
                        stockUtil.saveCache();
                        status.setText("正在过滤风险股票....");
                        progressBarQushi.setMax(datas.size());
                        progressBarQushi.setProgress(datas.size());
                        progressBarSuoliang.setMax(datasSpecial.size());
                        progressBarSuoliang.setProgress(datasSpecial.size());
                        stockUtil.filterStockByKey(0);
                        stockUtil.filterStockByKey(1);

                    }
                    break;
                case 1:
                    stockCount = msg.arg1;
                    progressBar.setMax(msg.arg1);
                    count.setText("股票数量："+msg.arg1 + "");
                    break;
                case 2:
                    String stock1 = String.format("%06d", msg.arg1);

                    break;
                case 3:
                    date = msg.getData().getString("date");
                    break;
                case 4:
                    if(needToWork){
                        needToWork = false;
                        reWork();
                    }
                    break;
                case 5:
                    utilThreadIsWait = true;
                    break;
                case 6:
                    status.setText("过滤市盈率市净率");
                  datas.clear();
                    for(StockInfo stockInfo:stockUtil.stocksQush){
                        datas.add(stockUtil.stockName.get(stockInfo.stockId));
                    }
                    resultArrayAdapter.notifyDataSetChanged();
                    //saveData(datas, date);
                    progressBarQushi.setMax(datas.size());
                    progressBarQushi.setProgress(datas.size());
                    stockUtil.sortStocksByPB(0);
                    successCount.setText("上涨趋势："+datas.size());
                    break;
                case 7:
                    status.setText("过滤市盈率市净率");
                    datasSpecial.clear();
                     for(StockInfo stockInfo:stockUtil.stocksSuoliang){
                         String xxx = stockUtil.stockName.get(stockInfo.stockId);
                         if (xxx==null) {
                             LogUtil.i(TAG,"stockid xxx:"+stockInfo.stockId+"null");
                             xxx = "";
                         }
                        datasSpecial.add(xxx);
                    }
                    resultsSpecialArrayAdapter.notifyDataSetChanged();
                   progressBarSuoliang.setMax(datasSpecial.size());
                    progressBarSuoliang.setProgress(datasSpecial.size());
                    stockUtil.sortStocksByPB(1);
                    resultsSpecialCount.setText("黄金坑股:"+datasSpecial.size());
                    break;
                case 8:
                    status.setText("排序完了");
                    datas.clear();
                    for(StockInfo stockInfo:stockUtil.stocksQush){
                        datas.add(stockUtil.stockName.get(stockInfo.stockId));
                    }
                    resultArrayAdapter.notifyDataSetChanged();
                    successCount.setText("上涨趋势："+datas.size());
                    break;
                case 9:
                    status.setText("排序完了");
                   datasSpecial.clear();
                    for(StockInfo stockInfo:stockUtil.stocksSuoliang){
                        datasSpecial.add(stockUtil.stockName.get(stockInfo.stockId));
                    }
                    resultsSpecialArrayAdapter.notifyDataSetChanged();
                    resultsSpecialCount.setText("黄金坑股:"+datasSpecial.size());
                    break;
                case 20:
                    int p = progressBarQushi.getProgress();
                    progressBarQushi.setProgress(p-1);
                    break;
                case 21:
                    int p2 = progressBarSuoliang.getProgress();
                    progressBarSuoliang.setProgress(p2-1);
                    break;
            }

        }
    };
    boolean test = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (test) {
            setContentView(R.layout.swip_test);
            try {
                stockUtil = new StockUtil(this.getCacheDir().getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            ArrayList<String> aa = new ArrayList<>();
            aa.add("601919");
            //stockUtil.sortStocksByPB(aa);
            return;
        }
        setContentView(R.layout.activity_main);
        count = (TextView) findViewById(R.id.count);
        count.setText("0");
        process = (TextView) findViewById(R.id.process);
        process.setText("0");
        status = (TextView) findViewById(R.id.status);
        status.setText("正在查找趋势股票和缩量股票");
        remainCount = (TextView) findViewById(R.id.remainCount);
        //  http://quote.eastmoney.com/sz002615.html
        progressBar = (ProgressBar) findViewById(R.id.progress_horizontal);
        progressBarQushi = (ProgressBar) findViewById(R.id.progress_qushi);
        progressBarSuoliang = (ProgressBar) findViewById(R.id.progress_suoliang);
        results = (ListView) findViewById(R.id.results);
        resultsSpecial = (ListView) findViewById(R.id.resultsSpecial);
        successCount = (TextView) findViewById(R.id.successCount);
        resultsSpecialCount = (TextView) findViewById(R.id.resultsSpecialCount);
        try{
            stockUtil = new StockUtil(this.getCacheDir().getCanonicalPath());
        }catch (Exception e){

            e.printStackTrace();
        }

        datas = new ArrayList<>();
        datas_flag = new ArrayList<>();
        datasSpecial = new ArrayList<>();


        resultArrayAdapter = new ArrayAdapter(this, R.layout.basic_text_view, datas) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return super.getView(position,convertView,parent);
                /*TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Color.WHITE);
                CharSequence cs = tv.getText();
                String stock = null;
                if(cs!=null){
                    stock = cs .toString();
                }else{
                    LogUtil.i(TAG,"stock null");
                    return tv;
                }

                tv.setBackgroundColor(0xff000000);
                for (String target : datas_flag) {
                    if (stock.equals(target)) {
                        tv.setBackgroundColor(Color.GREEN);
                        break;
                    }
                }

                return tv;
                */
            }

        };
        resultsSpecialArrayAdapter = new ArrayAdapter(this, R.layout.basic_text_view, datasSpecial);
        results.setAdapter(resultArrayAdapter);
        resultsSpecial.setAdapter(resultsSpecialArrayAdapter);
        stockUtil.init(handler);

        ActionBar ab = getActionBar();
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayUseLogoEnabled(true);
        ab.setSubtitle("你懂的");
        ab.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        ArrayAdapter modes = ArrayAdapter.createFromResource(this, R.array.selectModes, R.layout.action_bar_text_view);

        ab.setListNavigationCallbacks(modes, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                LogUtil.i(TAG,itemPosition+"");
                stockUtil.setGraph(itemPosition);
                changeWorkMode(currentMode);
                return true;
            }
        });
    }
    //是否连接WIFI
    public static boolean isWifiConnected(Context context)
    {
        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(wifiNetworkInfo.isConnected())
        {
            return true ;
        }

        return false ;
    }

    public void saveData(ArrayList<String> datas,String date){
        BufferedReader br = null;
        FileInputStream fis = null;
        InputStreamReader isr = null;
        File file = null;
        ArrayList<String> oldData = new ArrayList<>();
        try {
            File dir = this.getFilesDir();
            file = new File(dir,"history.csv");
            if(!file.exists()){
                file.createNewFile();
            }
            fis = this.openFileInput("history.csv");
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);
        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }
        CsvReader csvReader = new CsvReader(br);

        try{
            int count = 0;
            while(csvReader.readRecord()){
                count++;
                oldData.add(csvReader.getRawRecord());
            }
            if(oldData.size()>0 ){
                String lastSelectData = oldData.get(oldData.size()-1);
                String lastselectDate = lastSelectData.split(",")[0];
                LogUtil.i(TAG,"比较数据是否保存 date:"+date+",最后一条记录查询日期:"+lastselectDate);
                if(date.equals(lastselectDate)){
                    Toast.makeText(MainActivity.this, "查询的数据保存过", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if(count >= saveRecountCountLimit){
                oldData.remove(0);
            }
        }catch (IOException io){
            io.printStackTrace();
        }finally {
            csvReader.close();
        }

        if(file.exists()){
            file.delete();
        }

        //this.openFileOutput("history.csv",MODE_PRIVATE);
        CsvWriter csvWriter  = null;
        try {
            csvWriter = new CsvWriter(new OutputStreamWriter(this.openFileOutput("history.csv",MODE_WORLD_READABLE)),',');
            for(int i = 0;i<oldData.size();i++){
                csvWriter.writeRecord(oldData.get(i).split(","));
            }
            String[] s = new String[datas.size()];
            datas.toArray(s);
            String[] ss = new String[s.length+1];
                    System.arraycopy(s,0,ss,1,s.length);
            ss[0] = date;
            csvWriter.writeRecord(ss);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        csvWriter.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    public void showRecentData(View v){
        ArrayList<String> records = new ArrayList<>();
        try {
            CsvReader csvReader = new CsvReader(new BufferedReader(new InputStreamReader(this.openFileInput("history.csv"))));
            while (csvReader.readRecord()){
                String record = csvReader.getRawRecord();
                records.add(record);
            }
            Intent intent = new Intent(this,ShowStock.class);
            intent.putStringArrayListExtra("datas",records);
            startActivity(intent);
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void deleteRecent(View v){
        ExitAskFragment exitAskFragment = new ExitAskFragment();
        exitAskFragment.show(getFragmentManager(),"exitAskFragment");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.confirm:
                File dir = this.getFilesDir();
                File file = new File(dir,"history.csv");
                File bak = new File(dir,"history.bak");
                if(bak.exists()){
                    bak.delete();
                }
                if(file.exists()){
                    file.renameTo(bak);
                    file.delete();
                }
                stockUtil.deleteCache();
                break;
            case R.id.cancel:
                try {
                    throw new Exception();
                }catch (Exception e){
                    e.printStackTrace();
                }

                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);

        //groupid menuitemid menuitemorder menuitemtext
        //menu.add(0, 0, 0, "困难");
        //menu.add(0,1,1,"普通");
        //menu.add(0, 2, 2, "简单");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            // action with ID action_refresh was selected
            case R.id.bkld:
                Intent intent = new Intent();
                intent.setClass(this,BanKuaiLunDong.class);
                startActivity(intent);
                break;
            // action with ID action_settings was selected
            case R.id.others:
                if (isWifiConnected(this)) {

                    stockUtil.start();
                } else {
                    Toast.makeText(this, "没有连接wifi", Toast.LENGTH_SHORT);
                }
                break;
            case R.id.cdg:
            Intent intent1 = new Intent();
            intent1.setClass(this,ChaoDieActivity.class);
            startActivity(intent1);
            case R.id.wudong:
                Intent intent2 = new Intent();
                intent2.setClass(this,WuDangActivity.class);
                startActivity(intent2);

            default:
                break;
        }
        return true;

       /* LogUtil.i(TAG,"currentMode:"+currentMode);
        if(currentMode == item.getItemId()){
            return true;
        }
        LogUtil.i(TAG,"currentMode:"+currentMode);
        changeWorkMode(item.getItemId());*/

        //return super.onOptionsItemSelected(item);
    }
    private void changeWorkMode(int mode){
        if(!utilThreadIsWait){
            Toast.makeText(this, "请稍后再试", Toast.LENGTH_SHORT);
            return  ;
        }
        needToWork = true;
        if(pd==null){
            pd = new ProgressDialog(this);
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.setCancelable(false);
            pd.setMessage("模式切换中...");
            pd.setIndeterminate(false);

        }
        pd.show();
        stockUtil.stop();
        stockUtil.setSelectMode(mode);
    }
    private  void reWork(){
        utilThreadIsWait = false;
        restore();
        stockUtil.start();
        pd.hide();
    }
    public void restore(){
        process.setText("");
        remainCount.setText("");
        successCount.setText("");
        datas.clear();
        datasSpecial.clear();
        datas_flag.clear();
        stockUtil.stocksSuoliang.clear();
        stockUtil.stocksQush.clear();
        resultsSpecialCount.setText("");
        resultArrayAdapter.notifyDataSetChanged();
        resultsSpecialArrayAdapter.notifyDataSetChanged();
    }
}
