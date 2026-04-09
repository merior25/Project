package com.example.project; // ודא שזה תואם לשם החבילה שלך

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.project.R;

public class CreatePostActivity extends AppCompatActivity {

    private EditText postEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post); // חיבור לקובץ ה-XML

        // הגדרת הסרגל העליון
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // הוספת כפתור החזור בסרגל
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // קישור לתיבת הטקסט מה-XML (למקרה שתרצה לשמור את הטקסט אחר כך)
        postEditText = findViewById(R.id.postEditText);
    }

    // פונקציה שמטפלת בלחיצה על כפתור ה"חזור"
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish(); // סוגר את המסך הנוכחי וחוזר למסך הקודם
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}