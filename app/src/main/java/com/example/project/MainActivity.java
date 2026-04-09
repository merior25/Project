package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // השהיה של 2.5 שניות ואז מעבר למסך ההתחברות (LoginScreen)
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, LoginScreen.class);
                startActivity(intent);
                finish(); // סגירת מסך הפתיחה כדי שלא יחזרו אליו בלחיצה על "חזור"
            }
        }, 2500);
    }
}