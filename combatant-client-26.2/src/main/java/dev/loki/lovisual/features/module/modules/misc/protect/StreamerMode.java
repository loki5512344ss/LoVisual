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
import dev.loki.lovisual.util.text.mask.NameMasker;

import java.util.List;
import dev.loki.lovisual.features.module.modules.misc.protect.NameProtect;

/**
 * Streamer mode: masks every nickname-like token in chat/tab/scoreboard text
 * (so both player names and server-brand words disappear deterministically),
 * and can replace real player skins with the default Steve/Alex texture.
 * Hook points reuse the NameProtect mixins plus a skin override in
 * AbstractClientPlayerMixin.
 */
@ModuleInfo(
        id = "streamermode",
        displayName = "Streamer Mode",
        aliases = {"streamermode", "streamer"},
        category = ModuleCategory.MISC
)
public class StreamerMode extends Module {
    private static final String SETTING_ALIAS = "alias";
    private static final String SETTING_MASK_CHAT = "mask_chat";
    private static final String SETTING_MASK_SERVER_WORDS = "mask_server_words";
    private static final String SETTING_HIDE_SKINS = "hide_skins";

    private final StringValue alias = text("streamerModeAlias", SETTING_ALIAS, "Player");
    private final BooleanValue maskChat = bool("streamerModeMaskChat", SETTING_MASK_CHAT, true);
    private final BooleanValue maskServerWords =
            visibleWhen(bool("streamerModeMaskServerWords", SETTING_MASK_SERVER_WORDS, true),
                    maskChat::get);
    private final BooleanValue hideSkins = bool("streamerModeHideSkins", SETTING_HIDE_SKINS, true);

    private volatile NameMasker cachedMasker;

    @Override
    public void onDisable() {
        cachedMasker = null;
    }

    public boolean shouldHideSkins() {
        return isEnabled() && hideSkins.get();
    }

    public boolean shouldMaskChat() {
        return isEnabled() && maskChat.get();
    }

    /** @return null when the module is off or chat masking is disabled */
    public static NameMasker activeMasker() {
        StreamerMode module = Modules.get(StreamerMode.class);
        if (module == null || !module.shouldMaskChat()) return null;
        return module.masker();
    }

    /** Rewrites chat/tab component text; mirrors NameProtect.protect usage. */
    public static String maskText(String text) {
        if (text == null) return null;
        NameMasker masker = activeMasker();
        if (masker == null) return text;
        return masker.mask(text);
    }

    /**
     * Component-tree variant for chat mixins: rewrites plain-text leaves and
     * returns the input instance when nothing changed.
     */
    public static net.minecraft.network.chat.Component protectComponent(net.minecraft.network.chat.Component text) {
        if (text == null) return null;
        NameMasker masker = activeMasker();
        if (masker == null) return text;

        net.minecraft.network.chat.ComponentContents contents = text.getContents();
        net.minecraft.network.chat.MutableComponent result;
        if (contents instanceof net.minecraft.network.chat.contents.PlainTextContents plain) {
            String replaced = masker.mask(plain.text());
            if (replaced.equals(plain.text())) {
                result = net.minecraft.network.chat.MutableComponent.create(contents).withStyle(text.getStyle());
            } else {
                result = net.minecraft.network.chat.Component.literal(replaced).withStyle(text.getStyle());
            }
        } else {
            result = net.minecraft.network.chat.MutableComponent.create(contents).withStyle(text.getStyle());
        }

        for (net.minecraft.network.chat.Component child : text.getSiblings()) {
            result.append(protectComponent(child));
        }
        return result;
    }

    private NameMasker masker() {
        NameMasker cached = cachedMasker;
        if (cached != null) return cached;

        List<String> words = maskServerWords.get()
                ? List.of("anarchy", "анархия", "hitech", "хайтек", "magic", "магия",
                "vanilla", "ванил", "skyblock", "скайблок")
                : List.of();
        NameMasker masker = new NameMasker(alias.get(), words);
        cachedMasker = masker;
        return masker;
    }

    /** Pure helper for the skin mixin: is this UUID worth hiding (not self). */
    public static boolean hideSkin(boolean enabled, boolean isSelf) {
        return enabled && !isSelf;
    }
}
