package com.circuitcraft.app.sim;

import com.circuitcraft.app.logic.Units;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CircuitEngine {
    public static final double DT = 1e-4;
    private static final int STEPS_PER_TICK = 300;
    private static final int WINDOW = 1000;

    private static final double GMIN = 1e-12;
    private static final double SRC_R = 0.1;            
    private static final double METER_G = 1e-7;         
    private static final double LOGIC_HIGH = 5.0, LOGIC_TH = 2.5, LOGIC_G = 0.02, LOGIC_IN_G = 1e-6;

    private static final double IS_DIODE = 1e-14, NVT_DIODE = 0.02585;
    private static final double IS_LED = 1e-18, NVT_LED = 0.0517;
    private static final double IS_BJT = 1e-14, VT = 0.02585, BETA_F = 100, BETA_R = 2;
    private static final double LED_BURN_A = 0.04;

    private static final int[] RAIL_Y = {0, 20, 300, 320};
    private static final int[] ROW_TOP = {60, 80, 100, 120, 140};
    private static final int[] ROW_BOT = {180, 200, 220, 240, 260};
    public static final int COLS = 20;

    private final CircuitModel model;
    private boolean dirty = true, running = false;
    private double time;
    public boolean hasGround;
    private final List<String> staticWarnings = new ArrayList<>();
    private final List<String> liveWarnings = new ArrayList<>();

    private final ArrayList<Integer> uf = new ArrayList<>();
    private final HashMap<Long, Integer> ptIds = new HashMap<>();

    private int nNodes, size;
    private Part[] devs = new Part[0];
    private int[][] nodes = new int[0][];
    private int[] extraIdx = new int[0];
    private double[][] A = new double[0][0];
    private double[] b = new double[0], x = new double[0], vOld = new double[1], vNew = new double[1];
    private double[] lim1 = new double[0], lim2 = new double[0];
    private boolean[] outState = new boolean[0], clkPrev = new boolean[0];
    private boolean nonlinear;
    private double[] accMeas = new double[0], accMeas2 = new double[0], accCur = new double[0], accVd = new double[0];
    private int winCount, windowsDone;
    private double maxSrc;

    public CircuitEngine(CircuitModel model) { this.model = model; }

    public void markDirty() { dirty = true; }
    public boolean isRunning() { return running; }
    public double getTime() { return time; }

    public void start() {
        rebuild();
        running = true;
    }

    public void stop() {
        running = false;
        for (Part p : model.parts) {
            p.reading = "";
            p.current = 0;
            p.volt = 0;
            p.burnt = false;
        }
    }

    public void tick() {
        if (!running) return;
        if (dirty) rebuild();
        if (!hasGround) return;
        for (int i = 0; i < STEPS_PER_TICK; i++) step();
    }

    public List<String> diagnose() {
        if (dirty) rebuild();
        List<String> all = new ArrayList<>(staticWarnings);
        all.addAll(liveWarnings);
        return all;
    }

    public int warningCount() { return staticWarnings.size() + liveWarnings.size(); }

    private int newId() {
        uf.add(uf.size());
        return uf.size() - 1;
    }

    private int idOf(int px, int py) {
        long key = ((long) px << 32) ^ (py & 0xffffffffL);
        Integer id = ptIds.get(key);
        if (id == null) {
            id = newId();
            ptIds.put(key, id);
        }
        return id;
    }

    private int find(int a) {
        while (uf.get(a) != a) {
            uf.set(a, uf.get(uf.get(a)));
            a = uf.get(a);
        }
        return a;
    }

    private void union(int a, int c) {
        int ra = find(a), rc = find(c);
        if (ra != rc) uf.set(ra, rc);
    }

    private void rebuild() {
        dirty = false;
        staticWarnings.clear();
        liveWarnings.clear();
        uf.clear();
        ptIds.clear();

        for (Part p : model.parts) {
            if (p.type != PartType.BREADBOARD) continue;
            int[] railG = new int[RAIL_Y.length];
            for (int k = 0; k < railG.length; k++) railG[k] = newId();
            for (int i = 0; i < COLS; i++) {
                int hx = p.x + 20 * i;
                for (int k = 0; k < RAIL_Y.length; k++) union(idOf(hx, p.y + RAIL_Y[k]), railG[k]);
                int gTop = newId(), gBot = newId();
                for (int r : ROW_TOP) union(idOf(hx, p.y + r), gTop);
                for (int r : ROW_BOT) union(idOf(hx, p.y + r), gBot);
            }
        }
        
        for (Part p : model.parts) {
            if (p.type == PartType.JUMPER) union(idOf(p.x, p.y), idOf(p.x2, p.y2));
        }
        
        List<Part> list = new ArrayList<>();
        for (Part p : model.parts) {
            if (p.type != PartType.BREADBOARD && p.type != PartType.JUMPER) list.add(p);
        }
        devs = list.toArray(new Part[0]);
        int[][] pinIds = new int[devs.length][];
        for (int i = 0; i < devs.length; i++) {
            pinIds[i] = new int[devs[i].pinCount()];
            for (int k = 0; k < pinIds[i].length; k++) {
                int[] w = devs[i].pin(k);
                pinIds[i][k] = idOf(w[0], w[1]);
            }
        }

        int groundRoot = -1;
        for (int i = 0; i < devs.length && groundRoot < 0; i++)
            if (devs[i].type == PartType.BATTERY) groundRoot = find(pinIds[i][1]);
        for (int i = 0; i < devs.length && groundRoot < 0; i++)
            if (devs[i].type == PartType.AC_SOURCE) groundRoot = find(pinIds[i][1]);

        hasGround = groundRoot >= 0;
        if (!hasGround) {
            staticWarnings.add("No power source: add a DC Battery or an AC Source.");
            nodes = new int[devs.length][];
            nNodes = 1;
            size = 0;
            clearParts();
            return;
        }

        Map<Integer, Integer> rootToNode = new HashMap<>();
        rootToNode.put(groundRoot, 0);
        Map<Integer, Integer> rootCount = new HashMap<>();
        nodes = new int[devs.length][];
        for (int i = 0; i < devs.length; i++) {
            nodes[i] = new int[pinIds[i].length];
            for (int k = 0; k < pinIds[i].length; k++) {
                int r = find(pinIds[i][k]);
                Integer n = rootToNode.get(r);
                if (n == null) {
                    n = rootToNode.size();
                    rootToNode.put(r, n);
                }
                nodes[i][k] = n;
                Integer c = rootCount.get(r);
                rootCount.put(r, c == null ? 1 : c + 1);
            }
        }
        nNodes = rootToNode.size();

        for (int i = 0; i < devs.length; i++) {
            for (int k = 0; k < pinIds[i].length; k++) {
                if (!devs[i].type.isRequiredPin(k)) continue;
                if (rootCount.get(find(pinIds[i][k])) == 1) {
                    staticWarnings.add(model.labelOf(devs[i]) + ": pin " + devs[i].type.pinNames[k]
                            + " is not connected to anything.");
                }
            }
        }

        extraIdx = new int[devs.length];
        int extra = 0;
        nonlinear = false;
        for (int i = 0; i < devs.length; i++) {
            extraIdx[i] = -1;
            if (isAmmeter(devs[i])) extraIdx[i] = nNodes - 1 + extra++;
            PartType t = devs[i].type;
            if (t == PartType.DIODE || t == PartType.LED || t == PartType.TRANSISTOR) nonlinear = true;
        }
        size = nNodes - 1 + extra;
        A = new double[size][size];
        b = new double[size];
        x = new double[size];
        vOld = new double[nNodes];
        vNew = new double[nNodes];
        int n = devs.length;
        lim1 = new double[n];
        lim2 = new double[n];
        outState = new boolean[n];
        clkPrev = new boolean[n];
        accMeas = new double[n];
        accMeas2 = new double[n];
        accCur = new double[n];
        accVd = new double[n];
        clearParts();
        time = 0;
        winCount = 0;
        windowsDone = 0;
        maxSrc = 0;
        for (Part d : devs) {
            if (d.type == PartType.VOLTMETER || d.type == PartType.AMMETER || d.type == PartType.MULTIMETER)
                d.reading = "…";
        }
    }

    private void clearParts() {
        for (Part p : model.parts) {
            p.reading = "";
            p.current = 0;
            p.volt = 0;
            p.burnt = false;
        }
    }

    private static boolean isAmmeter(Part p) {
        return p.type == PartType.AMMETER || (p.type == PartType.MULTIMETER && p.mode == 1);
    }

    private void step() {
        
        for (int i = 0; i < devs.length; i++) {
            PartType t = devs[i].type;
            int[] nd = nodes[i];
            if (t.isGate()) {
                boolean a = vOld[nd[0]] > LOGIC_TH;
                boolean c = t != PartType.NOT && vOld[nd[1]] > LOGIC_TH;
                outState[i] = evalGate(t, a, c);
            } else if (t == PartType.FLIPFLOP) {
                boolean clk = vOld[nd[1]] > LOGIC_TH;
                if (clk && !clkPrev[i]) outState[i] = vOld[nd[0]] > LOGIC_TH;   
                clkPrev[i] = clk;
            }
        }

        System.arraycopy(vOld, 0, vNew, 0, nNodes);
        int maxIter = nonlinear ? 80 : 1;
        for (int it = 0; it < maxIter; it++) {
            for (int r = 0; r < size; r++) {
                java.util.Arrays.fill(A[r], 0);
                b[r] = 0;
            }
            stampAll();
            solve();
            double maxd = 0;
            for (int nIdx = 1; nIdx < nNodes; nIdx++) {
                double nv = x[nIdx - 1];
                maxd = Math.max(maxd, Math.abs(nv - vNew[nIdx]));
                vNew[nIdx] = nv;
            }
            if (nonlinear && it > 0 && maxd < 1e-7) break;
        }

        measure();
        System.arraycopy(vNew, 0, vOld, 0, nNodes);
        time += DT;
        winCount++;
        if (winCount >= WINDOW) endWindow();
    }

    private static boolean evalGate(PartType t, boolean a, boolean c) {
        switch (t) {
            case AND:  return a && c;
            case OR:   return a || c;
            case NAND: return !(a && c);
            case NOR:  return !(a || c);
            case XOR:  return a ^ c;
            case XNOR: return !(a ^ c);
            default:   return !a;      
        }
    }

    private void add(int r, int c, double v) {
        if (r > 0 && c > 0) A[r - 1][c - 1] += v;
    }

    private void addB(int r, double v) {
        if (r > 0) b[r - 1] += v;
    }

    private void stampG(int p, int q, double g) {
        add(p, p, g);
        add(q, q, g);
        add(p, q, -g);
        add(q, p, -g);
    }

    private double sourceVoltage(Part p, double t) {
        if (p.type == PartType.AC_SOURCE) return p.value * Math.sin(2 * Math.PI * p.value2 * t);
        return p.value;
    }

    private void stampAll() {
        for (int r = 0; r < nNodes - 1; r++) A[r][r] += GMIN;
        for (int i = 0; i < devs.length; i++) {
            Part p = devs[i];
            int[] nd = nodes[i];
            switch (p.type) {
                case RESISTOR:
                    stampG(nd[0], nd[1], 1.0 / Math.max(p.value, 1e-3));
                    break;
                case CAPACITOR: {
                    double geq = Math.max(p.value, 1e-12) / DT;
                    double vprev = vOld[nd[0]] - vOld[nd[1]];
                    stampG(nd[0], nd[1], geq);
                    addB(nd[0], geq * vprev);
                    addB(nd[1], -geq * vprev);
                    break;
                }
                case BATTERY:
                case AC_SOURCE: {
                    double g = 1.0 / SRC_R, v = sourceVoltage(p, time + DT);
                    stampG(nd[0], nd[1], g);
                    addB(nd[0], v * g);
                    addB(nd[1], -v * g);
                    break;
                }
                case SWITCH:
                    stampG(nd[0], nd[1], p.mode == 1 ? 1e3 : 1e-9);
                    break;
                case DIODE:
                    stampDiode(i, nd[0], nd[1], IS_DIODE, NVT_DIODE);
                    break;
                case LED:
                    if (!p.burnt) stampDiode(i, nd[0], nd[1], IS_LED, NVT_LED);
                    break;
                case TRANSISTOR:
                    stampBjt(i, nd[0], nd[1], nd[2]);
                    break;
                case VOLTMETER:
                    stampG(nd[0], nd[1], METER_G);
                    break;
                case AMMETER:
                    stampAmmeter(i);
                    break;
                case MULTIMETER:
                    if (p.mode == 1) stampAmmeter(i);
                    else stampG(nd[0], nd[1], METER_G);
                    break;
                case FLIPFLOP:
                    stampG(nd[0], 0, LOGIC_IN_G);
                    stampG(nd[1], 0, LOGIC_IN_G);
                    stampOut(nd[2], outState[i]);
                    stampOut(nd[3], !outState[i]);
                    break;
                default:
                    if (p.type.isGate()) {
                        int last = nd.length - 1;
                        for (int k = 0; k < last; k++) stampG(nd[k], 0, LOGIC_IN_G);
                        stampOut(nd[last], outState[i]);
                    }
                    break;
            }
        }
    }

    private void stampOut(int node, boolean high) {
        stampG(node, 0, LOGIC_G);
        addB(node, (high ? LOGIC_HIGH : 0.0) * LOGIC_G);
    }

    private void stampAmmeter(int i) {
        int k = extraIdx[i];
        int p = nodes[i][0], m = nodes[i][1];
        if (p > 0) { A[p - 1][k] += 1; A[k][p - 1] += 1; }
        if (m > 0) { A[m - 1][k] -= 1; A[k][m - 1] -= 1; }
    }

    private static double pnjlim(double vnew, double vold, double vt, double vcrit) {
        if (vnew > vcrit && Math.abs(vnew - vold) > 2 * vt) {
            if (vold > 0) {
                double arg = 1 + (vnew - vold) / vt;
                vnew = arg > 0 ? vold + vt * Math.log(arg) : vcrit;
            } else {
                vnew = vt * Math.log(vnew / vt);
            }
        }
        return vnew;
    }

    private void stampDiode(int i, int a, int k, double is, double nvt) {
        double vcrit = nvt * Math.log(nvt / (Math.sqrt(2) * is));
        double vd = pnjlim(vNew[a] - vNew[k], lim1[i], nvt, vcrit);
        lim1[i] = vd;
        double ex = Math.exp(vd / nvt);
        double id = is * (ex - 1);
        double gd = is * ex / nvt + GMIN;
        stampG(a, k, gd);
        double ieq = id - gd * vd;
        addB(a, -ieq);
        addB(k, ieq);
    }

    private void stampBjt(int i, int c, int bn, int e) {
        double vcrit = VT * Math.log(VT / (Math.sqrt(2) * IS_BJT));
        double vbe = pnjlim(vNew[bn] - vNew[e], lim1[i], VT, vcrit);
        double vbc = pnjlim(vNew[bn] - vNew[c], lim2[i], VT, vcrit);
        lim1[i] = vbe;
        lim2[i] = vbc;
        double exf = Math.exp(vbe / VT), exr = Math.exp(vbc / VT);
        double iF = IS_BJT * (exf - 1), iR = IS_BJT * (exr - 1);
        double gf = IS_BJT * exf / VT + GMIN, gr = IS_BJT * exr / VT + GMIN;

        double ic = iF - iR * (1 + 1 / BETA_R);
        double ib = iF / BETA_F + iR / BETA_R;
        double ie = -(ic + ib);
        double icBe = gf, icBc = -gr * (1 + 1 / BETA_R);
        double ibBe = gf / BETA_F, ibBc = gr / BETA_R;
        stampTerm(c, c, bn, e, ic, icBe, icBc, vbe, vbc);
        stampTerm(bn, c, bn, e, ib, ibBe, ibBc, vbe, vbc);
        stampTerm(e, c, bn, e, ie, -(icBe + ibBe), -(icBc + ibBc), vbe, vbc);
    }

    private void stampTerm(int t, int c, int bn, int e, double i0, double dBe, double dBc, double vbe, double vbc) {
        add(t, bn, dBe + dBc);
        add(t, e, -dBe);
        add(t, c, -dBc);
        addB(t, -(i0 - dBe * vbe - dBc * vbc));
    }

    private void solve() {
        int n = size;
        for (int c = 0; c < n; c++) {
            int piv = c;
            double best = Math.abs(A[c][c]);
            for (int r = c + 1; r < n; r++) {
                if (Math.abs(A[r][c]) > best) { best = Math.abs(A[r][c]); piv = r; }
            }
            if (best < 1e-18) continue;
            if (piv != c) {
                double[] tr = A[c]; A[c] = A[piv]; A[piv] = tr;
                double tb = b[c]; b[c] = b[piv]; b[piv] = tb;
            }
            for (int r = c + 1; r < n; r++) {
                double f = A[r][c] / A[c][c];
                if (f == 0) continue;
                for (int k = c; k < n; k++) A[r][k] -= f * A[c][k];
                b[r] -= f * b[c];
            }
        }
        for (int i = n - 1; i >= 0; i--) {
            double s = b[i];
            for (int j = i + 1; j < n; j++) s -= A[i][j] * x[j];
            x[i] = Math.abs(A[i][i]) < 1e-18 ? 0 : s / A[i][i];
        }
    }

    private void measure() {
        for (int i = 0; i < devs.length; i++) {
            Part p = devs[i];
            int[] nd = nodes[i];
            switch (p.type) {
                case VOLTMETER:
                case MULTIMETER:
                case AMMETER: {
                    double m = isAmmeter(p) ? x[extraIdx[i]] : vNew[nd[0]] - vNew[nd[1]];
                    accMeas[i] += m;
                    accMeas2[i] += m * m;
                    break;
                }
                case LED:
                case DIODE: {
                    double vd = vNew[nd[0]] - vNew[nd[1]];
                    double is = p.type == PartType.LED ? IS_LED : IS_DIODE;
                    double nvt = p.type == PartType.LED ? NVT_LED : NVT_DIODE;
                    double id = p.burnt ? 0 : is * (Math.exp(Math.min(vd / nvt, 60)) - 1);
                    accCur[i] += id;
                    accVd[i] += vd;
                    break;
                }
                case BATTERY:
                case AC_SOURCE: {
                    double i2 = (sourceVoltage(p, time + DT) - (vNew[nd[0]] - vNew[nd[1]])) / SRC_R;
                    maxSrc = Math.max(maxSrc, Math.abs(i2));
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void endWindow() {
        if (windowsDone++ == 0) {          
            for (int i = 0; i < devs.length; i++) accMeas[i] = accMeas2[i] = accCur[i] = accVd[i] = 0;
            maxSrc = 0;
            winCount = 0;
            return;
        }
        double n = winCount;
        liveWarnings.clear();
        for (int i = 0; i < devs.length; i++) {
            Part p = devs[i];
            switch (p.type) {
                case VOLTMETER:
                case MULTIMETER:
                case AMMETER: {
                    double mean = accMeas[i] / n, rms = Math.sqrt(accMeas2[i] / n);
                    String unit = isAmmeter(p) ? "A" : "V";
                    boolean ac = rms > 1e-9 && (rms - Math.abs(mean)) > 0.03 * rms;
                    if (rms < (unit.equals("V") ? 1e-6 : 1e-9)) p.reading = "0 " + unit;
                    else p.reading = ac ? "~" + Units.eng(rms, unit) + " rms" : Units.eng(mean, unit);
                    if (isAmmeter(p) && rms > 1.0) {
                        liveWarnings.add(model.labelOf(p) + " carries over 1 A. An ammeter must be in SERIES with the load, never across a source.");
                    }
                    break;
                }
                case LED:
                case DIODE: {
                    p.current = accCur[i] / n;
                    p.volt = accVd[i] / n;
                    if (p.type == PartType.LED) {
                        if (!p.burnt && p.current > LED_BURN_A) p.burnt = true;
                        if (p.burnt) {
                            liveWarnings.add(model.labelOf(p) + " burnt out (over 40 mA). Add a series resistor, e.g. R = (Vsupply - 2 V) / 0.015 A.");
                        } else if (p.volt < -1.0 && p.current < 1e-6) {
                            liveWarnings.add(model.labelOf(p) + " is connected backwards: current only flows from A (anode) to K (cathode).");
                        }
                    }
                    break;
                }
                default:
                    break;
            }
            accMeas[i] = accMeas2[i] = accCur[i] = accVd[i] = 0;
        }
        if (maxSrc > 2.0) {
            liveWarnings.add("SHORT CIRCUIT: a source is connected through almost no resistance (over 2 A flows). Add a resistor.");
        }
        maxSrc = 0;
        winCount = 0;
    }
}
