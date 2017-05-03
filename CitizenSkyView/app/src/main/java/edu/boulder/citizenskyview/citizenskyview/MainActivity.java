package edu.boulder.citizenskyview.citizenskyview;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static Context mContext;
    private Button startButton;
    private Button helpButton;

    private String cameraID;
    private CameraDevice skyViewCamera;
    private CameraDevice.StateCallback skyViewCameraStateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            skyViewCamera = cameraDevice;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraDevice.close();
            skyViewCamera = null;

        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice,int error) {
            cameraDevice.close();
            skyViewCamera = null;

        }
    };

    private HandlerThread cameraThread;
    private Handler cameraHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupCamera();

        helpButton = (Button) findViewById(R.id.help_button);
        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Help Menu", Toast.LENGTH_SHORT).show();
            }
        });

        startButton = (Button) findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Starting sky capture", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        setupCamera();
    }

    @Override
    public void onWindowFocusChanged(boolean focused){
        super.onWindowFocusChanged(focused);
        View decorView = getWindow().getDecorView();
        if(focused){
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    |View.SYSTEM_UI_FLAG_FULLSCREEN
                    |View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    protected void onPause(){
        disconectCamera();
        super.onPause();
    }

    public static Context getContext(){
        return mContext;
    }

    private void disconectCamera() {
        if(skyViewCamera != null){
            skyViewCamera.close();
            skyViewCamera = null;
        }
    }

    private void startCameraThread(){
        cameraThread = new HandlerThread("CitizenSkyViewCameraThread");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    private void stopCameraThread(){
        cameraThread.quitSafely();
        try {
            cameraThread.join();
            cameraThread = null;
            cameraHandler = null;
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    private void setupCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
                if(cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }
                cameraID = cameraId;
                return;
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }
}
