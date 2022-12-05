package com.example.ponyo;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1;

    String[] sourceItem ={"ko"};
    String[] targetItem = {"en", "ja", "zh-CN", "zh-TW", "es", "fr", "ru", "vi", "th", "id", "de", "it"};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        Button button = findViewById(R.id.btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(getApplicationContext(), AwindowService.class));
                Intent intent = new Intent(getApplicationContext(), BackgroundService.class);
                stopService(intent);
            }
        });
        Spinner source = (Spinner) findViewById(R.id.sourceSpinner);
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_dropdown_item, sourceItem);
        source.setAdapter(sourceAdapter);
        Spinner target = (Spinner) findViewById(R.id.targetSpinner);
        ArrayAdapter<String> targetAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_spinner_dropdown_item, targetItem);
        target.setAdapter(targetAdapter);

        SharedPreferences sharedPreferences= getSharedPreferences("lang", MODE_PRIVATE);
        String sourceLang = sharedPreferences.getString("sourceLang","ko");
        for(int i = 0; i < sourceItem.length; i++){
            if(sourceLang.equals(sourceItem[i])) source.setSelection(i);
        }
        String targetLang = sharedPreferences.getString("targetLang","en");
        for(int i = 0; i < targetItem.length; i++){
            if(targetLang.equals(targetItem[i])) target.setSelection(i);
        }

        source.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ((Language) getApplication()).setSourceLang(String.valueOf(sourceItem[arg2]));
                SharedPreferences sharedPreferences= getSharedPreferences("lang", MODE_PRIVATE);
                SharedPreferences.Editor editor= sharedPreferences.edit();
                editor.putString("sourceLang",String.valueOf(sourceItem[arg2]));
                editor.commit();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        target.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                ((Language) getApplication()).setTargetLang(String.valueOf(targetItem[arg2]));
                SharedPreferences sharedPreferences= getSharedPreferences("lang", MODE_PRIVATE);
                SharedPreferences.Editor editor= sharedPreferences.edit();
                editor.putString("targetLang",String.valueOf(targetItem[arg2]));
                editor.commit();
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        Button memeButton = findViewById(R.id.meme_button);
        memeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MemeActivity.class);
                startActivity(intent);
            }
        });

    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // 마시멜로우 이상일 경우
            if (!Settings.canDrawOverlays(this)) {              // 다른앱 위에 그리기 체크
                Uri uri = Uri.fromParts("package" , getPackageName(), null);
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri);
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            } else {
                startMain();
            }
        } else {
            startMain();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                finish();
            } else {
                startMain();
            }
        }
    }


    void startMain(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, AwindowService.class));
        } else {
            startService(new Intent(this, AwindowService.class));
        }
    }

    @Override
    protected void onUserLeaveHint() {          // 홈 버튼 감지
        super.onUserLeaveHint();
        System.exit(0);
    }
}