package com.circuitcraft.app.ui;

import androidx.lifecycle.ViewModel;

import com.circuitcraft.app.model.QuizQuestion;

import java.util.List;

/** Keeps the quiz alive when the screen rotates (the Activity is destroyed and re-created, this object is not). */
public class QuizViewModel extends ViewModel {
    public List<QuizQuestion> questions;
    public int index, score, elapsed, selected = -1;
    public boolean answered, timerStarted, finished, saved;
}
