package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class EmployerScheduledInterviewsActivity extends AppCompatActivity {
    private ListView scheduledListView;
    private ArrayList<String> scheduledList;
    private ArrayList<String> invitationIdsList; // רשימה נסתרת למזהי הזימון
    private ArrayAdapter<String> adapter;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String jobId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_scheduled_interviews);

        jobId = getIntent().getStringExtra("JOB_ID");
        scheduledListView = findViewById(R.id.scheduledListView);

        scheduledList = new ArrayList<>();
        invitationIdsList = new ArrayList<>(); // אתחול
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, scheduledList);
        scheduledListView.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // --- לחיצה על ראיון שנקבע פותחת את מסך ההחלטה הסופית ---
        scheduledListView.setOnItemClickListener((parent, view, position, id) -> {
            if (invitationIdsList.isEmpty()) return;

            String selectedInvitationId = invitationIdsList.get(position);
            Intent intent = new Intent(EmployerScheduledInterviewsActivity.this, EmployerFinalDecisionActivity.class);
            intent.putExtra("INVITATION_ID", selectedInvitationId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (jobId != null) {
            loadScheduledInterviews();
        }
    }

    private void loadScheduledInterviews() {
        db.collection("Invitations")
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", "interview_scheduled")
                .get()
                .addOnCompleteListener(task -> {
                    scheduledList.clear();
                    invitationIdsList.clear();
                    adapter.notifyDataSetChanged();

                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        ArrayList<String> processedWorkerIds = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String invitationId = doc.getId(); // המזהה
                            String date = doc.getString("interviewDate");
                            String time = doc.getString("interviewTime");
                            String workerId = doc.getString("workerId");

                            if (workerId != null && !processedWorkerIds.contains(workerId)) {
                                processedWorkerIds.add(workerId);

                                db.collection("Users").document(workerId).get().addOnSuccessListener(user -> {
                                    if (user.exists()) {
                                        String name = user.getString("firstName") + " " + user.getString("lastName");
                                        scheduledList.add("ראיון עם: " + name + "\nבתאריך: " + date + " בשעה: " + time);
                                        invitationIdsList.add(invitationId); // שומרים במקביל
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                    } else if (task.isSuccessful()) {
                        scheduledList.add("עדיין לא נקבעו ראיונות למשרה זו.");
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}