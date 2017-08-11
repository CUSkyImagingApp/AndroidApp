package edu.boulder.citizenskyview.citizenskyview;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {

    CountDownTimer waitEvent = new CountDownTimer(2000, 2000) {
        @Override
        public void onTick(long millisUntilFinished) {
            //
        }

        @Override
        public void onFinish() {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launch();

    }

    public void launch(){
        waitEvent.start();
    }
}
