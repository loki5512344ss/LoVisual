package dev.loki.lovisual.mixins.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.bars.CustomHealthBar;

/**
 * Vanilla heart-layout redirects for {@link GuiMixin}. When the custom health bar takes over, the
 * vanilla {@code extractPlayerHealth} pass is fed neutral values (absorption 0, max health 20) so it
 * reserves the correct space without drawing hearts we replace ourselves.
 */
final class GuiHealthLayoutHandler {
    private GuiHealthLayoutHandler() {
    }

    static float ignoreAbsorptionForLayout(Player player) {
        if (!Lifecycle.canRunHud()) {
            return player.getAbsorptionAmount();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && player instanceof LocalPlayer clientPlayer) {
            float actualMaxHealth = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
            if (CustomHealthBar.shouldRenderHealthBar(mc, clientPlayer, actualMaxHealth)) {
                return 0.0f;
            }
        }
        return player.getAbsorptionAmount();
    }

    static double clampMaxHealthForLayout(Player player, Holder<Attribute> attribute) {
        if (!Lifecycle.canRunHud()) {
            return player.getAttributes().getValue(attribute);
        }
        if (attribute == Attributes.MAX_HEALTH) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && player instanceof LocalPlayer clientPlayer) {
                float actualMaxHealth = (float) player.getAttributes().getValue(attribute);
                if (CustomHealthBar.shouldRenderHealthBar(mc, clientPlayer, actualMaxHealth)) {
                    return 20.0;
                }
            }
        }
        return player.getAttributes().getValue(attribute);
    }
}
