package edu.boulder.citizenskyview.citizenskyview;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


public class EventActivity extends AppCompatActivity implements SensorEventListener{

    private Button vibrateButton;
    private Button startButton;
    private static final String TAG = EventActivity.class.getSimpleName();
    Vibrator v;
    private static SensorManager sensorService;
    Sensor accelerometer;
    Sensor magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;
    float azimuth;
    String a =  "0";
    @BindView(R.id.trueTime) TextView timeGMT;
    @BindView(R.id.devTime) TextView timeDeviceTime;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        //UPDATE Sensors for calibration
        sensorService = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magnetometer = sensorService.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //UPDATE Calibrate button causes phone to vibrate for 10 seconds
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE); //v is a Vibrator used as a variable
        vibrateButton = (Button) findViewById(R.id.calibrate_button); //set variable for button calibrate_button
        vibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //If calibrate_button is clicked do:
                v.vibrate(10000);
                Toast.makeText(getApplicationContext(), a, Toast.LENGTH_SHORT).show();
            }
        });

        //UPDATE Initialize TrueTime
        new InitTrueTimeAsyncTask().execute();
        ButterKnife.bind(this);

    }
    protected void onResume(){
        super.onResume();
        //UPDATE Continue updating current compass direction
        sensorService.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_UI);
        sensorService.registerListener(this, magnetometer,SensorManager.SENSOR_DELAY_UI);
    }
    //UPDATE Another way to set up a button click listener:
    @OnClick(R.id.time_event) //Do following function when time_event button is clicked
    public void onBtnPush(){
        if (!TrueTime.isInitialized()) {
            Toast.makeText(this, "Sorry TrueTime not yet initialized. Trying again.", Toast.LENGTH_SHORT).show();
            new InitTrueTimeAsyncTask().execute();
            return;
        }
        Date trueTime = TrueTime.now();
        Date deviceTime = new Date();

        timeGMT.setText(getString(R.string.true_time,
                _formatDate(trueTime, "yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("MST"))));

        timeDeviceTime.setText(getString(R.string.dev_time,
                _formatDate(deviceTime, "yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("MST"))));
        //UPDATE Displays true time and device time
    }
    //UPDATE Function for formatting date
    private String _formatDate(Date date, String pattern, TimeZone timeZone) {
        DateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
        format.setTimeZone(timeZone);
        return format.format(date);
    }

    @OnClick(R.id.start_event)  //UPDATE launch ImagingActivity on start_event button
    public void imageBtnPush() {
        Intent myIntent = new Intent(EventActivity.this, ImagingActivity.class);
        startActivity(myIntent);
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
            }
        }
        a = String.valueOf(azimuth);

        //UPDATE If compass direction is between 0.2 and -0.2 Radians stop the vibration
        if(Float.valueOf(a) > -0.2 && Float.valueOf(a) < 0.2 ){
            v.cancel();
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



}

