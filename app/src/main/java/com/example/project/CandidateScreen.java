package com.example.project; // שימי לב שזה תואם לשם הפרויקט שלך

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CandidateScreen extends AppCompatActivity {

    private TextView tvCandidateName, tvCandidateAge, tvCandidateExperience, tvCandidateRole, tvCandidatePhone;
    private Button btnDownloadCV, btnRecruit, btnInviteCandidate;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String candidateId;
    private String jobId;
    private String cvDownloadUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_candidate_screen);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvCandidateName = findViewById(R.id.tvCandidateName);
        tvCandidateAge = findViewById(R.id.tvCandidateAge);
        tvCandidateExperience = findViewById(R.id.tvCandidateExperience);
        tvCandidateRole = findViewById(R.id.tvCandidateRole);
        tvCandidatePhone = findViewById(R.id.tvCandidatePhone);

        btnDownloadCV = findViewById(R.id.btnDownloadCV);
        btnRecruit = findViewById(R.id.btnRecruit);
        btnInviteCandidate = findViewById(R.id.btnInviteCandidate);

        // קבלת מזהה העובד ומזהה המשרה מהמסך הקודם
        candidateId = getIntent().getStringExtra("CANDIDATE_ID");
        jobId = getIntent().getStringExtra("JOB_ID");

        if (candidateId != null && !candidateId.isEmpty()) {
            loadCandidateData();
        } else {
            Toast.makeText(this, "שגיאה: לא התקבל מזהה מועמד", Toast.LENGTH_SHORT).show();
            finish();
        }

        // שליחת זימון לעובד
        btnInviteCandidate.setOnClickListener(v -> sendInvitation());

        // כפתור חזרה אחורה
        btnRecruit.setOnClickListener(v -> finish());

        // הורדת קורות חיים
        btnDownloadCV.setOnClickListener(v -> {
            if (!cvDownloadUrl.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(cvDownloadUrl));
                startActivity(browserIntent);
            } else {
                Toast.makeText(CandidateScreen.this, "למועמד זה אין קורות חיים במערכת", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCandidateData() {
        db.collection("Users").document(candidateId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String age = documentSnapshot.getString("age");
                        String experience = documentSnapshot.getString("experience");
                        String role = documentSnapshot.getString("fieldOfWork"); // התיקון שלנו: שולפים את התפקיד מ-fieldOfWork
                        String phone = documentSnapshot.getString("phone");

                        if (documentSnapshot.contains("cvUrl")) {
                            cvDownloadUrl = documentSnapshot.getString("cvUrl");
                        }

                        tvCandidateName.setText("שם המועמד: " + firstName + " " + lastName);
                        tvCandidateAge.setText("גיל: " + (age != null ? age : "לא צוין"));
                        tvCandidateExperience.setText("שנות ניסיון: " + (experience != null ? experience : "לא צוין"));
                        tvCandidateRole.setText("תפקיד נוכחי/מבוקש: " + (role != null ? role : "לא צוין"));
                        tvCandidatePhone.setText("טלפון ליצירת קשר: " + (phone != null ? phone : "לא צוין"));

                    } else {
                        Toast.makeText(CandidateScreen.this, "המועמד לא נמצא במערכת", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(CandidateScreen.this, "שגיאה בטעינת נתונים: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void sendInvitation() {
        if (mAuth.getCurrentUser() == null || jobId == null) {
            Toast.makeText(this, "שגיאה: חסרים נתוני משרה או מעסיק", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> invitation = new HashMap<>();
        invitation.put("jobId", jobId);
        invitation.put("employerId", mAuth.getCurrentUser().getUid());
        invitation.put("workerId", candidateId);
        invitation.put("status", "pending");

        db.collection("Invitations").add(invitation)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(CandidateScreen.this, "הזימון נשלח בהצלחה!", Toast.LENGTH_LONG).show();
                    btnInviteCandidate.setText("זימון נשלח");
                    btnInviteCandidate.setEnabled(false);
                })
                .addOnFailureListener(e -> Toast.makeText(CandidateScreen.this, "שגיאה בשליחת הזימון", Toast.LENGTH_SHORT).show());
    }
}