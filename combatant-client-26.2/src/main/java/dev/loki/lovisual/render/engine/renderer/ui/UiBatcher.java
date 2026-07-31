/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui;

import dev.loki.lovisual.render.engine.command.UiCommand;
import dev.loki.lovisual.render.engine.command.UiCommandBuffer;
import dev.loki.lovisual.render.engine.command.UiCommandKind;

public final class UiBatcher {
    public UiBatchPlan compile(UiCommandBuffer commands) {
        if (commands == null || commands.size() == 0) {
            return UiBatchPlan.EMPTY;
        }
        int shapes = 0;
        int paths = 0;
        int textures = 0;
        int text = 0;
        int items = 0;
        int effects = 0;
        int backend = 0;
        for (UiCommand command : commands.commands()) {
            UiCommandKind kind = command.kind();
            switch (kind) {
                case SHAPE -> shapes++;
                case PATH -> paths++;
                case TEXTURE -> textures++;
                case TEXT -> text++;
                case ITEM -> items++;
                case BLUR_REGION, LIQUID_GLASS_REGION, EFFECT_REGION -> effects++;
                case PRIMITIVE -> backend++;
            }
        }
        UiBatchPlan plan = new UiBatchPlan(commands.size(), shapes, paths, textures, text, items, effects, backend);
        commands.stats().addCompiledBatches(plan.batchCount());
        return plan;
    }
}
