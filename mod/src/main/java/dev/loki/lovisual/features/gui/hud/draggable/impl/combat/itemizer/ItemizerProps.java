/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.script.HudScriptLayouts;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.math.ColorMath;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.module.UiScriptModule;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.runtime.CachedUiScriptRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ItemizerProps {

    private ItemizerProps() {}

    static UiScriptModule ensureModule(Itemizer host) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getResourceManager() == null) return null;
        HudScriptLayouts.pollReloadCombo(mc);
        if (host.moduleHandle.consumeChanged()) {
            host.runtime.reset();
        }
        if (!host.moduleHandle.ensureLoaded(mc.getResourceManager())) {
            HudScriptLayouts.reportLoadError(host.moduleHandle);
            return null;
        }
        host.moduleHandle.consumeChanged();
        return host.moduleHandle.module();
    }

    static LinkedHashMap<String, Object> props(Itemizer host, List<Itemizer.Entry> visible, float width, float height, float baseScale, float drawScale) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("id", "itemizer");
        out.put("width", width);
        out.put("height", height);
        out.put("baseScale", baseScale);
        out.put("drawScale", drawScale);
        out.put("mode", host.displayMode.get().id());
        out.put("direction", host.direction.get().id());
        out.put("entryWidth", ItemizerModel.baseEntryWidth(host) * baseScale);
        out.put("entryHeight", ItemizerModel.baseEntryHeight(host) * baseScale);
        out.put("gap", Itemizer.BASE_GAP * baseScale);
        out.put("blur", host.blur.get());
        out.put("palette", paletteProps(host));
        List<LinkedHashMap<String, Object>> itemProps = new ArrayList<>();
        for (Itemizer.Entry entry : visible) {
            itemProps.add(entryProps(host, entry));
        }
        out.put("items", itemProps.toArray());
        return out;
    }

    static LinkedHashMap<String, Object> paletteProps(Itemizer host) {
        Themes.Theme t = Theme.theme();
        int alpha = host.bgAlpha.get();
        int bg1 = HudRenderUtil.setAlpha(HudRenderUtil.mixColor(t.windowBg(), t.surface(), 0.24f), alpha);
        int bg2 = HudRenderUtil.setAlpha(HudRenderUtil.mixColor(t.surface(), 0xFF000000, 0.22f), Math.min(255, alpha + 18));
        int stroke = HudRenderUtil.setAlpha(HudRenderUtil.mixColor(t.windowStroke(), t.accentSoft(), 0.22f), 176);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("bg1", hex(bg1));
        out.put("bg2", hex(bg2));
        out.put("stroke", hex(stroke));
        out.put("text", hex(t.textPrimary()));
        out.put("muted", hex(t.textMuted()));
        out.put("accent", hex(t.accent()));
        out.put("glow", hex(HudRenderUtil.setAlpha(t.accent(), 155)));
        out.put("blurAlpha", Math.min(1.0f, alpha / 255.0f));
        return out;
    }

    static LinkedHashMap<String, Object> entryProps(Itemizer host, Itemizer.Entry entry) {
        ItemStack stack = entry.stack != null ? entry.stack : ItemStack.EMPTY;
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("key", entry.event.id());
        out.put("kind", entry.event.id());
        out.put("label", entry.label);
        out.put("stack", stack.copy());
        out.put("item", stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        out.put("count", stack.isEmpty() ? 1 : stack.getCount());
        out.put("damage", stack.isEmpty() ? 0 : stack.getDamageValue());
        out.put("maxDamage", stack.isEmpty() ? 0 : stack.getMaxDamage());
        out.put("alpha", AnimationUtility.clamp01(entry.anim));
        out.put("life", entry.totalLife <= 0.0f ? 1.0f : AnimationUtility.clamp01(entry.life / entry.totalLife));
        out.put("pulse", AnimationUtility.clamp01(entry.punch));
        out.put("accent", hex(entryAccent(host, entry.event)));
        return out;
    }

    static int entryAccent(Itemizer host, Itemizer.ItemizerEvent event) {
        Themes.Theme t = Theme.theme();
        int target = switch (event) {
            case AUTO_EAT -> 0xFF74E083;
            case AUTO_TOTEM -> 0xFFFFD15C;
            case ELYTRA_SWAP -> 0xFF77D7FF;
        };
        return ColorMath.colorWithAlpha(HudRenderUtil.mixColor(t.accent(), target, 0.72f), 255);
    }

    static long signature(Itemizer host, Map<String, Object> props) {
        long h = 0xcbf29ce484222325L;
        h = CachedUiScriptRuntime.mix(h, host.displayMode.get().id());
        h = CachedUiScriptRuntime.mix(h, host.direction.get().id());
        h = CachedUiScriptRuntime.mix(h, host.getWidth());
        h = CachedUiScriptRuntime.mix(h, host.getHeight());
        h = CachedUiScriptRuntime.mix(h, host.bgAlpha.get());
        h = CachedUiScriptRuntime.mix(h, host.blur.get());
        Object itemsValue = props.get("items");
        Object[] items = itemsValue instanceof Object[] arr ? arr : new Object[0];
        h = CachedUiScriptRuntime.mix(h, items.length);
        for (Object itemValue : items) {
            if (!(itemValue instanceof Map<?, ?> item)) continue;
            h = CachedUiScriptRuntime.mix(h, string(item.get("key")));
            h = CachedUiScriptRuntime.mix(h, string(item.get("label")));
            h = CachedUiScriptRuntime.mix(h, string(item.get("item")));
            h = CachedUiScriptRuntime.mix(h, intValue(item.get("count")));
            h = CachedUiScriptRuntime.mix(h, Math.round(floatValue(item.get("alpha")) * 1000.0f));
            h = CachedUiScriptRuntime.mix(h, Math.round(floatValue(item.get("life")) * 1000.0f));
            h = CachedUiScriptRuntime.mix(h, Math.round(floatValue(item.get("pulse")) * 1000.0f));
        }
        return h;
    }

    static String hex(int argb) {
        return String.format("#%08X", argb);
    }

    static String string(Object value) {
        return value instanceof String s ? s : "";
    }

    static int intValue(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    static float floatValue(Object value) {
        return value instanceof Number n ? n.floatValue() : 0.0f;
    }
}
