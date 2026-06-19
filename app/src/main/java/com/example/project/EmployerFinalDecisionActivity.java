package com.example.project;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EmployerFinalDecisionActivity extends AppCompatActivity {

    private TextView tvCandidateNameFinal;
    private FirebaseFirestore db;
    private String invitationId;
    private String currentWorkerId; // שומרים את מזהה העובד כדי לעדכן אותו

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_final_decision);

        invitationId = getIntent().getStringExtra("INVITATION_ID");
        db = FirebaseFirestore.getInstance();

        tvCandidateNameFinal = findViewById(R.id.tvCandidateNameFinal);

        if (invitationId != null) {
            db.collection("Invitations").document(invitationId).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    currentWorkerId = doc.getString("workerId");
                    if (currentWorkerId != null) {
                        db.collection("Users").document(currentWorkerId).get().addOnSuccessListener(userDoc -> {
                            if (userDoc.exists()) {
                                String name = userDoc.getString("firstName") + " " + userDoc.getString("lastName");
                                tvCandidateNameFinal.setText("האם ברצונך לקבל את " + name + " לעבודה?");
                            }
                        });
                    }
                }
            });
        }

        // קבלה לעבודה - מעדכן גם את הזימון וגם את העובד!
        findViewById(R.id.btnHireCandidate).setOnClickListener(v -> {
            if (invitationId == null || currentWorkerId == null) return;

            // 1. מעדכנים את הזימון לתקבל ומוסיפים תאריך התחלה
            Map<String, Object> inviteUpdates = new HashMap<>();
            inviteUpdates.put("status", "hired");
            String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            inviteUpdates.put("hiredDate", todayDate);

            db.collection("Invitations").document(invitationId).update(inviteUpdates).addOnSuccessListener(aVoid -> {

                // 2. מעדכנים את העובד אוטומטית: מועסק, לא מחפש עבודה, ומוסתר מהלוח הראשי
                Map<String, Object> workerUpdates = new HashMap<>();
                workerUpdates.put("isEmployed", true);
                workerUpdates.put("isLookingForJob", false);
                workerUpdates.put("isVisibleToEmployers", false);

                db.collection("Users").document(currentWorkerId).update(workerUpdates).addOnSuccessListener(aVoid2 -> {
                    Toast.makeText(this, "המועמד התקבל לעבודה! הסטטוס שלו עודכן במערכת.", Toast.LENGTH_LONG).show();
                    finish();
                });
            });
        });

        // דחיית המועמד
        findViewById(R.id.btnRejectCandidate).setOnClickListener(v -> {
            if (invitationId != null) {
                db.collection("Invitations").document(invitationId).update("status", "rejected").addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "המועמד נדחה.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}