/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.misc;

import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.util.mctiers.MCTiersCache;
import dev.loki.lovisual.util.mctiers.MCTiersTier;

/**
 * Тиры игроков с mctiers.com (референс — Soup Balancer): выбор игрового
 * режима, тир по которому смотрим. Референсный Balancer сам почти не содержит
 * логики — она была в недоступной нам {@code McTiersModule}-базе; сеть/кэш/
 * парсинг написаны заново в {@link MCTiersCache}. Смотреть тир игрока:
 * {@code %tier <ник>} (см. {@code TierCommand}) или {@link #tierFor(String)}
 * для точечной интеграции в другие модули (например NameTags).
 */
@ModuleInfo(
        id = "balancer",
        displayName = "Balancer",
        category = ModuleCategory.MISC,
        description = "Тиры игроков с mctiers.com по выбранному игровому режиму"
)
public final class Balancer extends Module {

    private final ModeValue fixType = modeSetting("fixType", "fix_type", "Vanilla",
            "Vanilla", "UHC", "Pot", "NethOP", "SMP", "Sword", "Axe", "Mace");

    public String fixType() {
        return fixType.get();
    }

    /** @return тир игрока по текущему выбранному режиму, либо null (нет данных ещё/вообще). */
    public MCTiersTier tierFor(String playerName) {
        return MCTiersCache.getTier(playerName, MCTiersTier.modeApiKey(fixType.get()));
    }
}
