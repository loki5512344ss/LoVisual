package dev.loki.lovisual.features.module.modules.visuals.items.armor;

/** Implemented by LivingEntityRenderState: whether ArmorColor applies to this entity (decided at extraction). */
public interface ArmorColorState {
    boolean lovisual$isArmorColored();

    void lovisual$setArmorColored(boolean colored);
}
