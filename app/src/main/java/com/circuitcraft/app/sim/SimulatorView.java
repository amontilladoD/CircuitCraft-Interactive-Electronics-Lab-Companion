package com.circuitcraft.app.sim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.circuitcraft.app.logic.Units;

public class SimulatorView extends View {
    public static final int WORLD_W = 460, WORLD_H = 640, GRID = CircuitModel.GRID;

    public interface Listener {
        void onSelectionChanged(Part selected);
    }

    private CircuitModel model = CircuitModel.withBreadboard();
    private CircuitEngine engine = new CircuitEngine(model);
    private Part selected;
    private Listener listener;

    private float scale = 1f, offX = 0f, offY = 0f, fitScale = 1f;
    private final ScaleGestureDetector scaleDetector;

    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int NONE = 0, PART = 1, END0 = 2, END1 = 3, WIRE = 4, PAN = 5;
    private int dragMode = NONE;
    private float downX, downY, lastX, lastY;
    private int grabDx, grabDy, wireVx, wireVy;
    private boolean moved;
    private Part touched;

    private static final int[] WIRE_COLORS = {0xFFD32F2F, 0xFF1976D2, 0xFF388E3C, 0xFFF57C00, 0xFF7B1FA2, 0xFF00838F};

    public SimulatorView(Context c) { this(c, null); }

