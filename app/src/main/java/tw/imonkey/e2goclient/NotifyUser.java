package tw.imonkey.e2goclient;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;

public class NotifyUser {
    private void SMSPUSH( String deviceId ,String memberEmail,String message){
        DatabaseReference mSMSMaster= FirebaseDatabase.getInstance().getReference("/SMS/");
        Map<String, Object> SMS = new HashMap<>();
        SMS.clear();
        SMS.put("message",message);
        SMS.put("deviceId",deviceId);
        SMS.put("memberEmail",memberEmail);
        SMS.put("timeStamp", ServerValue.TIMESTAMP);
        mSMSMaster.push().setValue(SMS);
    }

    private void emailPUSH( String deviceId ,String memberEmail,String message ){
        DatabaseReference mEMAILMaster= FirebaseDatabase.getInstance().getReference("/EMAIL/");
        Map<String, Object> EMAIL = new HashMap<>();
        EMAIL.clear();
        EMAIL.put("message",message);
        EMAIL.put("deviceId",deviceId);
        EMAIL.put("memberEmail",memberEmail);
        EMAIL.put("timeStamp", ServerValue.TIMESTAMP);
        mEMAILMaster.push().setValue(EMAIL);
    }

    private void topicsPUSH( String deviceId ,String memberEmail,String message_title,String message_body){
        Map<String, Object> message = new HashMap<>();
        message.clear();
        message.put("title",message_title);
        message.put("body",message_body);

        DatabaseReference mPUSHMaster= FirebaseDatabase.getInstance().getReference("/PUSHTopics/");
        Map<String, Object> PUSH = new HashMap<>();
        PUSH.clear();
        PUSH.put("message",message);
        PUSH.put("deviceId",deviceId);
        PUSH.put("memberEmail",memberEmail);
        PUSH.put("timeStamp", ServerValue.TIMESTAMP);
        mPUSHMaster.push().setValue(PUSH);
    }

    private void IIDPUSH(String deviceId,String memberEmail,String message_title,String message_body){
        Map<String, Object> message = new HashMap<>();
        message.clear();
        message.put("title",message_title);
        message.put("body",message_body);

        DatabaseReference mPUSHMaster= FirebaseDatabase.getInstance().getReference("/PUSH/");
        Map<String, Object> PUSH = new HashMap<>();
        PUSH.clear();
        PUSH.put("message",message);
        PUSH.put("deviceId",deviceId);
        PUSH.put("memberEmail",memberEmail);
        PUSH.put("timeStamp", ServerValue.TIMESTAMP);
        mPUSHMaster.push().setValue(PUSH);
    }


}
