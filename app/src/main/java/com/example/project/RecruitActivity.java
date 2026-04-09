package com.example.project;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class RecruitActivity extends AppCompatActivity {

    private ListView workersListView;
    private ArrayList<String> workersList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // תוודאי שזה השם של קובץ העיצוב שלך!
        setContentView(R.layout.activity_recruit);

        // 1. חיבור הרשימה מהעיצוב
        workersListView = findViewById(R.id.workersListView);

        // 2. הכנת הרשימה והמתאם (הגשר שמצייר את הנתונים)
        workersList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, workersList);
        workersListView.setAdapter(adapter);

        // 3. משיכת הנתונים מפיירבייס
        loadWorkersFromDatabase();
    }

    private void loadWorkersFromDatabase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // שאילתה פשוטה - תביא את כל המסמכים באוסף Users שהסוג שלהם הוא 'עובד'
        db.collection("Users")
                .whereEqualTo("userType", "עובד")
                .get()
                .addOnCompleteListener(task -> {
                    workersList.clear();

                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                            String workerName = document.getString("firstName");

                            if (workerName != null) {
                                workersList.add("שם העובד: " + workerName);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } else if (task.isSuccessful()) {
                        Toast.makeText(RecruitActivity.this, "עדיין אין עובדים במערכת", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RecruitActivity.this, "שגיאה מול השרת", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}