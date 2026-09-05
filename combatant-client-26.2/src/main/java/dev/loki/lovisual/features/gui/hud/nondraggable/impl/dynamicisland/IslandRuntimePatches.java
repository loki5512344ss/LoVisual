/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.CachedUiScriptRuntime;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.UiScriptPatchSet;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.UiScriptProps;

import java.util.Map;

import static dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.IslandPatchOps.*;

/**
 * Рантайм-патчи UI-скрипта Dynamic Island (свойства узлов по режимам) и структурная сигнатура.
 */
public final class IslandRuntimePatches {

    private IslandRuntimePatches() {
    }

    public static long islandStructuralSignature(UiScriptProps props) {
        long h = 0xcbf29ce484222325L;
        h = CachedUiScriptRuntime.mix(h, stringProp(props, "mode", ""));
        h = CachedUiScriptRuntime.mix(h, boolProp(props, "blur", false));
        h = CachedUiScriptRuntime.mix(h, numberProp(props, "blurAlpha", 0.0f));
        h = CachedUiScriptRuntime.mix(h, !stringProp(props, "artworkTexture", "").isEmpty());
        h = CachedUiScriptRuntime.mix(h, "music".equals(stringProp(props, "mode", "")));
        h = CachedUiScriptRuntime.mix(h, boolProp(props, "showShuffle", false));
        h = CachedUiScriptRuntime.mix(h, boolProp(props, "showRepeat", false));
        return h;
    }

