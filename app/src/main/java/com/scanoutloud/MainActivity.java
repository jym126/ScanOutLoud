package com.scanoutloud;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static android.Manifest.permission.CAMERA;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private SurfaceView surfaceView;

    private CameraSource cameraSource;

    private TextToSpeech textToSpeech;
    private String stringResult = null;

    Button saveButton, readFile, clearButton;
    String myData = "";
    File myExternalFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{CAMERA}, PackageManager.PERMISSION_GRANTED);
        textToSpeech = new TextToSpeech(this, status -> {
        });


    }

    //Verifying external storage//
    private static boolean isExternalStorageReadOnly() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extStorageState)) {
            return true;
        }
        return false;
    }

    private static boolean isExternalStorageAvailable() {
        String extStorageState = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(extStorageState)) {
            return true;
        }
        return false;
    }
    
    //to change the layout portrait/landscape according to orientation
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_main);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            setContentView(R.layout.activity_main);
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraSource.release();
    }

    //Scanning processs//
    private void textRecognizer() {
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                .setRequestedPreviewSize(1280, 1024)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setAutoFocusEnabled(true)
                .setRequestedFps(25.0f)
                .build();

        surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {

                    if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                        int requestPermissionID = 0;
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                requestPermissionID);
                        return;
                    }
                    cameraSource.start(surfaceView.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });


        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {

                SparseArray<TextBlock> sparseArray = detections.getDetectedItems();
                StringBuilder stringBuilder = new StringBuilder();

                for (int i = 0; i < sparseArray.size(); ++i) {
                    TextBlock textBlock = sparseArray.valueAt(i);
                    if (textBlock != null) {
                        textBlock.getValue();
                        stringBuilder.append(textBlock.getValue()).append(" ");
                    }
                }

                final String stringText = stringBuilder.toString();

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> {
                    stringResult = stringText;
                    resultObtained();

                });

            }
        });
    }

                                         // Aplication Buttons //
    //Read again button//
    private void resultObtained() {
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        textView.setText(stringResult);
        textToSpeech.speak(stringResult, TextToSpeech.QUEUE_FLUSH, null, null);

        saveButton = findViewById(R.id.saveButton);
        readFile = findViewById(R.id.readFileButton);
        clearButton = findViewById(R.id.clearButton);
        textView = findViewById(R.id.textView);
        String text = textView.getText().toString();

        // save file buttons //

        saveButton.setOnClickListener(v -> {
            try {
                FileOutputStream fos = new FileOutputStream(myExternalFile);
                byte[] textArray = text.getBytes();
                fos.write(textArray);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            textView.setText("");
            Toast.makeText(getApplicationContext(),"Data saved",Toast.LENGTH_LONG).show();

        });

        //read file Button //
        readFile.setOnClickListener(v -> {
            try {
                FileInputStream fis = new FileInputStream(myExternalFile);
                DataInputStream in = new DataInputStream(fis);
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(in));
                String strLine;
                while ((strLine = br.readLine()) != null) {
                    myData = myData + strLine;
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            textView.setText(myData);
            Toast.makeText(getApplicationContext(),"Data retrieved from storage",Toast.LENGTH_LONG).show();
        });

        if (!isExternalStorageAvailable() || isExternalStorageReadOnly()) {
            saveButton.setEnabled(false);
        }
        else {
            String filename = "Textoutloud.txt";
            String filepath = "MyFileStorage";
            myExternalFile = new File(getExternalFilesDir(filepath), filename);
        }
    }
    //Start Button //
    public void buttonStart(View view) {
        setContentView(R.layout.surfaceview);
        textRecognizer();
    }

    public  void clearButton(View view){
        textView.setText(" ");
    }

    public void buttonReadAgain(View view){
        resultObtained();
    }

}