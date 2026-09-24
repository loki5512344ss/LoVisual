package dev.loki.lovisual.features.module.modules.visuals.esp.overlay;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.modules.visuals.esp.overlay.crystal.Hole;
import dev.loki.lovisual.features.module.modules.visuals.esp.overlay.crystal.HoleRenderer;
import dev.loki.lovisual.features.module.modules.visuals.esp.overlay.crystal.HoleScanner;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * Highlights blast-safe "holes" (single cells and two-block / 2x2 shapes fully surrounded by
 * bedrock or at least one indestructible block) around the player. Detection lives in
 * {@link HoleScanner}/{@link dev.loki.lovisual.features.module.modules.visuals.esp.overlay.crystal.BlockSafety},
 * drawing in {@link HoleRenderer}; this class is the module facade holding settings, the periodic
 * rescan tick and the world-render hook.
 */
@ModuleInfo(
        id = "holeesp",
        displayName = "CrystalHoles",
        aliases = {"holes", "crystalholes", "crystal"},
        category = ModuleCategory.VISUALS,
        description = "module.holeesp.description"
)
public final class CrystalHoles extends Module {
    private static final int RESCAN_TICKS = 10;

    private final NumberValue<Integer> rangeXZ =
            num("holeesp_range_xz", "range_xz", 10, 1, 128);
    private final NumberValue<Integer> rangeY =
            num("holeesp_range_y", "range_y", 5, 1, 128);
    private final NumberValue<Float> height =
            num("holeesp_height", "height", 1.0f, 0.01f, 5.0f);
    private final NumberValue<Float> lineWidth =
            num("holeesp_line_width", "line_width", 0.5f, 0.01f, 5.0f);
    private final BooleanValue culling =
            bool("holeesp_culling", "culling", true);
    private final RGBAColorValue indestructibleColor =
            color("holeesp_indestructible_color", "indestructible_color", "#FF7A00FF");
    private final RGBAColorValue bedrockColor =
            color("holeesp_bedrock_color", "bedrock_color", "#FF00FF51");

    private final HoleScanner scanner =
            new HoleScanner(rangeXZ, rangeY, height, indestructibleColor, bedrockColor);
    private final HoleRenderer holeRenderer = new HoleRenderer(rangeXZ);

    private List<Hole> holes = List.of();
    private int ticks;

    @Override
    public void onDisable() {
        holes = List.of();
        ticks = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) {
            holes = List.of();
            return;
        }

        if (ticks++ % RESCAN_TICKS == 0) {
            holes = scanner.scan(mc.level, mc.player.position());
        }
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || renderer == null || mc.level == null || mc.player == null || holes.isEmpty()) {
            return;
        }

        float previousLineWidth = RenderState.lineWidth;
        RenderState.lineWidth = Math.max(0.01f, lineWidth.get());
        try {
            for (Hole hole : holes) {
                if (culling.get() && !HoleRenderer.shouldRender(hole.box())) {
                    continue;
                }
                holeRenderer.render(renderer, hole);
            }
        } finally {
            RenderState.lineWidth = previousLineWidth;
        }
    }
}
