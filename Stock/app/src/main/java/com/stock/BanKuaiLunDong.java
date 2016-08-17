package com.stock;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.stock.adapter.HorizontalListViewAdapter;
import com.stock.bean.BKInfo;
import com.stock.bean.BKInfoUI;
import com.stock.util.BKLDUtil;
import com.stock.view.HorizontalListView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by Administrator on 2016/8/12.
 */
public class BanKuaiLunDong   extends Activity {
    HorizontalListView horizontalListView;
    HorizontalListViewAdapter hListViewAdapter;
    private ArrayList<BKInfoUI>  datas = new ArrayList<>();
    private int[] lens;
    TextView status,tongjitianshu;
    SeekBar seekBar;
    BKLDUtil bkldUtil;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                case 2:
                case 3:
                    status.setText(bkldUtil.status);
                    break;
                case 4:
                    status.setText(bkldUtil.status);
                    datas.clear();
                    BKInfo bkInfoMax = bkldUtil.bkInfos.get(bkldUtil.bkInfos.size()-1);
                    NumberFormat num = NumberFormat.getPercentInstance();
                    num.setMaximumIntegerDigits(2);
                    num.setMaximumFractionDigits(3);
                    for(BKInfo bkInfo:bkldUtil.bkInfos){
                        BKInfoUI bkInfoUI = new BKInfoUI();
                        bkInfoUI.max = bkInfoMax.score;

                        String xx = num.format(bkInfo.score/10000.0);
                        bkInfoUI.name = xx +"ggxx"+ bkInfo.name;
                        bkInfoUI.score = bkInfo.score;
                        datas.add(bkInfoUI);
                    }
                    Collections.reverse(datas);
                    hListViewAdapter.notifyDataSetChanged();
                    seekBar.setEnabled(true);
                    break;
                case 5:
                    status.setText(bkldUtil.status+",剩余数量:"+msg.arg1);
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        setContentView(R.layout.bkld_main);
        bkldUtil = new BKLDUtil(handler);
        horizontalListView = (HorizontalListView)findViewById(R.id.horizon_listview);
        status = (TextView)findViewById(R.id.status);
        tongjitianshu = (TextView)findViewById(R.id.tongjitianshu);
        seekBar = (SeekBar)findViewById(R.id.bkSeekBar);
        seekBar.setMax(29);
        seekBar.setProgress(bkldUtil.days-1);
        seekBar.setEnabled(false);
        tongjitianshu.setText("统计天数:"+bkldUtil.days);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tongjitianshu.setText("统计天数:"+(progress+1));
                bkldUtil.updateDays((progress+1));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        hListViewAdapter = new HorizontalListViewAdapter(getApplicationContext(),datas);
        horizontalListView.setAdapter(hListViewAdapter);

        Thread m = new Thread(){
            @Override
            public void run() {

                bkldUtil.start();
            }
        };

        m.start();

        super.onCreate(savedInstanceState);
    }

}
