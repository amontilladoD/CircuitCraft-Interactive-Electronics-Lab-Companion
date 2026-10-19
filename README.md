<div align="center">

# ⚡ CircuitCraft
### *Interactive Electronics Lab Companion*

**CircuitCraft** is an interactive virtual electronics laboratory built for Android. It bridges the gap between circuit theory and real-world breadboarding by coupling an intuitive drag-and-drop workspace with a high-performance **Modified Nodal Analysis (MNA)** SPICE-style simulation engine.

</div>

## 🌟 Overview

CircuitCraft empowers students, hobbyists, and engineers to build, simulate, and debug electrical circuits without burning physical components. Featuring realistic breadboard layout connections, real-time diagnostic checks, engineering calculators, and timed quizzes, it is a complete portable lab companion.

---

## ✨ Key Features

<table>
<tr>
<td width="50%" valign="top">

### 🔬 Interactive Virtual Breadboard
- Realistic 20-column breadboard with top/bottom power rails and partitioned tie-point rows ($a-e$, $f-j$).
- Drag-and-drop jumper wires with terminal snapping.
- Complete component palette: DC & AC sources, resistors, capacitors, switches, diodes, LEDs, NPN transistors, logic gates (AND, OR, NAND, NOR, XOR, XNOR, NOT), and D flip-flops.
- Multi-touch canvas with smooth pinch-to-zoom, panning, and component rotation.

</td>
<td width="50%" valign="top">

### ⚠️ Real-Time Safety & Diagnostics
- **Overcurrent Protection:** Visual LED burnout warnings when forward current exceeds $40\text{ mA}$.
- **Short Circuit Detection:** Immediate alerts when source current exceeds $2.0\text{ A}$.
- **Polarity Inspection:** Alerts for backward-biased diodes and LEDs.
- **Instrument Safeguards:** Warns against placing ammeters in parallel across voltage sources.
- **Floating Node Alerts:** Automatic detection of unconnected pins.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🧮 Precision Electrical Calculator
- Instant calculation for Ohm’s Law ($V = I \cdot R$) and Joule’s Law ($P = V \cdot I$).
- Series and parallel equivalent resistance solver.
- Smart engineering notation engine supporting standard metric prefixes ($\text{p}$, $\text{n}$, $\mu$, $\text{m}$, $\text{k}$, $\text{M}$, $\text{G}$).

</td>
<td width="50%" valign="top">

### 🧠 Timed Diagnostic Quizzes
- Comprehensive quiz modules on electronic principles and circuit analysis.
- Powered by a dedicated background Android Service (`QuizTimerService`).
- Dynamic question syncing via GitHub JSON with offline local asset fallbacks.
- Explanations and performance summaries after every session.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📚 Components Encyclopedia
- Illustrated reference guide for common electronics components.
- Detailed schematics, pinout diagrams, operating specifications, and typical application notes.

</td>
<td width="50%" valign="top">

### ☁️ Cloud Sync & Local Persistence
- Secure user authentication via **Firebase Auth**.
- **Cloud Firestore** synchronization for saved circuits and quiz progress.
- Resilient offline caching using an optimized **SQLite** database exposed via a custom **Android ContentProvider**.

</td>
</tr>
</table>

---

## ⚙️ How the Simulator Works

The simulation engine (`CircuitEngine`) runs independently of Android UI dependencies, performing numeric circuit solving using **Modified Nodal Analysis (MNA)**:

```text
[SimulatorView]  ──(Canvas / Touch Gestures)──>  [CircuitModel]
       │                                                │
       └──────────────> [CircuitEngine] <───────────────┘
```

1. **Topology Discovery (Union-Find):**
   Breadboard tie-points, power rails, and jumper wires are connected into electrical nets using an efficient disjoint-set (union-find) data structure.
2. **Matrix Formulation ($A \cdot x = b$):**
   - **Linear Elements:** Resistors, ideal voltage sources, and switches are stamped directly into the conductance matrix.
   - **Dynamic Elements (Capacitors):** Modeled using Backward-Euler companion conductance models ($G_{eq} = \frac{C}{\Delta t}$).
   - **Nonlinear Elements (Diodes, LEDs, BJTs):** Solved iteratively using the **Newton-Raphson method** with Ebers-Moll exponential equations and P-N junction limiting (`pnjlim`) to guarantee numeric convergence.
3. **Meter Processing:**
   Voltmeters and ammeters sample node voltages and branch currents over time, automatically distinguishing between DC average values and true RMS (Root Mean Square) for AC signals.

---

## 🏛️ Android Architecture & Best Practices

CircuitCraft demonstrates a comprehensive, decoupled Android system architecture:

```
com.circuitcraft.app
├── 📁 data        # SQLite database (DbHelper), ContentProvider, & Repositories
├── 📁 logic       # Engineering calculations, Ohm's Law, & Unit conversions
├── 📁 model       # Data models (ComponentItem, Favourite, QuizQuestion)
├── 📁 receiver    # Connectivity broadcast receiver (NetworkReceiver)
├── 📁 service     # Background countdown timer (QuizTimerService)
├── 📁 sim         # MNA Engine, CircuitModel, Serialization (CircuitIO), & SimulatorView
└── 📁 ui          # Modern Activities (Lab, Calculator, Quiz, Favourites, Auth)
```

- **View Binding:** Zero `findViewById` boilerplate; clean null-safe UI binding across all activities.
- **Service & Broadcast Receiver:** Decoupled background timers and live network state observation.
- **Content Provider:** SQLite access abstraction for favourite circuit storage.
- **Custom View:** Custom canvas-rendered interactive workbench handling drag, zoom, and redraw ticks.

---

## 📂 Project Structure

```text
📁 CircuitCraft
├── 📱 app/
│   ├── 📜 src/main/AndroidManifest.xml
│   ├── ☕ src/main/java/com/circuitcraft/app/
│   ├── 📦 src/main/assets/questions.json
│   ├── 🎨 src/main/res/
│   │   ├── 🖼️ drawable/
│   │   ├── 📐 layout/
│   │   ├── 📱 mipmap/
│   │   └── 🎨 values/
│   └── 🐘 build.gradle                 # Module-level build script
├── 🐘 gradle/
│   ├── ⚙️ wrapper/gradle-wrapper.properties
│   └── 📑 libs.versions.toml           # Modern Gradle version catalog
├── 🐘 build.gradle                     # Project-level build script
├── ⚙️ gradle.properties                # Build environment configuration
├── ⚙️ settings.gradle                  # Root project & module settings
├── 📄 questions.json                   # Remote quiz dataset
├── 📄 LICENSE                          # MIT License
└── 📄 README.md                        # Documentation
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** Hedgehog / Iguana (or newer)
- **JDK:** Java 11 or Java 17
- **Android SDK:** Compile SDK 33, Minimum SDK 24 (Android 7.0+)

### Setup Instructions

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/amontilladoD/CircuitCraft-Interactive-Electronics-Lab-Companion.git
   ```

2. **Open in Android Studio:**
   - Launch Android Studio.
   - Select **File → Open** and select the root directory of this project.
   - Allow Gradle to sync dependencies automatically.

3. **Configure Firebase (Optional for Authentication & Cloud Sync):**
   - Add your `google-services.json` file inside the `app/` folder.
   - In the Firebase Console, enable **Email/Password** authentication and create a **Cloud Firestore** database.

4. **Run the Application:**
   - Connect an Android device (via USB Debugging) or launch an Android Emulator.
   - Click **Run ▶** in Android Studio to build and launch CircuitCraft!

---

## 🛡️ License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for complete details.
