package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class WorkerInboxActivity extends AppCompatActivity {

    private ListView workerInboxListView;
    private Button btnBack;

    private ArrayList<String> inboxDisplayList;
    private ArrayList<String> invitationIdsList; // רשימה נסתרת לשמירת המזהה של הזימון עצמו
    private ArrayAdapter<String> adapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_inbox);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        workerInboxListView = findViewById(R.id.workerInboxListView);
        btnBack = findViewById(R.id.btnBackFromWorkerInbox);

        inboxDisplayList = new ArrayList<>();
        invitationIdsList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, inboxDisplayList);
        workerInboxListView.setAdapter(adapter);

        // --- התיקון: לחיצה על זימון מחליטה לאן ללכת לפי הסטטוס ---
        workerInboxListView.setOnItemClickListener((parent, view, position, id) -> {
            if (invitationIdsList.isEmpty()) return; // הגנת קריסה

            String selectedInvitationId = invitationIdsList.get(position);

            db.collection("Invitations").document(selectedInvitationId).get().addOnSuccessListener(doc -> {
                String status = doc.getString("status");

                if ("pending".equals(status)) {
                    // העובד עוד לא ענה לזימון הראשוני
                    showResponseDialog(selectedInvitationId);
                } else if ("interview_employer_proposed".equals(status)) {
                    // המעסיק קבע ראיון! נפתח את מסך התגובה לראיון
                    Intent intent = new Intent(WorkerInboxActivity.this, WorkerInterviewResponseActivity.class);
                    intent.putExtra("INVITATION_ID", selectedInvitationId);
                    startActivity(intent);
                } else if ("interview_scheduled".equals(status)) {
                    Toast.makeText(this, "הראיון כבר נקבע וסגור!", Toast.LENGTH_SHORT).show();
                } else if ("hired".equals(status)) {
                    Toast.makeText(this, "מזל טוב! התקבלת לעבודה!", Toast.LENGTH_LONG).show();
                } else if ("rejected".equals(status)) {
                    Toast.makeText(this, "בהצלחה בפעם הבאה!", Toast.LENGTH_SHORT).show();
                } else if ("accepted".equals(status)) {
                    Toast.makeText(this, "אישרת הגעה. ממתין לקביעת ראיון מהמעסיק.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "הסטטוס הנוכחי לא דורש פעולה מצידך.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnBack.setOnClickListener(v -> finish());
    }

    // הוספנו רענון אוטומטי כשחוזרים ממסך הראיון
    @Override
    protected void onResume() {
        super.onResume();
        loadWorkerInvitations();
    }

    private void loadWorkerInvitations() {
        if (mAuth.getCurrentUser() == null) return;
        String currentWorkerId = mAuth.getCurrentUser().getUid();

        // שולפים את כל הזימונים ששייכים לעובד הזה
        db.collection("Invitations")
                .whereEqualTo("workerId", currentWorkerId)
                .get()
                .addOnCompleteListener(task -> {
                    inboxDisplayList.clear();
                    invitationIdsList.clear();

                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String invitationId = doc.getId();
                            String jobId = doc.getString("jobId");
                            String status = doc.getString("status");

                            // מעדכנים לסטטוסים החדשים
                            String displayStatus = "ממתין לתשובה";
                            if ("accepted".equals(status)) displayStatus = "אישרת. ממתין למעסיק";
                            if ("declined".equals(status)) displayStatus = "דחית את הזימון";
                            if ("interview_employer_proposed".equals(status)) displayStatus = "המעסיק הציע ראיון (לחץ לצפייה!)";
                            if ("interview_worker_proposed".equals(status)) displayStatus = "הצעת מועד חלופי. ממתין למעסיק";
                            if ("interview_scheduled".equals(status)) displayStatus = "ראיון נקבע!";
                            if ("interview_declined".equals(status)) displayStatus = "ביטלת את הראיון";

                            // הסטטוסים החדשים של ההחלטה הסופית!
                            if ("hired".equals(status)) displayStatus = "התקבלת לעבודה! 🎉";
                            if ("rejected".equals(status)) displayStatus = "לצערנו לא התקבלת הפעם";

                            // כדי להציג מידע יפה, אנחנו הולכים לשלוף גם את פרטי המשרה לפי ה-jobId
                            if (jobId != null) {
                                String finalDisplayStatus = displayStatus;
                                db.collection("JobPosts").document(jobId).get().addOnSuccessListener(jobDoc -> {
                                    if (jobDoc.exists()) {
                                        String jobType = jobDoc.getString("jobType");

                                        inboxDisplayList.add("הצעת עבודה: " + jobType + "\nסטטוס: " + finalDisplayStatus);
                                        invitationIdsList.add(invitationId); // שומרים את מזהה הזימון במקביל
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                    } else if (task.isSuccessful()) {
                        inboxDisplayList.add("אין לך הצעות עבודה כרגע.");
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "שגיאה בטעינת הנתונים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showResponseDialog(String invitationId) {
        new AlertDialog.Builder(this)
                .setTitle("מענה להצעת עבודה")
                .setMessage("האם ברצונך לאשר או לדחות את הזימון?")
                .setPositiveButton("אישור זימון", (dialog, which) -> {
                    updateInvitationStatus(invitationId, "accepted");
                })
                .setNegativeButton("דחיית זימון", (dialog, which) -> {
                    updateInvitationStatus(invitationId, "declined");
                })
                .setNeutralButton("ביטול", null)
                .show();
    }

    private void updateInvitationStatus(String invitationId, String newStatus) {
        db.collection("Invitations").document(invitationId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "הסטטוס עודכן בהצלחה!", Toast.LENGTH_SHORT).show();
                    loadWorkerInvitations(); // מרעננים את הרשימה כדי לראות את השינוי
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בעדכון הסטטוס", Toast.LENGTH_SHORT).show());
    }
}