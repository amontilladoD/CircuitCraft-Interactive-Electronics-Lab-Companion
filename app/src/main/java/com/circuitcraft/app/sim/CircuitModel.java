package com.circuitcraft.app.sim;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public class CircuitModel {
    public static final int GRID = 20;
    public static final int BOARD_X = 40, BOARD_Y = 60;

    public final List<Part> parts = new ArrayList<>();


    public static CircuitModel withBreadboard() {
        CircuitModel m = new CircuitModel();
        m.add(PartType.BREADBOARD);
        return m;
    }


    public Part add(PartType t) {
        if (t == PartType.BREADBOARD) {
            for (Part p : parts) if (p.type == PartType.BREADBOARD) return null;
            Part bb = new Part(t, BOARD_X, BOARD_Y);
            parts.add(0, bb);
            return bb;
        }
        int[] spot = freeSpot();
        Part p = new Part(t, spot[0], spot[1]);
        parts.add(p);
        return p;
    }


    private int[] freeSpot() {
        for (int y = 500; y <= 1220; y += 80)
            for (int x = 40; x <= 740; x += 140)
                if (isFree(x, y)) return new int[]{x, y};
        for (int y = 80; y <= 380; y += 80)
            for (int x = 500; x <= 780; x += 140)
                if (isFree(x, y)) return new int[]{x, y};
        return new int[]{40, 500};
    }

    private boolean isFree(int cx, int cy) {
        for (Part q : parts) {
            if (q.type == PartType.BREADBOARD) continue;
            if (Math.abs(q.x - cx) < 130 && Math.abs(q.y - cy) < 70) return false;
            if (q.type == PartType.JUMPER && Math.abs(q.x2 - cx) < 130 && Math.abs(q.y2 - cy) < 70) return false;
        }
        return true;
    }

    public void remove(Part p) { parts.remove(p); }

    public boolean hasRealParts() {
        for (Part p : parts) if (p.type != PartType.BREADBOARD) return true;
        return false;
    }


    public String labelOf(Part p) {
        if (p.type == PartType.BREADBOARD) return "Breadboard";
        int n = 0;
        for (Part q : parts) {
            if (q.type == p.type) n++;
            if (q == p) break;
        }
        return p.type.label + " " + n;
    }

    public String summary() {
        Map<PartType, Integer> count = new EnumMap<>(PartType.class);
        for (Part p : parts) {
            if (p.type == PartType.BREADBOARD) continue;
            Integer c = count.get(p.type);
            count.put(p.type, c == null ? 1 : c + 1);
        }
        if (count.isEmpty()) return "Empty breadboard";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<PartType, Integer> e : count.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getValue()).append("× ").append(e.getKey().label);
        }
        return sb.toString();
    }
}
