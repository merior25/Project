package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class workersc extends AppCompatActivity {

    private TextView welcomeText, tvJobDetails;
    private Button btnWorkerInbox;
    private SwitchCompat switchEmployed, switchLookingForJob;
    private CardView cardCurrentJob;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workersc);

        welcomeText = findViewById(R.id.greetingTextWorker);
        tvJobDetails = findViewById(R.id.tvJobDetails);
        btnWorkerInbox = findViewById(R.id.btnWorkerInbox);
        switchEmployed = findViewById(R.id.switchEmployed);
        switchLookingForJob = findViewById(R.id.switchLookingForJob);
        cardCurrentJob = findViewById(R.id.cardCurrentJob);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        String nameFromIntent = getIntent().getStringExtra("USER_NAME");
        if (nameFromIntent != null && !nameFromIntent.isEmpty()) {
            welcomeText.setText("שלום, " + nameFromIntent);
        } else {
            welcomeText.setText("שלום, עובד");
        }

        btnWorkerInbox.setOnClickListener(v -> {
            Intent intent = new Intent(workersc.this, WorkerInboxActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId != null) {
            checkCurrentEmploymentStatus();
        }
    }

    private void checkCurrentEmploymentStatus() {
        // קודם כל נבדוק אם יש זימון שהסתיים בסטטוס "התקבל לעבודה" (hired)
        db.collection("Invitations")
                .whereEqualTo("workerId", userId)
                .whereEqualTo("status", "hired")
                .get()
                .addOnSuccessListener(task -> {
                    if (!task.isEmpty()) {
                        // העובד מועסק דרך האפליקציה!
                        DocumentSnapshot inviteDoc = task.getDocuments().get(0);
                        String jobId = inviteDoc.getString("jobId");
                        String employerId = inviteDoc.getString("employerId");

                        // מעדכנים את המתגים אוטומטית שיהיו "מועסק"
                        switchEmployed.setOnCheckedChangeListener(null);
                        switchLookingForJob.setOnCheckedChangeListener(null);

                        switchEmployed.setChecked(true);
                        switchLookingForJob.setVisibility(View.VISIBLE);

                        // מחזירים את ההאזנה (כדי שיוכל לשנות אם הוא עדיין מחפש)
                        switchEmployed.setOnCheckedChangeListener((btn, isChecked) -> {
                            switchLookingForJob.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                            updateStatusInFirestore();
                        });
                        switchLookingForJob.setOnCheckedChangeListener((btn, isChecked) -> updateStatusInFirestore());

                        // מושכים את פרטי המשרה והמיקום להצגה
                        if (jobId != null && employerId != null) {
                            db.collection("JobPosts").document(jobId).get().addOnSuccessListener(jobDoc -> {
                                String jobType = jobDoc.getString("jobType");

                                db.collection("Users").document(employerId).get().addOnSuccessListener(empDoc -> {
                                    String location = empDoc.getString("businessLocation");
                                    String busName = empDoc.getString("businessName");

                                    cardCurrentJob.setVisibility(View.VISIBLE);
                                    tvJobDetails.setText("תחום: " + jobType + "\nעסק: " + busName + "\nמיקום: " + location);
                                });
                            });
                        }
                    } else {
                        // אם הוא לא התקבל לעבודה דרך האפליקציה, נטען את הסטטוס הרגיל שלו
                        cardCurrentJob.setVisibility(View.GONE);
                        loadManualWorkerStatus();
                    }
                });
    }

    private void loadManualWorkerStatus() {
        db.collection("Users").document(userId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Boolean isEmployed = doc.getBoolean("isEmployed");
                Boolean isLooking = doc.getBoolean("isLookingForJob");

                switchEmployed.setOnCheckedChangeListener(null);
                switchLookingForJob.setOnCheckedChangeListener(null);

                if (isEmployed != null) switchEmployed.setChecked(isEmployed);
                if (isLooking != null) switchLookingForJob.setChecked(isLooking);

                switchLookingForJob.setVisibility(switchEmployed.isChecked() ? View.VISIBLE : View.GONE);

                switchEmployed.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    switchLookingForJob.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    updateStatusInFirestore();
                });
                switchLookingForJob.setOnCheckedChangeListener((btn, isChecked) -> updateStatusInFirestore());
            }
        });
    }

    private void updateStatusInFirestore() {
        if (userId == null) return;

        boolean employed = switchEmployed.isChecked();
        boolean looking = switchLookingForJob.isChecked();

        boolean isVisible = !employed || looking;

        Map<String, Object> updates = new HashMap<>();
        updates.put("isEmployed", employed);
        updates.put("isLookingForJob", looking);
        updates.put("isVisibleToEmployers", isVisible);

        db.collection("Users").document(userId).update(updates);
    }
}