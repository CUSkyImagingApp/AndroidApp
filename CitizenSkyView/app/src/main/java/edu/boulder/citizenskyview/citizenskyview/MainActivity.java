package edu.boulder.citizenskyview.citizenskyview;


import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.ImageReader;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.ArgumentMarshaller;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

import com.instacart.library.truetime.TrueTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;


import static android.os.SystemClock.uptimeMillis;


public class MainActivity extends AppCompatActivity {

   /* CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
            getApplicationContext(),
            BuildConfig.DYNAMODB_API_KEY,
            Regions.US_WEST_2
    );
    */
   //Toast.makeText(MainActivity.this, BuildConfig.DYNAMODB_API_KEY, Toast.LENGTH_SHORT).show();

/*
    AmazonDynamoDBClient ddbClient = new AmazonDynamoDBClient(credentialsProvider);
    DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);
    SkyViewEvent e1 = new SkyViewEvent();
    SkyViewPhoto p1 = new SkyViewPhoto();*/


//    Handler tempH = new Handler();
//    int delay = 30000; //30 seconds
//    Runnable runnable = new Runnable() {
//        @Override
//        public void run() {
//            dispatchTakePictureIntent();
//        }
//    };
//
//    public void takeNphotos(int N){
//        long currentTime = uptimeMillis();
//        for (;N>0;N--) {
//            tempH.postAtTime(runnable, currentTime+delay*N);
//        }
//    }

    private String APIKey = BuildConfig.AMAZON_API_KEY;
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int requestCamera = 1;
    private static final int requestWrite = 1;
    private static final int requestInternet = 0;
    private static final int requestAccessFineLocation = 1;
    private static final int requestRead = 0;
    private static final int requestVibrate = 0;

    private static Context mContext;
    private Button startButton;
    private Button boxButton1;
    private Button boxButton2;
    private Button helpButton;
    private CognitoCachingCredentialsProvider credentialsProvider;
    private AmazonDynamoDBClient ddbClient;
    private DynamoDBMapper mapper;
    List<SkyViewEvent> LSVE;

    @NonNull
    @Override
    public LayoutInflater getLayoutInflater() {
        return super.getLayoutInflater();
    }


    @BindView(R.id.upload_btn) Button uploadBtn;
    @BindView(R.id.refresh_btn) Button refreshBtn;

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
            //Toast.makeText(MainActivity.this, "Camera Found!", Toast.LENGTH_SHORT).show();
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

    CountDownTimer trueTimeWait = new CountDownTimer(10000,1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            if(!TrueTime.isInitialized()){
                new MainActivity.InitTrueTimeAsyncTask().execute();
            }
            else{
                Toast.makeText(MainActivity.this, "Your phone time is synced", Toast.LENGTH_SHORT).show();
                trueTimeWait.cancel();
            }
        }

