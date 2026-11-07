package com.circuitcraft.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.circuitcraft.app.data.FavouriteRepository;
import com.circuitcraft.app.databinding.ActivityLabBinding;
import com.circuitcraft.app.logic.CircuitCalculator;
import com.circuitcraft.app.logic.Units;
import com.circuitcraft.app.model.Favourite;
import com.circuitcraft.app.receiver.SystemEventReceiver;
import com.circuitcraft.app.sim.CircuitEngine;
import com.circuitcraft.app.sim.CircuitIO;
import com.circuitcraft.app.sim.CircuitModel;
import com.circuitcraft.app.sim.Part;
import com.circuitcraft.app.sim.PartType;
import com.circuitcraft.app.sim.SimulatorView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Locale;

/**
 * Circuit Lab (the simulator screen).
 *  Palette  -> adds a part to the workbench
 *  Workbench (SimulatorView) -> drag parts / wires, tap to select
 *  Run      -> CircuitEngine simulates every 30 ms (Handler loop) and the meters update
 *  Save     -> stores the whole circuit as a favourite
 */
public class LabActivity extends BaseActivity implements SimulatorView.Listener {
    public static final String EXTRA_JSON = "circuit_json";

    private ActivityLabBinding b;
    private LabViewModel vm;
    private SystemEventReceiver systemReceiver;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running = false;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            b.simView.getEngine().tick();     // simulate ~30 ms of circuit time
            b.simView.invalidate();           // redraw LEDs, meters
            updateStatus();
            handler.postDelayed(this, 30);
        }
    };

    private interface NumberCallback { void onNumber(double v); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLabBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // The circuit lives in the ViewModel, so it survives screen rotation.
        vm = new ViewModelProvider(this).get(LabViewModel.class);
        if (vm.model == null) {
            String json = getIntent().getStringExtra(EXTRA_JSON);   // opened from Favourites?
            vm.model = json != null ? CircuitIO.fromJson(json) : CircuitModel.withBreadboard();
        }
        b.simView.setModel(vm.model);
        b.simView.setListener(this);
        if (vm.selected >= 0 && vm.selected < vm.model.parts.size()) {
            b.simView.select(vm.model.parts.get(vm.selected));
        }

        buildPalette();
        b.btnRun.setOnClickListener(v -> toggleRun());
        b.btnView.setOnClickListener(v -> b.simView.toggleOverview());
        b.btnRotate.setOnClickListener(v -> rotateSelected());
        b.btnValue.setOnClickListener(v -> editValue());
        b.btnDelete.setOnClickListener(v -> deleteSelected());
        b.btnCheck.setOnClickListener(v -> showCheck());
        b.btnSave.setOnClickListener(v -> askNameAndSave());
        updateStatus();
    }

    // pause the simulation when the screen is hidden / rotating, continue afterwards
    @Override
    protected void onPause() {
        super.onPause();
        if (running) {
            vm.resumeRun = true;
            stopRun();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vm.resumeRun) {
            vm.resumeRun = false;
            startRun();
        }
    }

    // BroadcastReceiver (system events): low battery -> stop the simulation to save power
    @Override
    protected void onStart() {
        super.onStart();
        systemReceiver = new SystemEventReceiver(new SystemEventReceiver.Simple() {
            @Override
            public void onBatteryLow() {
                vm.resumeRun = false;
                if (running) {
                    stopRun();
                    toast("🔋 Battery low - simulation stopped to save power");
                }
            }
        });
        registerReceiver(systemReceiver, SystemEventReceiver.filter());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (systemReceiver != null) {
            unregisterReceiver(systemReceiver);
            systemReceiver = null;
        }
    }

    // ------------------------------------------------------------ palette
    private void buildPalette() {
        for (PartType t : PartType.values()) {
            MaterialButton btn = new MaterialButton(this, null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            btn.setText(t.emoji + " " + t.label);
            btn.setAllCaps(false);
            btn.setTextSize(12);
            boolean vertical = b.paletteRow.getOrientation() == LinearLayout.VERTICAL;
            btn.setLayoutParams(new LinearLayout.LayoutParams(
                    vertical ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            btn.setOnClickListener(v -> addPart(t));
            b.paletteRow.addView(btn);
        }
    }

    private void addPart(PartType t) {
        Part p = b.simView.getModel().add(t);
        if (p == null) {
            Toast.makeText(this, "The breadboard is already on the workbench", Toast.LENGTH_SHORT).show();
            return;
        }
        b.simView.getEngine().markDirty();
        b.simView.select(p);
        Toast.makeText(this, t == PartType.JUMPER
                ? "Drag each end of the wire onto a hole or pin"
                : "Drag the part onto the breadboard", Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------ toolbar actions
    private void toggleRun() {
        vm.resumeRun = false;
        if (running) stopRun();
        else startRun();
    }

    private void startRun() {
        b.simView.getEngine().start();
        running = true;
        b.btnRun.setText("■ Stop");
        handler.post(ticker);
        updateStatus();
    }

    private void stopRun() {
        running = false;
        handler.removeCallbacks(ticker);
        b.simView.getEngine().stop();
        b.btnRun.setText("▶ Run");
        b.simView.invalidate();
        updateStatus();
    }

    private void rotateSelected() {
        Part p = b.simView.getSelected();
        if (p == null) { toast("Tap a part first"); return; }
        if (p.type == PartType.JUMPER || p.type == PartType.BREADBOARD) { toast("This part cannot be rotated"); return; }
        p.rot = (p.rot + 1) % 4;
        b.simView.getEngine().markDirty();
        b.simView.invalidate();
    }

    private void deleteSelected() {
        Part p = b.simView.getSelected();
        if (p == null) { toast("Tap a part first"); return; }
        b.simView.getModel().remove(p);
        b.simView.getEngine().markDirty();
        b.simView.select(null);
    }

    private void editValue() {
        Part p = b.simView.getSelected();
        if (p == null) { toast("Tap a part first"); return; }
        switch (p.type) {
            case RESISTOR:
                askNumber("Resistance (Ω)", "e.g. 220, 4.7k, 1M", CircuitCalculator.format(p.value),
                        v -> { p.value = Math.max(v, 0.001); changed(); });
                break;
            case CAPACITOR:
                askNumber("Capacitance (µF)", "e.g. 100", CircuitCalculator.format(p.value * 1e6),
                        v -> { p.value = Math.max(v * 1e-6, 1e-12); changed(); });
                break;
            case BATTERY:
                askNumber("Battery voltage (V)", "e.g. 9", CircuitCalculator.format(p.value),
                        v -> { p.value = Math.max(v, 0); changed(); });
                break;
            case AC_SOURCE:
                askAc(p);
                break;
            case CLOCK:
                askNumber("Clock frequency (Hz)", "e.g. 1, 2, 50 (max 1000)", CircuitCalculator.format(p.value),
                        v -> { p.value = Math.max(0.05, Math.min(v, 1000)); changed(); });
                break;
            case MULTIMETER:
                p.mode = p.mode == 1 ? 0 : 1;
                toast(p.mode == 1 ? "Multimeter: AMMETER mode (put it in series)"
                        : "Multimeter: VOLTMETER mode (put it in parallel)");
                changed();
                break;
            case SWITCH:
                p.mode = p.mode == 1 ? 0 : 1;
                changed();
                break;
            default:
                toast("This part has no value to edit");
        }
    }

    private void changed() {
        b.simView.getEngine().markDirty();
        b.simView.invalidate();
        updateStatus();
    }

    private void askNumber(String title, String hint, String initial, NumberCallback cb) {
        EditText box = new EditText(this);
        box.setHint(hint);
        box.setText(initial);
        box.setInputType(InputType.TYPE_CLASS_TEXT);
        box.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(box)
                .setPositiveButton("OK", (d, w) -> {
                    try {
                        cb.onNumber(Units.parse(box.getText().toString()));
                    } catch (NumberFormatException e) {
                        toast("Not a valid number");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askAc(Part p) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(48, 16, 48, 0);
        EditText peak = new EditText(this);
        peak.setHint("Peak voltage (V)");
        peak.setText(CircuitCalculator.format(p.value));
        EditText freq = new EditText(this);
        freq.setHint("Frequency (Hz)");
        freq.setText(CircuitCalculator.format(p.value2));
        col.addView(peak);
        col.addView(freq);
        new AlertDialog.Builder(this)
                .setTitle("AC source")
                .setView(col)
                .setPositiveButton("OK", (d, w) -> {
                    try {
                        p.value = Math.max(Units.parse(peak.getText().toString()), 0);
                        p.value2 = Math.max(Units.parse(freq.getText().toString()), 0.1);
                        changed();
                    } catch (NumberFormatException e) {
                        toast("Not a valid number");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------------------------------------------------ check wiring
    private void showCheck() {
        List<String> warnings = b.simView.getEngine().diagnose();
        String msg;
        if (warnings.isEmpty()) {
            msg = b.simView.getModel().hasRealParts()
                    ? "✔ No wiring problems found."
                    : "The breadboard is empty. Add a battery, a resistor and an LED to start.";
            if (!running && b.simView.getModel().hasRealParts())
                msg += "\n\nTip: tap Run to check the circuit while it is simulating (burnt LEDs, short circuits, reversed LEDs).";
        } else {
            StringBuilder sb = new StringBuilder();
            for (String w : warnings) sb.append("✘ ").append(w).append("\n\n");
            msg = sb.toString().trim();
        }
        new AlertDialog.Builder(this)
                .setTitle("Wiring check")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    // ------------------------------------------------------------ favourites
    private void askNameAndSave() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        CircuitModel model = b.simView.getModel();
        if (user == null) return;
        if (!model.hasRealParts()) { toast("Add some parts before saving"); return; }

        EditText nameBox = new EditText(this);
        nameBox.setHint("e.g. LED with resistor");
        nameBox.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
                .setTitle("Save to favourite circuits")
                .setView(nameBox)
                .setPositiveButton("Save", (d, w) -> {
                    Favourite f = new Favourite();
                    f.owner = user.getUid();
                    f.name = nameBox.getText().toString().trim().isEmpty()
                            ? "My circuit" : nameBox.getText().toString().trim();
                    f.data = CircuitIO.toJson(model);
                    f.summary = model.summary();
                    long id = FavouriteRepository.save(this, f);
                    toast(id > 0 ? "Saved to favourites ⭐" : "Could not save");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ------------------------------------------------------------ status line
    @Override
    public void onSelectionChanged(Part p) {
        if (vm != null) vm.selected = p == null ? -1 : vm.model.parts.indexOf(p);
        updateStatus();
    }

    private void updateStatus() {
        CircuitEngine engine = b.simView.getEngine();
        Part sel = b.simView.getSelected();
        StringBuilder sb = new StringBuilder();
        if (running) {
            sb.append(String.format(Locale.US, "▶ Running   t = %.2f s", engine.getTime()));
            if (!engine.hasGround) sb.append("\n⚠ Add a DC Battery or AC Source");
            else if (engine.warningCount() > 0) sb.append("   ⚠ ").append(engine.warningCount()).append(" warning(s) - tap Check");
        } else {
            sb.append("Drag parts onto the breadboard, join them with wires, then tap Run.\n"
                    + "Drag empty space to move around - pinch to zoom - 🔍 View shows the whole bench.");
        }
        if (sel != null) {
            sb.append("\nSelected: ").append(b.simView.getModel().labelOf(sel));
            if (sel.type == PartType.SWITCH) sb.append(" (tap it to switch ON/OFF)");
        }
        b.tvStatus.setText(sb.toString());
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
