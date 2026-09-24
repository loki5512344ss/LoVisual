package dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland;

import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.controls.IslandControls;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiBounds;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.patch.UiBoundsPatchSet;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.script.render.UiScriptProps;
import net.minecraft.util.Util;

import java.util.Map;

import static dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.IslandPatchOps.*;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.IslandPatchOps;

/**
 * Bounds-патчи узлов UI-скрипта Dynamic Island по режимам (time/pvp/music/clickgui).
 */
public final class IslandBoundsPatches {

    public static final float CONTEXT_GAP = 7f;
    public static final float CONTEXT_CHIP_H = 20f;
    public static final float CONTEXT_CHIP_Y = 7.5f;
    public static final float PVP_CONTEXT_WIDTH = 46f;

    private IslandBoundsPatches() {
    }

    public static Map<String, UiBounds> islandBoundsPatches(UiScriptProps props,
                                                            float rootX,
                                                            float rootY,
                                                            IslandControls controls) {
        UiBoundsPatchSet patches = UiBoundsPatchSet.create(48);
        float width = numberProp(props, "width", 0.0f);
        float height = numberProp(props, "height", 0.0f);
        float mainX = numberProp(props, "mainX", 0.0f);
        float mainWidth = numberProp(props, "mainWidth", width);
        float leftContextWidth = numberProp(props, "leftContextWidth", 0.0f);
        float rightContextWidth = numberProp(props, "rightContextWidth", 0.0f);
        String mode = stringProp(props, "mode", "");
        boolean musicMode = "music".equals(mode);
        boolean clickGuiMode = "clickgui".equals(mode);

        putBounds(patches, "dynamic-island", rootX, rootY, width, height);
        putBounds(patches, "content", rootX, rootY, width, height);

        if (musicMode || clickGuiMode) {
            putBounds(patches, "shell:blur", rootX + mainX, rootY, mainWidth, height);
            putBounds(patches, "shell:shadow", rootX + mainX, rootY, mainWidth, height);
            putBounds(patches, "shell:fill", rootX + mainX, rootY, mainWidth, height);
            putBounds(patches, "shell:tint", rootX + mainX, rootY, mainWidth, height);
            putBounds(patches, "shell:stroke", rootX + mainX, rootY, mainWidth, height);
        }

        if (musicMode || clickGuiMode) {
            putBounds(patches, "context:time:blur", rootX, rootY + CONTEXT_CHIP_Y, leftContextWidth, CONTEXT_CHIP_H);
            putBounds(patches, "context:time:box", rootX, rootY + CONTEXT_CHIP_Y, leftContextWidth, CONTEXT_CHIP_H);
            putBounds(patches, "context:time", rootX, rootY + 10.1f, leftContextWidth, 14.0f);
            float rightX = mainX + mainWidth + CONTEXT_GAP;
            putBounds(patches, "context:pvp:blur", rootX + rightX, rootY + CONTEXT_CHIP_Y, rightContextWidth, CONTEXT_CHIP_H);
            putBounds(patches, "context:pvp:box", rootX + rightX, rootY + CONTEXT_CHIP_Y, rightContextWidth, CONTEXT_CHIP_H);
            putBounds(patches, "context:pvp", rootX + rightX, rootY + 10.1f, rightContextWidth, 14.0f);
        }

        if (musicMode) {
            putMusicCompactBounds(patches, rootX, rootY, props, mainX, mainWidth);
            putMusicExpandedBounds(patches, rootX, rootY, props, mainWidth, controls);
        } else if ("pvp".equals(mode)) {
            float chipW = Math.min(mainWidth - 16.0f, Math.max(42.0f, PVP_CONTEXT_WIDTH + 8.0f));
            float chipX = mainX + (mainWidth - chipW) * 0.5f;
            putBounds(patches, "pvp:chip", rootX + chipX, rootY + CONTEXT_CHIP_Y, chipW, CONTEXT_CHIP_H);
            putBounds(patches, "pvp:timer", rootX + chipX, rootY + 10.1f, chipW, 14.0f);
        } else {
            putBounds(patches, "time:box", rootX + mainX, rootY + CONTEXT_CHIP_Y, mainWidth, CONTEXT_CHIP_H);
            putBounds(patches, "time", rootX + mainX, rootY + 7.8f, mainWidth, 20.0f);
        }

        return patches.asMap();
    }

