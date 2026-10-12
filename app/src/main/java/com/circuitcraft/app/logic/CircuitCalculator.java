package com.circuitcraft.app.logic;

import java.util.Locale;

public class CircuitCalculator {

    public static double voltage(double current, double resistance) { return current * resistance; }
    public static double current(double voltage, double resistance) { return voltage / resistance; }
    public static double resistance(double voltage, double current) { return voltage / current; }

    public static double power(double voltage, double current) { return voltage * current; }

    public static String format(double v) {
        if (Double.isInfinite(v)) return "∞";
        if (Double.isNaN(v)) return "-";
        String s = String.format(Locale.US, "%.3f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            s = s.replaceAll("\\.$", "");
        }
        return s;
    }
}
