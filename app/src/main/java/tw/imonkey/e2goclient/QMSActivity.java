package tw.imonkey.e2goclient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class QMSActivity extends AppCompatActivity {
    TextView TVQMSClientValue ,TVQMSServerValue,TVQMSClientTile,TVQMSDevice;
    ImageView QMSQRImage ;
    TextToSpeech tts ;

    String memberEmail,device,deviceId;
    Map<String, Object> countClient = new HashMap<>();

    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;
    DatabaseReference mQMSServer,mQMSClient,presenceRef,lastOnlineRef,connectedRef, mFriend,connectedRefF,mQMSServerLive;

    public static final String devicePrefs = "devicePrefs";
    public static final String service="QMS"; //取號機 deviceType

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 //       this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_qms);
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
        //QRcode generater*****************************************
        TVQMSDevice = (TextView) findViewById(R.id.textViewDevice);
        TVQMSDevice.setText(device);
        TVQMSClientValue  = (TextView) findViewById(R.id.textViewClientQR);
        TVQMSClientTile = (TextView) findViewById(R.id.textViewClientTile);
        QMSQRImage = (ImageView) findViewById(R.id.imageViewQRClient);
        TVQMSServerValue =(TextView)findViewById(R.id.textViewServer);
    }
    private void setup(){
        Intent intent = new Intent(QMSActivity.this, AddDeviceActivity.class);
        intent.putExtra("memberEmail", memberEmail);
        intent.putExtra("deviceType", "取號機");
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
        deviceOnline();
        mQMSClient= FirebaseDatabase.getInstance().getReference("/LOG/QMS/"+deviceId+"/CLIENT");
        mQMSClient.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                String normalText = deviceId+":"+service+":"+dataSnapshot.child("message").getValue().toString()+":"+mQMSClient.push().getKey();
                String normalTextEnc=encry(normalText);
                    Bitmap bitmap = QRCode.from(normalTextEnc).withSize(250, 250).bitmap();
                    QMSQRImage.setImageBitmap(bitmap);
                    TVQMSClientValue.setText(dataSnapshot.child("message").getValue().toString());
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

        TVQMSClientValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQMSClient= FirebaseDatabase.getInstance().getReference("/LOG/QMS/"+deviceId+"/CLIENT");
                countClient.clear();
                countClient.put("message",Integer.parseInt(TVQMSClientValue .getText().toString())+1);
                countClient.put("memberEmail",memberEmail);
                countClient.put("timeStamp", ServerValue.TIMESTAMP);
                mQMSClient.push().setValue(countClient);
                String toSpeak ="你的號碼是"+TVQMSClientValue .getText().toString()+"號";
                tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null,null);
                Toast.makeText(QMSActivity.this,"你的號碼:"+TVQMSClientValue .getText().toString()+"號",Toast.LENGTH_LONG).show();

            }
        });
        TVQMSClientTile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(QMSActivity.this,"相機啟動,稍等一下!",Toast.LENGTH_LONG).show();
                Intent intent = new Intent(QMSActivity.this,QRActivity.class);
                intent.putExtra("deviceId",deviceId);
                intent.putExtra("memberEmail",memberEmail);
                intent.putExtra("number",TVQMSClientValue.getText().toString());
                startActivity(intent);
                finish();

            }
        });

        mQMSServer= FirebaseDatabase.getInstance().getReference("/LOG/QMS/"+deviceId+"/SERVER");
        mQMSServer.limitToLast(1).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                TVQMSServerValue.setText(dataSnapshot.child("message").getValue().toString());
                //Todo:alert customer
                NotifyUser.IIDPUSH(deviceId,"i0932702567@gmail.com","message_title","message_body");
                NotifyUser.topicsPUSH(deviceId,"i0932702567@gmail.com","message_title","message_body");
                NotifyUser.emailPUSH(deviceId,"i0932702567@gmail.com","message");
                NotifyUser.SMSPUSH(deviceId,"i0932702567@gmail.com","message");

                String toSpeak ="號碼牌"+dataSnapshot.child("message").getValue().toString()+"號";
                tts.speak(toSpeak,TextToSpeech.QUEUE_ADD, null,null);
                Animation anim = new AlphaAnimation(0.0f, 1.0f);
                anim.setDuration(100); //You can manage the time of the blink with this parameter
                anim.setStartOffset(20);
                anim.setRepeatMode(Animation.REVERSE);
                anim.setRepeatCount(10);
                TVQMSServerValue.startAnimation(anim);

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
        mQMSServerLive=FirebaseDatabase.getInstance().getReference("/LOG/QMS/"+deviceId+"/connection");
        mQMSServerLive.setValue(true);
        mQMSServerLive.onDisconnect().setValue(null);

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
                    mQMSServerLive.setValue(true);
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
