package dev.loki.lovisual.render.engine.renderer.ui.item;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import static dev.loki.lovisual.render.engine.renderer.Renderer2D.ITEM_OVERLAY_COOLDOWN;
import static dev.loki.lovisual.render.engine.renderer.Renderer2D.ITEM_OVERLAY_COUNT;
import static dev.loki.lovisual.render.engine.renderer.Renderer2D.ITEM_OVERLAY_DURABILITY;
import static dev.loki.lovisual.render.engine.renderer.Renderer2D.ITEM_OVERLAY_DURABILITY_TEXT;

final class ItemBatchOverlays {
    private final int trackColor = 0x72000000;
    private final int trackHighlight = 0x26FFFFFF;
    boolean hasDurabilityBar(ItemDrawCommand c) {
        return (c.overlayFlags & ITEM_OVERLAY_DURABILITY) != 0
                && (c.overlayFlags & ITEM_OVERLAY_DURABILITY_TEXT) == 0 && isDurabilityVisible(c);
    }
    boolean hasDurabilityText(ItemDrawCommand c) { return (c.overlayFlags & ITEM_OVERLAY_DURABILITY_TEXT) != 0 && isDurabilityVisible(c); }
    boolean isDurabilityVisible(ItemDrawCommand c) {
        ItemStack s = c.stack;
        if (s == null || s.isEmpty() || !s.isDamageableItem() || !s.isBarVisible()) return false;
        return durabilityPercentInt(s) <= Mth.clamp(c.durabilityThresholdPercent, 0, 100);
    }
    boolean hasCooldownOverlay(Minecraft mc, ItemDrawCommand c) {
        if ((c.overlayFlags & ITEM_OVERLAY_COOLDOWN) == 0) return false;
        LocalPlayer p = mc.player;
        float prog = p == null ? 0.0f : p.getCooldowns().getCooldownPercent(c.stack, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        return prog > 0.0f;
    }
    void appendDurabilityBar(ItemDrawCommand cmd, MeshBuilder glowMesh, MeshBuilder roundedMesh, ItemBatchGeometry g) {
        ItemStack stack = cmd.stack;
        float scale = cmd.scaleX;
        float barX = cmd.x + 2.0f * scale; float barY = cmd.y + 13.15f * scale;
        float barW = 13.0f * scale; float barH = Math.max(1.35f * scale, 1.0f);
        float radius = Math.max(0.85f * scale, barH * 0.5f);
        float pct = g.durabilityPercent(stack);
        float fillW = Mth.clamp(barW * pct, 0.0f, barW);
        if (fillW <= 0.01f) return;
        int base = ARGB.opaque(stack.getBarColor());
        int bright = g.adjustRgb(base, 1.18f); int deep = g.adjustRgb(base, 0.74f);
        int glow = g.multiplyAlpha(g.withAlpha(base, Mth.clamp(96 + Math.round((1.0f - pct) * 48.0f), 96, 144)), cmd.alpha);
        int innerGlow = g.multiplyAlpha(g.withAlpha(g.adjustRgb(base, 1.35f), 118), cmd.alpha);
        int trackTop = g.multiplyAlpha(trackHighlight, cmd.alpha);
        int trackBottom = g.multiplyAlpha(trackColor, cmd.alpha);
        int brightAlpha = g.multiplyAlpha(bright, cmd.alpha); int deepAlpha = g.multiplyAlpha(deep, cmd.alpha);
        g.appendRoundedGlowQuad(glowMesh, barX, barY, fillW, barH, radius, Math.max(3.0f * scale, 2.0f), glow);
        g.appendRoundedRectQuad(roundedMesh, barX, barY, barW, barH, barX, barY, barW, barH, radius, trackTop, trackTop, trackBottom, trackBottom);
        g.appendRoundedRectQuad(roundedMesh, barX, barY, fillW, barH, barX, barY, barW, barH, radius, brightAlpha, brightAlpha, deepAlpha, deepAlpha);
        g.appendRoundedRectQuad(roundedMesh, barX, barY, Math.min(fillW, Math.max(1.2f * scale, fillW * 0.28f)), barH,
                barX, barY, barW, barH, radius, innerGlow,
                g.multiplyAlpha(g.withAlpha(innerGlow, 70), cmd.alpha),
                g.multiplyAlpha(g.withAlpha(innerGlow, 34), cmd.alpha),
                g.multiplyAlpha(g.withAlpha(innerGlow, 58), cmd.alpha));
    }
    void appendCooldownQuads(Minecraft mc, ItemDrawCommand cmd, MeshBuilder mesh, ItemBatchGeometry g) {
        if ((cmd.overlayFlags & ITEM_OVERLAY_COOLDOWN) == 0) return;
        LocalPlayer p = mc.player;
        float prog = p == null ? 0.0f : p.getCooldowns().getCooldownPercent(cmd.stack, mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
        if (prog > 0.0f) {
            float scale = cmd.scaleX;
            float top = cmd.y + Mth.floor(16.0f * (1.0f - prog)) * scale;
            float h = Mth.ceil(16.0f * prog) * scale;
            g.appendQuad(mesh, cmd.x, top, 16.0f * scale, h, g.multiplyAlpha(Integer.MAX_VALUE, cmd.alpha));
        }
    }
    @Nullable String getOverlayText(ItemDrawCommand c) { return getOverlayText(c.stack, c.overlayFlags, c.stackCountText); }
    @Nullable String getOverlayText(ItemStack s, int flags, @Nullable String txt) {
        if (s == null || s.isEmpty()) return null;
        if ((flags & ITEM_OVERLAY_COUNT) != 0 && (s.getCount() != 1 || txt != null)) return txt == null ? String.valueOf(s.getCount()) : txt;
        return null;
    }
    @Nullable String getDurabilityText(ItemDrawCommand c) { if (!hasDurabilityText(c)) return null; return durabilityPercentInt(c.stack) + "%"; }
    int getDurabilityTextColor(ItemDrawCommand c) {
        int pct = durabilityPercentInt(c.stack);
        int thr = Mth.clamp(c.durabilityTextColorThresholdPercent, 0, 100);
        return pct <= thr ? ARGB.opaque(c.stack.getBarColor()) : 0xFFFFFFFF;
    }
    int handleOverlays(dev.loki.lovisual.render.engine.renderer.ui.batches.ItemBatch batch, Minecraft mc, ItemBatchMeshes meshes, ItemBatchGeometry g) {
        MeshBuilder glow = null, rounded = null, cooldown = null;
        for (ItemDrawCommand c : batch.commands) {
            if (hasDurabilityBar(c)) {
                if (glow == null) glow = meshes.beginDurabilityGlowMesh(batch.commands.size());
                if (rounded == null) rounded = meshes.beginDurabilityRoundedMesh(batch.commands.size());
                appendDurabilityBar(c, glow, rounded, g);
            }
            if (hasCooldownOverlay(mc, c)) {
                if (cooldown == null) cooldown = meshes.beginCooldownMesh(batch.commands.size());
                appendCooldownQuads(mc, c, cooldown, g);
            }
        }
        int calls = 0;
        if (meshes.submitOverlayMesh(mc, LoVisualRenderPipelines.UI_ROUNDED_GLOW_BATCH, glow, true)) calls++;
        if (meshes.submitOverlayMesh(mc, LoVisualRenderPipelines.UI_ROUNDED_BATCH, rounded, true)) calls++;
        if (meshes.submitOverlayMesh(mc, LoVisualRenderPipelines.UI_COLORED, cooldown, false)) calls++;
        return calls;
    }
    void renderTexts(dev.loki.lovisual.render.engine.renderer.ui.batches.ItemBatch batch, ItemBatchGeometry g) {
        TextRenderer r = null; float scale = Float.NaN;
        try {
            for (ItemDrawCommand c : batch.commands) {
                if (hasDurabilityText(c)) {
                    String t = getDurabilityText(c);
                    if (t != null && !t.isEmpty()) {
                        float ts = Math.max(0.0001f, c.scaleX * 0.44f);
                        if (r == null) r = TextRenderer.get();
                        if (!r.isBuilding() || Float.compare(scale, ts) != 0) {
                            if (r.isBuilding()) r.end(); r.begin(ts, false, false); scale = ts;
                        }
                        float w = (float) r.getWidth(t, false);
                        float x = c.x + (16.0f * c.scaleX - w) * 0.5f; float y = c.y + 11.0f * c.scaleX;
                        r.render(t, x, y, new RenderColor(g.multiplyAlpha(getDurabilityTextColor(c), c.alpha)), true);
                    }
                }
                String ov = getOverlayText(c);
                if (ov == null || ov.isEmpty()) continue;
                float ts = Math.max(0.0001f, c.scaleX * 0.5f);
                if (r == null) r = TextRenderer.get();
                if (!r.isBuilding() || Float.compare(scale, ts) != 0) {
                    if (r.isBuilding()) r.end(); r.begin(ts, false, false); scale = ts;
                }
                float x = c.x + 19.0f * c.scaleX - 2.0f * c.scaleX - (float) r.getWidth(ov, false);
                float y = c.y + 9.0f * c.scaleX;
                r.render(ov, x, y, new RenderColor(g.multiplyAlpha(0xFFFFFFFF, c.alpha)), true);
            }
        } finally { if (r != null && r.isBuilding()) r.end(); }
    }
    private float durabilityPercent(ItemStack s) {
        int max = s.getMaxDamage(); if (max <= 0) return 1.0f;
        return Mth.clamp((max - s.getDamageValue()) / (float) max, 0.0f, 1.0f);
    }
    private int durabilityPercentInt(ItemStack s) { return Math.round(durabilityPercent(s) * 100.0f); }
}
