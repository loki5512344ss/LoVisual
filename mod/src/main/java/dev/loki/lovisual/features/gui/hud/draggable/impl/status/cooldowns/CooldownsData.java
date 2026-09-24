package dev.loki.lovisual.features.gui.hud.draggable.impl.status;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.combat.pvp.PvpCooldowns;
import dev.loki.lovisual.mixins.accessors.inventory.ItemCooldownEntryAccessor;
import dev.loki.lovisual.mixins.accessors.inventory.ItemCooldownManagerAccessor;
import dev.loki.lovisual.util.pvp.client.CooldownsState;
import dev.loki.lovisual.util.pvp.cooldown.CooldownRegistry;
import dev.loki.lovisual.util.pvp.cooldown.ItemCooldownSnapshot;
import dev.loki.lovisual.util.pvp.state.PvpState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import static dev.loki.lovisual.features.theme.Theme.theme;

final class CooldownsData {

    private CooldownsData() {}

    static List<Cooldowns.Row> collectRows(Cooldowns host) {
        Minecraft mc = Minecraft.getInstance();
        List<Cooldowns.Row> rows = new ArrayList<>();
        if (mc != null && mc.player != null) {
            var manager = mc.player.getCooldowns();
            int nowTick = ((ItemCooldownManagerAccessor) manager).getTick();
            ((ItemCooldownManagerAccessor) manager).getEntries().forEach((Identifier id, Object entry) -> {
                int remainingTicks = ((ItemCooldownEntryAccessor) entry).getEndTick() - nowTick;
                if (remainingTicks <= 0) return;
                Item item = id != null ? BuiltInRegistries.ITEM.getValue(id) : Items.AIR;
                ItemStack stack = new ItemStack(item);
                String name = stack.isEmpty() ? (id != null ? id.getPath() : "Unknown") : stack.getHoverName().getString();
                String key = "vanilla:" + (id != null ? id.toString() : name);
                rows.add(new Cooldowns.Row(key, stack, name, formatSeconds(remainingTicks / 20.0f), remainingTicks, host.uiMuted != 0 ? host.uiMuted : theme().textMuted()));
            });
            PvpCooldowns pvpCooldowns = Modules.get(PvpCooldowns.class);
            boolean pvpEnabled = pvpCooldowns != null && pvpCooldowns.isSystemEnabled();
            if (pvpEnabled && pvpCooldowns.shouldRenderWidget()) {
                boolean inPvp = CooldownsState.MANAGER.isInPvp();
                int stateColor = inPvp ? Cooldowns.PVP_ACTIVE_COLOR : Cooldowns.PVP_IDLE_COLOR;
                for (Item item : CooldownRegistry.trackedItems()) {
                    ItemCooldownSnapshot snapshot = CooldownsState.MANAGER.snapshot(item);
                    if (!snapshot.visible()) continue;
                    float seconds = snapshot.cooling() ? snapshot.cooldownRemainingMs() / 1000.0f : snapshot.useWindowRemainingMs() / 1000.0f;
                    String text = snapshot.cooling() ? formatSeconds(seconds) : snapshot.compactText();
                    int remainingTicks = snapshot.cooling() ? Math.max(1, Math.round(seconds * 20.0f)) : Math.max(1, Math.round(Math.max(1.0f, seconds) * 20.0f));
                    ItemStack stack = new ItemStack(item);
                    rows.add(new Cooldowns.Row("pvp:" + BuiltInRegistries.ITEM.getKey(item), stack, stack.getHoverName().getString(), text, remainingTicks, snapshot.cooling() ? stateColor : (host.uiMuted != 0 ? host.uiMuted : theme().textMuted())));
                }
            }
            if (pvpEnabled && PvpState.isActive()) {
                Float secondsLeft = PvpState.getSecondsLeft();
                if (secondsLeft != null && secondsLeft > 0.0f) {
                    int remainingTicks = Math.max(1, Math.round(secondsLeft * 20.0f));
                    ItemStack pearlStack = new ItemStack(Items.ENDER_PEARL);
                    rows.add(new Cooldowns.Row("pvpban:" + BuiltInRegistries.ITEM.getKey(Items.ENDER_PEARL), pearlStack, pearlStack.getHoverName().getString(), formatSeconds(secondsLeft), remainingTicks, Cooldowns.PVP_BLOCKED_COLOR));
                    ItemStack chorusStack = new ItemStack(Items.CHORUS_FRUIT);
                    rows.add(new Cooldowns.Row("pvpban:" + BuiltInRegistries.ITEM.getKey(Items.CHORUS_FRUIT), chorusStack, chorusStack.getHoverName().getString(), formatSeconds(secondsLeft), remainingTicks, Cooldowns.PVP_BLOCKED_COLOR));
                }
            }
        }
        rows.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.name(), b.name()));
        return rows;
    }

    static String formatSeconds(float seconds) {
        if (seconds >= 10.0f) return String.format("%.0fs", seconds);
        return String.format("%.1fs", Math.round(seconds * 10.0f) / 10.0f);
    }

    static ItemStack[] previewStacks(Cooldowns host) {
        if (Cooldowns.previewStacks == null || Cooldowns.previewStacks.length != Cooldowns.PREVIEW_ITEMS.length) {
            Cooldowns.previewStacks = buildPreviewStacks();
        }
        return Cooldowns.previewStacks;
    }

    private static ItemStack[] buildPreviewStacks() {
        ItemStack[] stacks = new ItemStack[Cooldowns.PREVIEW_ITEMS.length];
        for (int i = 0; i < Cooldowns.PREVIEW_ITEMS.length; i++) stacks[i] = new ItemStack(Cooldowns.PREVIEW_ITEMS[i]);
        return stacks;
    }

    static void queueItemIcon(List<Cooldowns.QueuedItemIcon> itemTasks, ItemStack stack, float iconX, float centerY, float baseScale, int seed) {
        if (itemTasks == null || stack.isEmpty()) return;
        float iconSize = Cooldowns.ROW_ICON_VISUAL_SIZE * baseScale;
        float itemScale = iconSize / 16.0f;
        float itemDrawX = iconX;
        float itemDrawY = centerY - (16.0f * itemScale * 0.5f);
        itemTasks.add(new Cooldowns.QueuedItemIcon(stack.copy(), itemDrawX, itemDrawY, itemScale, seed));
    }
}
