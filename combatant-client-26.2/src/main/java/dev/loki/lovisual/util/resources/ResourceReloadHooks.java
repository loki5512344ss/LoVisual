/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.resources;

import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.gui.clickgui.settings.I18nDuplicateScanner;
import dev.loki.lovisual.features.gui.clickgui.settings.I18nPreflightManager;
import net.minecraft.server.packs.resources.ResourceManager;
import dev.loki.lovisual.features.module.modules.visuals.NameTags;
import dev.loki.lovisual.render.engine.TextureStorage;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.prewarm.RenderPrewarmManager;
import dev.loki.lovisual.render.engine.svg.SvgRegistry;
import dev.loki.lovisual.render.engine.text.Fonts;
import dev.loki.lovisual.render.engine.visuals.LoVisualVisuals;
import dev.loki.lovisual.render.iris.IrisRuntime;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.util.media.MediaSessionService;
import dev.loki.lovisual.util.wav.CustomSoundEngine;

public enum ResourceReloadHooks {
    ;

    public static void onReload(ResourceManager manager) {
        if (manager == null) return;

        try {
            // Refresh resource-backed systems
            Fonts.refresh();
            TextureStorage.preload();
            SvgRegistry.reload(manager);
            CustomSoundEngine.get().reset();
            I18nDuplicateScanner.scan(manager, "resource reload");
            I18nPreflightManager.preflight("resource reload");

            // Clear cached font renderers that keep old textures
            NameTags tags = Modules.get(NameTags.class);
            if (tags != null) {
                tags.onResourceReload();
            }

            // Reset dynamic media textures (e.g. artwork) so they re-register.
            MediaSessionService.get().onResourceReload();
            LoVisualVisuals.onResourceReload(manager);

            try {
                IrisRuntime.registerLoVisualPipelines();
                LoVisualRenderPipelines.precompile(manager);
                RenderPrewarmManager.prewarm("shader reload complete");
            } catch (Throwable t) {
                DebugLog.error("[LoVisual] Shader precompile failed after resource reload", t);
            }
        } finally {
            RenderResourceReadiness.markReady("shader reload complete");
        }

    }
}
