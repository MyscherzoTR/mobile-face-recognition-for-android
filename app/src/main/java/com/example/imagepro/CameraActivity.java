package com.example.imagepro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.IOException;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";

    TextView identityName, dateOfBirth, gender, job;

    private Mat mRgba;
    private Mat mGray;
    private CameraBridgeViewBase mOpenCvCameraView;


    private ImageView flip_camera;
    /*define int that represent camera
            0 - back camera
            1- front camera
            initially it will start with back camera
            */
    private  int mCameraId=0;


    FirebaseDatabase database;
    DatabaseReference veriyolu;


    private  face_Recognition face_Recognition;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface
                        .SUCCESS: {
                    Log.i(TAG, "OpenCv is loaded");
                    mOpenCvCameraView.enableView();
                }
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public CameraActivity() {
        Log.i(TAG, "Instantiate new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);



        int MY_PERMISSIONS_REQUEST_CAMERA=0;

        // if camera permission is not given it will ask for it on device
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED)
        {
                ActivityCompat.requestPermissions(CameraActivity.this,new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA );

        }


        setContentView(R.layout.activity_camera);



        identityName=findViewById(R.id.identity_nameTXT);
        dateOfBirth = findViewById(R.id.dateOf_birthTXT);
        gender = findViewById(R.id.genderTXT);
        job = findViewById(R.id.jobTXT);


       /* database = FirebaseDatabase.getInstance();
        veriyolu = database.getReference("veriler");

        // Read from the database
        veriyolu.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot veritabanıVerisi) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                //veritabanı değeri veri tutucu da sakla
                String degerTutucu = veritabanıVerisi.getValue(String.class);
               identityName.setText(degerTutucu);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Toast.makeText(CameraActivity.this, ""+error, Toast.LENGTH_SHORT).show();
            }
        });*/


        database = FirebaseDatabase.getInstance();
        veriyolu = database.getReference("humanID");



        veriyolu.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot snp : snapshot.getChildren()){
                    String ad = (String) snp.child("ad").getValue();
                    System.out.println(snp.getKey() + " : " + snp.getValue());
                    if (snp.getKey() == "name"){
                        identityName.setText(snp.getValue().toString());
                    }
                    else if (snp.getKey() == "dateOfBirth"){
                        dateOfBirth.setText(snp.getValue().toString());
                    }
                    else if (snp.getKey() == "gender"){
                        gender.setText(snp.getValue().toString());
                    }
                    else if (snp.getKey() == "job"){
                        job.setText(snp.getValue().toString());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });




        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        flip_camera = findViewById(R.id.flip_camera);
        // when flip camera button is clicked
        flip_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {swapCamera();}
        });

        //mOpenCvCameraView.enableFpsMeter();

        try {
            int inputSize=105;
            //input size of model is 105
            face_Recognition = new face_Recognition(getAssets(),
                    CameraActivity.this,
                    "Newmodel.tflite",
                    inputSize);
        }
        catch ( IOException e){
            e.printStackTrace();
            Log.d("CameraActivity", "model is not loaded");
        }
    }

    private void swapCamera() {

         mCameraId = mCameraId^1;
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(mCameraId);
        mOpenCvCameraView.enableView();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()){
            Log.d(TAG,"Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            Log.d(TAG,"Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallback);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView !=null){
            mOpenCvCameraView.disableView();
        }
    }

    public  void  onDestroy()
    {
        super.onDestroy();
        if (mOpenCvCameraView != null){
            mOpenCvCameraView.disableView();
        }
    }

    public  void onCameraViewStarted(int width, int height){
        mRgba = new Mat(height,width, CvType.CV_8UC4);
        mGray = new Mat(height,width,CvType.CV_8UC1);
    }

    public  void  onCameraViewStopped()
    {
        mRgba.release();
    }

    public  Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        mRgba=inputFrame.rgba();
        mGray=inputFrame.gray();


        if (mCameraId==1)
        {
            Core.flip(mRgba,mRgba,-1);
            Core.flip(mGray,mGray,-1);
        }

        mRgba=face_Recognition.recognizeImage(mRgba);

        return  mRgba;
    }
}