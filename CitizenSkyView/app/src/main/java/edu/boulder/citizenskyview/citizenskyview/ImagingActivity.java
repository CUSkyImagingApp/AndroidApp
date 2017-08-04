package edu.boulder.citizenskyview.citizenskyview;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImagingActivity extends HiddenCameraActivity {

    private CameraConfig mCameraConfig;
    int num = 0;
    @BindView(R.id.btn_takepicture) Button start_btn;
    @BindView(R.id.finished) TextView finishedText;
    Vibrator v;



    Runnable syncUp = new Runnable() {
        @Override
        public void run() {


            int second = 61;
            while (num < 4) {
                while (second % 5 != 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    second = getSecond();
                }
                if (num < 3){
                    takePicture();
                }
                num++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                second = getSecond();
                if (num > 3){
                    v.vibrate(3000);
                }
            }
            Intent returnIntent = new Intent();
            setResult(RESULT_OK, returnIntent);
            finish();

        }
    };
    public int getSecond(){
        Date time = TrueTime.now();
        SimpleDateFormat tFormat = new SimpleDateFormat("ss", Locale.US);
        String sec = tFormat.format(time);
        return Integer.valueOf(sec);
    }

    private void syncNow(){
        start_btn.setVisibility(View.GONE);
        Thread eventThread = new Thread(syncUp);
        eventThread.start();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_imaging);
        ButterKnife.bind(this);

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);


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

        File newpic = new File(imageFile.getParent(),"newpic"+num+".jpeg");
        if(imageFile.renameTo(newpic)){
            Toast.makeText(ImagingActivity.this, "Saved: " + newpic.getName() , Toast.LENGTH_LONG).show();
        } else{
            Toast.makeText(ImagingActivity.this, "failure", Toast.LENGTH_LONG).show();
        }

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

}