package edu.boulder.citizenskyview.citizenskyview.network;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;

import edu.boulder.citizenskyview.citizenskyview.MainActivity;

/**
 * Created by jtr09 on 4/17/2017.
 */

public class S3Service {
    private TransferUtility transferUtility;
    private Context context;

    public S3Service(){
        context = MainActivity.getContext();
        try{
            //TODO: Get this configuration from a gradle.properties file
            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                    context,
                    "us-west-2:43473766-619f-4209-996b-7dc61e65ccf1",
                    Regions.US_WEST_2
            );
            AmazonS3 s3 = new AmazonS3Client(credentialsProvider);
            transferUtility = new TransferUtility(s3, context);
        }catch(Exception e){
            Log.e("error", e.getMessage());
        }
    }

    public void S3Upload (File img) throws Exception{
        if(transferUtility == null){
            //TODO: Custom exceptions
            throw new Exception("AWS S3 client not initialized.");
        }
        TransferObserver observer = transferUtility.upload(
                "cu-sky-imager",
                "test-img.jpg",
                img
        );
    }
}
