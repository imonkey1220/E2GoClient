package tw.imonkey.e2goclient;


import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.google.android.gms.vision.CameraSource.CAMERA_FACING_FRONT;

public class QRActivity extends AppCompatActivity {
    BarcodeDetector barcodeDetector;
    CameraSource cameraSource;
    SurfaceView cameraView;
    TextView barcodeValue;
    TextToSpeech tts ;
    DatabaseReference mCountClient;
    Map<String, Object> countClient = new HashMap<>();
    String memberEmail,deviceId,number;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);
        Bundle extras = getIntent().getExtras();
        memberEmail = extras.getString("memberEmail");
        deviceId = extras.getString("deviceId");
        number= extras.getString("number");
        cameraView = (SurfaceView) findViewById(R.id.camera_view);
        barcodeValue = (TextView) findViewById(R.id.code_info);
        tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.TAIWAN);
                }
            }
        });
        takeCard();
    }
    public void takeCard(){

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.ALL_FORMATS)
                .build();
        if (!barcodeDetector.isOperational()) {
            barcodeValue.setText("Could not set up the detector!");
            return;
        }

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .setAutoFocusEnabled(true) //you should add this feature
                .setFacing(CAMERA_FACING_FRONT)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    //noinspection MissingPermission
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException ex) {
                    ex.printStackTrace();
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

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    barcodeValue.post(new Runnable() {
                        @Override
                        public void run() {
                            barcodeValue.setText(barcodes.valueAt(0).displayValue);
                            barcodeDetected(barcodes.valueAt(0).displayValue);
                            cameraSource.stop();
                        }
                    });
                }
            }
        });
    }

    private void barcodeDetected(String s){
        Toast.makeText(this,"你的號碼:"+number+"號",Toast.LENGTH_LONG).show();
        mCountClient= FirebaseDatabase.getInstance().getReference("/LOG/QMS/"+deviceId+"/CLIENT");
        countClient.clear();
        countClient.put("message",Integer.parseInt(number)+1);
        countClient.put("memberEmail",s);
        countClient.put("timeStamp", ServerValue.TIMESTAMP);
        mCountClient.push().setValue(countClient);
        String toSpeak ="你的號碼是"+number+"號";
        tts.speak(toSpeak, TextToSpeech.QUEUE_ADD, null,null);
        Intent intent = new Intent(this,QMSActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraSource!=null) {
            cameraSource.release();
        }
        if (barcodeDetector!=null) {
            barcodeDetector.release();
        }
        if(tts !=null){
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this,QMSActivity.class);
        startActivity(intent);
        finish();
    }
}
