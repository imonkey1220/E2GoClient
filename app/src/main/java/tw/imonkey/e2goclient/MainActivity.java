package tw.imonkey.e2goclient;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    public static final String devicePrefs = "devicePrefs";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    public void ToQMS(View v){
        Intent intent = new Intent(this,QMSActivity.class);
        startActivity(intent);
        finish();

    }
    public void ToTC(View v){
        Intent intent = new Intent(this,TCActivity.class);
        startActivity(intent);
        finish();

    }
    public void ToMENU(View v){
        Intent intent = new Intent(this,MENUActivity.class);
        startActivity(intent);
        finish();

    }

}
