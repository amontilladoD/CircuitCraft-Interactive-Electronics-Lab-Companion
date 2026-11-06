package com.circuitcraft.app.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "Share this app": sends the installed APK file + the GitHub link + a short message.
 *  1. copy our own APK (ApplicationInfo.sourceDir) into the cache folder (background thread)
 *  2. give it a content:// Uri with FileProvider (other apps may not read our private files directly)
 *  3. open the system share sheet with ACTION_SEND (file + text)
 */
public class AppSharer {
    private static final String TAG = "AppSharer";
    public static final String REPO_URL =
            "https://github.com/amontilladoD/CircuitCraft-Interactive-Electronics-Lab-Companion";
    private static final String MESSAGE =
            "⚡ CircuitCraft - build & simulate electronic circuits on your phone!\n"
                    + "📲 Install the attached APK\n"
                    + "💻 " + REPO_URL;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static void share(Activity activity) {
        Toast.makeText(activity, "Preparing the app file…", Toast.LENGTH_SHORT).show();
        final Handler main = new Handler(Looper.getMainLooper());
        EXECUTOR.execute(() -> {
            Uri uri = null;
            try {
                uri = copyApk(activity);
            } catch (Exception e) {
                Log.w(TAG, "Could not prepare APK: " + e.getMessage());
            }
            final Uri apkUri = uri;
            main.post(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                Intent send = new Intent(Intent.ACTION_SEND);
                send.putExtra(Intent.EXTRA_SUBJECT, "CircuitCraft - Interactive Electronics Lab Companion");
                send.putExtra(Intent.EXTRA_TEXT, MESSAGE);
                if (apkUri != null) {
                    send.setType("application/vnd.android.package-archive");
                    send.putExtra(Intent.EXTRA_STREAM, apkUri);
                    send.setClipData(ClipData.newRawUri("CircuitCraft.apk", apkUri));
                    send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    send.setType("text/plain");
                    Toast.makeText(activity, "Could not attach the APK - sharing the GitHub link only",
                            Toast.LENGTH_LONG).show();
                }
                activity.startActivity(Intent.createChooser(send, "Share CircuitCraft"));
            });
        });
    }

    private static Uri copyApk(Context c) throws IOException {
        File src = new File(c.getApplicationInfo().sourceDir);          // the installed base.apk
        File dir = new File(c.getCacheDir(), "apk");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("cache folder");
        File dst = new File(dir, "CircuitCraft.apk");
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        return FileProvider.getUriForFile(c, c.getPackageName() + ".fileprovider", dst);
    }
}
