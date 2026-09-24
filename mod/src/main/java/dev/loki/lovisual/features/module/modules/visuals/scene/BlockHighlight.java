package dev.loki.lovisual.features.module.modules.visuals.scene;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;

//todo Description
@ModuleInfo(
        id = "blockhighlight",
        displayName = "BlockHighlight",
        category = ModuleCategory.VISUALS
)
public class BlockHighlight extends Module {

    private final BlockHighlightColors colors = new BlockHighlightColors();
    private final BlockHighlightGeometry geometry = new BlockHighlightGeometry();
    private final BlockHighlightOutline outline = new BlockHighlightOutline();
    private final BlockHighlightBreaking breaking = new BlockHighlightBreaking();
    private final BooleanValue outlineEnabled =
            bool(
                    "outline_enabled",
                    true
            );
    private final EnumValue<OutlineMode> outlineMode =
            visibleWhen(enumMode(
                    "outline_mode",
                    OutlineMode.LINES,
                    OutlineMode.values()
            ), outlineEnabled::get);
    private final RGBAColorValue outlineColor =
            visibleWhen(color(
                    "outline_color",
                    "#66FFFFFF"
            ), outlineEnabled::get);
    private final EnumValue<ColorMode> outlineColorMode =
            visibleWhen(enumCommon(
                    "outline_color_mode",
                    "outline_color_mode",
                    CommonSettingSchemas.RENDER_COLOR_MODE,
                    ColorMode.STATIC,
                    ColorMode.values()
            ), outlineEnabled::get);
    private final RGBAColorValue outlineColor2 =
            visibleWhen(color(
                    "outline_color2",
                    "#FFFFAA00"
            ), this::usesSecondaryOutlineColor);
    private final EnumValue<ShaderStyle> outlineShaderStyle =
            visibleWhen(enumMode(
                    "outline_shader_style",
                    ShaderStyle.WATER_CAUSTIC,
                    ShaderStyle.values()
            ), this::usesShaderOutline);
    private final NumberValue<Integer> outlineColorSpeed =
            visibleWhen(num(
                    "outline_color_speed",
                    18,
                    2,
                    54
            ), outlineEnabled::get);
    private final NumberValue<Float> outlineThickness =
            visibleWhen(num(
                    "outline_thickness",
                    1.0f,
                    0.5f,
                    4.0f
            ), outlineEnabled::get);
    private final BooleanValue outlineDepthTest =
            visibleWhen(bool(
                    "outline_depth_test",
                    false
            ), outlineEnabled::get);
    private final BooleanValue breakingEnabled =
            bool(
                    "breaking_enabled",
                    true
            );
    private final BooleanValue breakingSelf =
            visibleWhen(bool(
                    "breaking_self",
                    true
            ), breakingEnabled::get);
    private final BooleanValue breakingOtherPlayers =
            visibleWhen(bool(
                    "breaking_other_players",
                    true
            ), breakingEnabled::get);
    private final EnumValue<BreakAnimation> breakingAnimation =
            visibleWhen(enumMode(
                    "breaking_animation",
                    BreakAnimation.GROW,
                    BreakAnimation.values()
            ), breakingEnabled::get);
    private final RGBAColorValue breakingColor =
            visibleWhen(color(
                    "breaking_color",
                    "#66FFFFFF"
            ), breakingEnabled::get);
    private final RGBAColorValue breakingColor2 =
            visibleWhen(color(
                    "breaking_color2",
                    "#22FFAA00"
            ), breakingEnabled::get);

    public boolean isOutlineActive() {
        return isEnabled() && outlineEnabled.get();
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.BEFORE_TRANSLUCENT;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        if (outlineEnabled.get()) {
            renderTargetOutline(renderer);
        }

        if (breakingEnabled.get()) {
            breaking.render(renderer, colors, geometry, breakingAnimation.get(),
                    breakingSelf.get(), breakingOtherPlayers.get(),
                    breakingColor.getArgb(), breakingColor2.getArgb());
        }
    }

    private void renderTargetOutline(Renderer3D renderer) {
        Minecraft mc = Minecraft.getInstance();
        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) return;
        if (hit.getType() == HitResult.Type.MISS) return;

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return;

        Entity cameraEntity = mc.getCameraEntity();
        CollisionContext context = cameraEntity != null ? CollisionContext.of(cameraEntity) : CollisionContext.empty();
        VoxelShape shape = state.getShape(mc.level, pos, context);
        if (shape.isEmpty()) return;

        VoxelShape worldShape = shape.move(pos.getX(), pos.getY(), pos.getZ());
        OutlineMode mode = outlineMode.get();
        boolean depthTest = outlineDepthTest.get();
        int primary = outlineColor.getArgb();
        int secondary = outlineColor2.getArgb();

        if (mode == OutlineMode.BOX || mode == OutlineMode.BOTH) {
            outline.renderBoxes(renderer, worldShape, depthTest, colors, geometry,
                    outlineColorMode.get(), outlineColorSpeed.get(), primary, secondary);
        }

        if (mode == OutlineMode.LINES || mode == OutlineMode.BOTH) {
            outline.renderLines(renderer, worldShape, depthTest, outlineThickness.get(), colors, geometry,
                    outlineColorMode.get(), outlineColorSpeed.get(), primary, secondary);
        }

        if (mode == OutlineMode.SHADER) {
            outline.renderShader(renderer, worldShape, outlineShaderStyle.get(), colors, geometry,
                    primary, secondary);
        }
    }

    private boolean usesSecondaryOutlineColor() {
        if (!outlineEnabled.get()) return false;
        return colors.usesSecondary(outlineColorMode.get());
    }

    private boolean usesShaderOutline() {
        return outlineEnabled.get() && outlineMode.get() == OutlineMode.SHADER;
    }

    public enum ColorMode implements EnumValue.IdProvider {
        STATIC("static"),
        RAINBOW("rainbow"),
        LIGHT_RAINBOW("light_rainbow"),
        SKY("sky"),
        FADE("fade"),
        DOUBLE_COLOR("double_color"),
        ANALOGOUS("analogous"),
        THEME("theme");

        private final String id;

        ColorMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    public enum OutlineMode implements EnumValue.IdProvider {
        LINES("lines"),
        BOX("box"),
        BOTH("both"),
        SHADER("shader");

        private final String id;

        OutlineMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    public enum ShaderStyle implements EnumValue.IdProvider {
        WATER_CAUSTIC("water_caustic"),
        SCAN_LINES("scan_lines"),
        HOLOGRAM("hologram");

        private final String id;

        ShaderStyle(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    public enum BreakAnimation implements EnumValue.IdProvider {
        GROW("grow"),
        SHRINK("shrink"),
        STATIC("static");

        private final String id;

        BreakAnimation(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }
}
