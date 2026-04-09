package com.example.project; // ודאי שזה תואם לשם החבילה שלך

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.project.R;
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

        // 2. קישור המשתנים לעיצוב (לפי ה-IDs שנתת ב-XML)
        spinnerJobType = findViewById(R.id.spinner);
        editNumWorkers = findViewById(R.id.editTextNumberDecimal);
        editRequirements = findViewById(R.id.editTextTextMultiLine);
        buttonSubmit = findViewById(R.id.buttonSubmit); // הכפתור שהוספנו

        // 3. מילוי ה-Spinner (התפריט הנפתח) ברשימת תפקידים
        String[] jobCategories = {"בחר תפקיד...", "מלצרות", "ברמנים", "טבחים", "אבטחה", "ניקיון", "מכירות", "אחר"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, jobCategories);
        spinnerJobType.setAdapter(adapter);

        // 4. מה קורה כשלוחצים על הכפתור?
        buttonSubmit.setOnClickListener(v -> saveJobToFirestore());
    }

    private void saveJobToFirestore() {
        // משיכת הנתונים שהמשתמש הזין
        String selectedJob = spinnerJobType.getSelectedItem().toString();
        String numWorkers = editNumWorkers.getText().toString().trim();
        String requirements = editRequirements.getText().toString().trim();

        // בדיקה שהמשתמש לא השאיר שדות ריקים
        if (selectedJob.equals("בחר תפקיד...") || numWorkers.isEmpty() || requirements.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות ולבחור תפקיד", Toast.LENGTH_SHORT).show();
            return;
        }

        // הכנת הנתונים לשמירה ב-Firestore
        Map<String, Object> jobPost = new HashMap<>();
        jobPost.put("jobType", selectedJob);
        jobPost.put("numberOfWorkers", numWorkers);
        jobPost.put("requirements", requirements);

        // נשמור גם מי המעסיק שפרסם את זה (כדי שנדע לשייך את המשרה אליו)
        if (mAuth.getCurrentUser() != null) {
            jobPost.put("employerId", mAuth.getCurrentUser().getUid());
        }

        // שמירה במסד הנתונים באוסף חדש בשם "JobPosts"
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