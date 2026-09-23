/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.events.impl.entity;

import net.minecraft.world.entity.LivingEntity;
import dev.loki.lovisual.events.Event;
import dev.loki.lovisual.util.target.TargetManager;

/**
 * Fired when the centralized target changes.
 */
public class EventTargetChanged extends Event {
    public final LivingEntity previous;
    public final TargetManager.Source previousSource;
    public final LivingEntity current;
    public final TargetManager.Source currentSource;

    public EventTargetChanged(LivingEntity previous,
                              TargetManager.Source previousSource,
                              LivingEntity current,
                              TargetManager.Source currentSource) {
        this.previous = previous;
        this.previousSource = previousSource;
        this.current = current;
        this.currentSource = currentSource;
    }
}
