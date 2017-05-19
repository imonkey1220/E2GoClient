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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import net.glxn.qrgen.android.QRCode;

import java.util.Locale;

public class POINTSActivity extends AppCompatActivity {
    String memberEmail,device,deviceId;
    FirebaseAuth mAuth;
    TextToSpeech tts ;
    FirebaseAuth.AuthStateListener mAuthListener;
    DatabaseReference mDevice,presenceRef,lastOnlineRef,connectedRef, mFriend,connectedRefF ;
    public static final String devicePrefs = "devicePrefs";
    public static final String service="POINTS"; //集點機 deviceType

    ImageView POINTSQRImage ;
    DatabaseReference  mPOINTS,mPOINTSServerLive;
    String ACT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_points);
        SharedPreferences settings = getSharedPreferences(devicePrefs+service, Context.MODE_PRIVATE);
        deviceId = settings.getString("deviceId",null);
        device = settings.getString("device","imonkey.tw");
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
        POINTSQRImage = (ImageView) findViewById(R.id.imageViewPoints);
    }

    private void setup(){
        Intent intent = new Intent(POINTSActivity.this, AddDeviceActivity.class);
        intent.putExtra("memberEmail", memberEmail);
        intent.putExtra("deviceType", "集點機");
        intent.putExtra("service",service);
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
        mDevice=FirebaseDatabase.getInstance().getReference("/DEVICE/"+deviceId+"/ACT/");
        mDevice.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ACT= snapshot.getValue().toString();
                mPOINTS= FirebaseDatabase.getInstance().getReference("/LOG/POINTS/"+deviceId+"/"+ACT+"/");
                mPOINTS.limitToLast(1).addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        String normalText = deviceId+":"+service+":"+ACT+","+dataSnapshot.child("message").getValue().toString()+":"+mPOINTS.push().getKey();
                        String normalTextEnc=encry(normalText);
                        Bitmap bitmap = QRCode.from(normalTextEnc).withSize(250, 250).bitmap();
                        POINTSQRImage.setImageBitmap(bitmap);
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
            }
            @Override
            public void onCancelled(DatabaseError error) {
            }
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
    private void deviceOnline(){
        mPOINTSServerLive= FirebaseDatabase.getInstance().getReference("/LOG/POINTS/"+deviceId+"/connection");
        mPOINTSServerLive.setValue(true);
        mPOINTSServerLive.onDisconnect().setValue(null);

        presenceRef = FirebaseDatabase.getInstance().getReference("/FUI/"+memberEmail.replace(".", "_")+"/"+deviceId+"/connection");
        presenceRef.setValue(true);
        presenceRef.onDisconnect().setValue(null);
        lastOnlineRef =FirebaseDatabase.getInstance().getReference("/FUI/"+memberEmail.replace(".", "_")+"/"+deviceId+"/lastOnline");
        lastOnlineRef.onDisconnect().setValue(ServerValue.TIMESTAMP);
        connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    presenceRef.setValue(true);
                    mPOINTSServerLive.setValue(true);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
            }
        });

        mFriend= FirebaseDatabase.getInstance().getReference("/DEVICE/"+deviceId+"/friend/");
        mFriend.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    final DatabaseReference  presenceRefF= FirebaseDatabase.getInstance().getReference("/FUI/"+childSnapshot.getValue().toString().replace(".", "_")+"/"+deviceId+"/connection");//childSnapshot.getValue().toString():email
                    presenceRefF.setValue(true);
                    presenceRefF.onDisconnect().setValue(null);
                    connectedRefF = FirebaseDatabase.getInstance().getReference(".info/connected");
                    connectedRefF.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            Boolean connected = snapshot.getValue(Boolean.class);
                            if (connected) {
                                presenceRefF.setValue(true);
                            }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                        }
                    });
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

}
