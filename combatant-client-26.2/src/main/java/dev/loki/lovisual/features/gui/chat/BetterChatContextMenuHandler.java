/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */
package dev.loki.lovisual.features.gui.chat;
import dev.loki.lovisual.features.command.CommandManager;
import dev.loki.lovisual.features.gui.hud.draggable.impl.BetterChat;
import dev.loki.lovisual.mixins.accessors.ChatScreenAccessor;
import dev.loki.lovisual.util.screen.ClientScreen;
import dev.loki.lovisual.util.text.ChatNameUtil;
import dev.loki.lovisual.util.text.ClipboardUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import java.util.ArrayList;
import java.util.List;
/**
 * Right-click context menu construction, click-event handling and chat prefill actions.
 */
final class BetterChatContextMenuHandler {
    private BetterChatContextMenuHandler() {
    }
    static void openContextMenu(BetterChatModel.PickResult pick, double mx, double my) {
        List<BetterChatContextMenuModel.ContextMenu.MenuEntry> entries = new ArrayList<>();
        ChatLine msg = pick.line().message();
        String full = msg.text().getString();
        BetterChat settings = BetterChat.get();
        long ts = msg.timestampMs();
        boolean tsEnabled = settings != null && settings.timestampEnabled();
        boolean tsUnix = settings != null && settings.timestampToggles().get("hover_unix");
        if (BetterChatRenderer.selection.appliesTo(pick.line().messageIndex()) && BetterChatRenderer.selection.hasRange()) {
            String selected = selectedText();
            entries.add(new BetterChatContextMenuModel.ContextMenu.MenuEntry(I18n.get("better_chat.context.copy_selection"), () -> ClipboardUtil.copy(selected)));
        } else {
            entries.add(new BetterChatContextMenuModel.ContextMenu.MenuEntry(I18n.get("better_chat.context.copy_message"), () -> ClipboardUtil.copy(full)));
        }
        List<String> nickCandidates = ChatNameUtil.extractNicks(full);
        String hovered = ChatNameUtil.normalizeNickCandidate(BetterChatEditBoxUtil.wordAt(full, pick.glyph().charIndex()));
        String targetNick = null;
        if (!hovered.isEmpty()
                && ChatNameUtil.isNickLike(hovered)
                && nickCandidates.stream().anyMatch(n -> n.equalsIgnoreCase(hovered))) {
            targetNick = hovered;
        } else if (nickCandidates.size() == 1) {
            targetNick = nickCandidates.getFirst();
        }
        if (isOnlinePlayer(targetNick)) {
            String nickToTell = targetNick;
            entries.add(new BetterChatContextMenuModel.ContextMenu.MenuEntry(I18n.get("better_chat.context.reply"), () -> prefillTell(nickToTell)));
        }
        if (BetterChatRenderer.hoverEntityUuid != null && !BetterChatRenderer.hoverEntityUuid.isEmpty()) {
            String uuidToCopy = BetterChatRenderer.hoverEntityUuid;
            entries.add(new BetterChatContextMenuModel.ContextMenu.MenuEntry(I18n.get("better_chat.context.copy_uuid"), () -> ClipboardUtil.copy(uuidToCopy)));
        }
        if (tsEnabled) {
            entries.add(new BetterChatContextMenuModel.ContextMenu.MenuEntry(I18n.get("better_chat.context.copy_time"), () -> ClipboardUtil.copy(BetterChatTooltipRenderer.formatTimestamp(ts, settings.timestampSeconds()))));
            entries.add(new BetterChatContextMenuModel.ContextMenu.MenuEntry(I18n.get("better_chat.context.copy_date"), () -> ClipboardUtil.copy(BetterChatTooltipRenderer.formatDate(ts))));
            if (tsUnix) {
                entries.add(new BetterChatContextMenuModel.ContextMenu.MenuEntry(I18n.get("better_chat.context.copy_unix"), () -> ClipboardUtil.copy(String.valueOf(ts / 1000L))));
            }
        }
        BetterChatRenderer.contextMenu = BetterChatContextMenuModel.ContextMenu.open((float) mx, (float) my, entries);
    }
    static void copySelectionToClipboard() {
        String selected = selectedText();
        if (selected.isEmpty()) return;
        ClipboardUtil.copy(selected);
    }
    static String selectedText() {
        if (!BetterChatRenderer.selection.hasRange()) return "";
        if (BetterChatRenderer.lastMessages == null || BetterChatRenderer.lastMessages.isEmpty()) return "";
        int startMsg = Mth.clamp(BetterChatRenderer.selection.startContext(), 0, BetterChatRenderer.lastMessages.size() - 1);
        int endMsg = Mth.clamp(BetterChatRenderer.selection.endContext(), 0, BetterChatRenderer.lastMessages.size() - 1);
        if (endMsg < startMsg) return "";
        StringBuilder out = new StringBuilder();
        for (int msgIdx = startMsg; msgIdx <= endMsg; msgIdx++) {
            ChatLine line = BetterChatRenderer.lastMessages.get(msgIdx);
            if (line == null || line.text() == null) continue;
            String full = line.text().getString();
            int len = full.length();
            int start = BetterChatRenderer.selection.startForLine(msgIdx);
            int end = BetterChatRenderer.selection.endForLine(msgIdx);
            int from = start == Integer.MIN_VALUE ? 0 : Mth.clamp(start, 0, len);
            int toInclusive = end == Integer.MAX_VALUE ? Math.max(0, len - 1) : Mth.clamp(end, 0, Math.max(0, len - 1));
            int to = Mth.clamp(toInclusive + 1, 0, len);
            if (to > from) {
                if (!out.isEmpty()) out.append(' ');
                out.append(BetterChatFontUtil.safeSub(full, from, to));
            }
        }
        return out.toString();
    }
    static boolean handleClickEvent(net.minecraft.network.chat.Style style, boolean ctrlDown) {
        if (style == null) return false;
        ClickEvent evt = style.getClickEvent();
        if (evt == null) return false;
        String val = switch (evt) {
            case ClickEvent.OpenUrl openUrl -> openUrl.uri().toString();
            case ClickEvent.OpenFile openFile -> openFile.path();
            case ClickEvent.RunCommand run -> run.command();
            case ClickEvent.SuggestCommand sug -> sug.command();
            case ClickEvent.CopyToClipboard copy -> copy.value();
            case ClickEvent.ShowDialog showDialog -> showDialog.dialog().value().toString();
            case ClickEvent.ChangePage changePage -> String.valueOf(changePage.page());
            case ClickEvent.Custom custom -> custom.id().toString();
            default -> null;
        };
        if (val == null || val.isEmpty()) return false;
        try {
            if (evt instanceof ClickEvent.OpenUrl || evt instanceof ClickEvent.OpenFile) {
                Minecraft mc = Minecraft.getInstance();
                if (ClientScreen.current() instanceof ChatScreen chat) {
                    ClientScreen.show(mc, new ConfirmLinkScreen(confirm -> {
                        if (confirm) Util.getPlatform().openUri(val);
                        ClientScreen.show(mc, chat);
                    }, val, false));
                } else {
                    Util.getPlatform().openUri(val);
                }
                return true;
            } else if (evt instanceof ClickEvent.CopyToClipboard) {
                ClipboardUtil.copy(val);
                return true;
            } else if (evt instanceof ClickEvent.RunCommand && val.startsWith("@")) {
                return CommandManager.handle(val);
            } else if (evt instanceof ClickEvent.SuggestCommand || evt instanceof ClickEvent.RunCommand) {
                if (!ctrlDown) return false;
                prefillChat(val);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
    static boolean tryPrefillTellFromClick(int msgIndex, int charIndex) {
        if (msgIndex < 0 || msgIndex >= BetterChatRenderer.lastMessages.size()) return false;
        String full = BetterChatRenderer.lastMessages.get(msgIndex).text().getString();
        List<String> nickCandidates = ChatNameUtil.extractNicks(full);
        if (nickCandidates.isEmpty()) return false;
        String hovered = ChatNameUtil.normalizeNickCandidate(BetterChatEditBoxUtil.wordAt(full, charIndex));
        String targetNick = null;
        if (!hovered.isEmpty()
                && ChatNameUtil.isNickLike(hovered)
                && nickCandidates.stream().anyMatch(n -> n.equalsIgnoreCase(hovered))) {
            targetNick = hovered;
        } else if (nickCandidates.size() == 1) {
            targetNick = nickCandidates.getFirst();
        }
        if (!isOnlinePlayer(targetNick)) return false;
        prefillTell(targetNick);
        return true;
    }
    static void prefillChat(String value) {
        if (value == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(ClientScreen.current() instanceof ChatScreen chatScreen)) return;
        EditBox field = ((ChatScreenAccessor) chatScreen).getChatField();
        if (field == null) return;
        field.setValue(value);
        BetterChatEditBoxUtil.setCursorSafe(field, value.length());
    }
    static void prefillTell(String nick) {
        Minecraft mc = Minecraft.getInstance();
        if (!(ClientScreen.current() instanceof ChatScreen chatScreen)) return;
        if (!isOnlinePlayer(nick)) return;
        EditBox field = ((ChatScreenAccessor) chatScreen).getChatField();
        if (field == null) return;
        String value = "/tell " + nick + " ";
        field.setValue(value);
    }
    static boolean isOnlinePlayer(String nick) {
        if (nick == null || nick.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.player.connection == null) return false;
        return mc.player.connection.getOnlinePlayers().stream()
                .anyMatch(e -> e.getProfile() != null && nick.equalsIgnoreCase(e.getProfile().name()));
    }
}