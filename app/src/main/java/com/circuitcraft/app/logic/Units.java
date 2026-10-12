package com.circuitcraft.app.logic;

import java.util.Locale;

public class Units {

    public static String eng(double v, String unit) {
        double a = Math.abs(v);
        if (a < 1e-12) return "0 " + unit;
        String[] pre = {"p", "n", "µ", "m", "", "k", "M"};
        double[] mult = {1e-12, 1e-9, 1e-6, 1e-3, 1, 1e3, 1e6};
        int idx;
        if (a >= 1e6) idx = 6;
        else if (a >= 1e3) idx = 5;
        else if (a >= 1) idx = 4;
        else if (a >= 1e-3) idx = 3;
        else if (a >= 1e-6) idx = 2;
        else if (a >= 1e-9) idx = 1;
        else idx = 0;
        double val = v / mult[idx];
        String fmt = Math.abs(val) >= 100 ? "%.0f" : (Math.abs(val) >= 10 ? "%.1f" : "%.2f");
        String s = String.format(Locale.US, fmt, val);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            s = s.replaceAll("\\.$", "");
        }
        return s + " " + pre[idx] + unit;
    }

    public static double parse(String text) {
        String s = text == null ? "" : text.trim().replace("Ω", "").replace("ohm", "").trim();
        if (s.isEmpty()) throw new NumberFormatException("empty");
        char last = s.charAt(s.length() - 1);
        double mult = 1;
        switch (last) {
            case 'k': case 'K': mult = 1e3; s = s.substring(0, s.length() - 1); break;
            case 'M': mult = 1e6; s = s.substring(0, s.length() - 1); break;
            case 'm': mult = 1e-3; s = s.substring(0, s.length() - 1); break;
            case 'u': case 'µ': mult = 1e-6; s = s.substring(0, s.length() - 1); break;
            case 'n': mult = 1e-9; s = s.substring(0, s.length() - 1); break;
            case 'p': mult = 1e-12; s = s.substring(0, s.length() - 1); break;
            default: break;
        }
        return Double.parseDouble(s.trim()) * mult;
    }
}
