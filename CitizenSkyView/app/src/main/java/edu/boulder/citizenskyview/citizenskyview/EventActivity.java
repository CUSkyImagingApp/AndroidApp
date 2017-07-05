package edu.boulder.citizenskyview.citizenskyview;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
/*
public class EventActivity extends AppCompatActivity {

    private Button vibrateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);


        vibrateButton = (Button) findViewById(R.id.calibrate_button);
        vibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(1000);
            }
        });


    }
}
*/

public class EventActivity extends AppCompatActivity implements SensorEventListener{

    private Button vibrateButton;
    Vibrator v;
    private static SensorManager sensorService;
    Sensor accelerometer;
    Sensor magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;
    float azimuth;
    String a =  "0";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        TrueTime.build().initialize();

        sensorService = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magnetometer = sensorService.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrateButton = (Button) findViewById(R.id.calibrate_button);
        vibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.vibrate(10000);
                Toast.makeText(getApplicationContext(), a, Toast.LENGTH_SHORT).show();
            }
        });

    }
    protected void onResume(){
        super.onResume();
        sensorService.registerListener(this, accelerometer,SensorManager.SENSOR_DELAY_UI);
        sensorService.registerListener(this, magnetometer,SensorManager.SENSOR_DELAY_UI);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
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
        if(Float.valueOf(a) > -0.2 && Float.valueOf(a) < 0.2 ){
            v.cancel();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}

