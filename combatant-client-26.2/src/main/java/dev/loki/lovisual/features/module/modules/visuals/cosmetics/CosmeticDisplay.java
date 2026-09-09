package dev.loki.lovisual.features.module.modules.visuals.cosmetics;

import dev.loki.lovisual.config.values.EnumValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.features.cosmetic.CosmeticManager;
import dev.loki.lovisual.features.cosmetic.CosmeticModel;
import dev.loki.lovisual.features.cosmetic.CosmeticRenderer;
import dev.loki.lovisual.features.cosmetic.CosmeticRepository;
import dev.loki.lovisual.features.cosmetic.CosmeticSlot;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.features.module.WorldPhase;
import dev.loki.lovisual.features.module.modules.visuals.cosmetics.CosmeticsScreen;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the locally equipped cosmetics (per body-zone slot) attached to the local
 * player. Right-click the module card in ClickGUI to open {@link CosmeticsScreen}
 * (3D preview + per-slot pickers). Offset/rotation/scale overrides are per accessory
 * slot (not per model) — see {@link #offsetsFor(CosmeticSlot)}.
 */
@ModuleInfo(
        id = "cosmeticdisplay",
        displayName = "Cosmetics",
        aliases = {"cosmetics"},
        category = ModuleCategory.VISUALS,
        description = "Отображает надетую косметику по слотам (модель/питомец/аксессуары)"
)
public final class CosmeticDisplay extends Module {

    /** Per-slot offset/rotation/scale user overrides, applied on top of the base attachment transform. */
    public record SlotOffsetSettings(NumberValue<Float> offsetX, NumberValue<Float> offsetY,
                                     NumberValue<Float> offsetZ, NumberValue<Float> rotation,
                                     NumberValue<Float> scale) {
    }

    private static final CosmeticSlot[] ACCESSORY_SLOTS = {
            CosmeticSlot.HEAD, CosmeticSlot.SHOULDERS, CosmeticSlot.BACK,
            CosmeticSlot.WAIST, CosmeticSlot.LEGS, CosmeticSlot.HELD
    };

    private final EnumValue<CosmeticSlot> activeSlot =
            enumSetting("cosmeticsActiveSlot", "active_slot", CosmeticSlot.HEAD, ACCESSORY_SLOTS);

    private final Map<CosmeticSlot, SlotOffsetSettings> slotSettings = buildSlotSettings();

    {
        action("Next Cosmetic", "NONE");
        action("Prev Cosmetic", "NONE");
        action("Open Cosmetics", "NONE");
    }

    private Map<CosmeticSlot, SlotOffsetSettings> buildSlotSettings() {
        Map<CosmeticSlot, SlotOffsetSettings> map = new EnumMap<>(CosmeticSlot.class);
        for (CosmeticSlot slot : ACCESSORY_SLOTS) {
            String prefix = "cosmetics" + capitalize(slot.getId());
            SlotOffsetSettings settings = new SlotOffsetSettings(
                    visibleWhen(num(prefix + "OffsetX", "offset_x", 0.0f, -1.0f, 1.0f), () -> activeSlot.get() == slot),
                    visibleWhen(num(prefix + "OffsetY", "offset_y", 0.0f, -1.0f, 1.0f), () -> activeSlot.get() == slot),
                    visibleWhen(num(prefix + "OffsetZ", "offset_z", 0.0f, -1.0f, 1.0f), () -> activeSlot.get() == slot),
                    visibleWhen(num(prefix + "Rotation", "rotation", 0.0f, -180.0f, 180.0f), () -> activeSlot.get() == slot),
                    visibleWhen(num(prefix + "Scale", "scale", 1.0f, 0.1f, 3.0f), () -> activeSlot.get() == slot)
            );
            map.put(slot, settings);
        }
        return map;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public SlotOffsetSettings offsetsFor(CosmeticSlot slot) {
        return slotSettings.get(slot);
    }

    public CosmeticSlot getActiveSlot() {
        return activeSlot.get();
    }

    public void setActiveSlot(CosmeticSlot slot) {
        activeSlot.set(slot);
    }

    @Override
    public void onEnable() {
        CosmeticRepository.get().reload();
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;

        if (isActionPressedOnce("Next Cosmetic")) {
            cycle(1);
        } else if (isActionPressedOnce("Prev Cosmetic")) {
            cycle(-1);
        } else if (isActionPressedOnce("Open Cosmetics")) {
            openScreen();
        }
    }

    public void openScreen() {
        ClientScreen.show(new CosmeticsScreen());
    }

    private void cycle(int direction) {
        List<CosmeticModel> models = CosmeticRepository.get().bySlot(CosmeticSlot.MODEL);
        if (models.isEmpty()) return;

        CosmeticModel current = CosmeticManager.get().getActive(CosmeticSlot.MODEL);
        int index = current == null ? -1 : models.indexOf(current);
        int next = Math.floorMod(index + direction, models.size());

        CosmeticManager.get().setActive(CosmeticSlot.MODEL, models.get(next));
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.AFTER_POST_PROCESS;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        for (CosmeticSlot slot : CosmeticSlot.values()) {
            CosmeticRenderer.renderOnPlayer(renderer, mc.player, slot, tickDelta);
        }
    }
}
