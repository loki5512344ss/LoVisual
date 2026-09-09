package dev.loki.lovisual.features.module.modules.visuals.cosmetics;

import dev.loki.lovisual.features.cosmetic.CosmeticSlot;

/**
 * Pure geometry for the clickable body-zone rectangles overlaid on the 3D preview.
 * Zones are fixed fractions of the preview region height (not bone-projected: the
 * inventory-style entity preview renders through its own camera space, so world-camera
 * bone projection doesn't apply here).
 */
public final class CosmeticsHotspot {
    private final CosmeticSlot slot;
    private final float x;
    private final float y;
    private final float width;
    private final float height;

    public CosmeticsHotspot(CosmeticSlot slot, float x, float y, float width, float height) {
        this.slot = slot;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public CosmeticSlot slot() {
        return slot;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    /**
     * Builds the four body-zone hotspots for a preview region. BACK reuses the
     * SHOULDERS rectangle (selected via a separate front/back view toggle in the screen).
     */
    public static CosmeticsHotspot[] forRegion(float regionX, float regionY, float regionW, float regionH, boolean backView) {
        float headH = regionH * 0.20f;
        float shouldersH = regionH * 0.15f;
        float waistH = regionH * 0.15f;
        float legsH = regionH - headH - shouldersH - waistH;

        float y0 = regionY;
        float y1 = y0 + headH;
        float y2 = y1 + shouldersH;
        float y3 = y2 + waistH;

        CosmeticSlot shouldersOrBack = backView ? CosmeticSlot.BACK : CosmeticSlot.SHOULDERS;

        return new CosmeticsHotspot[]{
                new CosmeticsHotspot(CosmeticSlot.HEAD, regionX, y0, regionW, headH),
                new CosmeticsHotspot(shouldersOrBack, regionX, y1, regionW, shouldersH),
                new CosmeticsHotspot(CosmeticSlot.WAIST, regionX, y2, regionW, waistH),
                new CosmeticsHotspot(CosmeticSlot.LEGS, regionX, y3, regionW, legsH)
        };
    }

    public static CosmeticSlot hitTest(CosmeticsHotspot[] hotspots, double mouseX, double mouseY) {
        for (CosmeticsHotspot hotspot : hotspots) {
            if (hotspot.contains(mouseX, mouseY)) return hotspot.slot();
        }
        return null;
    }
}
