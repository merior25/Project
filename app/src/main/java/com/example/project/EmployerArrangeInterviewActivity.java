package com.example.project;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EmployerArrangeInterviewActivity extends AppCompatActivity {
    private EditText etDate, etTime, etLocation, etDesc;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String invitationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employer_arrange_interview);

        invitationId = getIntent().getStringExtra("INVITATION_ID");
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etLocation = findViewById(R.id.etLocation);
        etDesc = findViewById(R.id.etDesc);

        if(invitationId != null) {
            db.collection("Invitations").document(invitationId).get().addOnSuccessListener(doc -> {
                if(doc.exists()) {
                    if(doc.contains("interviewDate")) etDate.setText(doc.getString("interviewDate"));
                    if(doc.contains("interviewTime")) etTime.setText(doc.getString("interviewTime"));
                    if(doc.contains("interviewLocation")) etLocation.setText(doc.getString("interviewLocation"));
                    if(doc.contains("interviewDesc")) etDesc.setText(doc.getString("interviewDesc"));
                }
            });
        }

        // פתיחת חלונית תאריך
        etDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String dateStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                        etDate.setText(dateStr);
                    }, year, month, day);
            datePickerDialog.show();
        });

        // פתיחת חלונית שעון
        etTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, selectedHour, selectedMinute) -> {
                        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                        etTime.setText(timeStr);
                    }, hour, minute, true); // true = פורמט 24 שעות
            timePickerDialog.show();
        });

        findViewById(R.id.btnSendInterview).setOnClickListener(v -> {
            String date = etDate.getText().toString();
            String time = etTime.getText().toString();

            if (date.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "חובה לבחור תאריך ושעה", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("interviewDate", date);
            updates.put("interviewTime", time);
            updates.put("interviewLocation", etLocation.getText().toString());
            updates.put("interviewDesc", etDesc.getText().toString());
            updates.put("status", "interview_employer_proposed");

            db.collection("Invitations").document(invitationId).update(updates).addOnSuccessListener(a -> {
                Toast.makeText(this, "הצעת הראיון נשלחה לעובד!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}