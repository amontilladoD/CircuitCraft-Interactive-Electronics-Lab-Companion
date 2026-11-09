package com.circuitcraft.app.sim;


public class Part {
    public PartType type;
    public int x, y;          // origin (= pin 0). For JUMPER: first end.
    public int x2, y2;        // JUMPER only: second end
    public int rot;           // 0..3  (x 90 degrees clockwise)
    public double value;      // ohms / farads / volts / peak volts
    public double value2;     // AC frequency (Hz)
    public int mode;          // switch: 1 = closed. multimeter: 1 = ammeter mode


    public String reading = "";
    public double current, volt;
    public boolean burnt;

    public Part(PartType type, int x, int y) {
        this.type = type;
        this.x = x;
        this.y = y;
        if (type == PartType.JUMPER) {
            x2 = x + 60;
            y2 = y;
        }
        switch (type) {
            case RESISTOR:  value = 220; break;
            case CAPACITOR: value = 100e-6; break;
            case BATTERY:   value = 9; break;
            case AC_SOURCE: value = 5; value2 = 50; break;
            case CLOCK:     value = 1; break;          // 1 Hz
            default: break;
        }
    }

    public int pinCount() { return type.pins.length; }


    public int[] pin(int i) {
        if (type == PartType.JUMPER) return i == 0 ? new int[]{x, y} : new int[]{x2, y2};
        int[] p = rotate(type.pins[i][0], type.pins[i][1]);
        return new int[]{x + p[0], y + p[1]};
    }


    public int[] rotate(int lx, int ly) {
        int a = lx, b = ly;
        for (int k = 0; k < rot; k++) {
            int na = -b, nb = a;
            a = na;
            b = nb;
        }
        return new int[]{a, b};
    }


    public float[] toWorld(float lx, float ly) {
        float a = lx, b = ly;
        for (int k = 0; k < rot; k++) {
            float na = -b, nb = a;
            a = na;
            b = nb;
        }
        return new float[]{x + a, y + b};
    }


    public boolean contains(float wx, float wy) {
        if (type == PartType.JUMPER) return false;   // handled by the view
        float a = wx - x, b = wy - y;
        for (int k = 0; k < rot; k++) {              // inverse rotation
            float na = b, nb = -a;
            a = na;
            b = nb;
        }
        float m = 6;
        if (a >= type.bl - m && a <= type.br + m && b >= type.bt - m && b <= type.bb + m) return true;
        for (int[] p : type.pins) {
            if (Math.abs(a - p[0]) < 10 && Math.abs(b - p[1]) < 10) return true;
        }
        return false;
    }
}
