package com.circuitcraft.app.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.circuitcraft.app.R;
import com.circuitcraft.app.data.QuizRepository;
import com.circuitcraft.app.data.ScoreRepository;
import com.circuitcraft.app.databinding.ActivityQuizBinding;
import com.circuitcraft.app.model.QuizQuestion;
import com.circuitcraft.app.service.QuizTimerService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Locale;

/**
 * Quiz flow: load questions (HTTP JSON, offline fallback) -> start the FOREGROUND service (stopwatch + notification)
 * -> Check answer -> Next ... -> Finish: stop service, save score to Firestore + Realtime Database.
 * All state lives in QuizViewModel, so rotating the phone does not lose progress.
 */
public class QuizActivity extends BaseActivity {
    private ActivityQuizBinding b;
    private QuizViewModel vm;
    private RadioButton[] radios;
    private AlertDialog resultDialog;
    private ActivityResultLauncher<String> notificationPermission;

    // Receives the Service's once-per-second broadcast
    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            vm.elapsed = intent.getIntExtra(QuizTimerService.EXTRA_SECONDS, 0);
            showTimer();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(this).get(QuizViewModel.class);
        b = ActivityQuizBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        radios = new RadioButton[]{b.rb0, b.rb1, b.rb2, b.rb3};

        // Android 13+: the notification needs a runtime permission. The service runs either way.
        notificationPermission = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> startTimerService());

        b.btnNext.setOnClickListener(v -> onButton());
        showTimer();

        if (vm.questions == null) {
            b.rgOptions.setVisibility(View.INVISIBLE);
            b.btnNext.setEnabled(false);
            loadQuestions();
        } else {                                   // re-created after rotation
            b.rgOptions.setVisibility(View.VISIBLE);
            render();
            if (vm.finished) showResultDialog();
        }
    }

    private void loadQuestions() {
        QuizRepository.load(this, (list, fromNetwork, error) -> {
            if (isFinishing() || isDestroyed()) return;
            if (list.isEmpty()) {
                Toast.makeText(this, "Could not load questions", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            vm.questions = list;
            Toast.makeText(this, fromNetwork
                    ? "Questions loaded online (" + list.size() + ")"
                    : "Using offline questions" + (error != null ? " - " + error : ""),
                    Toast.LENGTH_LONG).show();
            b.rgOptions.setVisibility(View.VISIBLE);
            ensureTimer();
            render();
        });
    }

    private void ensureTimer() {
        if (vm.timerStarted) return;
        vm.timerStarted = true;
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            startTimerService();
        }
    }

    private void startTimerService() {
        ContextCompat.startForegroundService(this, new Intent(this, QuizTimerService.class));
    }

    private void showTimer() {
        b.tvTimer.setText(String.format(Locale.US, "⏱ %02d:%02d", vm.elapsed / 60, vm.elapsed % 60));
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
        if (resultDialog != null) resultDialog.dismiss();
        // on rotation the Activity is destroyed too - keep the service (and the clock) running then
        if (!isChangingConfigurations()) stopService(new Intent(this, QuizTimerService.class));
    }

    /** Draws the current question from the ViewModel state. */
    private void render() {
        QuizQuestion q = vm.questions.get(vm.index);
        b.tvProgress.setText("Question " + (vm.index + 1) + " of " + vm.questions.size());
        b.tvQuestion.setText(q.text);
        for (int i = 0; i < radios.length; i++) {
            if (i < q.options.length) {
                radios[i].setVisibility(View.VISIBLE);
                radios[i].setText(q.options[i]);
                radios[i].setEnabled(!vm.answered);
            } else {
                radios[i].setVisibility(View.GONE);
            }
        }
        if (vm.selected >= 0) radios[vm.selected].setChecked(true);

        if (vm.answered) {
            boolean correct = vm.selected == q.answerIndex;
            b.tvFeedback.setText(correct
                    ? "✔ Correct! " + q.explanation
                    : "✘ Wrong. Correct answer: " + q.options[q.answerIndex] + "\n" + q.explanation);
            b.tvFeedback.setTextColor(getColor(correct ? R.color.ok_green : R.color.error_red));
            b.tvFeedback.setVisibility(View.VISIBLE);
            b.btnNext.setText(vm.index == vm.questions.size() - 1 ? "Finish" : "Next question");
        } else {
            b.tvFeedback.setVisibility(View.GONE);
            b.btnNext.setText("Check answer");
        }
        b.btnNext.setEnabled(true);
    }

    private int selectedIndex() {
        for (int i = 0; i < radios.length; i++) if (radios[i].isChecked()) return i;
        return -1;
    }

    private void onButton() {
        QuizQuestion q = vm.questions.get(vm.index);
        if (!vm.answered) {
            int pick = selectedIndex();
            if (pick < 0) {
                Toast.makeText(this, "Select an answer first", Toast.LENGTH_SHORT).show();
                return;
            }
            vm.selected = pick;
            vm.answered = true;
            if (pick == q.answerIndex) vm.score++;
            render();
        } else if (vm.index >= vm.questions.size() - 1) {
            finishQuiz();
        } else {
            vm.index++;
            vm.answered = false;
            vm.selected = -1;
            b.rgOptions.clearCheck();
            render();
        }
    }

    private void finishQuiz() {
        vm.finished = true;
        stopService(new Intent(this, QuizTimerService.class));
        if (!vm.saved) {
            vm.saved = true;
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) ScoreRepository.save(user, vm.score, vm.questions.size(), vm.elapsed);
        }
        showResultDialog();
    }

    private void showResultDialog() {
        resultDialog = new AlertDialog.Builder(this)
                .setTitle("Quiz finished 🎉")
                .setMessage("Score: " + vm.score + " / " + vm.questions.size()
                        + "\nTime: " + String.format(Locale.US, "%02d:%02d", vm.elapsed / 60, vm.elapsed % 60))
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> finish())
                .setNegativeButton("Leaderboard", (d, w) -> {
                    startActivity(new Intent(this, LeaderboardActivity.class));
                    finish();
                })
                .show();
    }
}
