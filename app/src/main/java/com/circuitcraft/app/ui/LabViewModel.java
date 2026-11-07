package com.circuitcraft.app.ui;

import androidx.lifecycle.ViewModel;

import com.circuitcraft.app.sim.CircuitModel;

/** Keeps the circuit you are building when the screen rotates. */
public class LabViewModel extends ViewModel {
    public CircuitModel model;
    public boolean resumeRun;      // simulation was running when the screen paused / rotated
    public int selected = -1;      // index of the selected part
}
