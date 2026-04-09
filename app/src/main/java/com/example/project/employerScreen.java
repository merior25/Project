package com.example.project; // <-- להשאיר את שלך

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class employerScreen extends AppCompatActivity {

    private TextView welcomeText;
    private Button btnRecruit; // השארנו פה רק את הכפתור שמחובר כדי למנוע קריסות
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_screen);

        // חיבור הרכיבים מה-XML לקוד
        welcomeText = findViewById(R.id.greetingText);
        btnRecruit = findViewById(R.id.btnRecruit);

        mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // משיכת שם המעסיק מהמסך הקודם
        String nameFromIntent = getIntent().getStringExtra("USER_NAME");

        if (nameFromIntent != null) {
            welcomeText.setText("שלום, " + nameFromIntent);
        } else if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            // --- העדכון ל-Firestore מתחיל כאן ---
            db.collection("Users").document(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    // יצירת אובייקט של המסמך שנשלף
                    DocumentSnapshot document = task.getResult();

                    // שליפת השדה הספציפי (במקרה שלך: firstName)
                    String nameFromDB = document.getString("firstName");

                    if (nameFromDB != null) {
                        welcomeText.setText("שלום, " + nameFromDB);
                    } else {
                        welcomeText.setText("שלום, מעסיק");
                    }
                } else {
                    welcomeText.setText("שלום, מעסיק");
                }
            });
            // --- העדכון ל-Firestore מסתיים כאן ---
        }

        // מעבר למסך גיוס עובדים (הרשימה)
        btnRecruit.setOnClickListener(v -> {
            Intent intent = new Intent(employerScreen.this, RecruitActivity.class);
            startActivity(intent);
        });

        // הקוד של הכפתור השני הוסר זמנית כדי שהאפליקציה לא תקרוס.
        // נוסיף אותו חזרה כשיהיה לו מסך!
    }
}