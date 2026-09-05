/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.config.values.BooleanValue;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.util.animation.SmoothValue;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Smooth appear/disappear animations for interface elements.
 * <p>
 * Every animated quantity is a {@link SmoothValue} whose boolean target is
 * computed and advanced in exactly one place: {@link #onFrame(float)}. Renderer
 * integration points (custom hotbar, container screens) only read the settled
 * per-frame values through the accessors below, so values can never be pulled
 * toward two different goals in the same frame.
 */
@ModuleInfo(
        id = "animations",
        displayName = "Animations",
        aliases = {"animations"},
        category = ModuleCategory.VISUALS,
        description = "module.animations.description"
)
public final class Animations extends Module {

    private static final String SETTING_HOTBAR_RAISE = "hotbar_raise";
    private static final String SETTING_INVENTORY_OPEN = "inventory";

    // Speeds are SmoothValue approach rates in 1/tick units (20 ticks per second).
    // Inventory: ~0.15s to visually settle (0.9 -> ~1.0).
    private static final float INVENTORY_OPEN_SPEED = 1.5f;
    // Hotbar: softer slide-in, ~0.3s.
    private static final float HOTBAR_RAISE_SPEED = 0.5f;

    private static final float INVENTORY_MIN_SCALE = 0.9f;
    private static final float HOTBAR_RAISE_OFFSET_PX = 26.0f;

    private final BooleanValue hotbarRaise =
            bool("animationsHotbarRaise", SETTING_HOTBAR_RAISE, true);
    private final BooleanValue inventoryOpen =
            bool("animationsInventoryOpen", SETTING_INVENTORY_OPEN, true);

    private final SmoothValue hotbarRaiseValue = new SmoothValue(HOTBAR_RAISE_SPEED);
    private final SmoothValue inventoryOpenValue = new SmoothValue(INVENTORY_OPEN_SPEED);

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();

        // If a container screen is already open, start settled so enabling
        // mid-game never pops the screen. Otherwise the open animation plays
        // naturally the next time a screen appears.
        boolean containerOpen = ClientScreen.current(mc) instanceof AbstractContainerScreen<?>;
        inventoryOpenValue.setInstant(containerOpen);

        // Reset the raise so the hotbar slides in when the module turns on.
        hotbarRaiseValue.setInstant(false);
    }

    /**
     * The single update point for every animated value: compute the boolean
     * target from the current game state, then advance each value exactly once.
     */
    @Override
    public void onFrame(float delta) {
        Minecraft mc = Minecraft.getInstance();
        boolean inWorld = mc != null && mc.player != null;
        boolean containerOpen = inWorld
                && ClientScreen.current(mc) instanceof AbstractContainerScreen<?>;

        hotbarRaiseValue.setTarget(inWorld);
        hotbarRaiseValue.update(delta);

        inventoryOpenValue.setTarget(containerOpen);
        inventoryOpenValue.update(delta);
    }

    /**
     * Vertical GUI-pixel offset the custom hotbar should be shifted down by while
     * it slides into place. Reads only; the value is updated in {@link #onFrame(float)}.
     */
    public float getHotbarRaiseOffset() {
        if (!isEnabled() || !hotbarRaise.get()) {
            return 0.0f;
        }
        return HOTBAR_RAISE_OFFSET_PX * (1.0f - hotbarRaiseValue.value());
    }

    /**
     * Scale container screens should be drawn with while they open
     * (1.0 when settled or when the animation is disabled).
     */
    public float getInventoryScale() {
        if (!isEnabled() || !inventoryOpen.get()) {
            return 1.0f;
        }
        return INVENTORY_MIN_SCALE + (1.0f - INVENTORY_MIN_SCALE) * inventoryOpenValue.value();
    }
}
