package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class WorkerSetupActivity extends AppCompatActivity {

    private Spinner spinnerField;
    private EditText etAge, etExperience;
    private Button btnSave;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userName; // נשמור פה את שם המשתמש שהגיע ממסך ההרשמה

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_setup);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        spinnerField = findViewById(R.id.spinnerWorkerField);
        etAge = findViewById(R.id.etWorkerAge);
        etExperience = findViewById(R.id.etWorkerExperience);
        btnSave = findViewById(R.id.btnSaveWorkerSetup);

        // מושכים את השם שהועבר ממסך ההרשמה
        userName = getIntent().getStringExtra("USER_NAME");

        // טעינת הרשימה המשותפת מתוך קובץ ה-strings.xml אל תוך הספינר
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.shared_job_fields, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerField.setAdapter(adapter);

        btnSave.setOnClickListener(v -> saveWorkerData());
    }

    private void saveWorkerData() {
        String selectedField = spinnerField.getSelectedItem().toString();
        String ageStr = etAge.getText().toString().trim();
        String expStr = etExperience.getText().toString().trim();

        // בדיקות תקינות קצרות
        if (selectedField.equals("בחר תחום עיסוק...")) {
            Toast.makeText(this, "אנא בחר תחום עיסוק", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ageStr.isEmpty() || expStr.isEmpty()) {
            Toast.makeText(this, "אנא מלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            // אריזת הנתונים למפה כדי לשלוח לפיירבייס
            Map<String, Object> workerData = new HashMap<>();

            // תזכורת: בסינון המשרות הקודם בדקנו מול השדה "fieldOfWork",
            // לכן אני שומר את זה כאן גם בתור "fieldOfWork" כדי שההתאמה תעבוד מושלם!
            workerData.put("fieldOfWork", selectedField);
            workerData.put("age", ageStr); // אפשר לשמור כסטרינג כדי שיתאים למה שקראנו מהמסכים הקודמים
            workerData.put("experience", expStr);
            workerData.put("setupCompleted", true); // סימון שהעובד סיים את השאלון

            // שמירה ב-Firestore תחת אוסף המשתמשים
            db.collection("Users").document(userId)
                    .set(workerData, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "הנתונים נשמרו בהצלחה!", Toast.LENGTH_SHORT).show();

                        // יצירת המעבר למסך העובד והעברת השם הלאה!
                        Intent intent = new Intent(WorkerSetupActivity.this, workersc.class);
                        if (userName != null) {
                            intent.putExtra("USER_NAME", userName);
                        }
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}