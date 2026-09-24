package dev.loki.lovisual.features.gui.hud.script;

import java.util.LinkedHashMap;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiBounds;

/**
 * Mutable frame snapshot for compact stat widgets such as FPS, TPS, ping, and game time.
 * Facade delegating to helpers in model/render subpackages (same package).
 */
public final class CompactHudStatModel {
    private final String id;
    private final Background background = new Background();
    private final Icon icon = new Icon();
    private final Divider divider = new Divider();
    private final TextRun value = new TextRun();
    private final TextRun unit = new TextRun();
    private final TextRun extra = new TextRun();
    private final Animation animation = new Animation();
    final LinkedHashMap<String, Object> data = new LinkedHashMap<>();
    private boolean visible;
    private float rootX, rootY, width, height, radius, scale;

    public CompactHudStatModel(String id) { this.id = id != null ? id : ""; }

    public static String colorString(int argb) { return CompactHudCodec.colorString(argb); }

    public String id() { return id; }
    public String getId() { return id; }
    public boolean visible() { return visible; }
    public float rootX() { return rootX; }
    public float rootY() { return rootY; }
    public float width() { return width; }
    public float height() { return height; }
    public float radius() { return radius; }
    public float scale() { return scale; }

    public CompactHudStatModel setVisible(boolean v) { this.visible = v; return this; }
    public CompactHudStatModel setRoot(float x, float y, float w, float h, float r, float s) {
        this.rootX = x; this.rootY = y; this.width = w; this.height = h; this.radius = r; this.scale = s; return this;
    }

    public Background background() { return background; }
    public String backgroundEffect() { return background.effect; }
    public float blurAlpha() { return background.blurAlpha; }
    public Icon icon() { return icon; }
    public Divider divider() { return divider; }
    public TextRun value() { return value; }
    public TextRun unit() { return unit; }
    public TextRun extra() { return extra; }
    public Animation animation() { return animation; }

    public CompactHudStatModel data(String k, Object v) { if (k != null && !k.isEmpty()) data.put(k, v); return this; }
    LinkedHashMap<String, Object> dataMap() { return data; }

    public LinkedHashMap<String, Object> toProps() {
        var p = new LinkedHashMap<String, Object>(72);
        p.put("id", id); p.put("visible", visible); p.put("rootX", rootX); p.put("rootY", rootY);
        p.put("width", width); p.put("height", height); p.put("radius", radius); p.put("scale", scale);
        background.writeTo(p); icon.writeTo(p); divider.writeTo(p);
        value.writeTo(p, "value"); unit.writeTo(p, "unit"); extra.writeTo(p, "extra");
        animation.writeTo(p); p.putAll(data); return p;
    }

    public LinkedHashMap<String, Object> toTemplateProps() {
        var p = toProps();
        float rv = CompactHudLayout.reservedValueWidth(this), ru = CompactHudLayout.reservedUnitWidth(this), re = CompactHudLayout.reservedExtraWidth(this);
        p.put("width", width); p.put("valueW", rv); p.put("unitW", ru); p.put("extraW", re);
        p.put("reservedValueWidth", rv); p.put("reservedUnitWidth", ru); p.put("reservedExtraWidth", re);
        p.put("valueText", value.text); p.put("previousValue", animation.previousValue);
        p.put("digitProgress", animation.digitProgress); p.put("digitOffset", animation.digitOffset);
        p.put("effectTime", 0.0f);
        p.put("xText", CompactHudLayout.str(this, "xText", "0")); p.put("yText", CompactHudLayout.str(this, "yText", "0"));
        p.put("zText", CompactHudLayout.str(this, "zText", "0")); p.put("netherText", CompactHudLayout.str(this, "netherText", ""));
        for (String k : new String[]{"fps","ping","tps","usedMiB","maxMiB","percent","bps","displayedBps","x","y","z","netherX","netherZ"}) p.remove(k);
        return p;
    }

    public long structuralSignature() {
        long h = 0xcbf29ce484222325L;
        h = CompactHudCodec.mix(h, id); h = CompactHudCodec.mix(h, visible); h = CompactHudCodec.mix(h, scale);
        h = background.mixStructure(h); h = icon.mixStructure(h); h = divider.mixStructure(h);
        h = value.mixStructure(h); h = unit.mixStructure(h); h = extra.mixStructure(h);
        h = animation.mixStructure(h); h = CompactHudLayout.mixDataShape(this, h); return h;
    }

    public long dynamicSignature() {
        long h = 0xcbf29ce484222325L;
        h = CompactHudCodec.mix(h, icon.color); h = CompactHudCodec.mix(h, divider.color);
        h = value.mixDynamic(h); h = unit.mixDynamic(h); h = extra.mixDynamic(h); h = animation.mixDynamic(h);
        for (var e : data.entrySet()) { h = CompactHudCodec.mix(h, e.getKey()); h = CompactHudCodec.mixObject(h, e.getValue()); }
        return h;
    }

    public long treeSignature() { return structuralSignature(); }

    public long layoutSignature(long s) {
        long h = s;
        h = CompactHudCodec.mix(h, rootX); h = CompactHudCodec.mix(h, rootY);
        h = CompactHudCodec.mix(h, CompactHudLayout.reservedRootWidth(this));
        h = CompactHudCodec.mix(h, height); h = CompactHudCodec.mix(h, radius);
        h = background.mixLayout(h); h = icon.mixLayout(h); h = divider.mixLayout(h);
        h = value.mixLayout(h, CompactHudLayout.reservedValueWidth(this));
        h = unit.mixLayout(h, CompactHudLayout.reservedUnitWidth(this));
        h = extra.mixLayout(h, CompactHudLayout.reservedExtraWidth(this)); return h;
    }

    public long layoutSignature(long s, long ignored) { return layoutSignature(s); }
    public long layoutSignature() { return layoutSignature(structuralSignature()); }

    public LinkedHashMap<String, LinkedHashMap<String, Object>> runtimePatches() { return CompactHudLayout.runtimePatches(this); }
    public LinkedHashMap<String, UiBounds> runtimeBoundsPatches() { return CompactHudLayout.runtimeBounds(this); }

    public static final class Background extends CompactHudParts.Background {}
    public static final class Icon extends CompactHudParts.Icon {}
    public static final class Divider extends CompactHudTextParts.Divider {}
    public static final class TextRun extends CompactHudTextParts.TextRun {}
    public static final class Animation extends CompactHudTextParts.Animation {}
}
