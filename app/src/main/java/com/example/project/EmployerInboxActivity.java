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

public class EmployerInboxActivity extends AppCompatActivity {

    private ListView inboxListView;
    private ArrayList<String> inboxList;
    private ArrayList<String> invitationIdsList;
    private ArrayAdapter<String> adapter;
    private Button btnBack;

    private String jobId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_inbox);

        jobId = getIntent().getStringExtra("JOB_ID");
        db = FirebaseFirestore.getInstance();

        inboxListView = findViewById(R.id.inboxListView);
        btnBack = findViewById(R.id.btnBack);

        inboxList = new ArrayList<>();
        invitationIdsList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, inboxList);
        inboxListView.setAdapter(adapter);

        inboxListView.setOnItemClickListener((parent, view, position, id) -> {
            if (invitationIdsList.isEmpty()) return;

            String selectedInvitationId = invitationIdsList.get(position);

            Intent intent = new Intent(EmployerInboxActivity.this, EmployerArrangeInterviewActivity.class);
            intent.putExtra("INVITATION_ID", selectedInvitationId);
            startActivity(intent);
        });

        if (jobId == null) {
            Toast.makeText(this, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (jobId != null) {
            loadInbox();
        }
    }

    private void loadInbox() {
        db.collection("Invitations")
                .whereEqualTo("jobId", jobId)
                .get()
                .addOnCompleteListener(task -> {
                    inboxList.clear();
                    invitationIdsList.clear();
                    adapter.notifyDataSetChanged();

                    if (task.isSuccessful() && !task.getResult().isEmpty()) {

                        ArrayList<String> processedWorkerIds = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String invitationId = doc.getId();
                            String workerId = doc.getString("workerId");
                            String status = doc.getString("status");

                            // התיקון שלנו: מדלגים על מי שכבר התקבל לעבודה כדי לנקות את תיבת הזימונים!
                            // (אפשר להוסיף לפה גם || "rejected".equals(status) אם את רוצה להעלים גם את מי שנדחה)
                            if ("hired".equals(status)) {
                                continue;
                            }

                            if (workerId != null && !processedWorkerIds.contains(workerId)) {
                                processedWorkerIds.add(workerId);

                                String displayStatus = "לא ידוע";
                                if ("pending".equals(status)) displayStatus = "ממתין לתשובה";
                                if ("accepted".equals(status)) displayStatus = "אישר הגעה (מוכן לראיון)";
                                if ("declined".equals(status)) displayStatus = "דחה זימון";
                                if ("interview_employer_proposed".equals(status)) displayStatus = "הצעת ראיון נשלחה לעובד";
                                if ("interview_worker_proposed".equals(status)) displayStatus = "העובד הציע מועד חלופי";
                                if ("interview_scheduled".equals(status)) displayStatus = "ראיון נקבע בהצלחה!";
                                if ("interview_declined".equals(status)) displayStatus = "העובד ביטל את הראיון";
                                if ("rejected".equals(status)) displayStatus = "המועמד נדחה";

                                String finalDisplayStatus = displayStatus;
                                db.collection("Users").document(workerId).get().addOnSuccessListener(userDoc -> {
                                    if (userDoc.exists()) {
                                        String firstName = userDoc.getString("firstName");
                                        String lastName = userDoc.getString("lastName");

                                        inboxList.add(firstName + " " + lastName + "\nסטטוס: " + finalDisplayStatus);
                                        invitationIdsList.add(invitationId);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                    } else if (task.isSuccessful()) {
                        inboxList.add("טרם נשלחו זימונים למשרה זו.");
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}