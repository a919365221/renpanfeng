

package com.stock;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import java.util.Timer;
import java.util.TimerTask;

public class WelcomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //LangHelper.initLanguage(this.getBaseContext());
        setContentView(R.layout.activity_welcome);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                WelcomeActivity.this.finish();
            }
        }, 1500);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //
        } else {
            //
        }
        //LangHelper.showLanguage(this.getBaseContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        //LangHelper.showLanguage(this.getBaseContext());
    }
}
