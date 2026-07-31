/*
 * This file is part of the LoVisual Client distribution.
 * LoVisual modifications copyright (c) 2026 pivosos2007.
 *
 * Portions of this file are based on LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce).
 * Copyright (c) 2015-2026 CCBlueX.
 *
 * LiquidBounce portions are licensed under GPLv3-or-later.
 * LoVisual modifications are licensed under GPLv3.
 * See THIRD_PARTY_NOTICES.md for details.
 */

package dev.loki.lovisual.util.aiming;

/**
 * Runnable that can only be executed once.
 * <p>
 * Ported from LiquidBounce (CCBlueX) concept.
 */
public final class RestrictedSingleUseAction {

    private final Runnable action;
    private boolean used;

    public RestrictedSingleUseAction(Runnable action) {
        this.action = action;
    }

    public void invoke() {
        if (used) return;
        used = true;
        if (action != null) {
            action.run();
        }
    }
}
