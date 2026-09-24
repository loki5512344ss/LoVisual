package dev.loki.lovisual.features.module.modules.player;

import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.events.meta.EventHandler;
import dev.loki.lovisual.events.impl.network.PacketEvent;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.Notifier;
import dev.loki.lovisual.util.protection.NotifyThrottle;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * Announces notable item/effect uses in the HUD: totem pops (self and
 * opponents) and selected combat effects applied to opponents. All decisions
 * are packet-driven, so nothing is missed by render-tick sampling.
 */
@ModuleInfo(
        id = "usetracker",
        displayName = "Use Tracker",
        aliases = {"usetracker"},
        category = ModuleCategory.PLAYER
)
public class UseTracker extends Module {
    static final byte TOTEM_POP_EVENT_ID = 35;

    private static final String SETTING_SELF_TOTEMS = "self_totems";
    private static final String SETTING_OTHER_TOTEMS = "other_totems";
    private static final String SETTING_OTHER_EFFECTS = "other_effects";

    private final BooleanValue selfTotems = bool("useTrackerSelfTotems", SETTING_SELF_TOTEMS, true);
    private final BooleanValue otherTotems = bool("useTrackerOtherTotems", SETTING_OTHER_TOTEMS, true);
    private final BooleanMapValue otherEffects = group("useTrackerOtherEffects", SETTING_OTHER_EFFECTS,
            UseEffects.defaultOptions());

    private final NotifyThrottle throttle = new NotifyThrottle(300L);

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (event.getPacket() instanceof ClientboundEntityEventPacket entityEvent) {
            handleEntityEvent(entityEvent);
        } else if (event.getPacket() instanceof ClientboundUpdateMobEffectPacket effectPacket) {
            handleEffectPacket(effectPacket);
        }
    }

    private void handleEntityEvent(ClientboundEntityEventPacket packet) {
        if (packet.getEventId() != TOTEM_POP_EVENT_ID) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        Entity entity = packet.getEntity(mc.level);
        if (!(entity instanceof Player player)) return;

        if (player instanceof LocalPlayer) {
            if (!selfTotems.get() || !throttle.tryAcquire(Util.getMillis())) return;
            Notifier.warning("Totem popped: " + countTotems(mc.player) + " left");
        } else if (otherTotems.get()) {
            Notifier.info(player.getGameProfile().name() + " popped a totem");
        }
    }

    private void handleEffectPacket(ClientboundUpdateMobEffectPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        Entity entity = mc.level.getEntity(packet.getEntityId());
        if (!(entity instanceof Player player) || player instanceof LocalPlayer) return;

        String key = UseEffects.optionKey(packet.getEffect());
        if (key == null || !otherEffects.get(key)) return;

        Notifier.info(player.getGameProfile().name() + " used " + UseEffects.displayName(key));
    }

    private static int countTotems(Player player) {
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) count++;
        }
        return count;
    }

    /**
     * Pure effect bookkeeping: which combat effects are trackable and how they
     * are displayed. Holder-to-key mapping happens in the module.
     */
    public static final class UseEffects {
        private UseEffects() {
        }

        public static Map<String, Boolean> defaultOptions() {
            return Map.of(
                    "invisibility", true,
                    "speed", false,
                    "strength", false,
                    "regeneration", false,
                    "fire_resistance", false,
                    "weakness", false
            );
        }

        static String optionKey(Holder<MobEffect> effect) {
            if (effect == MobEffects.INVISIBILITY) return "invisibility";
            if (effect == MobEffects.SPEED) return "speed";
            if (effect == MobEffects.STRENGTH) return "strength";
            if (effect == MobEffects.REGENERATION) return "regeneration";
            if (effect == MobEffects.FIRE_RESISTANCE) return "fire_resistance";
            if (effect == MobEffects.WEAKNESS) return "weakness";
            return null;
        }

        static String displayName(String key) {
            return switch (key) {
                case "invisibility" -> "Invisibility";
                case "speed" -> "Speed";
                case "strength" -> "Strength";
                case "regeneration" -> "Regeneration";
                case "fire_resistance" -> "Fire Resistance";
                case "weakness" -> "Weakness";
                default -> "an effect";
            };
        }
    }
}
