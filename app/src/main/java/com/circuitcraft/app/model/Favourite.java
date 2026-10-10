package com.circuitcraft.app.model;

import java.util.HashMap;
import java.util.Map;

public class Favourite {
    public long id;
    public String owner;      
    public String name;
    public String data;       
    public String summary;    

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("summary", summary);
        m.put("circuit", data);
        m.put("createdAt", System.currentTimeMillis());
        return m;
    }
}
