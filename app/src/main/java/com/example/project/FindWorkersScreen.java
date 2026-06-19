package com.example.project; // ודאי שזה תואם לשם החבילה שלך

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FindWorkersScreen extends AppCompatActivity {

    // הגדרת המשתנים של המסך
    private Spinner spinnerJobType;
    private EditText editNumWorkers;
    private EditText editRequirements;
    private Button buttonSubmit;

    // משתני פיירבייס
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ודאי ששם הקובץ פה תואם לשם קובץ ה-XML שלך
        setContentView(R.layout.activity_find_workers_screen);

        // 1. אתחול פיירבייס
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 2. קישור המשתנים לעיצוב
        spinnerJobType = findViewById(R.id.spinner);
        editNumWorkers = findViewById(R.id.editTextNumberDecimal);
        editRequirements = findViewById(R.id.editTextTextMultiLine);
        buttonSubmit = findViewById(R.id.buttonSubmit);

        // 3. מילוי ה-Spinner מתוך קובץ ה-strings.xml (מעודכן ל-shared_job_fields)
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.shared_job_fields, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerJobType.setAdapter(adapter);

        // 4. מה קורה כשלוחצים על הכפתור?
        buttonSubmit.setOnClickListener(v -> saveJobToFirestore());
    }

    private void saveJobToFirestore() {
        // משיכת הנתונים שהמשתמש הזין
        String selectedJob = spinnerJobType.getSelectedItem().toString();
        String numWorkers = editNumWorkers.getText().toString().trim();
        String requirements = editRequirements.getText().toString().trim();

        // בדיקה שהמשתמש לא השאיר שדות ריקים (בודקים אם המיקום הוא 0, שזה פריט החובה הראשון)
        if (spinnerJobType.getSelectedItemPosition() == 0 || numWorkers.isEmpty() || requirements.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות ולבחור תפקיד", Toast.LENGTH_SHORT).show();
            return;
        }

        // הכנת הנתונים לשמירה ב-Firestore
        Map<String, Object> jobPost = new HashMap<>();
        jobPost.put("jobType", selectedJob);
        jobPost.put("numberOfWorkers", numWorkers);
        jobPost.put("requirements", requirements);

        // נשמור גם מי המעסיק שפרסם את זה
        if (mAuth.getCurrentUser() != null) {
            jobPost.put("employerId", mAuth.getCurrentUser().getUid());
        }

        // שמירה במסד הנתונים
        db.collection("JobPosts")
                .add(jobPost)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(FindWorkersScreen.this, "המשרה פורסמה בהצלחה!", Toast.LENGTH_LONG).show();
                    // סגירת המסך הזה וחזרה למסך הקודם של המעסיק
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(FindWorkersScreen.this, "שגיאה בפרסום המשרה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}