        @Override
        public void onFinish() {
            Toast.makeText(MainActivity.this, "Unable to sync phone time, please refresh again", Toast.LENGTH_SHORT).show();
        }
    };

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


    Runnable getEvents = new Runnable() {
        @Override
        public void run() {
//            SkyViewEvent sve = new SkyViewEvent();
//            SkyViewEvent selectedEvent = mapper.load(SkyViewEvent.class, "1");
//            String start = selectedEvent.getStart();
            DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
            LSVE = mapper.scan(SkyViewEvent.class, scanExpression);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = MainActivity.this;
        supportRequestWindowFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        createSkyViewImageFolder();
        checkAccessFineLocationPermission();
        checkInternetPermission();
        checkVibrationPermission();
        checkReadToExternalStoragePermission();
        checkWriteToExternalStoragePermission();

        setupCamera();
        connectToCamera();

        updateEventFile();
        updateEventButtons();
        updateIdFile();
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                BuildConfig.DYNAMODB_API_KEY,
                Regions.US_WEST_2
        );
        ddbClient = new AmazonDynamoDBClient(credentialsProvider);
        ddbClient.setRegion(Region.getRegion(Regions.US_WEST_2));
        mapper = new DynamoDBMapper(ddbClient);
        ButterKnife.bind(this);


    }


    @OnClick(R.id.upload_btn)
    public void uploadLaunch(){
        Intent myIntent = new Intent(MainActivity.this, UploadActivity.class);
        startActivity(myIntent);
    }

    @OnClick(R.id.refresh_btn)
    public void refreshEvents(){
        Thread mthread = new Thread(getEvents);
        mthread.start();
        String contents = "";
        Date sDate = new Date();
        Date eDate = new Date();
        String start = "";
        String end = "";
        String datePattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
        try{
            mthread.join();
            for(SkyViewEvent event : LSVE){
                start = event.getStart().toString();
                end = event.getEnd().toString();
                    try {
                        sDate = dateFormat.parse(start);
                        eDate = dateFormat.parse(end);
                        if (contents.equals("")) {
                            contents = start + "split" + end;
                        } else {
                            contents = contents + "\n" + start + "split" + end;
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }


            }
            //Collections.sort(startDates);
        }
        catch(Exception e){
            e.printStackTrace();
        }
        Context context = getContext();
        String filename = "eventlist.txt";
        File file = new File(context.getFilesDir(), filename);
        FileOutputStream outputStream;
        try{
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(contents.getBytes());
            outputStream.close();
        } catch (Exception e){
            Toast.makeText(MainActivity.this, "File Failed", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        updateEventButtons();
        syncTrueTime();
    }

    public void getEventFromServer(){


    }

    private int randomID(){
        Random r = new Random();
        int res = 0;
        for(int i = 0; i<42; i++){
            res = res*10 + r.nextInt(10);
        }
        return res;
    }

    public void updateIdFile(){
        Context context = getContext();
        String filename = "uid.txt";
        String contents = "";
        File file = new File(context.getFilesDir(), filename);
        if (!file.exists()){
            contents = Integer.toString(randomID());
            FileOutputStream outputStream;
            try{
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(contents.getBytes());
                outputStream.close();
            } catch (Exception e){
                Toast.makeText(MainActivity.this, "File Failed", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }else{
        }

    }

    public String getIdFromFile(){
        String uid = "";
        try {
            Context context = getContext();
            File dir = context.getFilesDir();
            File file = new File(dir,"uid.txt");

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            line = br.readLine().replace("\n", "");
            uid = line;


            br.close() ;
        }catch (IOException e) {
            e.printStackTrace();
        }
        return uid;
    }

    public void db(){
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        List<SkyViewEvent> result = mapper.scan(SkyViewEvent.class, scanExpression);
        for (SkyViewEvent event : result){
            String start = event.getStart().toString();
            Toast.makeText(MainActivity.this, start, Toast.LENGTH_SHORT).show();
        }
    }

    public void updateEventFile(){
        Context context = getContext();
        String filename = "eventlist.txt";
        String contents = "";
        File file = new File(context.getFilesDir(), filename);
        if (!file.exists()){
            contents = "1999-01-01 12:00:00split1999-01-01 12:00:00";
            FileOutputStream outputStream;
            try{
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(contents.getBytes());
                outputStream.close();
            } catch (Exception e){
                Toast.makeText(MainActivity.this, "File Failed", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }else{
        }

    }

    public String getDateFromFile(int eventNum){
        String date = "1999-01-01 00:00:00split";
        StringBuilder text = new StringBuilder();
        try {
            Context context = getContext();
            File dir = context.getFilesDir();
            File file = new File(dir,"eventlist.txt");

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close() ;
        }catch (IOException e) {
            e.printStackTrace();
        }
        try{
            String[] lines = text.toString().split("\\n");
            date = lines[(eventNum)];
        } catch(Exception e){
            e.printStackTrace();
        }

        return date;
    }

    public void syncTrueTime(){
        trueTimeWait.start();
    }

    public void updateEventButtons(){
        int eventsInFile = 0;
        String[] date = getDateFromFile(eventsInFile).split("split");
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ConstraintLayout parent = (ConstraintLayout) inflater.inflate(R.layout.activity_main,
                null);
        LinearLayout eventL = (LinearLayout) parent.findViewById(R.id.list_event);
        while(!date[0].equalsIgnoreCase("1999-01-01 00:00:00")){
            View EventItem = inflater.inflate(R.layout.event_item, null);
            Button e = (Button) EventItem.findViewById(R.id.eventbutton);
            e.setText(formatEventString(date[0] + "\n"));
            e.setTag(Integer.toString(eventsInFile));
            e.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Button bbb = (Button) view;
                    launchEvent(Integer.parseInt(bbb.getTag().toString()));
                }
            });
            eventL.addView(EventItem);
            eventsInFile++;
            date = getDateFromFile(eventsInFile).split("split");
        }
        String datePattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat format = new SimpleDateFormat(datePattern);
        Date curTime = new Date();
        date[0] = format.format(curTime);

//        View EventItem = inflater.inflate(R.layout.event_item, null);
//        Button e = (Button) EventItem.findViewById(R.id.eventbutton);
//        e.setText(formatEventString(date));
//        e.setTag(Integer.toString(-1));
//        e.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Button bbb = (Button) view;
//                launchEvent(Integer.parseInt(bbb.getTag().toString()));
//            }
//        });
//        eventL.addView(EventItem);

        setContentView(parent);
        ButterKnife.bind(this);

    }

    private String formatEventString(String dateStr) {
        String datePattern = "yyyy-MM-dd HH:mm:ss";
        String eventPattern = "EEE, MMM d h:mma";
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
        SimpleDateFormat eventFormat = new SimpleDateFormat(eventPattern);
        Date eventDate = new Date();
        try{
            eventDate = dateFormat.parse(dateStr);
        } catch(ParseException e) {
            e.printStackTrace();
        }
        String ret = eventFormat.format(eventDate) + " Event";
        String r = ret.replace("AM", "am").replace("PM","pm");
        return r;

    }


    public void launchEvent(int eventNum){
        String datePattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
        String dateStr[] = {"",""};
        Calendar twoHourEvent = Calendar.getInstance();
        if(eventNum == -1){
            Date curTime = new Date();
            dateStr[0] = dateFormat.format(curTime);
            try{
                twoHourEvent.setTime( dateFormat.parse(dateStr[0]));
            } catch(ParseException e) {
                e.printStackTrace();
            }
            twoHourEvent.add(Calendar.HOUR, 2);
            dateStr[1] = dateFormat.format(twoHourEvent);
        }else{
            dateStr = getDateFromFile(eventNum).split("split");
        }
        Date sDate = new Date();
        Date eDate = new Date();
        Date curTime = new Date();
        try{
            sDate = dateFormat.parse(dateStr[0]);
            eDate = dateFormat.parse(dateStr[1]);
        } catch(ParseException e) {
            e.printStackTrace();
        }


        if(sDate.compareTo(curTime) * curTime.compareTo(eDate) >= 0){
            checkVibrationPermission();
            Intent myIntent = new Intent(MainActivity.this, EventActivity.class);
            myIntent.putExtra("Date", dateStr[0]);
            myIntent.putExtra("EndDate", dateStr[1]);
            myIntent.putExtra("uid", getIdFromFile());
            startActivity(myIntent);
        } else {
            Toast.makeText(MainActivity.this, "The event is not currently active", Toast.LENGTH_SHORT).show();
        }
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

        if(grantResults.length > 0){
        if(requestCode == requestCamera){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,
                        "Camera is required to document the sky", Toast.LENGTH_SHORT).show();

            }
        }
        if(requestCode == requestWrite) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                /*
                try {
                    createImageFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
            }else{
                Toast.makeText(MainActivity.this,
                        "File access is required to document the sky", Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode == requestRead){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,
                        "File access is required to document the sky", Toast.LENGTH_SHORT).show();

            }
        }
        if(requestCode == requestInternet){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MainActivity.this,
                        "Internet access is required to document the sky", Toast.LENGTH_SHORT).show();

            }
        }
        if(requestCode == requestAccessFineLocation){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MainActivity.this,
                        "File access is required to document the sky", Toast.LENGTH_SHORT).show();

            }
        }
        if(requestCode == requestVibrate){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MainActivity.this,
                        "Vibrator access is required to document the sky", Toast.LENGTH_SHORT).show();

            }
        }
    }
    }



    @Override
    public void onWindowFocusChanged(boolean focused){
        super.onWindowFocusChanged(focused);
        View decorView = getWindow().getDecorView();
        if(focused){
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    //|View.SYSTEM_UI_FLAG_FULLSCREEN
                    |View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    //|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
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
                        Toast.makeText(MainActivity.this, "Camera is required to document the sky.", Toast.LENGTH_SHORT).show();
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

    //Needs to be updated in Manifest does not work for more recent versions
    private File createImageFileName() throws IOException{
        String timeOfImage = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File imageFile = File.createTempFile(timeOfImage, ".png", skyViewImageFolder);
        skyViewImageName = imageFile.getAbsolutePath();
        return imageFile;
    }

    static final int REQUEST_TAKE_PHOTO = 1;

    private void dispatchTakePictureIntent() { //TODO
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFileName();
            } catch (IOException ex) {
                Toast.makeText(MainActivity.this, "IO Error", Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                        "com.boulderSkyView.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }



    private void checkWriteToExternalStoragePermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED){
                /*try {
                    createImageFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
            }else{
                if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    Toast.makeText(MainActivity.this, "The app needs to save some temperary files to your phone", Toast.LENGTH_SHORT).show();

                }
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestWrite);
            }

        }else{ //TODO
            /*try {
                createImageFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        }
    }
    private void checkReadToExternalStoragePermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED){

            }else{
                if(shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Toast.makeText(MainActivity.this, "The app needs to access tempory files on your phone", Toast.LENGTH_SHORT).show();

                }
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestRead);
            }
        }else{

        }
    }
    private void checkInternetPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET)
                    == PackageManager.PERMISSION_GRANTED){

            }else{
                if(shouldShowRequestPermissionRationale(Manifest.permission.INTERNET)){
                    Toast.makeText(MainActivity.this, "The app needs access to the internet", Toast.LENGTH_SHORT).show();

                }
                requestPermissions(new String[]{Manifest.permission.INTERNET}, requestInternet);
            }
        }else{

        }
    }
    private void checkAccessFineLocationPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED){

            }else{
                if(shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                    Toast.makeText(MainActivity.this, "The app needs access to file locations", Toast.LENGTH_SHORT).show();

                }
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, requestAccessFineLocation);
            }
        }else{

        }
    }
    private void checkVibrationPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.VIBRATE)
                    == PackageManager.PERMISSION_GRANTED){
                /*try {
                    createImageFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/

            }else{
                if(shouldShowRequestPermissionRationale(Manifest.permission.VIBRATE)){
                    Toast.makeText(MainActivity.this, "The app needs access to the phones vibrator", Toast.LENGTH_SHORT).show();

                }
                requestPermissions(new String[]{Manifest.permission.VIBRATE}, requestVibrate);
            }
        }else{
            /*try {
                    createImageFileName();
                } catch (IOException e) {
                    e.printStackTrace();
                }*/

        }
    }

}
