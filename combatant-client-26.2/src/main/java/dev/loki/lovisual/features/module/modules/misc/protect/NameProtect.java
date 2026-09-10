/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.misc.protect;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.StringValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.relations.PlayerRelations;
import dev.loki.lovisual.util.text.mask.NameProtector;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;

/**
 * Hides the local nickname and friend nicknames in rendered chat/tab text,
 * replacing them with a configurable alias. Hook points are the mixins on
 * ChatComponent.addMessage and PlayerInfo.getTabListDisplayName.
 */
@ModuleInfo(
        id = "nameprotect",
        displayName = "Name Protect",
        category = ModuleCategory.MISC
)
public class NameProtect extends Module {

    private static final String SETTING_ALIAS = "alias";
    private static final String SETTING_PROTECT_SELF = "protect_self";
    private static final String SETTING_PROTECT_FRIENDS = "protect_friends";
    private static final long CACHE_TTL_MS = 100L;

    private final StringValue alias = text("nameProtectAlias", SETTING_ALIAS, "Protected");
    private final BooleanValue protectSelf = bool("nameProtectSelf", SETTING_PROTECT_SELF, true);
    private final BooleanValue protectFriends = bool("nameProtectFriends", SETTING_PROTECT_FRIENDS, true);

    private volatile NameProtector cachedProtector;
    private volatile long cachedAt;

    @Override
    public void onDisable() {
        cachedProtector = null;
    }

    /**
     * @return null when the module is off or nothing is configured to protect
     */
    public static NameProtector activeProtector() {
        NameProtect module = Modules.get(NameProtect.class);
        if (module == null || !module.isEnabled()) return null;
        return module.protector();
    }

    /** Rewrites plain-text leaves of a component tree; returns input when nothing changes. */
    public static Component protect(Component text) {
        if (text == null) return null;
        NameProtector protector = activeProtector();
        if (protector == null) return text;
        return rewrite(text, protector);
    }

    private NameProtector protector() {
        long now = System.currentTimeMillis();
        NameProtector cached = cachedProtector;
        if (cached != null && now - cachedAt < CACHE_TTL_MS) {
            return cached.isEmpty() ? null : cached;
        }

        NameProtector protector = build();
        cachedProtector = protector;
        cachedAt = now;
        return protector.isEmpty() ? null : protector;
    }

    private NameProtector build() {
        NameProtector protector = new NameProtector();
        String replacement = alias.get();
        if (replacement == null) replacement = "";
        Minecraft mc = Minecraft.getInstance();

        if (protectSelf.get() && mc != null && mc.getUser() != null) {
            protector.put(mc.getUser().getName(), replacement);
        }
        if (protectFriends.get()) {
            for (String friend : PlayerRelations.get().getFriends()) {
                protector.put(friend, replacement);
            }
        }
        return protector;
    }

    private static Component rewrite(Component text, NameProtector protector) {
        ComponentContents contents = text.getContents();
        MutableComponent result;

        if (contents instanceof PlainTextContents plain) {
            String replaced = protector.replace(plain.text());
            result = Component.literal(replaced).withStyle(text.getStyle());
        } else {
            result = MutableComponent.create(contents).withStyle(text.getStyle());
        }

        for (Component child : text.getSiblings()) {
            result.append(rewrite(child, protector));
        }
        return result;
    }
}
