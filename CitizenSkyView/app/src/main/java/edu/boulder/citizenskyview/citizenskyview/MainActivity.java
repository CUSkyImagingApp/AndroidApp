package edu.boulder.citizenskyview.citizenskyview;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int requestCamera = 0;
    private static final int requestWrite = 1;
    private static Context mContext;
    private Button startButton;
    private Button helpButton;

    private boolean skyViewActive = false;
    private ImageReader skyViewImageReader;
    private final ImageReader.OnImageAvailableListener skyViewImageAvailableListner = new
            ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {

                }
            };

    private String cameraID;
    private CameraDevice skyViewCamera;
    private CameraDevice.StateCallback skyViewCameraStateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            skyViewCamera = cameraDevice;
            //for testing remove later
            Toast.makeText(getApplicationContext(), "Camera Found!", Toast.LENGTH_SHORT).show();
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

    private File skyViewImageFolder;
    private String skyViewImageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = getApplicationContext();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        setupCamera();
        connectToCamera();
        createSkyViewImageFolder();


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
                skyViewActive = true;
                checkWriteToExternalStoragePermission();
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        setupCamera();
        connectToCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == requestCamera){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(),
                        "Camera is required to document the sky", Toast.LENGTH_SHORT).show();

            }
        }
        if(requestCode == requestWrite) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                try {
                    createImageFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                Toast.makeText(getApplicationContext(),
                        "File access is required to document the sky", Toast.LENGTH_SHORT).show();
            }
        }
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

    private void connectToCamera(){
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED){
                    cameraManager.openCamera(cameraID,skyViewCameraStateCallBack,cameraHandler);
                }else{
                    if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                        Toast.makeText(this, "Camera is required to document the sky.", Toast.LENGTH_SHORT).show();
                    }
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, requestCamera);
                }

            }else{
                cameraManager.openCamera(cameraID, skyViewCameraStateCallBack, cameraHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void createSkyViewImageFolder(){
        File base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        skyViewImageFolder = new File(base, "SkyViewImages");
        if(!skyViewImageFolder.exists()){
            skyViewImageFolder.mkdirs();
        }
    }

    private File createImageFileName() throws IOException{
        String timeOfImage = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File imageFile = File.createTempFile(timeOfImage, ".png", skyViewImageFolder);
        skyViewImageName = imageFile.getAbsolutePath();
        return imageFile;
    }

    private void checkWriteToExternalStoragePermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED){
                try {
                    createImageFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else{
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    Toast.makeText(this, "The app needs to save some temperary files to your phone", Toast.LENGTH_SHORT).show();

                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestWrite);
            }

        }else{

            try {
                createImageFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
