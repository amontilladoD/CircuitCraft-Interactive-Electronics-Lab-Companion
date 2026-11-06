package com.circuitcraft.app.ui;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.circuitcraft.app.data.FavouriteRepository;
import com.circuitcraft.app.databinding.ActivityFavouritesBinding;
import com.circuitcraft.app.model.Favourite;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lists saved circuits (read through OUR ContentProvider). Tap = open in Lab, Delete = remove.
 * Share = reads the phone's CONTACTS through the system ContactsContract content provider
 * (runtime permission READ_CONTACTS), lets you pick a contact, then opens an email app (implicit intent).
 */
public class FavouritesActivity extends BaseActivity {
    private ActivityFavouritesBinding b;
    private Favourite pendingShare;
    private ActivityResultLauncher<String> contactsPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityFavouritesBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.rvFavourites.setLayoutManager(new LinearLayoutManager(this));

        contactsPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (pendingShare == null) return;
            if (granted) {
                showContactPicker(pendingShare);
            } else {
                Toast.makeText(this, "Contacts permission denied - using the normal share menu", Toast.LENGTH_LONG).show();
                shareWithChooser(pendingShare);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadList();      // refresh every time the screen comes back
    }

    private void loadList() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        List<Favourite> list = FavouriteRepository.loadAll(this, user.getUid());
        b.tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        b.rvFavourites.setAdapter(new FavouriteAdapter(list, new FavouriteAdapter.Listener() {
            @Override
            public void onOpen(Favourite f) {
                Intent i = new Intent(FavouritesActivity.this, LabActivity.class);
                i.putExtra(LabActivity.EXTRA_JSON, f.data);   // the whole circuit travels in the Intent
                startActivity(i);
            }

            @Override
            public void onShare(Favourite f) {
                pendingShare = f;
                if (ContextCompat.checkSelfPermission(FavouritesActivity.this,
                        Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    showContactPicker(f);
                } else {
                    contactsPermission.launch(Manifest.permission.READ_CONTACTS);
                }
            }

            @Override
            public void onDelete(Favourite f) {
                new AlertDialog.Builder(FavouritesActivity.this)
                        .setTitle("Delete \"" + f.name + "\"?")
                        .setPositiveButton("Delete", (d, w) -> {
                            FavouriteRepository.delete(FavouritesActivity.this, f);
                            loadList();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }));
    }

    /** Query the system Contacts provider for contacts that have an email address. */
    private void showContactPicker(Favourite f) {
        List<String> labels = new ArrayList<>();
        List<String> emails = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Cursor c = getContentResolver().query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Email.ADDRESS},
                null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
        if (c != null) {
            try {
                int nameCol = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                int mailCol = c.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
                while (c.moveToNext() && labels.size() < 300) {
                    String mail = mailCol >= 0 ? c.getString(mailCol) : null;
                    if (mail == null || !seen.add(mail)) continue;
                    String name = nameCol >= 0 ? c.getString(nameCol) : null;
                    labels.add((name == null ? mail : name + "  (" + mail + ")"));
                    emails.add(mail);
                }
            } finally {
                c.close();
            }
        }

        if (labels.isEmpty()) {
            Toast.makeText(this, "No contacts with an email address - using the normal share menu", Toast.LENGTH_LONG).show();
            shareWithChooser(f);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Share \"" + f.name + "\" with…")
                .setItems(labels.toArray(new String[0]), (d, which) -> emailCircuit(emails.get(which), f))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String messageFor(Favourite f) {
        return "I built a circuit in CircuitCraft!\n\nName: " + f.name + "\nParts: " + f.summary
                + "\n\nOpen the CircuitCraft app, build the same parts in the Circuit Lab and try it.";
    }

    private void emailCircuit(String address, Favourite f) {
        Intent mail = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));   // implicit intent: any email app
        mail.putExtra(Intent.EXTRA_EMAIL, new String[]{address});
        mail.putExtra(Intent.EXTRA_SUBJECT, "CircuitCraft circuit: " + f.name);
        mail.putExtra(Intent.EXTRA_TEXT, messageFor(f));
        try {
            startActivity(mail);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No email app installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareWithChooser(Favourite f) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, messageFor(f));
        startActivity(Intent.createChooser(send, "Share circuit"));
    }
}
