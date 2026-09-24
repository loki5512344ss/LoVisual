package dev.loki.lovisual.features.gui.hud.draggable.impl.targethud;

import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class TargetHudModel {

    private TargetHudModel() {
    }

    public record EquipmentEntry(ItemStack stack, float width) {
    }

    public record EquipmentLayout(List<EquipmentEntry> entries, float width, float height) {
    }

    public record EquipmentRenderTask(ItemStack stack, float x, float y, float scale) {
    }

    public record EffectChip(MobEffectInstance effect, Identifier iconId, String text, float width) {
    }

    public record EffectLayout(List<EffectChip> chips, float width, float height) {
    }
}
