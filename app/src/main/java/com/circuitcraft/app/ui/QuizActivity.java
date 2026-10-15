package com.circuitcraft.app.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.circuitcraft.app.data.QuizRepository;
import com.circuitcraft.app.databinding.ActivityQuizBinding;
import com.circuitcraft.app.model.QuizQuestion;
import com.circuitcraft.app.service.QuizTimerService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuizActivity extends AppCompatActivity {
    private ActivityQuizBinding b;
    private RadioButton[] radios;
    private List<QuizQuestion> questions = new ArrayList<>();
    private int index = 0, score = 0, elapsed = 0;
    private boolean answered = false;

    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            elapsed = intent.getIntExtra(QuizTimerService.EXTRA_SECONDS, 0);
            b.tvTimer.setText(String.format(Locale.US, "⏱ %02d:%02d", elapsed / 60, elapsed % 60));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityQuizBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        radios = new RadioButton[]{b.rb0, b.rb1, b.rb2, b.rb3};
        b.rgOptions.setVisibility(View.INVISIBLE);

        b.btnNext.setOnClickListener(v -> onButton());

        QuizRepository.load(this, (list, fromNetwork) -> {
            if (isFinishing() || isDestroyed()) return;
            if (list.isEmpty()) {
                Toast.makeText(this, "Could not load questions", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            questions = list;
            Toast.makeText(this, fromNetwork ? "Questions loaded online" : "Using offline questions",
                    Toast.LENGTH_SHORT).show();
            startService(new Intent(this, QuizTimerService.class));   
            b.rgOptions.setVisibility(View.VISIBLE);
            showQuestion();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(tickReceiver, new IntentFilter(QuizTimerService.ACTION_TICK));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(tickReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, QuizTimerService.class));
    }

    private void showQuestion() {
        QuizQuestion q = questions.get(index);
        answered = false;
        b.tvProgress.setText("Question " + (index + 1) + " of " + questions.size());
        b.tvQuestion.setText(q.text);
        b.rgOptions.clearCheck();
        for (int i = 0; i < radios.length; i++) {
            if (i < q.options.length) {
                radios[i].setVisibility(View.VISIBLE);
                radios[i].setText(q.options[i]);
                radios[i].setEnabled(true);
            } else {
                radios[i].setVisibility(View.GONE);
            }
        }
        b.tvFeedback.setVisibility(View.GONE);
        b.btnNext.setText("Check answer");
        b.btnNext.setEnabled(true);
    }

    private int selected() {
        for (int i = 0; i < radios.length; i++) if (radios[i].isChecked()) return i;
        return -1;
    }

    private void onButton() {
        QuizQuestion q = questions.get(index);
        if (!answered) {
            int pick = selected();
            if (pick < 0) {
                Toast.makeText(this, "Select an answer first", Toast.LENGTH_SHORT).show();
                return;
            }
            answered = true;
            for (RadioButton rb : radios) rb.setEnabled(false);
            boolean correct = pick == q.answerIndex;
            if (correct) score++;
            b.tvFeedback.setText(correct
                    ? "✔ Correct! " + q.explanation
                    : "✘ Wrong. Correct answer: " + q.options[q.answerIndex] + "\n" + q.explanation);
            b.tvFeedback.setTextColor(getColor(correct ? com.circuitcraft.app.R.color.ok_green
                    : com.circuitcraft.app.R.color.error_red));
            b.tvFeedback.setVisibility(View.VISIBLE);
            b.btnNext.setText(index == questions.size() - 1 ? "Finish" : "Next question");
        } else {
            index++;
            if (index >= questions.size()) finishQuiz();
            else showQuestion();
        }
    }

    private void finishQuiz() {
        stopService(new Intent(this, QuizTimerService.class));
        saveScore();
        new AlertDialog.Builder(this)
                .setTitle("Quiz finished 🎉")
                .setMessage("Score: " + score + " / " + questions.size()
                        + "\nTime: " + String.format(Locale.US, "%02d:%02d", elapsed / 60, elapsed % 60))
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> finish())
                .show();
    }

    private void saveScore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("uid", user.getUid());
        data.put("email", user.getEmail());
        data.put("score", score);
        data.put("total", questions.size());
        data.put("seconds", elapsed);
        data.put("timestamp", System.currentTimeMillis());
        FirebaseFirestore.getInstance().collection("scores").add(data);
    }
}
