package com.example.project; // שימי לב שזה תואם לשם הפרויקט שלך

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class CandidateScreen extends AppCompatActivity {

    // הגדרת משתני המסך
    private TextView tvCandidateName, tvCandidateAge, tvCandidateExperience, tvCandidateRole, tvCandidatePhone;
    private Button btnDownloadCV, btnRecruit;

    // משתני פיירבייס
    private FirebaseFirestore db;
    private String candidateId; // ה-ID של המועמד הספציפי שאנחנו רוצים להציג
    private String cvDownloadUrl = ""; // הקישור לקורות החיים שלו (אם יש)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ודאי ששם קובץ ה-XML תואם לשם שלך
        setContentView(R.layout.activity_candidate_screen);

        // אתחול פיירבייס
        db = FirebaseFirestore.getInstance();

        // קישור המשתנים לעיצוב (לפי ה-IDs החדשים שעשינו)
        tvCandidateName = findViewById(R.id.tvCandidateName);
        tvCandidateAge = findViewById(R.id.tvCandidateAge);
        tvCandidateExperience = findViewById(R.id.tvCandidateExperience);
        tvCandidateRole = findViewById(R.id.tvCandidateRole);
        tvCandidatePhone = findViewById(R.id.tvCandidatePhone);

        btnDownloadCV = findViewById(R.id.btnDownloadCV);
        btnRecruit = findViewById(R.id.btnRecruit);

        // קבלת ה-ID של המועמד מהמסך הקודם (כדי שנדע את מי להציג)
        // אם לא הועבר ID, נשתמש במשהו ריק כדי למנוע קריסה
        candidateId = getIntent().getStringExtra("CANDIDATE_ID");

        if (candidateId != null && !candidateId.isEmpty()) {
            loadCandidateData();
        } else {
            Toast.makeText(this, "שגיאה: לא התקבל מזהה מועמד", Toast.LENGTH_SHORT).show();
        }

        // מה קורה כשלוחצים על חזרה אחורה?
        btnRecruit.setOnClickListener(v -> {
            finish(); // סוגר את המסך הנוכחי וחוזר לקודם
        });

        // מה קורה כשלוחצים על הורדת קורות חיים?
        btnDownloadCV.setOnClickListener(v -> {
            if (!cvDownloadUrl.isEmpty()) {
                // פותח את הקישור של ה-PDF בדפדפן של הטלפון
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(cvDownloadUrl));
                startActivity(browserIntent);
            } else {
                Toast.makeText(CandidateScreen.this, "למועמד זה אין קורות חיים במערכת", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCandidateData() {
        // שולפים את המידע מאוסף "Users" לפי ה-ID של המועמד
        db.collection("Users").document(candidateId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // שולפים את הנתונים מפיירבייס (וודאי שהשמות פה תואמים לשמות ששמרת בפיירבייס!)
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String age = documentSnapshot.getString("age"); // בהנחה ששמרת כטקסט, אם זה מספר נשנה את זה
                        String experience = documentSnapshot.getString("experience");
                        String role = documentSnapshot.getString("jobType"); // או איך שקראת לשדה התפקיד
                        String phone = documentSnapshot.getString("phone");

                        // שמירת הקישור לקורות חיים למשתנה שלנו (כדי שהכפתור יוכל להשתמש בו)
                        if (documentSnapshot.contains("cvUrl")) {
                            cvDownloadUrl = documentSnapshot.getString("cvUrl");
                        }

                        // מכניסים את הנתונים למסך
                        tvCandidateName.setText("שם המועמד: " + firstName + " " + lastName);
                        tvCandidateAge.setText("גיל: " + (age != null ? age : "לא צוין"));
                        tvCandidateExperience.setText("שנות ניסיון: " + (experience != null ? experience : "לא צוין"));
                        tvCandidateRole.setText("תפקיד נוכחי/מבוקש: " + (role != null ? role : "לא צוין"));
                        tvCandidatePhone.setText("טלפון ליצירת קשר: " + (phone != null ? phone : "לא צוין"));

                    } else {
                        Toast.makeText(CandidateScreen.this, "המועמד לא נמצא במערכת", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CandidateScreen.this, "שגיאה בטעינת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}