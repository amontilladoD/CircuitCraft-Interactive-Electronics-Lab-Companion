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
