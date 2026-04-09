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

import java.util.HashMap;
import java.util.Map;

public class EmployerSetupScreen extends AppCompatActivity {

    private EditText editBusinessName, editBusinessLocation, editBusinessDescription;
    private Spinner spinnerBusinessCategory;
    private Button btnSaveBusinessProfile;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_setup_screen);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        editBusinessName = findViewById(R.id.editBusinessName);
        editBusinessLocation = findViewById(R.id.editBusinessLocation);
        editBusinessDescription = findViewById(R.id.editBusinessDescription);
        spinnerBusinessCategory = findViewById(R.id.spinnerBusinessCategory);
        btnSaveBusinessProfile = findViewById(R.id.btnSaveBusinessProfile);

        String[] categories = {"בחר תחום עסק...", "מסעדות ובתי קפה", "אבטחה ושמירה", "ניקיון ואחזקה", "אירועים והפקות", "מכירות וקמעונאות", "מלונאות ותיירות", "אחר"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerBusinessCategory.setAdapter(adapter);

        btnSaveBusinessProfile.setOnClickListener(v -> saveBusinessProfile());
    }

    private void saveBusinessProfile() {
        String name = editBusinessName.getText().toString().trim();
        String location = editBusinessLocation.getText().toString().trim();
        String description = editBusinessDescription.getText().toString().trim();
        String category = spinnerBusinessCategory.getSelectedItem().toString();

        if (name.isEmpty() || location.isEmpty() || category.equals("בחר תחום עסק...")) {
            Toast.makeText(this, "נא למלא את שם העסק, מיקום ותחום", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "שגיאה: משתמש לא מחובר", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> businessUpdates = new HashMap<>();
        businessUpdates.put("businessName", name);
        businessUpdates.put("businessLocation", location);
        businessUpdates.put("businessDescription", description);
        businessUpdates.put("businessCategory", category);

        businessUpdates.put("isProfileComplete", true);

        db.collection("Users").document(userId)
                .update(businessUpdates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EmployerSetupScreen.this, "הפרופיל עודכן בהצלחה!", Toast.LENGTH_SHORT).show();

                    // מעבר למסך הראשי של המעסיק!
                    Intent intent = new Intent(EmployerSetupScreen.this, employerScreen.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EmployerSetupScreen.this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}