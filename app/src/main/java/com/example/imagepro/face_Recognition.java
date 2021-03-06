package com.example.imagepro;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.function.Function;

public class face_Recognition  extends AppCompatActivity {

    String dateOfBirth, gender, job;



    private Interpreter interpreter;


    private  int INPUT_SIZE;


    private  int height=0;
    private int width=0;


    private GpuDelegate gpuDelegate=null;
    // This is used to run model using GPU


    private CascadeClassifier cascadeClassifier;


    face_Recognition(AssetManager assetManager, Context context, String modelPath, int input_size) throws IOException{
        // call this class in CameraActivity

        INPUT_SIZE = input_size;


        Interpreter.Options options = new Interpreter.Options();
        gpuDelegate = new GpuDelegate();



        // before load add number of threads
        options.setNumThreads(5); // choose number of thread according to your phone
        //if you want to increase frame-rate use maximum frame rate you phone support.
        // if you phone slow down due to this app reduce the number of threads



        interpreter = new Interpreter(loadModel(assetManager, modelPath), options);

        Log.d("face_Recognition", "model is loaded");


        try {

            InputStream inputStream=context.getResources().openRawResource(R.raw.haarcascade_frontalface_alt);

            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);

            File mCascadeFile = new File(cascadeDir,"haarcascade_frontalface_alt");

            FileOutputStream outputStream = new FileOutputStream(mCascadeFile);

            // create empty byte buffer to store byte
            byte[] buffer = new byte[4096];
            int bytRead;

            // when it read -1 that means no data to read
            while ((bytRead=inputStream.read(buffer)) != -1){
                outputStream.write(buffer,0,bytRead);
            }

            //when reading file is complete
            inputStream.close();
            outputStream.close();

            //now load cascade classifier
            //                                          path of save file
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());

