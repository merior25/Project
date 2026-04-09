package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView; // הוספנו את זה
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginScreen extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView registerButton; // שינינו מ-Button ל-TextView

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton); // עכשיו זה ימצא את ה-TextView החדש

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "נא למלא פרטים", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "מתחבר...", Toast.LENGTH_SHORT).show();

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String userId = mAuth.getCurrentUser().getUid();

                            db.collection("Users").document(userId).get()
                                    .addOnCompleteListener(userTask -> {
                                        if (userTask.isSuccessful() && userTask.getResult() != null) {
                                            DocumentSnapshot document = userTask.getResult();

                                            if (document.exists()) {
                                                String userType = document.getString("userType");
                                                Boolean isProfileComplete = document.getBoolean("isProfileComplete");
                                                String firstName = document.getString("firstName");

                                                Intent intent;

                                                if (userType != null && userType.equals("עסק")) {
                                                    if (isProfileComplete != null && isProfileComplete) {
                                                        intent = new Intent(LoginScreen.this, employerScreen.class);
                                                    } else {
                                                        intent = new Intent(LoginScreen.this, EmployerSetupScreen.class);
                                                    }
                                                } else {
                                                    intent = new Intent(LoginScreen.this, workersc.class);
                                                }

                                                intent.putExtra("USER_NAME", firstName);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                startActivity(new Intent(LoginScreen.this, workersc.class));
                                                finish();
                                            }
                                        } else {
                                            Toast.makeText(this, "שגיאה בטעינת נתוני משתמש", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "שגיאה: אימייל או סיסמה לא נכונים", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        // כפתור המעבר להרשמה (עכשיו כ-TextView)
        registerButton.setOnClickListener(v -> startActivity(new Intent(this, RegisterScreen.class)));
    }
}