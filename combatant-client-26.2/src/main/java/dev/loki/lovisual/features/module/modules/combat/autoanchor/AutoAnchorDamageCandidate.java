/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.combat.autoanchor;

public interface AutoAnchorDamageCandidate {
    float damage();

    float selfDamage();

    boolean overrideDamage();
}
