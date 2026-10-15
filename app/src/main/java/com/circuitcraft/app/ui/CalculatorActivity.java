package com.circuitcraft.app.ui;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.circuitcraft.app.databinding.ActivityCalculatorBinding;
import com.circuitcraft.app.logic.CircuitCalculator;

public class CalculatorActivity extends AppCompatActivity {
    private ActivityCalculatorBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityCalculatorBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.btnOhm.setOnClickListener(v -> solveOhm());
    }

    private Double read(EditText et) {
        String s = et.getText().toString().trim();
        if (s.isEmpty()) return null;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private void solveOhm() {
        Double v = read(b.etV), i = read(b.etI), r = read(b.etR);
        int empty = (v == null ? 1 : 0) + (i == null ? 1 : 0) + (r == null ? 1 : 0);
        if (empty != 1) {
            b.tvOhmResult.setText("Fill exactly two boxes and leave the one you want to find empty.");
            return;
        }
        if ((v != null && v.isNaN()) || (i != null && i.isNaN()) || (r != null && r.isNaN())) {
            b.tvOhmResult.setText("Please enter valid numbers.");
            return;
        }

        String formula;
        double vv, ii, rr;
        if (v == null) {
            vv = CircuitCalculator.voltage(i, r);
            ii = i;
            rr = r;
            formula = "V = I × R\nV = " + CircuitCalculator.format(ii) + " × " + CircuitCalculator.format(rr)
                    + " = " + CircuitCalculator.format(vv) + " V";
        } else if (i == null) {
            if (r == 0) { b.tvOhmResult.setText("Resistance cannot be 0 (that is a short circuit!)."); return; }
            vv = v;
            rr = r;
            ii = CircuitCalculator.current(vv, rr);
            formula = "I = V ÷ R\nI = " + CircuitCalculator.format(vv) + " ÷ " + CircuitCalculator.format(rr)
                    + " = " + CircuitCalculator.format(ii) + " A";
        } else {
            if (i == 0) { b.tvOhmResult.setText("Current cannot be 0 here (division by zero)."); return; }
            vv = v;
            ii = i;
            rr = CircuitCalculator.resistance(vv, ii);
            formula = "R = V ÷ I\nR = " + CircuitCalculator.format(vv) + " ÷ " + CircuitCalculator.format(ii)
                    + " = " + CircuitCalculator.format(rr) + " Ω";
        }
        double p = CircuitCalculator.power(vv, ii);
        b.tvOhmResult.setText(formula + "\n\nPower: P = V × I = " + CircuitCalculator.format(p) + " W");
    }
}
