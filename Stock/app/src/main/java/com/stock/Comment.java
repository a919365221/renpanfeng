package com.stock;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.stock.util.StockUtil;

public class Comment extends Activity {

    private WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.comment);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //StockUtil stockUtil = new StockUtil();
                //stockUtil.getCommentCount("002224");

            }
        });
        //thread.start();
        webView = (WebView)findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        webView.loadUrl("http://guba.eastmoney.com/");
    }


}
