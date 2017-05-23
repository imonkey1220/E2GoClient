package tw.imonkey.e2goclient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import net.glxn.qrgen.android.QRCode;

import java.util.Locale;

public class STAMPActivity extends AppCompatActivity {

    String memberEmail,deviceId;
    FirebaseAuth mAuth;
    TextToSpeech tts ;
    FirebaseAuth.AuthStateListener mAuthListener;
    DatabaseReference connectedRef;
    public static final String devicePrefs = "devicePrefs";
    public static final String service="STAMP"; //集章機 deviceType

    ImageView STAMPQRImage ;
    DatabaseReference  mSTAMP,mSTAMPServerLive;
    String clientNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stamp);
        SharedPreferences settings = getSharedPreferences(devicePrefs+service, Context.MODE_PRIVATE);
        deviceId = settings.getString("deviceId",null);
        clientNo= settings.getString("clientNo","0");
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user!=null){
                    memberEmail=user.getEmail();
                    if (deviceId==null){
                        setup();
                    }else{
                        init();
                    }
                }
            }
        };
        STAMPQRImage = (ImageView) findViewById(R.id.imageViewPoints);
    }

    private void setup(){
        Intent intent = new Intent(STAMPActivity.this, QRSTAMPActivity.class);
        startActivity(intent);
        finish();
    }

    private void init() {
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.TAIWAN);
                }
            }
        });

        mSTAMP= FirebaseDatabase.getInstance().getReference("/LOG/STAMP/"+deviceId+"/"+clientNo+"/");
        mSTAMP.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String normalText = deviceId+":"+service+":"+clientNo+","+dataSnapshot.child("message").getValue().toString()+":"+mSTAMP.push().getKey();
                String normalTextEnc=encry(normalText);
                Bitmap bitmap = QRCode.from(normalTextEnc).withSize(250, 250).bitmap();
                STAMPQRImage.setImageBitmap(bitmap);
            }
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        deviceOnline();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mAuth.removeAuthStateListener(mAuthListener);
        if(tts !=null){
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        finish();
    }



    private String encry(String normalText){
        // encrypt and decrypt using AES Algorithms
        try {
            String seedValue = "imonkey.tw";
            String normalTextEnc;
            normalTextEnc = AESHelper.encrypt(seedValue, normalText);
            //       Toast.makeText(MainActivity.this,AESHelper.decrypt(seedValue,normalTextEnc),Toast.LENGTH_LONG).show();
            return  normalTextEnc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "err";
    }

    //device online check
    private void deviceOnline() {
        mSTAMPServerLive = FirebaseDatabase.getInstance().getReference("/LOG/STAMP/" + deviceId + "/" + clientNo + "/connection");
        mSTAMPServerLive.setValue(true);
        mSTAMPServerLive.onDisconnect().setValue(null);

        connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    mSTAMPServerLive.setValue(true);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }
}
