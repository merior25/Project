package com.example.project;

import android.content.Intent; // הוספנו את ה-Intent בשביל המעבר למסך הראשי
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class EditRecruitActivity extends AppCompatActivity {

    private EditText editNumWorkers, editRequirements;
    private ListView invitedWorkersListView;
    private Button btnSaveChanges, btnDeleteRecruit;

    private FirebaseFirestore db;
    private String jobId; // המזהה של הגיוס הספציפי במסד הנתונים

    private ArrayList<String> invitedWorkersList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recruit);

        db = FirebaseFirestore.getInstance();

        // קבלת מזהה הגיוס מהמסך הקודם
        jobId = getIntent().getStringExtra("JOB_ID");

        editNumWorkers = findViewById(R.id.editNumWorkers);
        editRequirements = findViewById(R.id.editRequirements);
        invitedWorkersListView = findViewById(R.id.invitedWorkersListView);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnDeleteRecruit = findViewById(R.id.btnDeleteRecruit);

        // הגדרת רשימת העובדים שהוזמנו
        invitedWorkersList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, invitedWorkersList);
        invitedWorkersListView.setAdapter(adapter);

        if (jobId != null) {
            loadJobData();
        } else {
            Toast.makeText(this, "שגיאה: לא נבחר גיוס", Toast.LENGTH_SHORT).show();
            finish();
        }

        // הסרת עובד בלחיצה על השם שלו ברשימה
        invitedWorkersListView.setOnItemClickListener((parent, view, position, id) -> {
            String workerToRemove = invitedWorkersList.get(position);

            new AlertDialog.Builder(this)
                    .setTitle("הסרת עובד")
                    .setMessage("האם אתה בטוח שברצונך להסיר את " + workerToRemove + " מהגיוס?")
                    .setPositiveButton("כן", (dialog, which) -> {
                        invitedWorkersList.remove(position);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "העובד הוסר מהרשימה (זכור לשמור שינויים)", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("ביטול", null)
                    .show();
        });

        // שמירת שינויים
        btnSaveChanges.setOnClickListener(v -> saveChangesToFirestore());

        // מחיקת הגיוס
        btnDeleteRecruit.setOnClickListener(v -> deleteRecruitFromFirestore());
    }

    private void loadJobData() {
        db.collection("JobPosts").document(jobId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        editNumWorkers.setText(documentSnapshot.getString("numberOfWorkers"));
                        editRequirements.setText(documentSnapshot.getString("requirements"));

                        ArrayList<String> workersFromDb = (ArrayList<String>) documentSnapshot.get("invitedWorkers");
                        if (workersFromDb != null) {
                            invitedWorkersList.addAll(workersFromDb);
                            adapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בטעינת הנתונים", Toast.LENGTH_SHORT).show());
    }

    private void saveChangesToFirestore() {
        String updatedNumWorkers = editNumWorkers.getText().toString().trim();
        String updatedRequirements = editRequirements.getText().toString().trim();

        if (updatedNumWorkers.isEmpty() || updatedRequirements.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("numberOfWorkers", updatedNumWorkers);
        updates.put("requirements", updatedRequirements);
        updates.put("invitedWorkers", invitedWorkersList);

        db.collection("JobPosts").document(jobId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "הגיוס עודכן בהצלחה!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בעדכון הגיוס", Toast.LENGTH_SHORT).show());
    }

    private void deleteRecruitFromFirestore() {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת גיוס")
                .setMessage("האם אתה בטוח שברצונך למחוק גיוס זה לצמיתות?")
                .setPositiveButton("כן, מחק", (dialog, which) -> {
                    db.collection("JobPosts").document(jobId).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "הגיוס נמחק בהצלחה!", Toast.LENGTH_SHORT).show();

                                // --- התיקון כאן: פותח ישירות את מסך המעסיק הראשי ומנקה את זיכרון המסכים הקודמים ---
                                Intent intent = new Intent(EditRecruitActivity.this, employerScreen.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish(); // סוגר את המסך הנוכחי
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "שגיאה במחיקת הגיוס: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}