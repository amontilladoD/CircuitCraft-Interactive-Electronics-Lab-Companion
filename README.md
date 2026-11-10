<div align="center">

# ⚡ CircuitCraft
### *Interactive Electronics Lab Companion for Android*

[![Android](https://img.shields.io/badge/Platform-Android_7.0%2B_(API_24%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Language-Java_8-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com)
[![Firebase](https://img.shields.io/badge/Firebase-Auth_%7C_Firestore_%7C_RTDB-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

<br/>

**CircuitCraft** is an interactive virtual electronics lab and learning companion built for Android. Whether you want to prototype breadboard circuits safely, double-check Ohm's Law calculations, test yourself with timed electronics quizzes, or race classmates on a live leaderboard, it bundles a portable electronics bench right onto your phone—powered by a custom **Modified Nodal Analysis (MNA)** simulation engine.

[Install APK](#-install-release-apk) • [Features](#-key-features) • [Simulation Engine](#-how-the-simulator-works) • [Architecture](#-android-architecture) • [Project Structure](#-project-structure) • [Getting Started](#-getting-started)

</div>

---

## 📲 Install Release APK

Don't want to open Android Studio first? A prebuilt release APK is included directly in the repository:

- Download and install **[`app/release/app-release.apk`](app/release/app-release.apk)** on any Android 7.0+ (API 24+) device.
- Once installed, you can also share the app directly with friends from the dashboard via **Share this app** (`AppSharer` + `FileProvider`).

---

## 🌟 Overview

Physical components are easy to burn when you are experimenting—especially LEDs without current-limiting resistors, reversed diodes, or accidental ammeter shorts. **CircuitCraft** lets students, hobbyists, and makers wire up analog and digital circuits on a virtual solderless breadboard, inspect live meter readings, catch wiring mistakes before anything "smokes," and reinforce core theory through an offline-ready component reference and timed quiz mode.

---

## ✨ Key Features

<table>
<tr>
<td width="50%" valign="top">

### 🔬 Interactive Breadboard Workbench
- **Expanded 900 × 1300 Canvas:** Pan freely, pinch-to-zoom, and toggle between a close-up breadboard view and full-workbench overview (`SimulatorView`).
- **20-Column Solderless Breadboard:** Top/bottom power rails and split `a–e` / `f–j` tie-point columns with grid snapping and non-overlapping part placement.
- **Rich Component Palette:**
  - *Sources & Control:* DC Battery, AC Source, Square-Wave Clock (`0–5 V`, 50% duty, adjustable frequency), Switch
  - *Passives & Semiconductors:* Resistor, Capacitor, Diode, LED, **NPN** & **PNP** Transistors
  - *Meters:* Voltmeter (`10 MΩ`), Ammeter, and dual-mode **Multimeter** (`V` / `A`)
  - *Digital Logic:* AND, OR, NAND, NOR, XOR, XNOR, NOT gates, plus rising-edge **D Flip-Flop IC** (`D`, `CK`, `Q`, `Q̅`)

</td>
<td width="50%" valign="top">

### ⚠️ Real-Time Safety & Diagnostics
- **LED Burnout Protection:** Glows proportionally to current and triggers a visual burnout alert above `40 mA`.
- **Short-Circuit Detection:** Flags excessive source draw above `2.0 A`.
- **Polarity Alerts:** Warns when an LED is reverse-biased (`Vd < -1.0 V`).
- **Meter Safeguards:** Detects parallel ammeter connections carrying over `1.0 A`.
- **Netlist Inspection:** Automatically reports missing power sources and floating/unconnected required pins (`Check` button).

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🧮 Ohm's Law & Power Calculator
- Leave any **one** of Voltage ($V$), Current ($I$), or Resistance ($R$) empty and solve immediately via $V = I \cdot R$.
- Automatically computes electrical power ($P = V \cdot I$) with clean formatting (`CircuitCalculator`).
- Engineering prefix utility (`Units`) supporting `p`, `n`, `µ` (`u`), `m`, `k`, and `M`.

</td>
<td width="50%" valign="top">

### 🧠 Timed Quizzes & Live Leaderboard
- **30-Question Bank:** Downloads fresh electronics questions over HTTP JSON with automatic offline fallback to [`assets/questions.json`](app/src/main/assets/questions.json).
- **Foreground Stopwatch Service:** `QuizTimerService` keeps elapsed time ticking with a live tray notification (`Elapsed time MM:SS`).
- **Live Top-20 Leaderboard:** Syncs each user's best score (tie-broken by fastest completion time) to **Firebase Realtime Database** using a live `ValueEventListener`, while storing full attempt history in **Cloud Firestore**.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📚 Components Reference Library
- Built-in guide covering **23** circuit parts, logic ICs, meters, and prototyping tools (`ComponentRepository`).
- Includes pin conventions, practical behavior notes, and common beginner mistakes to avoid.

</td>
<td width="50%" valign="top">

### 💾 Saved Circuits, Dark Mode & Sharing
- **Local + Cloud Persistence:** Save/reopen JSON circuit builds (`CircuitIO`) through a custom SQLite `ContentProvider` (`FavouriteProvider`) mirrored to Cloud Firestore.
- **Contact & App Sharing:** Pick an email contact via `ContactsContract` (`READ_CONTACTS`) to email saved circuits, or share the APK directly from the dashboard.
- **Adaptive Dark Mode & System Alerts:** Sun/Moon theme toggle across screens (`BaseActivity` + `ThemeHelper`), rotation-safe `ViewModel` state, landscape layouts for Login/Lab, and live low-battery/airplane/network banners (`SystemEventReceiver`, `NetworkReceiver`).

</td>
</tr>
</table>

---

## ⚙️ How the Simulator Works

`CircuitEngine` runs independently of Android views and solves the active circuit using **Modified Nodal Analysis (MNA)**:

```text
[SimulatorView]  ──(Touch / Zoom / Render)──>  [CircuitModel]
       ▲                                              │
       └──────────(Meter Readings & Alerts)────  [CircuitEngine]
```

1. **Topology Discovery (Union-Find):**
   Breadboard power rails, 5-hole vertical strips (`a–e` and `f–j`), and jumper wires are merged into electrical nets with a disjoint-set union-find structure. Ground (`node 0`) is referenced to the negative pin of the first DC Battery, AC Source, or standalone Clock.
2. **Matrix Formulation ($A \cdot x = b$):**
   - **Linear Devices:** Resistors, sources (`0.1 Ω` internal resistance), switches, and voltmeters (`10 MΩ`) are stamped directly into the conductance matrix; ammeters add branch-current unknowns.
   - **Capacitors:** Integrated per step ($\Delta t = 10^{-4}\text{ s}$) with a Backward-Euler companion model ($G_{eq} = C / \Delta t$).
   - **Nonlinear Semiconductors (Diodes, LEDs, NPN & PNP BJTs):** Solved iteratively via **Newton-Raphson** (up to 80 iterations) using Ebers-Moll equations with SPICE-style junction limiting (`pnjlim`) for stable convergence.
   - **Digital Logic & Clock:** Logic gates, square-wave Clock, and rising-edge D Flip-Flops evaluate previous node voltages against a `2.5 V` threshold and drive `5 V` / `0 V` output stages.
3. **Linear Solve & Averaging Window:**
   Each step is solved via Gaussian elimination with partial pivoting (`300` steps per `30 ms` UI tick). Voltmeters, ammeters, and multimeters accumulate readings across `1000`-step windows (skipping the initial startup transient) and automatically distinguish DC mean values from true AC RMS values.

---

## 🏛️ Android Architecture

CircuitCraft keeps UI screens, state holders, persistence, background execution, and circuit math cleanly separated across 10 activities:

```text
com.circuitcraft.app
├── CircuitCraftApp.java       # Application startup & saved night-mode initialization
├── MainActivity.java          # Dashboard, explicit/implicit intents, network & system alerts
├── data/                      # DbHelper (SQLite), FavouriteProvider (ContentProvider), Repositories & RealtimeDb
├── logic/                     # CircuitCalculator (Ohm's Law & Power) and Units (SI formatting/parsing)
├── model/                     # ComponentItem, Favourite, QuizQuestion, LeaderboardEntry
├── receiver/                  # NetworkReceiver & SystemEventReceiver (low battery, airplane mode)
├── service/                   # QuizTimerService (foreground service + live notification ticks)
├── sim/                       # PartType, Part, CircuitModel, CircuitEngine, CircuitIO, SimulatorView
└── ui/                        # BaseActivity, ThemeHelper, AppSharer, ViewModels, Adapters & Activities
```

### Android Components Highlighted
- **View Binding & Base Theme Toolbar:** All screens use null-safe View Binding (`viewBinding true`), with `BaseActivity` providing the persistent light/dark mode menu action.
- **Rotation-Safe State:** `LabViewModel` keeps the circuit model and running state across configuration changes; `QuizViewModel` preserves loaded questions, progress, and score; dedicated `layout-land/` layouts adapt Login and Lab screens to landscape mode.
- **Custom + System Content Providers:** `FavouriteProvider` manages local SQLite circuit storage, while `FavouritesActivity` queries `ContactsContract` (with runtime `READ_CONTACTS` permission) for contact sharing.
- **Foreground Service + Broadcasts:** `QuizTimerService` broadcasts `ACTION_TICK` updates every second and updates an ongoing notification (`POST_NOTIFICATIONS` on Android 13+).

---

## 📂 Project Structure

```text
CircuitCraft/
├── app/
│   ├── release/
│   │   ├── app-release.apk                # Ready-to-install release APK
│   │   └── output-metadata.json
│   ├── src/main/
│   │   ├── AndroidManifest.xml            # 10 activities, service, providers & permissions
│   │   ├── assets/questions.json          # Offline fallback bank (30 quiz questions)
│   │   ├── java/com/circuitcraft/app/     # Core app, data, logic, model, receiver, service, sim, ui
│   │   └── res/                           # drawables, drawable-nodpi, layout, layout-land, menu, mipmap, values, values-night, xml
│   └── build.gradle                       # App module build config (compileSdk 33, minSdk 24)
├── gradle/
│   ├── libs.versions.toml                 # Gradle version catalog (AGP, Material, Firebase BOM 32.0.0)
│   └── wrapper/gradle-wrapper.properties  # Gradle 8.0 wrapper settings
├── build.gradle                           # Root plugin configuration
├── gradle.properties                      # JVM & AndroidX flags
├── settings.gradle                        # Project & module configuration
├── .gitignore                             # Excludes google-services.json, local.properties, keystores & build outputs
├── LICENSE                                # MIT License
└── README.md
```

---

## 🚀 Getting Started

### Option 1: Quick Install (No Build Required)
Download **[`app/release/app-release.apk`](app/release/app-release.apk)** onto your Android phone (Android 7.0+) and install it.

### Option 2: Build from Source

#### Prerequisites
- **Android Studio** (Hedgehog / Iguana or newer)
- **JDK 11 or JDK 17** (project targets Java 8 bytecode compatibility)
- **Android SDK:** Compile SDK `33`, Min SDK `24` (Gradle `8.0`)

#### Setup Steps
1. **Clone the repository:**
   ```bash
   git clone https://github.com/amontilladoD/CircuitCraft-Interactive-Electronics-Lab-Companion.git
   ```
2. **Add your Firebase configuration:**
   > `app/google-services.json` and `local.properties` are intentionally ignored in `.gitignore` so API keys and local machine paths stay private.
   - Create a Firebase project and register an Android app with package name `com.circuitcraft.app`.
   - Download `google-services.json` and place it inside `app/`.
3. **Enable Firebase services in the Firebase Console:**
   - **Authentication:** Enable **Email/Password** sign-in.
   - **Cloud Firestore:** Create a database (used for user profiles `users/{uid}`, synced `favourites`, and quiz `scores`).
   - **Realtime Database:** Create a database for the live leaderboard and publish rules with an index on `score`:
     ```json
     {
       "rules": {
         ".read": "auth != null",
         ".write": "auth != null",
         "leaderboard": {
           ".indexOn": ["score"]
         }
       }
     }
     ```
   - Re-download `google-services.json` into `app/` after creating Realtime Database so `firebase_url` is included (or set `RealtimeDb.DB_URL` if needed).
4. **Open & Run:**
   - Open the project root in **Android Studio**, let Gradle sync finish, and click **Run ▶** on an emulator or physical device.

---

## 🛡️ License

Licensed under the **MIT License** — see [LICENSE](LICENSE) for details.

---

<div align="center">

Built with ⚡ and curiosity for students, makers, and electronics enthusiasts.

</div>
