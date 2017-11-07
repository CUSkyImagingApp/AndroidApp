package edu.boulder.citizenskyview.citizenskyview;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidhiddencamera.CameraConfig;
import com.androidhiddencamera.CameraError;
import com.androidhiddencamera.HiddenCameraActivity;
import com.androidhiddencamera.config.CameraFacing;
import com.androidhiddencamera.config.CameraImageFormat;
import com.androidhiddencamera.config.CameraResolution;
import com.androidhiddencamera.config.CameraRotation;
import com.instacart.library.truetime.TrueTime;

import org.w3c.dom.Text;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class ImagingActivity extends HiddenCameraActivity {

    private CameraConfig mCameraConfig;
    int num = 0;
//    @BindView(R.id.btn_takepicture) Button start_btn;
//    @BindView(R.id.finished) TextView finishedText;
    String longitude;
    String latitude;
    String a =  "0";
    String p = "0";
    String r = "0";
    Date eventStart = new Date();
    Calendar eventEnd = Calendar.getInstance();
    Vibrator v;
    String uid;



    Runnable syncUp = new Runnable() {
        @Override
        public void run() {


            int second = 61;
            Date curTime = TrueTime.now();
            while (num < 60 && eventStart.compareTo(curTime) * curTime.compareTo(eventEnd.getTime()) >= 0) {
                while (second % 30 != 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    second = getSecond();
                }
                if (num < 60){
                    takePicture();
                }
                num++;
                curTime = TrueTime.now();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                second = getSecond();


            }
            v.vibrate(5000);
            done();

        }
    };

    private void syncNow(){
//        start_btn.setVisibility(View.GONE);
        Thread eventThread = new Thread(syncUp);
        eventThread.start();

    }
    private void done(){
        Intent returnIntent = getIntent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_imaging);
        ButterKnife.bind(this);

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        Intent myIntent = getIntent();
        a = myIntent.getStringExtra("azimuth");
        p = myIntent.getStringExtra("pitch");
        r = myIntent.getStringExtra("roll");

        mCameraConfig = new CameraConfig()
                .getBuilder(this)
                .setCameraFacing(CameraFacing.REAR_FACING_CAMERA)
                .setCameraResolution(CameraResolution.MEDIUM_RESOLUTION)
                .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                .setImageRotation(CameraRotation.ROTATION_90)
                .build();


        //Check for the camera permission for the runtime
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            //Start camera preview
            startCamera(mCameraConfig);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }

        //Take a picture
//        findViewById(R.id.btn_takepicture).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                //Take picture using the camera without preview.
//                syncNow();
//            }
//        });
        uid = myIntent.getStringExtra("uid");
        getPhoneLocation();
        setTime();
        syncNow();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //noinspection MissingPermission
                startCamera(mCameraConfig);
            } else {
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onImageCapture(@NonNull File imageFile) {
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inPreferredConfig = Bitmap.Config.RGB_565;
//        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

        //Display the image to the image view
        //Toast.makeText(ImagingActivity.this, rootDir.toString(), Toast.LENGTH_LONG).show();
        //((ImageView) findViewById(R.id.texture)).setImageBitmap(bitmap);
        String name = getFilesDir().getPath()+"/" + generateName() + ".jpeg";
        File newpic = new File(name);

        FileInputStream inputStream;
        FileOutputStream outputStream;
        try{
            inputStream = new FileInputStream(imageFile);
            outputStream = new FileOutputStream(newpic);
            byte[] buffer = new byte[1024];
            int length;
            while((length = inputStream.read(buffer))>0){
                outputStream.write(buffer, 0, length);
            }
            inputStream.close();
            outputStream.close();
            imageFile.delete();
        } catch (Exception e){
            e.printStackTrace();
        }
//        if(imageFile.renameTo(newpic)){
//            Toast.makeText(ImagingActivity.this, "Saved: " + newpic.getName() , Toast.LENGTH_LONG).show();
//        } else{
//            Toast.makeText(ImagingActivity.this, "failure", Toast.LENGTH_LONG).show();
//        }

    }

    public String getPhoneLocation(){
        String re = "";
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Location location;
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener(){
                @Override
                public void onLocationChanged(Location location) {
                    // reverse geo-code location

                }

                @Override
                public void onProviderDisabled(String provider) {
                    // Auto-generated method stub

                }

                @Override
                public void onProviderEnabled(String provider) {
                    // Auto-generated method stub

                }

                @Override
                public void onStatusChanged(String provider, int status,
                                            Bundle extras) {
                    // Auto-generated method stub

                }
            }, null);
            location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            longitude = Double.toString(location.getLongitude());
            latitude = Double.toString(location.getLatitude());
            re = latitude + longitude;
        } else {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//            Toast.makeText(ImagingActivity.this, "Need Location Permission", Toast.LENGTH_LONG).show();
//            v.vibrate(3000);
//            Intent returnIntent = new Intent();
//            setResult(RESULT_CANCELED, returnIntent);
//            finish();
        }

        return re;
    }

    public void setTime(){
        String datePattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
        Intent myIntent = getIntent();
        String dateStr = myIntent.getStringExtra("Date");
        String dateEnd = myIntent.getStringExtra("EndDate");
        try{
            eventStart = dateFormat.parse(dateStr);
            eventEnd.setTime(dateFormat.parse(dateEnd));
        } catch(ParseException e) {
            e.printStackTrace();
        }

    }



    public String generateName(){
        String re = "";
        String d = getDay();
        String h = Integer.toString(getHour());
        String m = Integer.toString(getMinute());
        String s = "";
        if(getSecond() < 30){
            s = "00";
        } else {
            s = "30";
        }
        String ret = uid + "_" + latitude + "_" + longitude + "_" + a + "_" + p + "_" + r + "_" + d + "T" + h + "_" + m + "_" + s;
        re = ret.replace(".", ",");
        return re;
    }




    @Override
    public void onCameraError(@CameraError.CameraErrorCodes int errorCode) {
        switch (errorCode) {
            case CameraError.ERROR_CAMERA_OPEN_FAILED:
                //Camera open failed. Probably because another application
                //is using the camera
                Toast.makeText(this, "Cannot open camera.", Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_IMAGE_WRITE_FAILED:
                //Image write failed. Please check if you have provided WRITE_EXTERNAL_STORAGE permission
                Toast.makeText(this, "Cannot write image captured by camera.", Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE:
                //camera permission is not available
                //Ask for the camra permission before initializing it.
                Toast.makeText(this, "Camera permission not available.", Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION:
                //This error will never happen while hidden camera is used from activity or fragment
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA:
                Toast.makeText(this, "Your device does not have front camera.", Toast.LENGTH_LONG).show();
                break;
        }
    }

    public int getSecond(){
        Date time = TrueTime.now();
        SimpleDateFormat tFormat = new SimpleDateFormat("ss", Locale.US);
        String sec = tFormat.format(time);
        return Integer.valueOf(sec);
    }
    public int getMinute(){
        Date time = TrueTime.now();
        SimpleDateFormat tFormat = new SimpleDateFormat("mm", Locale.US);
        String min = tFormat.format(time);
        return Integer.valueOf(min);
    }
    public int getHour(){
        Date time = TrueTime.now();
        SimpleDateFormat tFormat = new SimpleDateFormat("hh", Locale.US);
        String hour = tFormat.format(time);
        return Integer.valueOf(hour);
    }
    public String getDay(){
        Date time = TrueTime.now();
        SimpleDateFormat tFormat = new SimpleDateFormat("yyyy_MM_dd", Locale.US);
        String day = tFormat.format(time);
        return day;
    }

}