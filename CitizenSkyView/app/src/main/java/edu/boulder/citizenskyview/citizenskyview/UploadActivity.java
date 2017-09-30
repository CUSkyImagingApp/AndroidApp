package edu.boulder.citizenskyview.citizenskyview;

import android.content.Context;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class UploadActivity extends AppCompatActivity {


    int count;
    int progress;
//    @BindView(R.id.progress_bar) ProgressBar pBar;
    CognitoCachingCredentialsProvider credentialsProvider;
    AmazonS3 s3;
    TransferUtility transferUtility;
    ProgressBar pBar;

    CountDownTimer waitEvent = new CountDownTimer(4000, 2000) {
        @Override
        public void onTick(long millisUntilFinished) {
            Toast.makeText(UploadActivity.this, "waiting", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFinish() {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                BuildConfig.DYNAMODB_API_KEY,
                Regions.US_WEST_2
        );
        s3 = new AmazonS3Client(credentialsProvider);
        transferUtility = new TransferUtility(s3, getApplicationContext());

        initialize();
        ButterKnife.bind(this);
    }

    public void initialize(){
        count = 0;
        for(File file : getFilesDir().listFiles()){
            if(!file.isDirectory() && file.getName().endsWith(".jpeg")){
                count++;
            }
        }
        if(count != 0){
            pBar = (ProgressBar) findViewById(R.id.progress_bar);
            pBar.setMax(count);
            upload();
        } else {
            Toast.makeText(UploadActivity.this, "There were no photos to upload, thanks", Toast.LENGTH_LONG).show();
            finish();
        }

    }

    public void upload(){
        progress = 0;
        for(File file : getFilesDir().listFiles()){
            if(!file.isDirectory() && file.getName().endsWith(".jpeg")){
                TransferObserver observer = transferUtility.upload(
                        "citizenskyview",
                        "android/" + file.getName(),
                        file
                );
                observer.setTransferListener(new TransferListener(){
                    boolean errored = false;

                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        // do something
                        if (state.equals(TransferState.COMPLETED)){
                            if(!errored) {
                                delete();
                            }

                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        int percentage = (int) (bytesCurrent/bytesTotal * 100);
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        Toast.makeText(UploadActivity.this, "Error", Toast.LENGTH_SHORT).show();
                        errored = true;
                    }

                });
            }

        }

    }

    public void delete(){
        progress++;
        pBar.setProgress(progress);
        if(progress == count){
            for(File file : getFilesDir().listFiles()){
                if(!file.isDirectory() && file.getName().endsWith(".jpeg")){
                    file.delete();
                }
            }
            finish();
        }
    }

}
