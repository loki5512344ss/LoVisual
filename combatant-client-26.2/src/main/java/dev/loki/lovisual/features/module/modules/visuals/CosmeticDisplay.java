package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.features.cosmetic.CosmeticManager;
import dev.loki.lovisual.features.cosmetic.CosmeticModel;
import dev.loki.lovisual.features.cosmetic.CosmeticRenderer;
import dev.loki.lovisual.features.cosmetic.CosmeticRepository;
import dev.loki.lovisual.features.cosmetic.CosmeticType;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.features.module.WorldPhase;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Renders the locally equipped cosmetic (model/pet/accessory) attached to the local player.
 * Equip picking is a minimal next/prev keybind for now; a proper catalog screen is a follow-up.
 */
@ModuleInfo(
        id = "cosmeticdisplay",
        displayName = "Cosmetics",
        aliases = {"cosmetics"},
        category = ModuleCategory.VISUALS,
        description = "Отображает надетую косметику (модель/питомец/аксессуар)"
)
public final class CosmeticDisplay extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    {
        action("Next Cosmetic", "NONE");
        action("Prev Cosmetic", "NONE");
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
        }
    }

    private void cycle(int direction) {
        List<CosmeticModel> models = CosmeticRepository.get().byType(CosmeticType.MODEL);
        if (models.isEmpty()) return;

        CosmeticModel current = CosmeticManager.get().getActive(CosmeticType.MODEL);
        int index = current == null ? -1 : models.indexOf(current);
        int next = Math.floorMod(index + direction, models.size());

        CosmeticManager.get().setActive(CosmeticType.MODEL, models.get(next));
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.AFTER_POST_PROCESS;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        CosmeticRenderer.renderOnPlayer(renderer, mc.player, CosmeticType.MODEL, tickDelta);
        CosmeticRenderer.renderOnPlayer(renderer, mc.player, CosmeticType.PET, tickDelta);
        CosmeticRenderer.renderOnPlayer(renderer, mc.player, CosmeticType.ACCESSORY, tickDelta);
    }
}
