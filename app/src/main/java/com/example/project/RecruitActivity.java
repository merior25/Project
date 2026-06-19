package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class RecruitActivity extends AppCompatActivity {

    private ListView workersListView, hiredListView;
    private TextView tvRecruitProgress;

    private ArrayList<String> workersList;
    private ArrayList<String> workerIdsList;
    private ArrayAdapter<String> adapter;

    private ArrayList<String> hiredList;
    private ArrayAdapter<String> hiredAdapter;

    private Button btnClose, btnInbox, btnScheduledInterviews;
    private String jobId;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recruit);

        jobId = getIntent().getStringExtra("JOB_ID");

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        workersListView = findViewById(R.id.workersListView);
        hiredListView = findViewById(R.id.hiredListView);
        tvRecruitProgress = findViewById(R.id.tvRecruitProgress);

        btnClose = findViewById(R.id.btnClose);
        btnInbox = findViewById(R.id.btnInbox);
        btnScheduledInterviews = findViewById(R.id.btnScheduledInterviews);

        // רשימת המועמדים הפוטנציאליים
        workersList = new ArrayList<>();
        workerIdsList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, workersList);
        workersListView.setAdapter(adapter);

        // רשימת העובדים שהתקבלו
        hiredList = new ArrayList<>();
        hiredAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, hiredList);
        hiredListView.setAdapter(hiredAdapter);

        workersListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedWorkerId = workerIdsList.get(position);
            Intent intent = new Intent(RecruitActivity.this, CandidateScreen.class);
            intent.putExtra("CANDIDATE_ID", selectedWorkerId);
            intent.putExtra("JOB_ID", jobId);
            startActivity(intent);
        });

        btnScheduledInterviews.setOnClickListener(v -> {
            Intent intent = new Intent(RecruitActivity.this, EmployerScheduledInterviewsActivity.class);
            intent.putExtra("JOB_ID", jobId);
            startActivity(intent);
        });

        btnInbox.setOnClickListener(v -> {
            Intent intent = new Intent(RecruitActivity.this, EmployerInboxActivity.class);
            intent.putExtra("JOB_ID", jobId);
            startActivity(intent);
        });

        btnClose.setOnClickListener(v -> {
            Intent intent = new Intent(RecruitActivity.this, EditRecruitActivity.class);
            intent.putExtra("JOB_ID", jobId);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (jobId != null) {
            loadRecruitProgressAndWorkers();
        }
    }

    private void loadRecruitProgressAndWorkers() {
        // שלב 1: שולפים את פרטי המשרה כדי לדעת כמה עובדים צריך
        db.collection("JobPosts").document(jobId).get().addOnSuccessListener(jobDoc -> {
            if (jobDoc.exists()) {
                String requiredJobType = jobDoc.getString("jobType");
                String numWorkersStr = jobDoc.getString("numberOfWorkers");
                int desiredWorkers = numWorkersStr != null ? Integer.parseInt(numWorkersStr) : 0;

                // שלב 2: שולפים את כל הזימונים שכבר נשלחו או התקבלו
                db.collection("Invitations").whereEqualTo("jobId", jobId).get().addOnSuccessListener(invitationsTask -> {

                    ArrayList<String> alreadyInvitedWorkerIds = new ArrayList<>();
                    int hiredCount = 0;
                    hiredList.clear();

                    for (QueryDocumentSnapshot doc : invitationsTask) {
                        String workerId = doc.getString("workerId");
                        String status = doc.getString("status");
                        alreadyInvitedWorkerIds.add(workerId);

                        if ("hired".equals(status)) {
                            hiredCount++;
                            String hiredDate = doc.getString("hiredDate");
                            if (workerId != null) {
                                db.collection("Users").document(workerId).get().addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("firstName") + " " + userDoc.getString("lastName");
                                    hiredList.add(name + " (התחיל ב-" + hiredDate + ")");
                                    hiredAdapter.notifyDataSetChanged();
                                });
                            }
                        }
                    }

                    // עדכון סטטוס גיוס
                    tvRecruitProgress.setText("סטטוס: גויסו " + hiredCount + " מתוך " + desiredWorkers + " עובדים");
                    if (hiredCount >= desiredWorkers) {
                        tvRecruitProgress.setText("הגיוס הושלם במלואו! (" + hiredCount + "/" + desiredWorkers + ")");
                        tvRecruitProgress.setTextColor(android.graphics.Color.parseColor("#1565C0"));
                    }

                    // שלב 3: שולפים רק עובדים שזמינים וגלויים!
                    db.collection("Users")
                            .whereEqualTo("userType", "עובד")
                            .whereEqualTo("fieldOfWork", requiredJobType)
                            .whereEqualTo("isVisibleToEmployers", true) // התיקון הקריטי: לא מציג עובדים שלא פנויים!
                            .get()
                            .addOnCompleteListener(task -> {
                                workersList.clear();
                                workerIdsList.clear();

                                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        if (!alreadyInvitedWorkerIds.contains(document.getId())) {
                                            String workerName = document.getString("firstName");
                                            if (workerName != null) {
                                                workersList.add("שם העובד: " + workerName);
                                                workerIdsList.add(document.getId());
                                            }
                                        }
                                    }
                                    adapter.notifyDataSetChanged();
                                }
                            });
                });
            }
        });
    }
}