    private static void putMusicCompactBounds(UiBoundsPatchSet patches,
                                              float rootX,
                                              float rootY,
                                              UiScriptProps props,
                                              float mainX,
                                              float width) {
        float waveX = mainX + width - 26.0f;
        putBounds(patches, "music:elapsed:compact", rootX + mainX + 11.0f, rootY + 10.5f, 38.0f, 13.0f);
        putBounds(patches, "music:divider", rootX + mainX + 49.0f, rootY + 7.0f, 1.0f, 18.0f);
        putBounds(patches, "artwork:compact", rootX + mainX + 58.0f, rootY + 6.5f, 22.0f, 22.0f);
        putBounds(patches, "artwork:compact:fallback", rootX + mainX + 58.0f, rootY + 6.5f, 22.0f, 22.0f);
        float titleW = Math.max(0.0f, width - 124.0f);
        putBounds(patches, "music:title:compact:clip", rootX + mainX + 89.0f, rootY + 8.5f, titleW, 18.0f);
        putBounds(patches, "music:title:compact", rootX + mainX + 89.0f, rootY + 8.5f, titleW, 18.0f);

        float centerY = 17.5f;
        float t = Util.getMillis() / 1000.0f;
        boolean playing = boolProp(props, "playing", false);
        for (int i = 0; i < 3; i++) {
            float barH = playing
                    ? (float) (3.0f + Math.abs(Math.sin(t * 5.2f + i * 0.85f)) * 8.0f)
                    : 4.0f + i;
            putBounds(patches, "wave:" + i, rootX + waveX + i * 5.0f, rootY + centerY - barH * 0.5f, 2.5f, barH);
        }
    }

    private static void putMusicExpandedBounds(UiBoundsPatchSet patches,
                                               float rootX,
                                               float rootY,
                                               UiScriptProps props,
                                               float width,
                                               IslandControls controls) {
        float mainX = numberProp(props, "mainX", 0.0f);
        float progressW = Math.max(0.0f, width - 26.0f);
        putBounds(patches, "artwork:expanded", rootX + mainX + 13.0f, rootY + 11.0f, 36.0f, 36.0f);
        putBounds(patches, "artwork:expanded:fallback", rootX + mainX + 13.0f, rootY + 11.0f, 36.0f, 36.0f);
        float titleW = Math.max(0.0f, width - 72.0f);
        putBounds(patches, "music:title:expanded:clip", rootX + mainX + 59.0f, rootY + 11.0f, titleW, 16.0f);
        putBounds(patches, "music:title:expanded", rootX + mainX + 59.0f, rootY + 11.0f, titleW, 16.0f);
        putBounds(patches, "music:artist:expanded:clip", rootX + mainX + 59.0f, rootY + 28.0f, titleW, 14.0f);
        putBounds(patches, "music:artist:expanded", rootX + mainX + 59.0f, rootY + 28.0f, titleW, 14.0f);
        putBounds(patches, "music:progress:bg", rootX + mainX + 13.0f, rootY + 52.0f, progressW, 5.0f);
        putBounds(patches, "music:progress:fill", rootX + mainX + 13.0f, rootY + 52.0f, progressW, 5.0f);
        putBounds(patches, "music:elapsed:expanded", rootX + mainX + 13.0f, rootY + 65.0f, 44.0f, 14.0f);
        putBounds(patches, "music:total:expanded", rootX + mainX + width - 57.0f, rootY + 65.0f, 44.0f, 14.0f);

        if (boolProp(props, "showShuffle", false)) {
            IslandPatchOps.putControlBounds(patches, "music:shuffle", controls.shuffleX, controls.shuffleY, controls.controlSize);
        }
        IslandPatchOps.putControlBounds(patches, "music:prev", controls.prevX, controls.prevY, controls.controlSize);
        IslandPatchOps.putControlBounds(patches, "music:play", controls.playX, controls.playY, controls.controlSize);
        IslandPatchOps.putControlBounds(patches, "music:next", controls.nextX, controls.nextY, controls.controlSize);
        if (boolProp(props, "showRepeat", false)) {
            IslandPatchOps.putSvgControlBounds(patches, "music:repeat", controls.repeatX, controls.repeatY, controls.controlSize);
        }
    }
}
