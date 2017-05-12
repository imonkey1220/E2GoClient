package tw.imonkey.e2goclient;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {

    Button loginButton;
    FirebaseAuth auth;
    private static final int RC_SIGN_IN = 200;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        if(isUserLogin()){
            loginUser();
        }
        setContentView(R.layout.activity_login);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        loginButton = (Button)findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                       // .setIsSmartLockEnabled(false)
                        //   .setLogo(R.drawable.logo_googleg_color_144dp)
                        .setProviders(Arrays.asList(
                                new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()
                        ))
                        .build(), RC_SIGN_IN);
            }
        });

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                loginUser();
            }
                if (resultCode == RESULT_CANCELED) {
                    displayMessage(getString(R.string.signin_failed));
                }
        }else {
            displayMessage(getString(R.string.unknown_response));
        }
    }
    private boolean isUserLogin(){
        return auth.getCurrentUser() != null;
    }
    private void loginUser(){
        Intent loginIntent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(loginIntent);
        finish();
    }
    private void displayMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}