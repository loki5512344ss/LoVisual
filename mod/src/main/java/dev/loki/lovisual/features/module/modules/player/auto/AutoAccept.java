/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.player.auto;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.collection.SetValue;
import dev.loki.lovisual.config.values.primitive.StringValue;
import dev.loki.lovisual.events.meta.EventHandler;
import dev.loki.lovisual.events.impl.pvp.PvpChatEvent;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.Notifier;
import dev.loki.lovisual.features.relations.PlayerRelations;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.Minecraft;

import java.util.Locale;
import java.util.Set;

/**
 * Answers teleport/clan requests automatically by sending a configurable
 * command once a chat message matches one of the configured patterns.
 * Optionally restricted to friends via PlayerRelations.
 */
@ModuleInfo(
        id = "autoaccept",
        displayName = "Auto Accept",
        aliases = {"autoaccept"},
        category = ModuleCategory.PLAYER
)
public class AutoAccept extends Module {
    private static final String SETTING_PATTERNS = "patterns";
    private static final String SETTING_COMMAND = "command";
    private static final String SETTING_ONLY_FRIENDS = "only_friends";
    private static final String SETTING_DELAY = "delay";
    private static final String SETTING_COOLDOWN = "cooldown";

    private static final long MIN_COOLDOWN_MS = 500L;

    private final SetValue patterns = textList("autoAcceptPatterns", SETTING_PATTERNS,
            TextListSetting.PickerMode.TEXT,
            Set.of(
                    "просит телепортироваться к себе",
                    "просит телепортироваться",
                    "хочет телепортироваться к вам",
                    "has requested to teleport to you",
                    "has requested to teleport",
                    "wants to teleport to you"
            ));
    private final StringValue command = text("autoAcceptCommand", SETTING_COMMAND, "tpaccept");
    private final BooleanValue onlyFriends = bool("autoAcceptOnlyFriends", SETTING_ONLY_FRIENDS, false);
    private final NumberValue<Integer> delay = num("autoAcceptDelay", SETTING_DELAY, 0, 0, 3000);
    private final NumberValue<Integer> cooldown =
            num("autoAcceptCooldown", SETTING_COOLDOWN, 2000, (int) MIN_COOLDOWN_MS, 10000);

    private String pendingSender;
    private long firstMatchMs;
    private long lastAcceptedMs = Long.MIN_VALUE;

    @Override
    public void onDisable() {
        pendingSender = null;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.getConnection() == null) {
            pendingSender = null;
            return;
        }
        if (pendingSender == null) return;

        long now = GameClock.millis();
        if (now - firstMatchMs < delay.get()) return;
        String sender = pendingSender;
        pendingSender = null;

        if (now - lastAcceptedMs < Math.max(MIN_COOLDOWN_MS, cooldown.get())) return;

        lastAcceptedMs = now;
        Notifier.info("Auto Accept: " + sender);
        mc.getConnection().sendCommand(command.get());
    }

    @EventHandler
    private void onChat(PvpChatEvent event) {
        if (event.message == null || event.message.isBlank()) return;

        String sender = TeleportRequestParser.extractSender(event.message, patterns.get());
        if (sender == null) return;
        if (onlyFriends.get() && !PlayerRelations.get().isFriend(sender)) return;
        if (pendingSender != null) return;

        pendingSender = sender;
        firstMatchMs = GameClock.millis();
    }

    /**
     * Pure chat-request parsing: finds the earliest configured pattern and
     * extracts the nickname-like token immediately before it.
     */
    public static final class TeleportRequestParser {
        private TeleportRequestParser() {
        }

        public static String extractSender(String message, Set<String> patterns) {
            if (message == null || patterns == null || patterns.isEmpty()) return null;

            String lower = message.toLowerCase(Locale.ROOT);
            int matchIndex = -1;
            for (String pattern : patterns) {
                if (pattern == null || pattern.isBlank()) continue;
                int idx = lower.indexOf(pattern.toLowerCase(Locale.ROOT));
                if (idx >= 0 && (matchIndex < 0 || idx < matchIndex)) matchIndex = idx;
            }
            if (matchIndex <= 0) return null;

            return lastNicknameIn(message.substring(0, matchIndex));
        }

        static String lastNicknameIn(String prefix) {
            java.util.regex.Matcher matcher =
                    java.util.regex.Pattern.compile("[A-Za-z0-9_]{3,16}").matcher(prefix);
            String found = null;
            while (matcher.find()) {
                found = matcher.group();
            }
            return found;
        }
    }
}
