package com.circuitcraft.app.sim;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CircuitModel {
    public static final int GRID = 20;
    public static final int BOARD_X = 40, BOARD_Y = 60;

    public final List<Part> parts = new ArrayList<>();
    private int slot = 0;

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
        int sx = 40 + (slot % 3) * 140 + (slot / 6) * 20;
        int sy = 500 + ((slot / 3) % 2) * 80;
        slot++;
        Part p = new Part(t, sx, sy);
        parts.add(p);
        return p;
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
