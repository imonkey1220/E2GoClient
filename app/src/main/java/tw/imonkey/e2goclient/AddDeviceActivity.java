package tw.imonkey.e2goclient;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class AddDeviceActivity extends AppCompatActivity {
    public static final String devicePrefs = "devicePrefs";
    private static final int RC_CHOOSE_PHOTO = 101;
    //private static final int RC_IMAGE_PERMS = 102;
    StorageReference mImageRef;
    DatabaseReference mAddDevice ,mAddMaster;
    String memberEmail,deviceId ,deviceType,service;// deviceId=shopId=topics_id
    ImageView imageViewAddDevice;
    Uri selectedImage ;
    EditText editTextAddCompanyId;
    EditText editTextAddDevice;
    EditText editTextAddDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);
        Bundle extras = getIntent().getExtras();
        memberEmail =extras.getString("memberEmail");
        deviceType =extras.getString("deviceType");
        service=extras.getString("service");
        imageViewAddDevice=(ImageView)(findViewById(R.id.imageViewAddDevice));
        editTextAddCompanyId = (EditText) (findViewById(R.id.editTextAddCompanyId));
        editTextAddDevice = (EditText) (findViewById(R.id.editTextAddDevice));
        editTextAddDescription = (EditText) (findViewById(R.id.editTextAddDescription));

    }

    public void addDevice(View view){
        String companyId = editTextAddCompanyId.getText().toString().trim();
        String device = editTextAddDevice.getText().toString().trim();
        String description = editTextAddDescription.getText().toString().trim();
        if (!(TextUtils.isEmpty(companyId) ||TextUtils.isEmpty(device)||TextUtils.isEmpty(description))) {
            mAddMaster= FirebaseDatabase.getInstance().getReference("/FUI/" +memberEmail.replace(".", "_"));
            deviceId =mAddMaster.push().getKey();
            Map<String, Object> addMaster = new HashMap<>();
            addMaster.put("companyId",companyId) ;
            addMaster.put("device",device);
            addMaster.put("deviceType",deviceType);
            addMaster.put("description",description);
            addMaster.put("masterEmail",memberEmail) ;
            addMaster.put("timeStamp", ServerValue.TIMESTAMP);
            addMaster.put("topics_id",deviceId) ;
            mAddMaster.child(deviceId).setValue(addMaster);

            mAddDevice = FirebaseDatabase.getInstance().getReference("/DEVICE/"+deviceId);
            Map<String, Object> addDevice = new HashMap<>();
            addDevice.put("companyId",companyId);
            addDevice.put("device",device);
            addDevice.put("deviceType",deviceType);
            addDevice.put("description",description);
            addDevice.put("masterEmail",memberEmail) ;
            addDevice.put("timeStamp",ServerValue.TIMESTAMP);
            addDevice.put("topics_id",deviceId) ;
            mAddDevice.setValue(addDevice);
            SharedPreferences.Editor editor = getSharedPreferences(devicePrefs+service, Context.MODE_PRIVATE).edit();
            editor.putString("deviceId",deviceId);
            editor.putString("device",device);
            editor.putString("companyId",companyId);
            editor.apply();
            if (selectedImage!=null) {
                uploadPhoto(selectedImage);
            }

            Toast.makeText(this, "add device...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AddDeviceActivity.this,MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_CHOOSE_PHOTO) {
            if (resultCode == RESULT_OK) {
                selectedImage = data.getData();
                imageViewAddDevice.setImageURI(selectedImage);

            } else {
                Toast.makeText(this, "No image chosen", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void choosePhoto(View view) {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RC_CHOOSE_PHOTO);
    }

    protected void uploadPhoto(Uri uri) {

        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();

        // Upload to Firebase Storage
        String devicePhotoPath = "/devicePhoto/"+deviceId;
        mImageRef = FirebaseStorage.getInstance().getReference(devicePhotoPath);
        mImageRef.putFile(uri)
                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //      Log.d("TAG", "uploadPhoto:onSuccess:" +
                        //              taskSnapshot.getMetadata().getReference().getPath());
                        Toast.makeText(AddDeviceActivity.this, "Image uploaded",
                                Toast.LENGTH_SHORT).show();


                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("TAG", "uploadPhoto:onError", e);
                        Toast.makeText(AddDeviceActivity.this, "Upload failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}