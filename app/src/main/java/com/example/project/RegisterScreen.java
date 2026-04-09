package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.CheckBox;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterScreen extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private EditText firstNameInput, lastNameInput, phoneInput, emailInput, passwordInput;
    private RadioButton radioBusiness, radioWorker;
    private Button registerButton;
    private CheckBox checkboxIsEmployed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_screen);

        mAuth = FirebaseAuth.getInstance();

        firstNameInput = findViewById(R.id.firstNameInputRegister);
        lastNameInput = findViewById(R.id.lastNameInputRegister);
        phoneInput = findViewById(R.id.editTextPhone);
        emailInput = findViewById(R.id.emailInputRegister);
        passwordInput = findViewById(R.id.passwordInputRegister);
        radioBusiness = findViewById(R.id.radioButton);
        radioWorker = findViewById(R.id.radioButton2);
        registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> {
            String firstName = firstNameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String lastName = lastNameInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();

            final String userType = radioBusiness.isChecked() ? "עסק" : (radioWorker.isChecked() ? "עובד" : "");

            if (firstName.isEmpty() || email.isEmpty() || password.isEmpty() || userType.isEmpty() || lastName.isEmpty()||phone.isEmpty()) {
                Toast.makeText(this, "נא למלא את כל השדות כולל סוג משתמש", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // התיקון הקריטי: שומרים את המשתמש תחת ה-UID המדויק שלו מ-Auth
                            String userId = mAuth.getCurrentUser().getUid();

                            Map<String, Object> userDetails = new HashMap<>();
                            userDetails.put("firstName", firstName);
                            userDetails.put("lastName", lastName);
                            userDetails.put("userType", userType);
                            userDetails.put("phone", phone);
                            userDetails.put("email", email);
                            userDetails.put("password", password);

                            // אם זה עסק, נגדיר לו מראש שהפרופיל עוד לא הושלם
                            if (userType.equals("עסק")) {
                                userDetails.put("isProfileComplete", false);
                            }

                            db.collection("Users").document(userId).set(userDetails)
                                    .addOnCompleteListener(dbTask -> {
                                        Intent intent;
                                        if (userType.equals("עסק")) {
                                            // מעסיק חדש נשלח ישר למסך השאלון!
                                            intent = new Intent(RegisterScreen.this, EmployerSetupScreen.class);
                                        } else {
                                            intent = new Intent(RegisterScreen.this, workersc.class);
                                        }
                                        intent.putExtra("USER_NAME", firstName);
                                        startActivity(intent);
                                        finish();
                                    });
                        } else {
                            try {
                                throw task.getException();
                            } catch (com.google.firebase.auth.FirebaseAuthUserCollisionException e) {
                                Toast.makeText(RegisterScreen.this, "האימייל הזה כבר רשום במערכת! נסי להתחבר ממסך הכניסה.", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(RegisterScreen.this, "שגיאה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }
}