            // if cascade classifier is succesfully loaded
            Log.d("face_recognition", "classifier is loaded");
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }


    public Mat recognizeImage(Mat mat_image){

        Core.flip(mat_image.t(),mat_image,1);


        //convert mat_image to grayscale
        Mat grayscaleImage =new Mat();
        //                 input,   ouput,            convert type
        Imgproc.cvtColor(mat_image,grayscaleImage,Imgproc.COLOR_RGBA2GRAY);


        height=grayscaleImage.height();
        width=grayscaleImage.width();


        //define minimum height & width and width of face in frame
        int absoluteFaceSize =(int) (height*0.1);
        MatOfRect faces = new MatOfRect();


        if (cascadeClassifier !=null){
            //detect face in frame
            cascadeClassifier.detectMultiScale(grayscaleImage,faces,1.1,2,2,
                    new Size(absoluteFaceSize,absoluteFaceSize),new Size());
        }


        //convert faces to array
        Rect[] faceArray = faces.toArray();


        // loop through each faces
        for (int i=0; i<faceArray.length; i++){
            Imgproc.rectangle(mat_image,faceArray[i].tl(),faceArray[i].br(),new Scalar(0,255,0,255), 2);



            Rect roi = new Rect((int)faceArray[i].tl().x, (int)faceArray[i].tl().y,
                    ((int)faceArray[i].br().x)-((int)faceArray[i].tl().x),
                    ((int)faceArray[i].br().y)-((int)faceArray[i].tl().y));

            // roi is used to crop faces from image
            Mat cropped_rgb = new Mat(mat_image,roi);

            // convert cropped_rgb to bitmap
            Bitmap bitmap = null;
            bitmap = Bitmap.createBitmap(cropped_rgb.cols(),cropped_rgb.rows(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped_rgb,bitmap);

            // scale bitmap to model input size 105
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,INPUT_SIZE,INPUT_SIZE,false);

            // convert scaledBitmap to byteBuffer
            ByteBuffer byteBuffer = convertBitmapToByteBuferr(scaledBitmap);

            // create otput
            float[][] face_value = new float[1][1];
            interpreter.run(byteBuffer,face_value);

            //if you want to see face_val
            Log.d("face_Recognition", "Out: " + Array.get(Array.get(face_value,0),0));



           // we will read face_value
           float read_face= (float) Array.get(Array.get(face_value,0),0);

           // create a new function input as read_face & output as name
            String face_name=get_identity_name(read_face);


            Imgproc.putText(mat_image,""+face_name,
                    new Point((int)faceArray[i].tl().x+10,(int) faceArray[i].tl().y+20),
                    1,1.5,new Scalar(255,255,255,150),2);


           Log.d("sadsad",face_name);


            FirebaseDatabase database = FirebaseDatabase.getInstance();//veri yolu
            DatabaseReference veriyolu = database.getReference("humanID");

            veriyolu.child("name").setValue(face_name);
            veriyolu.child("dateOfBirth").setValue(dateOfBirth);
            veriyolu.child("gender").setValue(gender);
            veriyolu.child("job").setValue(job);
        }

        Core.flip(mat_image.t(),mat_image,0);

        return  mat_image;
    }

    private String get_identity_name(float read_face) {
        String identityName="";


        if (read_face >=0 & read_face <0.5){
            identityName="Courteney Cox";
            dateOfBirth = "15.06.1964";
            gender = "Female";
            job = "Actress / Producer / Director";
        }
        else if (read_face>=0.5 & read_face < 1.5){
            identityName="Arnold Schwarzenegger";
            dateOfBirth = "30.07.1947";
            gender = "Male";
            job = "Actor / Producer / Director";
        }
        else if (read_face>=1.5 & read_face < 2.5){
            identityName="Bhuvan Bam";
            dateOfBirth = "22.01.1994";
            gender = "Male";
            job = "Actor / Writer / Director";
        }
        else if (read_face>=2.5 & read_face < 3.5){
            identityName="Hardik Pandya";
            dateOfBirth = "11.10.1993";
            gender = "Male";
            job = "Actor";
        }
        else if (read_face>=3.5 & read_face < 4.5){
            identityName="David Schwimmer";
            dateOfBirth = "2.11.1966";
            gender = "Male";
            job = "Actor / Producer / Director";
        }
        else if (read_face>=4.5 & read_face < 5.5){
            identityName="Matt LeBlanc";
            dateOfBirth = "25.07.1967";
            gender = "Male";
            job = "Actor / Producer / Soundtrack";
        }
        else if (read_face>=5.5 & read_face < 6.5){
            identityName="Simon Helberg";
            dateOfBirth = "9.12.1980";
            gender = "Male";
            job = "Actor / Producer / Writer";
        }
        else if (read_face>=6.5 & read_face < 7.5){
            identityName="Scarlett Johansson";
            dateOfBirth = "22.11.1984";
            gender = "Female";
            job = "Actress / Producer / Director";
        }
        else if (read_face>=7.5 & read_face < 8.5){
            identityName="Pankaj Tripathi";
            dateOfBirth = "05.09.1976";
            gender = "Male";
            job = "Actor";
        }
        else if (read_face>=8.5 & read_face < 9.5){
            identityName="AYDIN BEKOGLU";
            dateOfBirth = "06.07.1997";
            gender = "Male";
            job = "Kocaeli University Student";
        }
       else if (read_face>=9.5 & read_face < 10.5){
            identityName="Matthew Perry";
            dateOfBirth = "19.08.1969";
            gender = "Male";
            job = "Actor / Producer / Writer";
        }
        else if (read_face>=10.5 & read_face < 11.5){
            identityName="Sylvester Stallone";
            dateOfBirth = "06.07.1964";
            gender = "Male";
            job = "Actor / Writer / Producer";
        }
        else if (read_face>=11.5 & read_face < 12.5){
            identityName="Messi";
            dateOfBirth = "24.06.1987";
            gender = "Male";
            job = "Football Player";
        }
        else if (read_face>=12.5 & read_face < 13.5){
            identityName="Jim Parsons";
            dateOfBirth = "24.03.1973";
            gender = "Male";
            job = "Actor / Soundtrack / Producer";
        }
        else if (read_face>=13.5 & read_face < 14.5){
            identityName="Not in Dataset";
            dateOfBirth = "None";
            gender = "None";
            job = "None";
        }
        else if (read_face>=14.5 & read_face < 15.5){
            identityName="Lisa Kudrow";
            dateOfBirth = "30.07.1963";
            gender = "Female";
            job = "Actress / Writer / Producer";
        }
        else if (read_face>=15.5 & read_face < 16.5){
            identityName="Muhammet Ali";
            dateOfBirth = "17.01.1942";
            gender = "Male";
            job = "Boxer";
        }
        else if (read_face>=16.5 & read_face < 17.5){
            identityName="Brad_Pitt";
            dateOfBirth = "18.12.1963";
            gender = "Male";
            job = "Producer / Actor";
        }
        else if (read_face>=17.5 & read_face < 18.5){
            identityName="Ronaldo";
            dateOfBirth = "05.02.1985";
            gender = "Male";
            job = "Football Player";
        }
        else if (read_face>=18.5 & read_face < 19.5){
            identityName="Virat Kohli";
            dateOfBirth = "05.11.1988";
            gender = "Male";
            job = "Avtor";
        }
        else if (read_face>=19.5 & read_face < 20.5){
            identityName="Angelina Jolie";
            dateOfBirth = "04.06.1975";
            gender = "Female";
            job = "Actress / Producer / Director";
        }
        else if (read_face>=20.5 & read_face < 21.5){
            identityName="Kunal Nayyar";
            dateOfBirth = "30.04.1981";
            gender = "Male";
            job = "Actor / Producer";
        }
        else if (read_face>=21.5 & read_face < 22.5){
            identityName="Manoj Bajpayee";
            dateOfBirth = "23.04.1969";
            gender = "Male";
            job = "Actor / Music Department / Producer";
        }
        else if (read_face>=22.5 & read_face < 23.5){
            identityName="Sachin Tendulkar";
            dateOfBirth = "24.04.1973";
            gender = "Male";
            job = "Actor / Producer";
        }
        else if (read_face>=23.5 & read_face < 24.5){
            identityName="Jennifer Aniston";
            dateOfBirth = "11.02.1969";
            gender = "Female";
            job = "Actress / Producer / Director";
        }
        else if (read_face>=24.5 & read_face < 25.5){
            identityName="SERKAN OZKAN";
            dateOfBirth = "1996";
            gender = "Male";
            job = "Kocaeli University Student";
        }
        else if (read_face>=25.5 & read_face < 26.5){
            identityName="Dhoni";
            dateOfBirth = "07.07.1981";
            gender = "Male";
            job = "Kriket Player";
        }
        else if (read_face>=26.5 & read_face < 27.5){
            identityName="Pewdiepie";
            dateOfBirth = "24.10.1989";
            gender = "Male";
            job = "Youtuber";
        }
        else if (read_face>=27.5 & read_face < 28.5){
            identityName="Aishwarya Rai";
            dateOfBirth = "01.11.1973";
            gender = "Female";
            job = "Actress / Producer / Soundtrack";
        }
        else if (read_face>=28.5 & read_face < 29.5){
            identityName="Johnny Galecki";
            dateOfBirth = "30.04.1975";
            gender = "Male";
            job = "Actor / Producer / Writer";
        }
        else if (read_face>=29.5 & read_face < 30.5){
            identityName="Rohit Sharma";
            dateOfBirth = "15.06.1985";
            gender = "Male";
            job = "Actor";
        }
        else{
            identityName="Suresh Raina";
            dateOfBirth = "28.05.1985";
            gender = "Male";
            job = "Actor";
        }
        return  identityName;
    }

    private ByteBuffer convertBitmapToByteBuferr(Bitmap scaledBitmap) {
        //define ByteBuffer
        ByteBuffer byteBuffer;

        //define input size
        int input_size=INPUT_SIZE;


        byteBuffer=ByteBuffer.allocateDirect(4*1*input_size*input_size*3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[input_size*input_size];
        scaledBitmap.getPixels(intValues,0,scaledBitmap.getWidth(),0,0,scaledBitmap.getWidth(),
                scaledBitmap.getHeight());
        int pixels =0;

        //loop throug each pixels
        for (int i=0; i<input_size;i++){
            for (int j=0; j<input_size; j++){
                // each pixels value
                final int val= intValues[pixels++];

                // put this pixel valye in byteBuffer
                byteBuffer.putFloat((((val>>16)&0xFF))/255.0f);
                byteBuffer.putFloat((((val>>8)&0xFF))/255.0f);
                byteBuffer.putFloat(((val&0xFF))/255.0f);

                //scaling pixels by from 0-255 to 0-1
            }
        }
        return  byteBuffer;
    }


    private MappedByteBuffer loadModel(AssetManager assetManager, String modelPath) throws IOException {

    // This will give description of modelPath
    AssetFileDescriptor assetFileDescriptor = assetManager.openFd(modelPath);

    //create a inputStream to read model path
    FileInputStream inputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
    FileChannel fileChannel = inputStream.getChannel();
    long startOffset= assetFileDescriptor.getStartOffset();
    long declaredLength=assetFileDescriptor.getDeclaredLength();
    return  fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }
}
