/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi.pipeline;

/**
 * High-level render domain used by pass compilers and profilers.
 */
public enum PipelineDomain {
    WORLD,
    UI,
    FULLSCREEN,
    TEXT,
    POSTPROCESS,
    UNKNOWN
}
