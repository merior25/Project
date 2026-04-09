package com.example.project;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class workersc extends AppCompatActivity {

    private TextView welcomeText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workersc);

        welcomeText = findViewById(R.id.greetingTextWorker);
        mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // הוספתי פה בדיקה קטנה שמוודאת שהמשתמש באמת מחובר כדי למנוע קריסות של האפליקציה
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
        }

        // מושכים את השם מהמסך הקודם
        String nameFromIntent = getIntent().getStringExtra("USER_NAME");

        // הנה החלק שהיה חסר! אנחנו בודקים אם הגיע שם, ואם כן - מדפיסים אותו
        if (nameFromIntent != null && !nameFromIntent.isEmpty()) {
            welcomeText.setText("שלום, " + nameFromIntent);
        } else {
            welcomeText.setText("שלום, עובד");
        }
    }
}