package com.stock.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.ViewGroup;

import com.stock.util.LogUtil;

/**
 * Created by Administrator on 2016/8/13.
 */
public class VerticalTextView extends View {
    public String text;
    public int len;
    public int max;
    Paint paint;
    int width;
    public VerticalTextView(Context context) {
        super(context);
        final float scale = context.getResources().getDisplayMetrics().density;
        width = (int) (30 * scale + 0.5f);
        paint = new Paint();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width,heightSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int height = getMeasuredHeight();
        int width = getMeasuredWidth();
        //LogUtil.i("VerticalTextView","width:"+width+",height:"+height);
        int rel = (int)((len*1.0f/max)*(height/2));
        paint.setStyle(Paint.Style.FILL);
        if(rel>=0){
            paint.setColor(Color.RED);
            canvas.drawRect(0,(height/2)-rel,width,(height/2),paint);
        }else{
            paint.setColor(Color.BLUE);
            canvas.drawRect(0,(height/2),width,(height/2)-rel,paint);
        }

        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.STROKE);
        if(rel>=0){
            canvas.drawRect(0,(height/2)-rel,width,(height/2),paint);
        }else{
            canvas.drawRect(0,(height/2),width,(height/2)-rel,paint);
        }

        paint.setColor(Color.WHITE);
        paint.setFakeBoldText(true);


        /*Path path = new Path();

        path.moveTo(width/6, 0);
        path.lineTo(width/6, height);

        canvas.drawTextOnPath(text, path, 0, 0, paint);*/
        float h = 0;
        if(text.contains("ggxx")){
            String []tmp = text.split("ggxx");
            paint.setTextSize(width*2.0f/6);
            float w = paint.measureText(tmp[0]);
            canvas.drawText(tmp[0],(width-w)/2,width*2/5,paint);
            h = h + 2 * width*2/5;
            text = tmp[1];
        }
        paint.setTextSize(width*2/3);

        float xx = width*2/3;
        for(int j= 0 ;j<text.length();j++){
            char c = text.charAt(j);

            String t = c+"";
            h = h + xx;
            float w = paint.measureText(t);
            canvas.drawText(t,(width-w)/2,h,paint);
        }
        super.onDraw(canvas);
    }

}
