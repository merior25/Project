package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class employerScreen extends AppCompatActivity {

    private TextView welcomeText;
    private Button btnRecruit;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ListView jobsListView, allHiredWorkersListView;

    private ArrayList<String> jobsList;
    private ArrayList<String> jobIdsList;
    private ArrayAdapter<String> adapter;

    private ArrayList<String> allHiredList;
    private ArrayList<String> allHiredWorkerIds;
    private ArrayAdapter<String> hiredAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_screen);

        welcomeText = findViewById(R.id.greetingText);
        btnRecruit = findViewById(R.id.btnRecruit);
        jobsListView = findViewById(R.id.jobsListView);
        allHiredWorkersListView = findViewById(R.id.allHiredWorkersListView);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        jobsList = new ArrayList<>();
        jobIdsList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, jobsList);
        jobsListView.setAdapter(adapter);

        allHiredList = new ArrayList<>();
        allHiredWorkerIds = new ArrayList<>();
        hiredAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, allHiredList);
        allHiredWorkersListView.setAdapter(hiredAdapter);

        String nameFromIntent = getIntent().getStringExtra("USER_NAME");

        if (nameFromIntent != null) {
            welcomeText.setText("שלום, " + nameFromIntent);
        } else if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            db.collection("Users").document(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    DocumentSnapshot document = task.getResult();
                    String nameFromDB = document.getString("firstName");

                    if (nameFromDB != null) {
                        welcomeText.setText("שלום, " + nameFromDB);
                    } else {
                        welcomeText.setText("שלום, מעסיק");
                    }
                }
            });
        }

        jobsListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedJobId = jobIdsList.get(position);
            Intent intent = new Intent(employerScreen.this, RecruitActivity.class);
            intent.putExtra("JOB_ID", selectedJobId);
            startActivity(intent);
        });

        allHiredWorkersListView.setOnItemClickListener((parent, view, position, id) -> {
            if (allHiredWorkerIds.isEmpty()) return;
            String selectedWorkerId = allHiredWorkerIds.get(position);

            Intent intent = new Intent(employerScreen.this, CandidateScreen.class);
            intent.putExtra("CANDIDATE_ID", selectedWorkerId);
            startActivity(intent);
        });

        btnRecruit.setOnClickListener(v -> {
            Intent intent = new Intent(employerScreen.this, FindWorkersScreen.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMyJobs();
        loadAllHiredWorkers();
    }

    private void loadMyJobs() {
        if (mAuth.getCurrentUser() == null) return;
        String currentUserId = mAuth.getCurrentUser().getUid();

        // מושכים קודם את המיקום של העסק מהפרופיל של המעסיק!
        db.collection("Users").document(currentUserId).get().addOnSuccessListener(userDoc -> {
            String businessLocation = userDoc.getString("businessLocation");
            String displayLocation = (businessLocation != null && !businessLocation.isEmpty()) ? businessLocation : "לא צוין מיקום";

            // לאחר מכן מושכים את המשרות
            db.collection("JobPosts")
                    .whereEqualTo("employerId", currentUserId)
                    .get()
                    .addOnCompleteListener(task -> {
                        jobsList.clear();
                        jobIdsList.clear();

                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String jobType = document.getString("jobType");
                                String numWorkers = document.getString("numberOfWorkers");

                                if (jobType != null && numWorkers != null) {
                                    // מציגים את התחום והמיקום במסך של המעסיק
                                    jobsList.add("תחום: " + jobType + "\nמיקום: " + displayLocation + "\nדרושים: " + numWorkers);
                                    jobIdsList.add(document.getId());
                                }
                            }
                            adapter.notifyDataSetChanged();
                        }
                    });
        });
    }

    private void loadAllHiredWorkers() {
        if (mAuth.getCurrentUser() == null) return;
        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("Invitations")
                .whereEqualTo("employerId", currentUserId)
                .whereEqualTo("status", "hired")
                .get()
                .addOnCompleteListener(task -> {
                    allHiredList.clear();
                    allHiredWorkerIds.clear();

                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String workerId = doc.getString("workerId");
                            String jobId = doc.getString("jobId");
                            String hiredDate = doc.getString("hiredDate");

                            String displayDate = (hiredDate != null) ? hiredDate : "תאריך לא ידוע";

                            if (workerId != null && jobId != null) {
                                db.collection("JobPosts").document(jobId).get().addOnSuccessListener(jobDoc -> {
                                    String jobType = jobDoc.getString("jobType");

                                    db.collection("Users").document(workerId).get().addOnSuccessListener(workerDoc -> {
                                        String name = workerDoc.getString("firstName") + " " + workerDoc.getString("lastName");

                                        allHiredList.add(name + "\nתפקיד: " + jobType + "\nהתחיל ב: " + displayDate);
                                        allHiredWorkerIds.add(workerId);
                                        hiredAdapter.notifyDataSetChanged();
                                    });
                                });
                            }
                        }
                    } else if (task.isSuccessful()) {
                        allHiredList.add("טרם גויסו עובדים.");
                        hiredAdapter.notifyDataSetChanged();
                    }
                });
    }
}