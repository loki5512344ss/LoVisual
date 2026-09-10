/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.postprocess.graph;

import dev.loki.lovisual.render.engine.core.frame.RenderFrameContext;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;
import dev.loki.lovisual.render.engine.rhi.resource.LoVisualRhi;

import java.util.Set;
import dev.loki.lovisual.render.engine.postprocess.graph.resource.PostProcessGraphResources;
import dev.loki.lovisual.render.engine.postprocess.graph.resource.PostProcessResolution;
import dev.loki.lovisual.render.engine.postprocess.graph.resource.PostProcessResource;

public interface PostProcessGraphPass {
    String id();

    default String getId() {
        return id();
    }

    int priority();

    PostProcessPass.Phase phase();

    Set<PostProcessResource> reads();

    Set<PostProcessResource> writes();

    PostProcessResolution resolution();

    boolean enabled(RenderFrameContext context);

    /**
     * Returns true when the pass wrote its destination and the graph should advance ping-pong state.
     */
    boolean execute(RenderFrameContext context, LoVisualRhi rhi, PostProcessGraphResources resources);
}
