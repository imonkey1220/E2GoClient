package tw.imonkey.e2goclient;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;


import java.io.IOException;

public class QRSTAMPActivity extends AppCompatActivity {
    BarcodeDetector barcodeDetector;
    CameraSource cameraSource;
    SurfaceView cameraView;
    TextView barcodeValue;
    public static final String devicePrefs = "devicePrefs";
    String deviceId,clientNo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrqms);
        cameraView = (SurfaceView) findViewById(R.id.camera_view);
        barcodeValue = (TextView) findViewById(R.id.code_info);
        takeCard();
    }
    public void takeCard(){

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        if (!barcodeDetector.isOperational()) {
            barcodeValue.setText("Could not set up the detector!");
            return;
        }

        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .setAutoFocusEnabled(true) //you should add this feature
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
                            barcodeDetected(barcodes.valueAt(0).displayValue);
                            cameraSource.stop();
                        }
                    });
                }
            }
        });
    }

    private void barcodeDetected(String normalTextEnc) {
        // encrypt and decrypt using AES Algorithms
        String normalText = "";

        try {
            String seedValue = "imonkey.tw";
            normalText = AESHelper.decrypt(seedValue, normalTextEnc);
        } catch (Exception e) {
            e.printStackTrace();
            barcodeValue.setText("err");
        }
        //    normalText=normalTextEnc;
        barcodeValue.setText(normalText);
        deviceId = normalText.split(":")[0];
        clientNo = normalText.split(":")[1];
        barcodeValue.setText(clientNo);
        SharedPreferences.Editor editor = getSharedPreferences(devicePrefs+"STAMP", Context.MODE_PRIVATE).edit();
        editor.putString("deviceId",deviceId);
        editor.putString("clientNo",clientNo);
        editor.apply();
        Intent intent = new Intent(this, STAMPActivity.class);
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
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        finish();
    }
}
