package tw.imonkey.e2goclient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;


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

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;


public class TCActivity extends AppCompatActivity {
    TextView TVTCState,TVDeviceTC;
    TextClock textClock ;
    ImageView IVQRTC ;
    TextToSpeech tts ;
    String memberEmail,device,deviceId;
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;
    DatabaseReference mTCClient,mTCServer,presenceRef,lastOnlineRef,connectedRef, mFriend,connectedRefF,mTCDeviceLive;
    public static final String devicePrefs = "devicePrefs";
    public static final String service="TC";//打卡機 deviceType

    private Handler handler;
    Runnable runnable;
    int AtWork1=8*60*60*1000,AtWork2=-1,OffWork1=-1,OffWork2=17*60*60*1000;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tc);
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Taipei"));
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
        textClock = (TextClock) findViewById(R.id.textClock);
        TVDeviceTC = (TextView) findViewById(R.id.textViewDeviceTC);
        TVTCState= (TextView) findViewById(R.id.textViewTCState);
        TVDeviceTC.setText(device);
        IVQRTC = (ImageView) findViewById(R.id.imageViewQRTC);


    }
    private void setup(){
        Intent intent = new Intent(TCActivity.this, AddDeviceActivity.class);
        intent.putExtra("memberEmail", memberEmail);
        intent.putExtra("deviceType", "打卡機");
        intent.putExtra("service",service);
        startActivity(intent);
        finish();

    }
    private void init(){
        tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.TAIWAN);
                }
            }
        });
        mTCClient= FirebaseDatabase.getInstance().getReference("/LOG/TC/"+deviceId+"/CLIENT");


        TVTCState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IVQRTC.setVisibility(View.VISIBLE);
                if(TVTCState.getText().toString().equals("上班")){
                    TVTCState.setText("下班");
                }else if(TVTCState.getText().toString().equals("下班")){
                    TVTCState.setText("加班");
                }else if(TVTCState.getText().toString().equals("加班")){
                    TVTCState.setText("結束");
                }else {
                    TVTCState.setText("上班");
                }
                makeQR();

            }
        });
        /*
        TVTCState.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String normalText = deviceId+":"+service+":"+TVTCState.getText().toString()+":"+mTCClient.push().getKey();
                String normalTextEnc=encry(normalText);
                Bitmap bitmap = QRCode.from(normalTextEnc).withSize(250, 250).bitmap();
                IVQRTC.setImageBitmap(bitmap);
            }
        });
*/

        deviceOnline();

        mTCClient.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
          //      TVTCState.setText(dataSnapshot.child("message").getValue().toString());
                String toSpeak =dataSnapshot.child("message").getValue().toString()+"打卡";
                tts.speak(toSpeak,TextToSpeech.QUEUE_ADD, null,null);
                Animation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(100); //You can manage the time of the blink with this parameter
                anim.setStartOffset(20);
                anim.setRepeatMode(Animation.REVERSE);
                anim.setRepeatCount(10);
                TVTCState.startAnimation(anim);

                makeQR();
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

      duty();
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
        mTCDeviceLive=FirebaseDatabase.getInstance().getReference("/LOG/TC/"+deviceId+"/connection");
        mTCDeviceLive.setValue(true);
        mTCDeviceLive.onDisconnect().setValue(null);

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
                    mTCDeviceLive.setValue(true);
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

    private void duty(){
        Calendar cTime = Calendar.getInstance();
        if ((cTime.getTimeInMillis()+8*60*60*1000)%(24*60*60*1000)<=(AtWork1)){
            if (!TVTCState.getText().equals("上班")) {
                IVQRTC.setVisibility(View.VISIBLE);
                TVTCState.setText("上班");
                TVTCState.setTextColor(Color.BLUE);
                makeQR();
            }

        }else if((cTime.getTimeInMillis()+8*60*60*1000)%(24*60*60*1000)>=(OffWork2)){
            if (!TVTCState.getText().equals("下班")) {
                IVQRTC.setVisibility(View.VISIBLE);
                TVTCState.setText("下班");
                TVTCState.setTextColor(Color.BLUE);
                makeQR();
            }
        }else{
            if (!TVTCState.getText().equals("選擇")) {
                TVTCState.setText("選擇");
                TVTCState.setTextColor(Color.RED);
                IVQRTC.setVisibility(View.INVISIBLE);
            }
        }
        if((AtWork2>=0)&&(OffWork1>=0)) {
            //todo
        }

        makeQR();

        DatabaseReference mTCServer=FirebaseDatabase.getInstance().getReference("/LOG/TC/"+deviceId+"/SERVER/");
        mTCServer.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                            //todo
                if(snapshot.child("OnWork1").exists()&& snapshot.child("OffWork2").exists()){
                    AtWork1 =Integer.parseInt(snapshot.child("AtWork1").getValue().toString().split(":")[0])*60*60*1000+
                             Integer.parseInt(snapshot.child("AtWork1").getValue().toString().split(":")[1])*60*1000 ;
                    OffWork2 =Integer.parseInt(snapshot.child("OffWork2").getValue().toString().split(":")[0])*60*60*1000+
                            Integer.parseInt(snapshot.child("OffWork2").getValue().toString().split(":")[1])*60*1000 ;

                }
                if(snapshot.child("AtWork2").exists()&& snapshot.child("OffWork1").exists()){
                    AtWork2 =Integer.parseInt(snapshot.child("AtWork2").getValue().toString().split(":")[0])*60*60*1000+
                            Integer.parseInt(snapshot.child("AtWork2").getValue().toString().split(":")[1])*60*1000 ;
                    OffWork1 =Integer.parseInt(snapshot.child("OffWork1").getValue().toString().split(":")[0])*60*60*1000+
                            Integer.parseInt(snapshot.child("OffWork1").getValue().toString().split(":")[1])*60*1000 ;
                }
                        }
                        @Override
                        public void onCancelled(DatabaseError error) {
                        }
                    });

        handler = new Handler();
        runnable = new Runnable()
        {
            @Override
            public void run()
            {
                Calendar cTime = Calendar.getInstance();
                if ((cTime.getTimeInMillis()+8*60*60*1000)%(24*60*60*1000)<=(AtWork1)){
                    TVTCState.setText("上班");
                }else if((cTime.getTimeInMillis()+8*60*60*1000)%(24*60*60*1000)>=(OffWork2)){
                        TVTCState.setText("下班");

                }else{
                    TVTCState.setText("異常");
                }
                if((AtWork2>=0)&&(OffWork1>=0)) {
                //todo
            }
                handler.postDelayed(this, 30000);
            }
        };
        handler.postDelayed(runnable, 30000);
    }
    private void makeQR(){
        String label;
        if (TVTCState.getText().equals("上班")) {
            label="A";
        }else if(TVTCState.getText().equals("下班")){
            label="B";
        }else if(TVTCState.getText().equals("加班")){
            label="C";
        }else if(TVTCState.getText().equals("結束")){
            label="D";
        }else{
            label="E";
        }
        String normalText = deviceId+":"+service+":"+label+":"+mTCClient.push().getKey();
        String normalTextEnc=encry(normalText);
        Bitmap bitmap = QRCode.from(normalTextEnc).withSize(250, 250).bitmap();
        IVQRTC.setImageBitmap(bitmap);
    }
}
