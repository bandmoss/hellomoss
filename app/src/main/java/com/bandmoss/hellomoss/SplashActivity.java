package com.bandmoss.hellomoss;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.bandmoss.hellomoss.util.Util;

/**
 * Created by rok on 2015. 3. 22..
 */
public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        Util.playIntro(getApplicationContext());
        findViewById(android.R.id.content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchMainActivity();
            }
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                launchMainActivity();
            }
        }, 2000);
    }

    private void launchMainActivity() {
        if(!isFinishing()) {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        }
    }
}
