/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.api.v0.addon;

import dev.loki.lovisual.events.meta.UsedImplicitly;
import dev.loki.lovisual.features.module.Module;

/**
 * Early addon context used before LoVisual's built-in modules are instantiated.
 *
 * <p>Only enabled addons receive this callback. Exclusions are startup-only: changing
 * an addon's enabled state after module discovery requires a client restart for the
 * built-in module set to be rebuilt.</p>
 */
public interface LoVisualModuleLoadContext {
    @UsedImplicitly
    String addonId();

    /**
     * Prevents an auto-loaded built-in module from being instantiated.
     *
     * <p>The reference may be a {@code ModuleInfo.id}, module alias, simple class name,
     * or fully-qualified class name.</p>
     *
     * @return {@code true} when the exclusion was accepted for the current startup;
     *         {@code false} when the reference is invalid or module discovery has already started
     */
    @UsedImplicitly
    boolean disableBuiltInModule(String moduleReference);

    /**
     * Class-based convenience overload for addons that directly depend on a built-in module type.
     */
    @UsedImplicitly
    default boolean disableBuiltInModule(Class<? extends Module> moduleType) {
        return moduleType != null && disableBuiltInModule(moduleType.getName());
    }
}
