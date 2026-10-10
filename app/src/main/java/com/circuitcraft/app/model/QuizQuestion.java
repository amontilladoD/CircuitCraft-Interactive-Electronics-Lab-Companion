package com.circuitcraft.app.model;

public class QuizQuestion {
    public final String text;
    public final String[] options;
    public final int answerIndex;
    public final String explanation;

    public QuizQuestion(String text, String[] options, int answerIndex, String explanation) {
        this.text = text;
        this.options = options;
        this.answerIndex = answerIndex;
        this.explanation = explanation;
    }
}
