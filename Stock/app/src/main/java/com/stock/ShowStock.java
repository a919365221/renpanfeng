package com.stock;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.PersistableBundle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.stock.util.StockHistory;


/**
 * Created by 907703 on 2016/3/18.
 */
public class ShowStock extends Activity {

    ExpandableListView expandablelist;
    ArrayList<StockHistory> datas = new ArrayList<>();
    MyexpandableListAdapter myexpandableListAdapter;
    private final  String TAG ="ShowStock";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.socklist);
        expandablelist = (ExpandableListView)findViewById(R.id.expandablelist);
        ArrayList<String> datas = getIntent().getStringArrayListExtra("datas");
        for(String data:datas){
            String[] tmp = data.split(",");
            ArrayList<String> mm = new ArrayList<>();
            int j=1;
            if(tmp.length>1){
                for(j=1;j<tmp.length;j++){

                    mm.add(tmp[j]);
                }
            }
            StockHistory stockHistory = new StockHistory(tmp[0],mm);
            this.datas.add(stockHistory);
        }
        myexpandableListAdapter = new MyexpandableListAdapter(this);
        expandablelist.setAdapter(myexpandableListAdapter);
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        getActionBar().setTitle("历史查询记录");
        //getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /***
     * 数据源
     *
     * @author Administrator
     *
     */
    class MyexpandableListAdapter extends BaseExpandableListAdapter  {
        private Context context;
        private LayoutInflater inflater;

        public MyexpandableListAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
        }

        // 返回父列表个数
        @Override
        public int getGroupCount() {
            return datas.size();
        }

        // 返回子列表个数
        @Override
        public int getChildrenCount(int groupPosition) {
            return datas.get(groupPosition).data.size();
        }

        @Override
        public Object getGroup(int groupPosition) {

            return datas.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return datas.get(groupPosition).data.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {

            return true;
        }


        class  GroupHolder{
            TextView textView;
            ImageView imageView;
        }
        class DeleteDataOpt implements View.OnClickListener{
            int index = 0;

            public DeleteDataOpt(int index) {
                this.index = index;
            }

            @Override
            public void onClick(View v) {

            }
        }
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            GroupHolder groupHolder = null;
            if (convertView == null) {
                groupHolder = new GroupHolder();
                //convertView = inflater.inflate(R.layout.swip_test,parent,false);
                convertView =(TextView) new TextView(this.context);
                groupHolder.textView = (TextView)convertView;//.findViewById(R.id.date_id);
                //groupHolder.imageView = (ImageView) convertView.findViewById(R.id.delete);
                //groupHolder.imageView.setOnClickListener(new DeleteDataOpt((int)getGroupId(groupPosition)));
                groupHolder.textView.setTextSize(15);
                //LinearLayout sl = (LinearLayout)convertView;
                /*sl.addSwipeListener(new SwipeLayout.SwipeListener() {

                    @Override
                    public void onStartOpen(SwipeLayout layout) {
                        Log.i(TAG, "onStartOpen");
                    }

                    @Override
                    public void onOpen(SwipeLayout layout) {
                        Log.i(TAG, "onOpen");
                    }

                    @Override
                    public void onStartClose(SwipeLayout layout) {
                        Log.i(TAG, "onStartClose");
                    }

                    @Override
                    public void onClose(SwipeLayout layout) {
                        Log.i(TAG, "onClose");
                    }

                    @Override
                    public void onUpdate(SwipeLayout layout, int leftOffset, int topOffset) {

                    }

                    @Override
                    public void onHandRelease(SwipeLayout layout, float xvel, float yvel) {

                    }
                });*/
                convertView.setTag(groupHolder);
            } else {
                groupHolder = (GroupHolder) convertView.getTag();
            }

            groupHolder.textView.setText(((StockHistory)getGroup(groupPosition)).date);
            if (isExpanded)// ture is Expanded or false is not isExpanded
                groupHolder.textView.setBackgroundColor(Color.GREEN);
            else
                groupHolder.textView.setBackgroundColor(Color.WHITE);
            return convertView;
        }


        @Override
        public View getChildView(int groupPosition, int childPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new TextView(this.context);
            }
            TextView textView = (TextView) convertView;
            textView.setTextSize(13);
            textView.setText(((StockHistory)getGroup(groupPosition)).data.get(childPosition));
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }
    public void deleteOneLineData(int index){
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
        }catch (IOException io){
            io.printStackTrace();
        }finally {
            csvReader.close();
        }

        if(file.exists()){
            file.delete();
        }
        oldData.remove(index);
        CsvWriter csvWriter  = null;
        try {
            csvWriter = new CsvWriter(new OutputStreamWriter(this.openFileOutput("history.csv", MODE_WORLD_READABLE)), ',');
            for (int i = 0; i < oldData.size(); i++) {
                //csvWriter.writeRecord(oldData.get(i).split(","));
                csvWriter.write(oldData.get(i));
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            csvWriter.close();
        }
        datas.remove(index);
        myexpandableListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
}
