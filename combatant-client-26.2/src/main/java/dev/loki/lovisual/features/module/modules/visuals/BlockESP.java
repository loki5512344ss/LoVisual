package dev.loki.lovisual.features.module.modules.visuals;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.AABB;
import dev.loki.lovisual.config.values.EnumValue;
import dev.loki.lovisual.config.values.ItemIdSetValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.config.values.RGBAColorValue;
import dev.loki.lovisual.features.gui.clickgui.settings.TextListSetting;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.features.module.WorldPhase;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.util.time.Timer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

//todo Description
@ModuleInfo(
        id = "blockesp",
        displayName = "BlockESP",
        category = ModuleCategory.VISUALS,
        description = "Подсвечивает выбранные блоки сквозь стены"
)
public final class BlockESP extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    private final ItemIdSetValue blockList =
            itemList("block_list", TextListSetting.PickerMode.BLOCKS);

    private final NumberValue<Integer> range =
            num("range", 64, 8, 160);

    private final NumberValue<Float> scanInterval =
            num("scan_interval", 2.0f, 0.5f, 10.0f);

    private final RGBAColorValue fillColor =
            color("fill_color", "#4000FFC8");

    private final RGBAColorValue fillColor2 =
            visibleWhen(color("fill_color2", "#40FFAA00"), this::usesSecondaryColor);

    private final EnumValue<ColorMode> colorMode =
            enumMode("color_mode", ColorMode.PER_BLOCK, ColorMode.values());

    private final NumberValue<Integer> colorSpeed =
            visibleWhen(num("color_speed", 18, 2, 54), () -> colorMode.get() != ColorMode.PER_BLOCK && colorMode.get() != ColorMode.STATIC);

    private final Timer scanTimer = new Timer();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "BlockESP-Scanner");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private final AtomicBoolean scanInProgress = new AtomicBoolean(false);
    private volatile List<BlockPos> foundBlocks = new ArrayList<>();

    public BlockESP() {
    }

    @Override
    public void onEnable() {
        scanTimer.setMs(0);
        scanning.set(true);
    }

    @Override
    public void onDisable() {
        scanning.set(false);
        foundBlocks = new ArrayList<>();
    }

    @Override
    public void onTick() {
        if (mc.level == null || mc.player == null) return;

        if (scanTimer.passedS(scanInterval.get())) {
            scanTimer.reset();
            requestScan();
        }
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.BEFORE_TRANSLUCENT;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        if (blockList.get().isEmpty()) return;

        List<BlockPos> snapshot = foundBlocks;
        for (BlockPos pos : snapshot) {
            BlockState state = mc.level.getBlockState(pos);
            if (state.isAir()) continue;
            if (!blockList.get().contains(blockId(state.getBlock()))) continue;

            int argb = resolveColor(state.getBlock(), pos);
            addFilledBox(renderer, new AABB(pos), argb);
        }
    }

    private void requestScan() {
        if (mc.level == null || mc.player == null) return;
        if (!scanning.get()) return;
        if (executor.isShutdown()) return;
        if (!scanInProgress.compareAndSet(false, true)) return;

        executor.submit(() -> {
            try {
                scan();
            } finally {
                scanInProgress.set(false);
            }
        });
    }

    private void scan() {
        if (mc.level == null || mc.player == null) return;

        List<BlockPos> result = new ArrayList<>();
        BlockPos center = mc.player.blockPosition();
        int r = range.get();

        int minX = center.getX() - r;
        int maxX = center.getX() + r;
        int minY = Math.max(mc.level.getMinY(), center.getY() - r);
        int maxY = Math.min(mc.level.getMaxY(), center.getY() + r);
        int minZ = center.getZ() - r;
        int maxZ = center.getZ() + r;

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!scanning.get()) return;
                if (!mc.level.hasChunk(cx, cz)) continue;

                int chunkMinX = Math.max(minX, cx << 4);
                int chunkMaxX = Math.min(maxX, (cx << 4) + 15);
                int chunkMinZ = Math.max(minZ, cz << 4);
                int chunkMaxZ = Math.min(maxZ, (cz << 4) + 15);

                for (int x = chunkMinX; x <= chunkMaxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = chunkMinZ; z <= chunkMaxZ; z++) {
                            if (!scanning.get()) return;

                            BlockPos pos = new BlockPos(x, y, z);
                            BlockState state = mc.level.getBlockState(pos);
                            if (state.isAir()) continue;

                            if (blockList.get().contains(blockId(state.getBlock()))) {
                                result.add(pos.immutable());
                            }
                        }
                    }
                }
            }
        }

        foundBlocks = result;
    }

    private String blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private int resolveColor(Block block, BlockPos pos) {
        if (colorMode.get() == ColorMode.PER_BLOCK) {
            MapColor mapColor = block.defaultMapColor();
            return withAlpha(mapColor.col, alpha(fillColor.getArgb()));
        }

        return AnimatedRenderColors.resolve(
                animatedColorMode(),
                colorSpeed.get(),
                spatialIndex(pos),
                fillColor.getArgb(),
                fillColor2.getArgb(),
                true
        );
    }

    private AnimatedRenderColors.Mode animatedColorMode() {
        return switch (colorMode.get()) {
            case PER_BLOCK, STATIC -> AnimatedRenderColors.Mode.STATIC;
            case RAINBOW -> AnimatedRenderColors.Mode.RAINBOW;
            case FADE -> AnimatedRenderColors.Mode.FADE;
        };
    }

    private boolean usesSecondaryColor() {
        return AnimatedRenderColors.usesSecondary(animatedColorMode());
    }

    private int spatialIndex(BlockPos pos) {
        return Math.floorMod(pos.getX() * 13 + pos.getY() * 7 + pos.getZ() * 17, 3600);
    }

    private static void addFilledBox(Renderer3D renderer, AABB box, int argb) {
        int a = alpha(argb);
        int r = red(argb);
        int g = green(argb);
        int b = blue(argb);

        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        renderer.quad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        renderer.quad(minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
        renderer.quad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        renderer.quad(minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
        renderer.quad(maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
        renderer.quad(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private static int red(int argb) {
        return (argb >>> 16) & 255;
    }

    private static int green(int argb) {
        return (argb >>> 8) & 255;
    }

    private static int blue(int argb) {
        return argb & 255;
    }

    private static int alpha(int argb) {
        return (argb >>> 24) & 255;
    }

    public enum ColorMode implements EnumValue.IdProvider {
        PER_BLOCK("per_block"),
        STATIC("static"),
        RAINBOW("rainbow"),
        FADE("fade");

        private final String id;

        ColorMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }
}
