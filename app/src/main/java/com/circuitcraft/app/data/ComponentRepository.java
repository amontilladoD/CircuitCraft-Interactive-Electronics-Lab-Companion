package com.circuitcraft.app.data;

import com.circuitcraft.app.model.ComponentItem;

import java.util.ArrayList;
import java.util.List;

public class ComponentRepository {
    public static List<ComponentItem> getAll() {
        List<ComponentItem> l = new ArrayList<>();
        l.add(new ComponentItem("🔋", "DC Battery (DC source)", "Power source",
                "Gives a constant voltage (default 9 V). Pin + is the long plate, pin - is the short plate. In the Lab the - pin of the first battery is the circuit's 0 V (ground).",
                "Never connect + and - directly with a wire: that is a short circuit and a huge current flows."));
        l.add(new ComponentItem("∿", "AC Source", "Power source",
                "Gives a sine-wave voltage that swings + and -. Set the peak voltage and frequency (default 5 V peak, 50 Hz). A meter on AC shows the RMS value (peak ÷ 1.414).",
                "Polarised parts (LED, electrolytic capacitor) do not like AC - an LED only lights on half of each cycle."));
        l.add(new ComponentItem("▭", "Resistor", "Passive",
                "Limits current. Ohm's law: V = I × R. Colour bands give its value. Tap Value in the Lab to change it (e.g. 330 or 4.7k).",
                "Too small a value lets too much current flow; too low a power rating (P = I²R) makes it hot."));
        l.add(new ComponentItem("⏸", "Capacitor", "Passive",
                "Stores charge. With a resistor it charges/discharges with time constant τ = R × C (1 kΩ × 100 µF = 0.1 s). Blocks DC once charged, passes AC.",
                "Electrolytic capacitors are polarised (+ / - marked) - connecting them backwards can make them burst."));
        l.add(new ComponentItem("▶|", "Diode", "Semiconductor",
                "Lets current flow only from anode (A) to cathode (K) and drops about 0.7 V. Used for rectifying AC and protection.",
                "Fitting it backwards blocks the current completely."));
        l.add(new ComponentItem("💡", "LED", "Semiconductor",
                "Light-emitting diode: drops about 2 V and glows at 5-20 mA. Brightness follows the current. Over 40 mA it burns out in the Lab.",
                "Never connect an LED without a series resistor, and check polarity (A = long leg = +)."));
        l.add(new ComponentItem("🔺", "NPN Transistor", "Semiconductor",
                "An electronic switch/amplifier. A small base current (B) controls a large collector current (C → E). Pins: C, B, E. Needs about 0.7 V between B and E.",
                "Forgetting the base resistor can destroy it; mixing up C, B, E stops it working."));
        l.add(new ComponentItem("Ⓐ", "Ammeter", "Meter",
                "Measures current in amps. It has almost zero resistance, so you place it IN SERIES: break the wire and put the ammeter in the gap.",
                "Putting an ammeter across a battery (in parallel) is a short circuit."));
        l.add(new ComponentItem("Ⓥ", "Voltmeter", "Meter",
                "Measures voltage between two points. It has a huge resistance (10 MΩ), so you place it IN PARALLEL across the part you want to measure.",
                "Placing a voltmeter in series blocks the circuit and the current drops to almost zero."));
        l.add(new ComponentItem("📟", "Multimeter", "Meter",
                "Combines both meters: tap Value in the Lab to switch between Voltmeter mode (V, parallel) and Ammeter mode (A, series).",
                "Using it in ammeter mode across a source = short circuit. Check the mode first."));
        l.add(new ComponentItem("🔘", "Switch", "Control",
                "Opens or closes the circuit. Tap the switch in the Lab to turn it ON/OFF. Handy for giving logic gates a 0 or 1.",
                "A switch wired across the load (instead of in series) shorts it out when closed."));
        l.add(new ComponentItem("&", "AND gate", "Logic IC",
                "Output Y is HIGH (5 V) only when A AND B are both HIGH. Inputs above 2.5 V count as 1. Unconnected inputs read as 0.",
                "Leaving an input floating - tie it to + (1) or - (0) through a wire or switch."));
        l.add(new ComponentItem("≥", "OR gate", "Logic IC",
                "Output Y is HIGH when A OR B (or both) is HIGH.",
                "Mixing up inputs and output pins - A and B are inputs, Y is the output."));
        l.add(new ComponentItem("⊼", "NAND gate", "Logic IC",
                "AND followed by NOT: Y is LOW only when both inputs are HIGH. A 'universal' gate - any circuit can be built from NANDs.",
                "Connecting two outputs together causes a conflict."));
        l.add(new ComponentItem("⊽", "NOR gate", "Logic IC",
                "OR followed by NOT: Y is HIGH only when both inputs are LOW. Two NORs make an SR latch.",
                "Forgetting that NOR is the opposite of OR."));
        l.add(new ComponentItem("⊕", "XOR gate", "Logic IC",
                "Y is HIGH when A and B are DIFFERENT. It is the sum bit of a half adder.",
                "Confusing XOR with OR - XOR is LOW when both inputs are HIGH."));
        l.add(new ComponentItem("⊙", "XNOR gate", "Logic IC",
                "Y is HIGH when A and B are the SAME (an equality checker).",
                "Confusing XNOR with XOR - it is the inverse."));
        l.add(new ComponentItem("¬", "NOT gate", "Logic IC",
                "The inverter: Y is the opposite of A. Has one input (A) and one output (Y).",
                "Expecting it to amplify - it only inverts the logic level."));
        l.add(new ComponentItem("⏯", "D Flip-Flop IC", "Logic IC",
                "Memory cell: on the rising edge of the clock input (CK) the output Q copies input D and holds it. Q̅ is the opposite of Q. Drive CK with a switch or an AC source.",
                "Changing D while CK is steady does nothing - only the clock edge stores the value."));
        l.add(new ComponentItem("🧱", "Breadboard", "Prototyping",
                "A solderless board. The 5 holes in each short column (a-e or f-j) are connected; the top and bottom rows are power rails that run the whole length. Push legs into holes - they snap to the grid.",
                "Putting both legs of a part in the same 5-hole column shorts it out."));
        l.add(new ComponentItem("➰", "Jumper wire", "Prototyping",
                "A wire with two ends. Drag each end onto a breadboard hole or a part pin to connect them. Drag the middle to move the whole wire.",
                "A wire end that isn't on a hole or pin connects to nothing."));
        return l;
    }
}
