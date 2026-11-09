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
            "https://raw.githubusercontent.com/amontilladoD/resource/main/electronics_quiz_questions.json";

    public interface Callback {

        void onLoaded(List<QuizQuestion> questions, boolean fromNetwork, String error);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static void load(Context context, Callback callback) {
        final Context app = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());

        EXECUTOR.execute(() -> {                       // runs on a background thread
            List<QuizQuestion> list = null;
            boolean network = false;
            String error = null;
            try {
                list = parse(download(QUIZ_URL));
                network = !list.isEmpty();
                if (!network) error = "online file has no valid questions";
            } catch (Exception e) {
                error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                Log.w(TAG, "Network load failed, using offline questions: " + error);
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
            final String err = error;
            main.post(() -> callback.onLoaded(result, fromNet, err));   // back on the UI thread
        });
    }

    private static String download(String address) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cache-Control", "no-cache");
        try {
            int code = conn.getResponseCode();
            if (code != 200) throw new Exception("HTTP " + code);
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

    /**
     * JSON format: {"questions":[{"q":"..","options":[..],"answer":0,"explain":".."}]}
     * (a plain top-level array, and the keys question / choices / correct / explanation also work).
     * Questions with fewer than 2 options or a bad answer index are skipped; at most 4 options are used.
     */
    static List<QuizQuestion> parse(String json) throws Exception {
        String t = json.trim();
        if (t.startsWith("\uFEFF")) t = t.substring(1);
        JSONArray arr = t.startsWith("[") ? new JSONArray(t) : new JSONObject(t).getJSONArray("questions");

        List<QuizQuestion> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String q = o.optString("q", o.optString("question", ""));
            JSONArray opts = o.optJSONArray("options");
            if (opts == null) opts = o.optJSONArray("choices");
            if (q.isEmpty() || opts == null || opts.length() < 2) continue;

            int n = Math.min(opts.length(), 4);
            String[] options = new String[n];
            for (int k = 0; k < n; k++) options[k] = opts.getString(k);

            int answer = o.optInt("answer", o.optInt("correct", -1));
            if (answer < 0 || answer >= n) continue;
            list.add(new QuizQuestion(q, options, answer, o.optString("explain", o.optString("explanation", ""))));
        }
        return list;
    }
}
