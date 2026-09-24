package dev.loki.lovisual.features.module.modules.visuals.nametags.parts;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.combat.pvp.PvpCooldowns;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.pvp.cooldown.CooldownRegistry;
import dev.loki.lovisual.util.pvp.cooldown.ItemCooldownSnapshot;
import dev.loki.lovisual.util.pvp.state.PvpTargetState;
import dev.loki.lovisual.util.pvp.opponents.OpponentCooldownManager;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTagModel;
import dev.loki.lovisual.features.module.modules.visuals.nametags.NameTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public final class NameTagOpponentRenderer {

    private static final int ICON_SIZE = 24;
    private static final int ICON_GAP = 5;
    private static final int EFFECT_LINE_HEIGHT = 26;
    private static final int EFFECT_X_OFFSET = 6;
    private static final int OPP_TIME_COLOR = 0xFFFFFFFF;
    private static final int OPP_TIME_COLOR_GRACE = 0xFF4DA6FF;
    private static final int OPP_STACK_Y_OFFSET = 10;

    private NameTagOpponentRenderer() {
    }

    static boolean shouldRenderOpponentStuff(NameTags self, Player p) {
        Minecraft mc = Minecraft.getInstance();
        if (p == null || mc.player == null) return false;
        if (p == mc.player) return false;
        if (!self.opponentAppleCooldowns.get()) return false;
        if (!isOpponentAppleTrackingEnabled()) return false;
        if (!PvpTargetState.isTargetInPvp(p.getUUID()) && !hasOpponentCooldowns(p.getUUID())) return false;
        double maxDist = NameTags.OPPONENT_DISTANCE;
        return !(maxDist > 0) || !(mc.player.distanceToSqr(p) > maxDist * maxDist);
    }

    public static boolean isOpponentAppleTrackingEnabled() {
        PvpCooldowns mod = Modules.get(PvpCooldowns.class);
        return mod != null && mod.isSystemEnabled();
    }

    private static boolean hasOpponentCooldowns(UUID playerId) {
        if (playerId == null) return false;
        for (Item item : CooldownRegistry.trackedItems()) {
            if (OpponentCooldownManager.snapshot(playerId, item).visible()) {
                return true;
            }
        }
        return false;
    }

    static int computeOpponentStackY(NameTagModel.ScreenRect rect, int nameY) {
        if (rect != null) {
            double rectH = rect.maxY() - rect.minY();
            double upperCenter = rect.minY() + rectH * 0.25;
            int target = (int) Math.round(upperCenter - ICON_SIZE * 0.5);
            return Math.max(target, nameY + 2);
        }
        return nameY + OPP_STACK_Y_OFFSET;
    }

    public static void drawOpponentCooldownsStackedIcons(NameTags self, GuiGraphicsExtractor ctx, Player player,
                                                  NameTagModel.ScreenRect rect, int centerX, int nameY) {
        if (!shouldRenderOpponentStuff(self, player)) return;

        int startX = rect != null
                ? (int) rect.maxX() + EFFECT_X_OFFSET
                : centerX + (ICON_SIZE + ICON_GAP) * 2 + 8;
        int startY = computeOpponentStackY(rect, nameY);

        int y = startY;
        int seed = 0;
        for (Item item : CooldownRegistry.trackedItems()) {
            ItemCooldownSnapshot snapshot = OpponentCooldownManager.snapshot(player.getUUID(), item);
            if (!snapshot.visible()) continue;

            ItemStack icon = new ItemStack(item);
            float scale = ICON_SIZE / 16f;
            Renderer2D.COLOR.item(icon, startX, y, scale, seed++, Renderer2D.ITEM_OVERLAY_NONE, null);

            y += EFFECT_LINE_HEIGHT;
        }
    }

    public static void drawOpponentCooldownsStackedText(NameTags self, TextRenderer textRenderer, Player player,
                                                 NameTagModel.ScreenRect rect, int centerX, int nameY) {
        if (!shouldRenderOpponentStuff(self, player)) return;

        int startX = rect != null
                ? (int) rect.maxX() + EFFECT_X_OFFSET
                : centerX + (ICON_SIZE + ICON_GAP) * 2 + 8;
        int startY = computeOpponentStackY(rect, nameY);

        int timeColor = PvpTargetState.isTargetInPvp(player.getUUID())
                ? OPP_TIME_COLOR
                : OPP_TIME_COLOR_GRACE;
        int y = startY;
        for (Item item : CooldownRegistry.trackedItems()) {
            ItemCooldownSnapshot snapshot = OpponentCooldownManager.snapshot(player.getUUID(), item);
            if (!snapshot.visible()) continue;

            String prefix = snapshot.compactText();
            if (!prefix.isEmpty()) {
                int textX = startX + ICON_SIZE + 6;
                int textY = y + 4;
                textRenderer.render(prefix + " ", textX, textY,
                        new RenderColor(snapshot.cooling() ? timeColor : OPP_TIME_COLOR_GRACE), true);
            }

            y += EFFECT_LINE_HEIGHT;
        }
    }
}
