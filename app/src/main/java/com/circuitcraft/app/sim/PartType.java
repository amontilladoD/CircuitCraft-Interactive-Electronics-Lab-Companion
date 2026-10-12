package com.circuitcraft.app.sim;

public enum PartType {
    BREADBOARD("Breadboard", "🧱", new int[][]{}, "", -20, -20, 400, 340),
    JUMPER("Jumper wire", "➰", new int[][]{{0, 0}, {60, 0}}, "1,2", 0, 0, 0, 0),
    RESISTOR("Resistor", "▭", new int[][]{{0, 0}, {80, 0}}, "1,2", 14, -9, 66, 9),
    CAPACITOR("Capacitor", "⏸", new int[][]{{0, 0}, {40, 0}}, "+,-", 12, -16, 28, 16),
    DIODE("Diode", "▶|", new int[][]{{0, 0}, {40, 0}}, "A,K", 8, -11, 32, 11),
    LED("LED", "💡", new int[][]{{0, 0}, {40, 0}}, "A,K", 8, -14, 32, 14),
    BATTERY("DC Battery", "🔋", new int[][]{{0, 0}, {40, 0}}, "+,-", 8, -16, 32, 16),
    AC_SOURCE("AC Source", "∿", new int[][]{{0, 0}, {40, 0}}, "+,-", 2, -18, 38, 18),
    AMMETER("Ammeter", "Ⓐ", new int[][]{{0, 0}, {40, 0}}, "+,-", 2, -18, 38, 18),
    VOLTMETER("Voltmeter", "Ⓥ", new int[][]{{0, 0}, {40, 0}}, "+,-", 2, -18, 38, 18),
    MULTIMETER("Multimeter", "📟", new int[][]{{0, 0}, {60, 0}}, "+,-", 0, -24, 60, 24),
    SWITCH("Switch", "🔘", new int[][]{{0, 0}, {40, 0}}, "1,2", 4, -16, 36, 10),
    TRANSISTOR("NPN Transistor", "🔺", new int[][]{{0, 0}, {20, 0}, {40, 0}}, "C,B,E", -2, -42, 42, -4),
    AND("AND gate", "&", new int[][]{{0, 0}, {20, 0}, {40, 0}}, "A,B,Y", -4, -48, 44, -4),
    OR("OR gate", "≥1", new int[][]{{0, 0}, {20, 0}, {40, 0}}, "A,B,Y", -4, -48, 44, -4),
    NAND("NAND gate", "⊼", new int[][]{{0, 0}, {20, 0}, {40, 0}}, "A,B,Y", -4, -48, 44, -4),
    NOR("NOR gate", "⊽", new int[][]{{0, 0}, {20, 0}, {40, 0}}, "A,B,Y", -4, -48, 44, -4),
    XOR("XOR gate", "⊕", new int[][]{{0, 0}, {20, 0}, {40, 0}}, "A,B,Y", -4, -48, 44, -4),
    XNOR("XNOR gate", "⊙", new int[][]{{0, 0}, {20, 0}, {40, 0}}, "A,B,Y", -4, -48, 44, -4),
    NOT("NOT gate", "¬", new int[][]{{0, 0}, {20, 0}}, "A,Y", -4, -48, 24, -4),
    FLIPFLOP("D Flip-Flop IC", "⏯", new int[][]{{0, 0}, {20, 0}, {40, 0}, {60, 0}}, "D,CK,Q,Q̅", -4, -48, 64, -4);

    public final String label, emoji;
    public final int[][] pins;
    public final String[] pinNames;
    public final int bl, bt, br, bb;

    PartType(String label, String emoji, int[][] pins, String names, int bl, int bt, int br, int bb) {
        this.label = label;
        this.emoji = emoji;
        this.pins = pins;
        this.pinNames = names.isEmpty() ? new String[0] : names.split(",");
        this.bl = bl;
        this.bt = bt;
        this.br = br;
        this.bb = bb;
    }

    public boolean isGate() {
        return this == AND || this == OR || this == NAND || this == NOR
                || this == XOR || this == XNOR || this == NOT;
    }

    public boolean isLogic() { return isGate() || this == FLIPFLOP; }

    public boolean isRequiredPin(int i) {
        if (isGate()) return i < pins.length - 1;     
        if (this == FLIPFLOP) return i < 2;           
        return true;
    }
}
