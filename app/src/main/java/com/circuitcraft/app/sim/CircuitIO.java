package com.circuitcraft.app.sim;

import org.json.JSONArray;
import org.json.JSONObject;

public class CircuitIO {

    public static String toJson(CircuitModel m) {
        try {
            JSONArray arr = new JSONArray();
            for (Part p : m.parts) {
                JSONObject o = new JSONObject();
                o.put("t", p.type.name());
                o.put("x", p.x);
                o.put("y", p.y);
                o.put("x2", p.x2);
                o.put("y2", p.y2);
                o.put("r", p.rot);
                o.put("v", p.value);
                o.put("v2", p.value2);
                o.put("m", p.mode);
                arr.put(o);
            }
            return new JSONObject().put("parts", arr).toString();
        } catch (Exception e) {
            return "{\"parts\":[]}";
        }
    }

    public static CircuitModel fromJson(String json) {
        CircuitModel m = new CircuitModel();
        try {
            JSONArray arr = new JSONObject(json).getJSONArray("parts");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Part p = new Part(PartType.valueOf(o.getString("t")), o.getInt("x"), o.getInt("y"));
                p.x2 = o.optInt("x2", p.x2);
                p.y2 = o.optInt("y2", p.y2);
                p.rot = o.optInt("r", 0);
                p.value = o.optDouble("v", p.value);
                p.value2 = o.optDouble("v2", p.value2);
                p.mode = o.optInt("m", 0);
                m.parts.add(p);
            }
            return m;
        } catch (Exception e) {
            return CircuitModel.withBreadboard();
        }
    }
}
