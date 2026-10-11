package com.circuitcraft.app.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.circuitcraft.app.model.QuizQuestion;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuizRepository {
    private static final String TAG = "QuizRepository";

    public static final String QUIZ_URL =
            "https://raw.githubusercontent.com/amontilladoD/CircuitCraft-Interactive-Electronics-Lab-Companion/main/app/src/main/assets/questions.json";

    public interface Callback {
        void onLoaded(List<QuizQuestion> questions, boolean fromNetwork);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static void load(Context context, Callback callback) {
        final Context app = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());

        EXECUTOR.execute(() -> {                       
            List<QuizQuestion> list = null;
            boolean network = false;
            try {
                list = parse(download(QUIZ_URL));
                network = !list.isEmpty();
            } catch (Exception e) {
                Log.w(TAG, "Network load failed, using offline questions: " + e.getMessage());
            }
            if (list == null || list.isEmpty()) {
                try {
                    list = parse(readAsset(app, "questions.json"));
                } catch (Exception e) {
                    Log.e(TAG, "Asset load failed", e);
                    list = new ArrayList<>();
                }
            }
            final List<QuizQuestion> result = list;
            final boolean fromNet = network;
            main.post(() -> callback.onLoaded(result, fromNet));   
        });
    }

    private static String download(String address) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        conn.setRequestMethod("GET");
        try {
            if (conn.getResponseCode() != 200) throw new Exception("HTTP " + conn.getResponseCode());
            return readAll(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    private static String readAsset(Context ctx, String name) throws Exception {
        return readAll(ctx.getAssets().open(name));
    }

    private static String readAll(InputStream in) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line).append('\n');
        br.close();
        return sb.toString();
    }

    static List<QuizQuestion> parse(String json) throws Exception {
        List<QuizQuestion> list = new ArrayList<>();
        JSONArray arr = new JSONObject(json).getJSONArray("questions");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            JSONArray opts = o.getJSONArray("options");
            String[] options = new String[opts.length()];
            for (int k = 0; k < opts.length(); k++) options[k] = opts.getString(k);
            list.add(new QuizQuestion(o.getString("q"), options, o.getInt("answer"), o.optString("explain", "")));
        }
        return list;
    }
}
