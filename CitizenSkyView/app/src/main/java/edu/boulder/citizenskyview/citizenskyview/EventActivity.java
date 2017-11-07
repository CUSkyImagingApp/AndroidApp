package edu.boulder.citizenskyview.citizenskyview;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.instacart.library.truetime.TrueTime;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class EventActivity extends AppCompatActivity implements SensorEventListener{


    private static final String TAG = EventActivity.class.getSimpleName();
    Vibrator v;
    private static SensorManager sensorService;
    Sensor accelerometer;
    Sensor magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;
    float azimuth;
    float pitch;
    float roll;
    String a =  "0";
    String p = "0";
    String r = "0";
    String eventStart;
    boolean calibrated = false;
    boolean launch = false;
    boolean north = false;
    int n = 0;
    int truTime = 0;
    static final int IMAGING_REQUEST = 1;
    @BindView(R.id.start_event) Button startButton;
    @BindView(R.id.event_text) TextView eventText;

    CountDownTimer cdt = new CountDownTimer(32000, 800){
        public void onTick(long millisUntilFinished){
            if(north){
                v.vibrate(600);
                n++;
                if(n > 5){
                    startImaging();
                }
            }else{
                n = 0;
            }
        }
        public void onFinish(){
//            northFailed();
            startImaging();
        }
    };
    CountDownTimer waitEvent = new CountDownTimer(4000, 4000) {
        @Override
        public void onTick(long millisUntilFinished) {
            //
        }

        @Override
        public void onFinish() {
            cdt.start();
        }
    };
    CountDownTimer trueTimeWait = new CountDownTimer(1000,1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            //
        }

        @Override
        public void onFinish() {
            if(!TrueTime.isInitialized() && truTime > 10){
                v.vibrate(5000);
                eventText.setText(getString(R.string.failed_sync));
            }
            else {
                truTime++;
                startImaging();
            }
        }
    };




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        //UPDATE Sensors for calibration
        sensorService = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magnetometer = sensorService.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); //v is a Vibrator used as a variable


        //UPDATE Initialize TrueTime
        new InitTrueTimeAsyncTask().execute();
        ButterKnife.bind(this);

        setUpEvent();

    }

    public void setUpEvent(){
        Intent myIntent = getIntent();
        eventStart = myIntent.getStringExtra("Date");
        String datePattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
        dateFormat.setTimeZone(TimeZone.getTimeZone("MST"));
        Date eDate = new Date();
        try{
            eDate = dateFormat.parse(eventStart);
        } catch(ParseException e) {
            e.printStackTrace();
        }
        String ret = getString(R.string.event_string, formatDate(eDate, "MM/dd/yy hh:mma", TimeZone.getTimeZone("MST")));
        String r = ret.replace("AM", "am").replace("PM","pm");
        eventText.setText(r);
    }

    @Override
    protected void onResume(){
        super.onResume();
        //UPDATE Continue updating current compass direction
        sensorService.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_UI);
        sensorService.registerListener(this, magnetometer,SensorManager.SENSOR_DELAY_UI);
    }

    //UPDATE Function for formatting date
    private String formatDate(Date date, String pattern, TimeZone timeZone) {
        DateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
        format.setTimeZone(timeZone);
        return format.format(date);
    }

    @OnClick(R.id.start_event)  //UPDATE launch ImagingActivity on start_event button
    public void imageBtnPush() {
        startButton.setVisibility(View.INVISIBLE);
        eventText.setText(getText(R.string.instructions));
        waitEvent.start();
//

    }

    public void startImaging(){
        cdt.cancel();
        if (!TrueTime.isInitialized()) {
            new InitTrueTimeAsyncTask().execute();
            trueTimeWait.start();
        }
        else{
            Intent myIntent = new Intent(EventActivity.this, ImagingActivity.class);
            myIntent.putExtra("azimuth", a);
            myIntent.putExtra("pitch", p);
            myIntent.putExtra("roll", r);
            myIntent.putExtra("Date", eventStart);
            startActivityForResult(myIntent, IMAGING_REQUEST);
        }

    }

    public void northFailed(){
        eventText.setText(getText(R.string.failed_north));
        v.vibrate(5000);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == IMAGING_REQUEST){
            if(resultCode == RESULT_OK){
                eventText.setTextSize(24);
                eventText.setText(getText(R.string.finished_str));
            }
            else if(resultCode == RESULT_CANCELED){
                //
            }
        }
    }




    @Override
    public void onSensorChanged(SensorEvent event) {
        //UPDATE Getting orientation of phone from Accelerometer and Magnetometer
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {

                // orientation contains azimut, pitch and roll
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                azimuth = orientation[0];
                pitch = orientation[1];
                roll = orientation[2];
            }
        }
        a = String.valueOf(azimuth);
        p = String.valueOf(pitch);
        r = String.valueOf(roll);

        //UPDATE If compass direction is between 0.2 and -0.2 Radians stop the vibration
        if(Float.valueOf(a) > -0.15 && Float.valueOf(a) < 0.15 ){
            north = true;
        } else {
            north = false;
        }
    }

    @Override //UPDATE Ignore for now
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    //UPDATE Initialize TrueTime object
    private class InitTrueTimeAsyncTask
            extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... params) {
            try {
                TrueTime.build()
                        .withNtpHost("0.north-america.pool.ntp.org")
                        .withLoggingEnabled(false)
                        .withConnectionTimeout(3_1428)
                        .initialize();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Exception when trying to get TrueTime", e);
            }
            return null;
        }
    }
    @Override
    protected void onPause(){
        super.onPause();
        v.cancel();
    }
    @Override
    protected void onStop(){
        super.onStop();
        v.cancel();
    }



}