    public SimulatorView(Context c, AttributeSet a) {
        super(c, a);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(2.5f);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setColor(0xFF263238);
        fill.setStyle(Paint.Style.FILL);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(13f);
        text.setColor(0xFF263238);
        selPaint.setStyle(Paint.Style.STROKE);
        selPaint.setStrokeWidth(2f);
        selPaint.setColor(0xFF1E88E5);
        selPaint.setPathEffect(new DashPathEffect(new float[]{6, 4}, 0));

        scaleDetector = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector d) {
                float ns = Math.max(fitScale * 0.8f, Math.min(fitScale * 4f, scale * d.getScaleFactor()));
                float f = ns / scale;
                offX = d.getFocusX() - (d.getFocusX() - offX) * f;
                offY = d.getFocusY() - (d.getFocusY() - offY) * f;
                scale = ns;
                invalidate();
                return true;
            }
        });
    }

    public void setModel(CircuitModel m) {
        model = m;
        engine = new CircuitEngine(m);
        select(null);
        invalidate();
    }

    public CircuitModel getModel() { return model; }
    public CircuitEngine getEngine() { return engine; }
    public Part getSelected() { return selected; }
    public void setListener(Listener l) { listener = l; }

    public void select(Part p) {
        selected = p;
        if (listener != null) listener.onSelectionChanged(p);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        fitScale = Math.min(w / (float) WORLD_W, h / (float) WORLD_H);
        scale = fitScale;
        offX = (w - WORLD_W * scale) / 2f;
        offY = 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e);
        if (scaleDetector.isInProgress() || e.getPointerCount() > 1) {
            dragMode = NONE;
            return true;
        }
        float wx = (e.getX() - offX) / scale, wy = (e.getY() - offY) / scale;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = e.getX();
                downY = lastY = e.getY();
                moved = false;
                hit(wx, wy);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragMode == NONE) return true;
                if (!moved && Math.hypot(e.getX() - downX, e.getY() - downY) > 14) moved = true;
                if (moved) drag(e.getX(), e.getY(), wx, wy);
                lastX = e.getX();
                lastY = e.getY();
                return true;
            case MotionEvent.ACTION_UP:
                if (!moved && touched != null && touched.type == PartType.SWITCH) {
                    touched.mode = touched.mode == 1 ? 0 : 1;      
                    invalidate();
                }
                dragMode = NONE;
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragMode = NONE;
                return true;
            default:
                return true;
        }
    }

    private void hit(float wx, float wy) {
        touched = null;
        dragMode = NONE;
        
        for (int i = model.parts.size() - 1; i >= 0; i--) {
            Part p = model.parts.get(i);
            if (p.type != PartType.JUMPER) continue;
            if (Math.hypot(wx - p.x, wy - p.y) < 16) { start(p, END0); return; }
            if (Math.hypot(wx - p.x2, wy - p.y2) < 16) { start(p, END1); return; }
            if (distToSegment(wx, wy, p.x, p.y, p.x2, p.y2) < 9) {
                grabDx = Math.round(wx - p.x);
                grabDy = Math.round(wy - p.y);
                wireVx = p.x2 - p.x;
                wireVy = p.y2 - p.y;
                start(p, WIRE);
                return;
            }
        }
        
        for (int i = model.parts.size() - 1; i >= 0; i--) {
            Part p = model.parts.get(i);
            if (p.type == PartType.JUMPER || p.type == PartType.BREADBOARD) continue;
            if (p.contains(wx, wy)) {
                grabDx = Math.round(wx - p.x);
                grabDy = Math.round(wy - p.y);
                start(p, PART);
                return;
            }
        }
        
        for (Part p : model.parts) {
            if (p.type == PartType.BREADBOARD && p.contains(wx, wy)) { start(p, PAN); return; }
        }
        dragMode = PAN;
        select(null);
    }

    private void start(Part p, int mode) {
        touched = p;
        dragMode = mode;
        if (selected != p) select(p);
    }

    private void drag(float sx, float sy, float wx, float wy) {
        switch (dragMode) {
            case PAN:
                offX += sx - lastX;
                offY += sy - lastY;
                break;
            case PART: {
                int nx = snapX(wx - grabDx), ny = snapY(wy - grabDy);
                if (nx != touched.x || ny != touched.y) {
                    touched.x = nx;
                    touched.y = ny;
                    engine.markDirty();
                }
                break;
            }
            case END0: {
                int nx = snapX(wx), ny = snapY(wy);
                if (nx != touched.x || ny != touched.y) { touched.x = nx; touched.y = ny; engine.markDirty(); }
                break;
            }
            case END1: {
                int nx = snapX(wx), ny = snapY(wy);
                if (nx != touched.x2 || ny != touched.y2) { touched.x2 = nx; touched.y2 = ny; engine.markDirty(); }
                break;
            }
            case WIRE: {
                int nx = snapX(wx - grabDx), ny = snapY(wy - grabDy);
                if (nx != touched.x || ny != touched.y) {
                    touched.x = nx;
                    touched.y = ny;
                    touched.x2 = nx + wireVx;
                    touched.y2 = ny + wireVy;
                    engine.markDirty();
                }
                break;
            }
            default:
                break;
        }
        invalidate();
    }

    private static int snap(float v, int max) {
        int s = Math.round(v / GRID) * GRID;
        return Math.max(0, Math.min(max, s));
    }

    private static int snapX(float v) { return snap(v, WORLD_W); }
    private static int snapY(float v) { return snap(v, WORLD_H); }

    private static float distToSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        float len2 = dx * dx + dy * dy;
        float t = len2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / len2));
        return (float) Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.drawColor(0xFFECEFF1);
        c.save();
        c.translate(offX, offY);
        c.scale(scale, scale);

        fill.setColor(Color.WHITE);
        c.drawRect(0, 0, WORLD_W, WORLD_H, fill);
        fill.setColor(0xFFCFD8DC);
        for (int gx = 0; gx <= WORLD_W; gx += GRID)
            for (int gy = 440; gy <= WORLD_H; gy += GRID) c.drawCircle(gx, gy, 1f, fill);

        for (Part p : model.parts) if (p.type == PartType.BREADBOARD) drawBreadboard(c, p);
        for (Part p : model.parts)
            if (p.type != PartType.BREADBOARD && p.type != PartType.JUMPER) drawPart(c, p);
        int wi = 0;
        for (Part p : model.parts) if (p.type == PartType.JUMPER) drawJumper(c, p, wi++);
        c.restore();
    }

    private void drawBreadboard(Canvas c, Part p) {
        RectF r = new RectF(p.x - 20, p.y - 20, p.x + 400, p.y + 340);
        fill.setColor(0xFFF3EBD3);
        c.drawRoundRect(r, 8, 8, fill);
        line.setStrokeWidth(2f);
        line.setColor(p == selected ? 0xFF1E88E5 : 0xFF9E9E9E);
        c.drawRoundRect(r, 8, 8, line);
        line.setColor(0xFF263238);
        line.setStrokeWidth(2.5f);
        fill.setColor(0xFFD7CCA8);
        c.drawRect(p.x - 20, p.y + 150, p.x + 400, p.y + 170, fill);     

        int[] railY = {0, 20, 300, 320};
        int[] rows = {60, 80, 100, 120, 140, 180, 200, 220, 240, 260};
        fill.setColor(0xFF78909C);
        for (int i = 0; i < CircuitEngine.COLS; i++) {
            int hx = p.x + 20 * i;
            for (int ry : railY) c.drawCircle(hx, p.y + ry, 3.2f, fill);
            for (int ry : rows) c.drawCircle(hx, p.y + ry, 3.2f, fill);
        }
        text.setTextSize(13f);
        text.setColor(0xFFD32F2F);
        c.drawText("+", p.x - 10, p.y + 4, text);
        c.drawText("+", p.x - 10, p.y + 304, text);
        text.setColor(0xFF1976D2);
        c.drawText("−", p.x - 10, p.y + 24, text);
        c.drawText("−", p.x - 10, p.y + 324, text);
        text.setColor(0xFF263238);
        text.setTextSize(9f);
        for (int i = 0; i < CircuitEngine.COLS; i += 5) c.drawText(String.valueOf(i + 1), p.x + 20 * i, p.y + 46, text);
        text.setTextSize(13f);
    }

    private void drawJumper(Canvas c, Part p, int index) {
        int col = WIRE_COLORS[index % WIRE_COLORS.length];
        if (p == selected) {
            line.setColor(0xFF1E88E5);
            line.setStrokeWidth(8f);
            c.drawLine(p.x, p.y, p.x2, p.y2, line);
        }
        line.setColor(col);
        line.setStrokeWidth(4.5f);
        c.drawLine(p.x, p.y, p.x2, p.y2, line);
        fill.setColor(col);
        c.drawCircle(p.x, p.y, 6f, fill);
        c.drawCircle(p.x2, p.y2, 6f, fill);
        line.setColor(0xFF263238);
        line.setStrokeWidth(2.5f);
    }

    private void leads2(Canvas c, int x1, int x2, int pinEnd) {
        c.drawLine(0, 0, x1, 0, line);
        c.drawLine(x2, 0, pinEnd, 0, line);
    }

    private void drawPart(Canvas c, Part p) {
        PartType t = p.type;
        c.save();
        c.translate(p.x, p.y);
        c.rotate(p.rot * 90f);
        line.setColor(0xFF263238);
        line.setStrokeWidth(2.5f);
        fill.setStyle(Paint.Style.FILL);

        switch (t) {
            case RESISTOR: {
                leads2(c, 14, 66, 80);
                Path z = new Path();
                z.moveTo(14, 0);
                float[] ys = {-8, 8, -8, 8, -8, 8};
                for (int i = 0; i < ys.length; i++) z.lineTo(20 + i * 8.7f, ys[i]);
                z.lineTo(66, 0);
                c.drawPath(z, line);
                break;
            }
            case CAPACITOR:
                leads2(c, 17, 23, 40);
                line.setStrokeWidth(3.5f);
                c.drawLine(17, -14, 17, 14, line);
                c.drawLine(23, -14, 23, 14, line);
                line.setStrokeWidth(2.5f);
                break;
            case DIODE:
            case LED: {
                leads2(c, 10, 30, 40);
                float glow = 0;
                if (t == PartType.LED && engine.isRunning() && !p.burnt)
                    glow = (float) Math.max(0, Math.min(1, p.current / 0.015));
                if (glow > 0.02f) {
                    fill.setColor(Color.argb((int) (140 * glow), 255, 60, 40));
                    c.drawCircle(20, 0, 26, fill);
                }
                int body = 0xFFE0E0E0;
                if (t == PartType.LED) body = p.burnt ? 0xFF424242 : blend(0xFFEF9A9A, 0xFFFF1744, glow);
                fill.setColor(body);
                Path tri = new Path();
                tri.moveTo(10, -11);
                tri.lineTo(10, 11);
                tri.lineTo(30, 0);
                tri.close();
                c.drawPath(tri, fill);
                c.drawPath(tri, line);
                c.drawLine(30, -11, 30, 11, line);
                if (t == PartType.LED) {                         
                    line.setStrokeWidth(1.5f);
                    c.drawLine(22, -13, 30, -22, line);
                    c.drawLine(28, -10, 36, -19, line);
                    line.setStrokeWidth(2.5f);
                }
                break;
            }
            case BATTERY:
                leads2(c, 12, 30, 40);
                line.setStrokeWidth(3.5f);
                c.drawLine(12, -15, 12, 15, line);
                c.drawLine(18, -8, 18, 8, line);
                c.drawLine(24, -15, 24, 15, line);
                c.drawLine(30, -8, 30, 8, line);
                line.setStrokeWidth(2.5f);
                break;
            case AC_SOURCE:
            case AMMETER:
            case VOLTMETER: {
                c.drawLine(0, 0, 2, 0, line);
                c.drawLine(38, 0, 40, 0, line);
                fill.setColor(t == PartType.AC_SOURCE ? 0xFFFFF9C4 : 0xFFE3F2FD);
                c.drawCircle(20, 0, 18, fill);
                c.drawCircle(20, 0, 18, line);
                if (t == PartType.AC_SOURCE) {
                    Path s = new Path();
                    s.moveTo(9, 0);
                    s.cubicTo(13, -14, 17, -14, 20, 0);
                    s.cubicTo(23, 14, 27, 14, 31, 0);
                    c.drawPath(s, line);
                }
                break;
            }
            case MULTIMETER: {
                RectF body = new RectF(0, -24, 60, 24);
                fill.setColor(0xFFFFC107);
                c.drawRoundRect(body, 6, 6, fill);
                c.drawRoundRect(body, 6, 6, line);
                fill.setColor(0xFFC8E6C9);
                c.drawRect(6, -18, 54, 2, fill);
                break;
            }
            case SWITCH:
                leads2(c, 8, 32, 40);
                fill.setColor(0xFF263238);
                c.drawCircle(8, 0, 3, fill);
                c.drawCircle(32, 0, 3, fill);
                line.setStrokeWidth(3.5f);
                if (p.mode == 1) c.drawLine(8, 0, 32, 0, line);
                else c.drawLine(8, 0, 30, -14, line);
                line.setStrokeWidth(2.5f);
                break;
            case TRANSISTOR: {
                for (int[] pn : t.pins) c.drawLine(pn[0], pn[1], pn[0], -6, line);
                Path d = new Path();
                d.moveTo(0, -6);
                d.lineTo(0, -28);
                d.quadTo(20, -50, 40, -28);
                d.lineTo(40, -6);
                d.close();
                fill.setColor(0xFF455A64);
                c.drawPath(d, fill);
                c.drawPath(d, line);
                break;
            }
            default: {   
                for (int[] pn : t.pins) c.drawLine(pn[0], pn[1], pn[0], -6, line);
                RectF chip = new RectF(t.bl, t.bt, t.br, -6);
                fill.setColor(0xFF37474F);
                c.drawRoundRect(chip, 4, 4, fill);
                c.drawRoundRect(chip, 4, 4, line);
                fill.setColor(0xFFB0BEC5);
                c.drawCircle(t.bl + 8, -14, 2.5f, fill);       
                break;
            }
        }

        if (p == selected) c.drawRect(t.bl - 5, t.bt - 5, t.br + 5, t.bb + 5, selPaint);
        c.restore();

        for (int i = 0; i < p.pinCount(); i++) {
            int[] w = p.pin(i);
            fill.setColor(0xFFEF6C00);
            c.drawCircle(w[0], w[1], 4f, fill);
        }
        drawTexts(c, p);
    }

    private void drawTexts(Canvas c, Part p) {
        PartType t = p.type;
        text.setTextSize(11f);
        
        boolean names = t == PartType.TRANSISTOR || t.isLogic() || t == PartType.LED || t == PartType.DIODE
                || t == PartType.BATTERY || t == PartType.AC_SOURCE || t == PartType.AMMETER
                || t == PartType.VOLTMETER || t == PartType.MULTIMETER || t == PartType.CAPACITOR;
        if (names) {
            text.setColor(0xFF455A64);
            boolean below = t == PartType.TRANSISTOR || t.isLogic();
            for (int i = 0; i < p.pinCount(); i++) {
                int[] w = p.pin(i);
                c.drawText(t.pinNames[i], w[0], w[1] + (below ? 17 : -9), text);
            }
        }
        
        text.setTextSize(12f);
        if (t.isLogic() || t == PartType.TRANSISTOR) {
            float[] ctr = p.toWorld((t.bl + t.br) / 2f, t == PartType.TRANSISTOR ? -22 : -24);
            text.setColor(Color.WHITE);
            String name = t == PartType.TRANSISTOR ? "NPN" : (t == PartType.FLIPFLOP ? "D-FF" : t.name());
            c.drawText(name, ctr[0], ctr[1] + 4, text);
        } else if (t == PartType.AMMETER || t == PartType.VOLTMETER) {
            float[] ctr = p.toWorld(20, 0);
            text.setColor(0xFF0D47A1);
            text.setTextSize(16f);
            c.drawText(t == PartType.AMMETER ? "A" : "V", ctr[0], ctr[1] + 6, text);
            text.setTextSize(12f);
        }
        
        String info = infoText(p);
        if (!info.isEmpty()) {
            float[] under = p.toWorld((t.bl + t.br) / 2f, t == PartType.MULTIMETER ? 0 : Math.max(t.bb, 0) + 16);
            if (t == PartType.TRANSISTOR || t.isLogic()) under = p.toWorld((t.bl + t.br) / 2f, 34);
            boolean meter = t == PartType.AMMETER || t == PartType.VOLTMETER || t == PartType.MULTIMETER;
            text.setColor(meter ? 0xFF1B5E20 : 0xFF263238);
            if (t == PartType.MULTIMETER) {
                float[] d = p.toWorld(30, -8);
                text.setTextSize(13f);
                c.drawText(info, d[0], d[1] + 4, text);
                float[] m = p.toWorld(30, 14);
                text.setTextSize(10f);
                text.setColor(0xFF263238);
                c.drawText(p.mode == 1 ? "A (series)" : "V (parallel)", m[0], m[1] + 3, text);
            } else {
                c.drawText(info, under[0], under[1], text);
            }
        }
        text.setColor(0xFF263238);
        text.setTextSize(13f);
    }

    private String infoText(Part p) {
        switch (p.type) {
            case RESISTOR:  return Units.eng(p.value, "Ω");
            case CAPACITOR: return Units.eng(p.value, "F");
            case BATTERY:   return Units.eng(p.value, "V");
            case AC_SOURCE: return Units.eng(p.value, "V") + "pk " + Math.round(p.value2) + "Hz";
            case SWITCH:    return p.mode == 1 ? "ON" : "OFF";
            case LED:       return p.burnt ? "BURNT" : "";
            case AMMETER:
            case VOLTMETER:
            case MULTIMETER:
                return p.reading.isEmpty() ? "-" : p.reading;
            default:        return "";
        }
    }

    private static int blend(int a, int b, float f) {
        int r = (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * f);
        int g = (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * f);
        int bl = (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f);
        return Color.rgb(r, g, bl);
    }
}
