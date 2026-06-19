package com.example.project;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class WorkerInterviewResponseActivity extends AppCompatActivity {
    private TextView tvDetails;
    private EditText etNewDate, etNewTime;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String invitationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_interview_response);

        invitationId = getIntent().getStringExtra("INVITATION_ID");
        tvDetails = findViewById(R.id.tvInterviewDetails);
        etNewDate = findViewById(R.id.etNewDate);
        etNewTime = findViewById(R.id.etNewTime);

        db.collection("Invitations").document(invitationId).get().addOnSuccessListener(doc -> {
            if(doc.exists()) {
                String details = "תאריך: " + doc.getString("interviewDate") + "\n" +
                        "שעה: " + doc.getString("interviewTime") + "\n" +
                        "מיקום: " + doc.getString("interviewLocation") + "\n" +
                        "הערות: " + doc.getString("interviewDesc");
                tvDetails.setText(details);
            }
        });

        // פתיחת חלונית תאריך
        etNewDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String dateStr = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                        etNewDate.setText(dateStr);
                    }, year, month, day);
            datePickerDialog.show();
        });

        // פתיחת חלונית שעון
        etNewTime.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, selectedHour, selectedMinute) -> {
                        String timeStr = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                        etNewTime.setText(timeStr);
                    }, hour, minute, true);
            timePickerDialog.show();
        });

        findViewById(R.id.btnAcceptInterview).setOnClickListener(v -> {
            db.collection("Invitations").document(invitationId).update("status", "interview_scheduled")
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "ראיון נקבע בהצלחה!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        });

        findViewById(R.id.btnProposeNew).setOnClickListener(v -> {
            String newDate = etNewDate.getText().toString();
            String newTime = etNewTime.getText().toString();

            if (newDate.isEmpty() || newTime.isEmpty()) {
                Toast.makeText(this, "חובה לבחור תאריך ושעה חלופיים", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("interviewDate", newDate);
            updates.put("interviewTime", newTime);
            updates.put("status", "interview_worker_proposed");

            db.collection("Invitations").document(invitationId).update(updates).addOnSuccessListener(a -> {
                Toast.makeText(this, "המועד החלופי נשלח למעסיק!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });

        findViewById(R.id.btnDecline).setOnClickListener(v -> {
            db.collection("Invitations").document(invitationId).update("status", "interview_declined");
            finish();
        });
    }
}