    public static Map<String, ? extends Map<String, ?>> islandRuntimePatches(UiScriptProps props) {
        UiScriptPatchSet patches = UiScriptPatchSet.create(48);
        float compactAlpha = numberProp(props, "compactAlpha", 0.0f);
        float expandedAlpha = numberProp(props, "expandedAlpha", 0.0f);
        float contextAlpha = numberProp(props, "contextAlpha", compactAlpha);
        float width = numberProp(props, "width", 0.0f);
        float mainWidth = numberProp(props, "mainWidth", width);
        float progress = clamp01(numberProp(props, "progress", 0.0f));
        String mode = stringProp(props, "mode", "");
        boolean musicMode = "music".equals(mode);
        boolean clickGuiMode = "clickgui".equals(mode);
        boolean shellMode = musicMode || clickGuiMode;
        boolean pvpMode = "pvp".equals(mode);
        boolean timeMode = "time".equals(mode);
        String primary = stringProp(props, "textPrimary", "#FFFFFFFF");
        String secondary = stringProp(props, "textSecondary", "#99FFFFFF");
        String muted = stringProp(props, "textMuted", "#66FFFFFF");
        String accent = stringProp(props, "accent", "#FFFFFFFF");
        String accentSoft = stringProp(props, "accentSoft", "#33FFFFFF");

        if (shellMode) {
            float shellBlurAlpha = numberProp(props, "blurAlpha", 0.0f) * numberProp(props, "alpha", 0.0f);
            patchShape(patches, "dynamic-island", "renderRadius", props.get("radius"), "renderBlurAlpha", shellBlurAlpha);
            patchShape(patches, "shell:blur", "blurAlpha", shellBlurAlpha, "radius", props.get("radius"));
            patchShape(patches, "shell:shadow", "fill", props.get("shadow"), "radius", props.get("radius"));
            patchShape(patches, "shell:fill", "startColor", props.get("fillTop"), "endColor", props.get("fillBottom"), "radius", props.get("radius"));
            patchShape(patches, "shell:tint", "startColor", props.get("tintTop"), "endColor", props.get("tintBottom"), "radius", props.get("radius"));
            patchShape(patches, "shell:stroke", "stroke", props.get("stroke"), "radius", props.get("radius"));
        }

        if (timeMode) {
            patchShape(patches, "time:box", "fill", props.get("timeFill"), "stroke", props.get("timeStroke"));
            patchText(patches, "time", stringProp(props, "time", ""), primary);
        }

        if (pvpMode) {
            patchShape(patches, "pvp:chip", "fill", props.get("pvpFill"), "stroke", props.get("pvpStroke"));
            patchText(patches, "pvp:timer", stringProp(props, "pvpTimer", "0s"), primary);
        }

        if (musicMode || clickGuiMode) {
            float contextBlurAlpha = numberProp(props, "blurAlpha", 0.0f) * contextAlpha;
            patchShape(patches, "context:time:blur", "blurAlpha", contextBlurAlpha, "radius", 10.0f);
            patchShape(patches, "context:time:box", "fill", scaleHexAlpha(stringProp(props, "contextFill", "#00000000"), contextAlpha), "stroke", scaleHexAlpha(stringProp(props, "contextStroke", "#00000000"), contextAlpha));
            patchText(patches, "context:time", stringProp(props, "time", ""), scaleHexAlpha(primary, contextAlpha));
            float pvpContextAlpha = boolProp(props, "pvpContextVisible", false) ? contextAlpha : 0.0f;
            patchShape(patches, "context:pvp:blur", "blurAlpha", numberProp(props, "blurAlpha", 0.0f) * pvpContextAlpha, "radius", 10.0f);
            patchShape(patches, "context:pvp:box", "fill", scaleHexAlpha(stringProp(props, "pvpFill", "#00000000"), pvpContextAlpha), "stroke", scaleHexAlpha(stringProp(props, "pvpStroke", "#00000000"), pvpContextAlpha));
            patchText(patches, "context:pvp", stringProp(props, "pvpTimer", ""), scaleHexAlpha(primary, pvpContextAlpha));
        }

        if (musicMode) {
            patchText(patches, "music:elapsed:compact", stringProp(props, "elapsed", "0:00"), scaleHexAlpha(secondary, compactAlpha));
            patchText(patches, "music:title:compact", stringProp(props, "title", ""), scaleHexAlpha(primary, compactAlpha));
            patchClippedText(patches, "music:title:compact", numberProp(props, "titleWidthCompact", 0.0f), Math.max(0.0f, mainWidth - 124.0f), numberProp(props, "titleScrollTime", 0.0f), 1.0f, 18.0f, 15.0f);
            patchShape(patches, "music:divider", "fill", scaleHexAlpha(muted, 0.4f * compactAlpha));
            patchShape(patches, "wave:0", "fill", scaleHexAlpha(accent, 0.75f * compactAlpha));
            patchShape(patches, "wave:1", "fill", scaleHexAlpha(accent, 0.83f * compactAlpha));
            patchShape(patches, "wave:2", "fill", scaleHexAlpha(accent, 0.91f * compactAlpha));
            patchImage(patches, "artwork:compact", stringProp(props, "artworkTexture", ""), scaleHexAlpha("#FFFFFFFF", compactAlpha));
            patchShape(patches, "artwork:compact:fallback", "fill", scaleHexAlpha(accent, 0.92f * compactAlpha), "stroke", scaleHexAlpha("#33FFFFFF", compactAlpha));
        }

        if (musicMode) {
            patchText(patches, "music:title:expanded", stringProp(props, "title", ""), scaleHexAlpha(primary, expandedAlpha));
            patchClippedText(patches, "music:title:expanded", numberProp(props, "titleWidthExpanded", 0.0f), Math.max(0.0f, mainWidth - 72.0f), numberProp(props, "titleScrollTime", 0.0f), 1.0f, 18.0f, 16.0f);
            patchText(patches, "music:artist:expanded", stringProp(props, "artist", ""), scaleHexAlpha(secondary, expandedAlpha));
            patchClippedText(patches, "music:artist:expanded", numberProp(props, "artistWidthExpanded", 0.0f), Math.max(0.0f, mainWidth - 72.0f), numberProp(props, "titleScrollTime", 0.0f), 1.4f, 14.0f, 16.0f);
            patchShape(patches, "music:progress:bg", "fill", scaleHexAlpha(stringProp(props, "progressBg", "#00FFFFFF"), expandedAlpha));
            patchShape(patches, "music:progress:fill",
                    "fill", scaleHexAlpha(accent, expandedAlpha),
                    "renderWidth", Math.max(0.0f, mainWidth - 26.0f) * progress);
            patchText(patches, "music:elapsed:expanded", stringProp(props, "elapsed", "0:00"), scaleHexAlpha(secondary, expandedAlpha));
            patchText(patches, "music:total:expanded", stringProp(props, "total", "0:00"), scaleHexAlpha(secondary, expandedAlpha));
            patchImage(patches, "artwork:expanded", stringProp(props, "artworkTexture", ""), scaleHexAlpha("#FFFFFFFF", expandedAlpha));
            patchShape(patches, "artwork:expanded:fallback", "fill", scaleHexAlpha(accent, 0.92f * expandedAlpha), "stroke", scaleHexAlpha("#33FFFFFF", expandedAlpha));
            if (boolProp(props, "showShuffle", false)) {
                patchControl(patches, "music:shuffle", stringProp(props, "iconShuffle", ""), stringProp(props, "shuffleColor", primary), accentSoft, expandedAlpha, numberProp(props, "shuffleHover", 0.0f));
            }
            patchControl(patches, "music:prev", stringProp(props, "iconPrev", ""), primary, accentSoft, expandedAlpha, numberProp(props, "prevHover", 0.0f));
            patchControl(patches, "music:play", stringProp(props, "iconPlay", ""), primary, accentSoft, expandedAlpha, numberProp(props, "playHover", 0.0f));
            patchControl(patches, "music:next", stringProp(props, "iconNext", ""), primary, accentSoft, expandedAlpha, numberProp(props, "nextHover", 0.0f));
            if (boolProp(props, "showRepeat", false)) {
                patchSvgControl(patches, "music:repeat", stringProp(props, "repeatAsset", "repeat-off"), stringProp(props, "repeatColor", "#99000000"), accentSoft, expandedAlpha, numberProp(props, "repeatHover", 0.0f));
            }
        }

        return patches.asMap();
    }
}
