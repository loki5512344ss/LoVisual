package dev.loki.lovisual.render.engine.renderer.ui.item;

import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.gui.render.GuiItemAtlas;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

final class ItemBatchGeometry {
    float clampRoundedRadius(float radius, double w, double h) {
        return (float) Mth.clamp(radius, 0.0f, (float) Math.min(w, h) * 0.5f);
    }
    void appendItemAtlasBlit(MeshBuilder mesh, ItemDrawCommand command, GuiItemAtlas.SlotView slot) {
        mesh.ensureQuadCapacity();
        double x0; double y0; double x1; double y1; double x2; double y2; double x3; double y3;
        if (command.pivoted) {
            x0 = transformItemX(command, 0.0f); y0 = transformItemY(command, 0.0f);
            x1 = transformItemX(command, 0.0f); y1 = transformItemY(command, 16.0f);
            x2 = transformItemX(command, 16.0f); y2 = transformItemY(command, 16.0f);
            x3 = transformItemX(command, 16.0f); y3 = transformItemY(command, 0.0f);
        } else {
            x0 = command.x; y0 = command.y;
            x1 = command.x; y1 = command.y + 16.0f * command.scaleY;
            x2 = command.x + 16.0f * command.scaleX; y2 = command.y + 16.0f * command.scaleY;
            x3 = command.x + 16.0f * command.scaleX; y3 = command.y;
        }
        int alpha = Mth.clamp(Math.round(command.alpha * 255.0f), 0, 255);
        int i1 = mesh.vec2(x0, y0).raw2(slot.u0(), slot.v0()).color(255, 255, 255, alpha).next();
        int i2 = mesh.vec2(x1, y1).raw2(slot.u0(), slot.v1()).color(255, 255, 255, alpha).next();
        int i3 = mesh.vec2(x2, y2).raw2(slot.u1(), slot.v1()).color(255, 255, 255, alpha).next();
        int i4 = mesh.vec2(x3, y3).raw2(slot.u1(), slot.v0()).color(255, 255, 255, alpha).next();
        mesh.quad(i1, i2, i3, i4);
    }
    double transformItemX(ItemDrawCommand c, float lx) { return c.x + c.pivotX + (lx - c.pivotX) * c.scaleX; }
    double transformItemY(ItemDrawCommand c, float ly) { return c.y + c.pivotY + (ly - c.pivotY) * c.scaleY; }
    void appendRoundedRectQuad(MeshBuilder mesh, double x, double y, double w, double h,
                               double maskX, double maskY, double maskW, double maskH,
                               float radius, int cTopLeft, int cTopRight, int cBottomRight, int cBottomLeft) {
        if (w <= 0.0 || h <= 0.0 || maskW <= 0.0 || maskH <= 0.0) return;
        mesh.ensureQuadCapacity();
        int tlA = (cTopLeft >>> 24) & 0xFF; int tlR = (cTopLeft >>> 16) & 0xFF;
        int tlG = (cTopLeft >>> 8) & 0xFF; int tlB = cTopLeft & 0xFF;
        int trA = (cTopRight >>> 24) & 0xFF; int trR = (cTopRight >>> 16) & 0xFF;
        int trG = (cTopRight >>> 8) & 0xFF; int trB = cTopRight & 0xFF;
        int brA = (cBottomRight >>> 24) & 0xFF; int brR = (cBottomRight >>> 16) & 0xFF;
        int brG = (cBottomRight >>> 8) & 0xFF; int brB = cBottomRight & 0xFF;
        int blA = (cBottomLeft >>> 24) & 0xFF; int blR = (cBottomLeft >>> 16) & 0xFF;
        int blG = (cBottomLeft >>> 8) & 0xFF; int blB = cBottomLeft & 0xFF;
        float cr = clampRoundedRadius(radius, maskW, maskH);
        int i1 = mesh.vec2(x, y).local2(x, y).color(tlR, tlG, tlB, tlA).vec4(maskX, maskY, maskW, maskH).vec4(cr, 0.0f, 0f, 0f).next();
        int i2 = mesh.vec2(x, y + h).local2(x, y + h).color(blR, blG, blB, blA).vec4(maskX, maskY, maskW, maskH).vec4(cr, 0.0f, 0f, 0f).next();
        int i3 = mesh.vec2(x + w, y + h).local2(x + w, y + h).color(brR, brG, brB, brA).vec4(maskX, maskY, maskW, maskH).vec4(cr, 0.0f, 0f, 0f).next();
        int i4 = mesh.vec2(x + w, y).local2(x + w, y).color(trR, trG, trB, trA).vec4(maskX, maskY, maskW, maskH).vec4(cr, 0.0f, 0f, 0f).next();
        mesh.quad(i1, i2, i3, i4);
    }
    void appendRoundedGlowQuad(MeshBuilder mesh, double x, double y, double w, double h,
                               float radius, float glow, int argb) {
        if (w <= 0.0 || h <= 0.0 || glow <= 0.0f) return;
        mesh.ensureQuadCapacity();
        int a = (argb >>> 24) & 0xFF; int r = (argb >>> 16) & 0xFF; int g = (argb >>> 8) & 0xFF; int b = argb & 0xFF;
        float cr = clampRoundedRadius(radius, w, h);
        double gx = x - glow; double gy = y - glow; double gw = w + glow * 2.0; double gh = h + glow * 2.0;
        int i1 = mesh.vec2(gx, gy).local2(gx, gy).color(r, g, b, a).vec4(x, y, w, h).vec4(cr, 0.0f, glow, 0f).next();
        int i2 = mesh.vec2(gx, gy + gh).local2(gx, gy + gh).color(r, g, b, a).vec4(x, y, w, h).vec4(cr, 0.0f, glow, 0f).next();
        int i3 = mesh.vec2(gx + gw, gy + gh).local2(gx + gw, gy + gh).color(r, g, b, a).vec4(x, y, w, h).vec4(cr, 0.0f, glow, 0f).next();
        int i4 = mesh.vec2(gx + gw, gy).local2(gx + gw, gy).color(r, g, b, a).vec4(x, y, w, h).vec4(cr, 0.0f, glow, 0f).next();
        mesh.quad(i1, i2, i3, i4);
    }
    void appendQuad(MeshBuilder mesh, double x, double y, double width, double height, int argb) {
        mesh.ensureQuadCapacity();
        int a = (argb >>> 24) & 0xFF; int r = (argb >>> 16) & 0xFF; int g = (argb >>> 8) & 0xFF; int b = argb & 0xFF;
        int i1 = mesh.vec2(x, y).color(r, g, b, a).next();
        int i2 = mesh.vec2(x, y + height).color(r, g, b, a).next();
        int i3 = mesh.vec2(x + width, y + height).color(r, g, b, a).next();
        int i4 = mesh.vec2(x + width, y).color(r, g, b, a).next();
        mesh.quad(i1, i2, i3, i4);
    }
    float durabilityPercent(ItemStack s) {
        int max = s.getMaxDamage(); if (max <= 0) return 1.0f;
        return Mth.clamp((max - s.getDamageValue()) / (float) max, 0.0f, 1.0f);
    }
    int durabilityPercentInt(ItemStack s) { return Math.round(durabilityPercent(s) * 100.0f); }
    int withAlpha(int argb, int alpha) { return (Mth.clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF); }
    int multiplyAlpha(int argb, float f) { int a = (argb >>> 24) & 0xFF; return withAlpha(argb, Math.round(a * Mth.clamp(f, 0.0f, 1.0f))); }
    int adjustRgb(int argb, float t) {
        int a = (argb >>> 24) & 0xFF;
        int r = Mth.clamp(Math.round(((argb >>> 16) & 0xFF) * t), 0, 255);
        int g = Mth.clamp(Math.round(((argb >>> 8) & 0xFF) * t), 0, 255);
        int b = Mth.clamp(Math.round((argb & 0xFF) * t), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
