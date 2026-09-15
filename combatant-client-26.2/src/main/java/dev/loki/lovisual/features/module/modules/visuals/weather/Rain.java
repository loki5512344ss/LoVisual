package dev.loki.lovisual.features.module.modules.visuals.weather;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.core.system.LoVisualWorldMatrices;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessContext;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessManager;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;
import dev.loki.lovisual.render.engine.renderer.FullScreenRenderer;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.uniform.impl.world.liquid.WetSurfaceUniforms;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Volumetric rain: falling drop streaks, ground splashes with debris droplets, ground mist and lightning flashes.
 * Wet-ground screen-space reflections are a separate follow-up (see {@link #renderWetSurfaceFrame}).
 */
@ModuleInfo(
        id = "rain",
        displayName = "Rain",
        category = ModuleCategory.VISUALS,
        description = "Реалистичный объёмный дождь с брызгами, туманом и молниями"
)
public final class Rain extends Module implements PostProcessPass {

    private final Matrix4f wetSurfaceProjection = new Matrix4f();
    private final Matrix4f wetSurfaceInvView = new Matrix4f();
    private boolean wetSurfaceDepthSupported = true;

    {
        PostProcessManager.register(this);
    }

    private static final double GRAVITY = 22.0;
    private static final double TERMINAL_VELOCITY = 34.0;
    private static final int RIPPLE_SEGMENTS = 14;
    private static final int MAX_DROPS = 4200;
    private static final int MAX_MIST = 90;

    private final EnumValue<Preset> preset = enumMode("preset", Preset.RAIN, Preset.values());
    private final NumberValue<Float> density = num("density", 1.0f, 0.1f, 3.0f);
    private final NumberValue<Float> radius = num("radius", 26.0f, 8.0f, 64.0f);
    private final NumberValue<Float> altitude = num("altitude", 16.0f, 6.0f, 40.0f);
    private final NumberValue<Float> dropSize = num("drop_size", 1.0f, 0.3f, 2.5f);
    private final NumberValue<Float> fallSpeed = num("fall_speed", 1.0f, 0.3f, 2.5f);
    private final NumberValue<Float> wind = num("wind", 1.0f, 0.0f, 3.0f);
    private final NumberValue<Float> opacity = num("opacity", 1.0f, 0.1f, 2.0f);

    private final BooleanValue splashes = bool("splashes", true);
    private final BooleanValue droplets = visibleWhen(bool("splash_droplets", true), splashes::get);
    private final BooleanValue groundMist = bool("ground_mist", true);
    private final BooleanValue lightning = bool("lightning", false);
    private final BooleanValue skyCheck = bool("sky_check", true);
    private final BooleanValue onlyWhenRaining = bool("only_when_raining", false);

    private final EnumValue<ColorMode> colorMode = enumMode("color_mode", ColorMode.REALISTIC, ColorMode.values());
    private final RGBAColorValue customColor = visibleWhen(color("color", "#FFBFD8E6"), () -> colorMode.get() == ColorMode.CUSTOM);

    private final BooleanValue wetGround = bool("wet_ground", true);
    private final NumberValue<Float> wetness = visibleWhen(num("wetness", 1.25f, 0.1f, 2.0f), wetGround::get);
    private final NumberValue<Float> puddleCoverage = visibleWhen(num("puddle_coverage", 0.90f, 0.05f, 1.0f), wetGround::get);
    private final NumberValue<Float> reflectionStrength = visibleWhen(num("reflection_strength", 1.55f, 0.1f, 2.0f), wetGround::get);
    private final NumberValue<Float> reflectionDistance = visibleWhen(num("reflection_distance", 56.0f, 12.0f, 96.0f), wetGround::get);
    private final EnumValue<ReflectionQuality> reflectionQuality = visibleWhen(enumMode("reflection_quality", ReflectionQuality.BALANCED, ReflectionQuality.values()), wetGround::get);
    private final BooleanValue puddleRipples = visibleWhen(bool("puddle_ripples", true), wetGround::get);
    private final NumberValue<Float> rippleStrength = visibleWhen(num("ripple_strength", 0.8f, 0.0f, 1.6f), () -> wetGround.get() && puddleRipples.get());

    private final List<Drop> drops = new ArrayList<>();
    private final List<Splash> splashList = new ArrayList<>();
    private final List<Droplet> dropletList = new ArrayList<>();
    private final List<Mist> mistList = new ArrayList<>();
    private final Map<Long, Integer> groundCache = new HashMap<>();
    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
    private final Random random = new Random();

    private long lastFrameNanos;
    private float clock;
    private float flash;
    private float nextFlashIn = 9.0f;
    private int cacheTicks;

    @Override
    public void onEnable() {
        lastFrameNanos = 0L;
        wetSurfaceDepthSupported = true;
        clearAll();
    }

    @Override
    public void onDisable() {
        clearAll();
    }

    private void clearAll() {
        drops.clear();
        splashList.clear();
        dropletList.clear();
        mistList.clear();
        groundCache.clear();
        flash = 0.0f;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            clearAll();
            return;
        }
        if (++cacheTicks > 30) {
            cacheTicks = 0;
            groundCache.clear();
        }
    }

    private float presetDensity() {
        return switch (preset.get()) {
            case DRIZZLE -> 0.45f;
            case DOWNPOUR -> 1.9f;
            case STORM -> 2.8f;
            case RAIN -> 1.0f;
        };
    }

    private float presetWind() {
        return switch (preset.get()) {
            case DRIZZLE -> 0.5f;
            case DOWNPOUR -> 1.35f;
            case STORM -> 2.4f;
            case RAIN -> 1.0f;
        };
    }

    private float presetMass() {
        return switch (preset.get()) {
            case DRIZZLE -> 0.55f;
            case DOWNPOUR -> 1.25f;
            case STORM -> 1.5f;
            case RAIN -> 1.0f;
        };
    }

    private int targetDropCount() {
        float r = radius.get();
        float area = r * r * 0.0155f;
        int count = (int) (area * density.get() * presetDensity() * 26.0f);
        return Mth.clamp(count, 40, MAX_DROPS);
    }

    private int resolveColorArgb() {
        return switch (colorMode.get()) {
            case CUSTOM -> customColor.getArgb();
            case THEME -> Theme.theme().accent();
            case REALISTIC -> 0xFFC8DCE8;
        };
    }

    private float random(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private int surfaceY(int x, int z) {
        Minecraft mc = Minecraft.getInstance();
        long key = (((long) x) << 32) ^ (z & 0xFFFFFFFFL);
        Integer cached = groundCache.get(key);
        if (cached != null) return cached;

        int top;
        try {
            top = mc.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).getY();
        } catch (Throwable ignored) {
            top = mc.level.getMinY();
        }
        if (groundCache.size() < 20000) groundCache.put(key, top);
        return top;
    }

    private boolean isBlocking(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        scratchPos.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));

        BlockState state = mc.level.getBlockState(scratchPos);
        if (!state.getFluidState().isEmpty()) return true;
        if (state.isAir()) return false;
        try {
            return !state.getCollisionShape(mc.level, scratchPos).isEmpty();
        } catch (Throwable ignored) {
            return true;
        }
    }

    private boolean isWater(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        scratchPos.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        try {
            return !mc.level.getBlockState(scratchPos).getFluidState().isEmpty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean skyVisible(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (!skyCheck.get() || mc.level == null) return true;
        scratchPos.set(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        try {
            return mc.level.canSeeSky(scratchPos);
        } catch (Throwable ignored) {
            return true;
        }
    }

    private void spawnDrop(Vec3 center, boolean fromTop) {
        float r = radius.get();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = Math.sqrt(random.nextDouble()) * r;

        double x = center.x + Math.cos(angle) * dist;
        double z = center.z + Math.sin(angle) * dist;
        double y = fromTop
                ? center.y + altitude.get() * random(0.75f, 1.0f)
                : center.y + random(-4.0f, altitude.get());

        if (!skyVisible(x, y, z)) return;

        Drop drop = new Drop();
        drop.x = x;
        drop.y = y;
        drop.z = z;
        drop.mass = random(0.6f, 1.4f) * presetMass();
        drop.vy = -random(6.0f, 11.0f) * drop.mass;
        drop.phase = random(0.0f, 62.8f);
        drop.brightness = random(0.55f, 1.0f);
        drop.ground = surfaceY(Mth.floor(x), Mth.floor(z));
        drops.add(drop);
    }

    private void updateDrops(Vec3 center, float dt) {
        int target = targetDropCount();

        drops.removeIf(d -> {
            double dx = d.x - center.x;
            double dz = d.z - center.z;
            float r = radius.get() + 6.0f;
            return d.dead || dx * dx + dz * dz > r * r || d.y < center.y - altitude.get() - 12.0;
        });

        boolean fill = drops.size() < target / 2;
        int spawnBudget = fill ? Math.min(target - drops.size(), 900) : Math.min(target - drops.size(), 260);
        for (int i = 0; i < spawnBudget; i++) {
            spawnDrop(center, !fill);
        }

        float speedMul = fallSpeed.get();
        float windMul = wind.get() * presetWind();

        for (int i = 0, size = drops.size(); i < size; i++) {
            Drop d = drops.get(i);

            double gust = 0.65 + 0.35 * Math.sin(clock * 0.55 + d.phase * 0.15)
                    + 0.18 * Math.sin(clock * 1.7 + d.phase);
            double windX = windMul * 3.1 * gust;
            double windZ = windMul * 1.7 * Math.sin(clock * 0.31 + d.phase * 0.05) * gust;

            double drag = 1.0 - Math.min(0.92, Math.abs(d.vy) / (TERMINAL_VELOCITY * d.mass));
            d.vy -= GRAVITY * d.mass * drag * dt;
            if (d.vy < -TERMINAL_VELOCITY * d.mass) d.vy = -TERMINAL_VELOCITY * d.mass;

            d.vx += (windX - d.vx) * Math.min(1.0, dt * 3.2);
            d.vz += (windZ - d.vz) * Math.min(1.0, dt * 3.2);

            double nx = d.x + d.vx * dt * speedMul;
            double ny = d.y + d.vy * dt * speedMul;
            double nz = d.z + d.vz * dt * speedMul;

            boolean hit = false;
            if (ny <= d.ground + 2.5) {
                if (isBlocking(nx, ny, nz)) {
                    hit = true;
                } else if (ny <= d.ground - 3.0) {
                    d.dead = true;
                }
            }

            if (hit) {
                if (splashes.get()) addSplash(nx, ny, nz, d);
                d.dead = true;
                continue;
            }

            d.x = nx;
            d.y = ny;
            d.z = nz;
        }
    }

    private void addSplash(double x, double y, double z, Drop d) {
        if (splashList.size() > 700) return;

        double impactY = Math.floor(y) + 1.0005;
        Splash splash = new Splash();
        splash.x = x;
        splash.y = impactY;
        splash.z = z;
        splash.life = 0.0f;
        splash.maxLife = random(0.30f, 0.52f);
        splash.scale = (float) (0.32 + 0.22 * d.mass);
        splash.water = isWater(x, y - 0.15, z);
        splashList.add(splash);

        if (!droplets.get() || dropletList.size() > 1400) return;

        int shards = splash.water ? 4 : 3;
        for (int i = 0; i < shards; i++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            double sp = random(1.1f, 2.6f) * d.mass;
            Droplet drop = new Droplet();
            drop.x = x;
            drop.y = impactY + 0.02;
            drop.z = z;
            drop.vx = Math.cos(a) * sp * 0.45 + d.vx * 0.08;
            drop.vz = Math.sin(a) * sp * 0.45 + d.vz * 0.08;
            drop.vy = random(1.9f, 4.1f);
            drop.maxLife = random(0.28f, 0.5f);
            dropletList.add(drop);
        }
    }

    private void updateDroplets(float dt) {
        for (int i = dropletList.size() - 1; i >= 0; i--) {
            Droplet dr = dropletList.get(i);
            dr.life += dt;
            if (dr.life >= dr.maxLife) {
                dropletList.remove(i);
                continue;
            }
            dr.vy -= GRAVITY * 0.55 * dt;
            dr.x += dr.vx * dt;
            dr.y += dr.vy * dt;
            dr.z += dr.vz * dt;
        }
    }

    private void updateSplashes(float dt) {
        for (int i = splashList.size() - 1; i >= 0; i--) {
            Splash s = splashList.get(i);
            s.life += dt;
            if (s.life >= s.maxLife) splashList.remove(i);
        }
    }

    private void updateMist(Vec3 center, float dt) {
        if (!groundMist.get()) {
            mistList.clear();
            return;
        }

        int target = (int) Mth.clamp(22 * density.get() * presetDensity(), 6, MAX_MIST);

        mistList.removeIf(m -> {
            m.life += dt;
            double dx = m.x - center.x;
            double dz = m.z - center.z;
            float r = radius.get();
            m.x += m.vx * dt;
            m.z += m.vz * dt;
            return m.life > m.maxLife || dx * dx + dz * dz > r * r;
        });

        while (mistList.size() < target) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(random.nextDouble()) * radius.get() * 0.85;
            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            int ground = surfaceY(Mth.floor(x), Mth.floor(z));

            Mist m = new Mist();
            m.x = x;
            m.z = z;
            m.y = ground + random(0.05f, 0.9f);
            m.size = random(2.2f, 5.6f);
            m.maxLife = random(2.4f, 5.5f);
            m.life = random(0.0f, 1.0f);
            m.vx = random(-0.35f, 0.35f) * wind.get();
            m.vz = random(-0.35f, 0.35f) * wind.get();
            mistList.add(m);
        }
    }

    private void updateLightning(float dt) {
        if (!lightning.get()) {
            flash = 0.0f;
            return;
        }
        flash = Math.max(0.0f, flash - dt * 3.4f);
        nextFlashIn -= dt;
        if (nextFlashIn <= 0.0f) {
            flash = random(0.55f, 1.0f);
            nextFlashIn = random(6.0f, 16.0f);
        }
    }

    private float ambientLight(Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        try {
            int light = mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, camera.blockPosition());
            return Mth.clamp(light / 15.0f, 0.3f, 1.0f);
        } catch (Throwable ignored) {
            return 1.0f;
        }
    }

    // --- Wet-ground screen-space reflection pass (PostProcessPass) ---

    private int reflectionSteps() {
        return switch (reflectionQuality.get()) {
            case HIGH -> 16;
            case BALANCED -> 10;
            case PERFORMANCE -> 6;
        };
    }

    private int binarySteps() {
        return switch (reflectionQuality.get()) {
            case HIGH -> 6;
            case BALANCED -> 4;
            case PERFORMANCE -> 3;
        };
    }

    private float rainAmount() {
        return Mth.clamp(0.55f + density.get() * presetDensity() * 0.28f, 0.55f, 1.45f);
    }

    @Override
    public boolean isActive() {
        Minecraft mc = Minecraft.getInstance();
        return isEnabled()
                && wetGround.get()
                && wetSurfaceDepthSupported
                && mc.player != null
                && mc.level != null
                && (!onlyWhenRaining.get() || mc.level.isRaining())
                && playerSeesSky(mc);
    }

    /**
     * Gates the screen-space wet-surface pass by the player's own sky exposure. The shader
     * has no per-pixel occlusion, so without this it wets cave floors and undersides of leaf
     * canopy whenever it's raining anywhere in the world. Checking the player's own position
     * (rather than every visible surface) is an approximation, but it correctly stops the
     * effect the moment the player themself is indoors/underground/under leaves — the two
     * cases actually being reported.
     */
    private boolean playerSeesSky(Minecraft mc) {
        if (!skyCheck.get() || mc.player == null || mc.level == null) return true;
        try {
            return mc.level.canSeeSky(mc.player.blockPosition());
        } catch (Throwable ignored) {
            return true;
        }
    }

    @Override
    public int getPriority() {
        return 2;
    }

    @Override
    public Phase getPhase() {
        return Phase.PRE_HAND;
    }

    @Override
    public boolean render(GpuTextureView src, GpuTextureView dst, float tickDelta) {
        return false;
    }

    @Override
    public boolean render(PostProcessContext context, GpuTextureView src, GpuTextureView dst) {
        if (!isActive() || context == null || src == null || dst == null) return false;

        GpuTextureView mainDepth = context.mainDepth();
        boolean hasDepth = mainDepth != null;
        if (!hasDepth) mainDepth = src;

        Matrix4f projection = LoVisualWorldMatrices.renderProjectionMatrix();
        if (projection == null) projection = RenderState.worldProjection;
        wetSurfaceProjection.set(projection);
        wetSurfaceInvView.set(new Matrix4f().rotation(RenderState.cameraRotation));

        WetSurfaceUniforms.update(
                wetSurfaceProjection,
                wetSurfaceInvView,
                (float) RenderState.cameraPos.x, (float) RenderState.cameraPos.y, (float) RenderState.cameraPos.z,
                context.width() > 0 ? 1.0f / context.width() : 0.0f,
                context.height() > 0 ? 1.0f / context.height() : 0.0f,
                clock,
                hasDepth,
                wetness.get(),
                puddleCoverage.get(),
                reflectionStrength.get(),
                puddleRipples.get() ? rippleStrength.get() : 0.0f,
                reflectionDistance.get(),
                reflectionSteps(),
                rainAmount(),
                puddleRipples.get(),
                binarySteps(),
                0.18f
        );

        try {
            FullScreenRenderer.ensureInit();
            FullScreenRenderer.begin("LoVisual WetSurface Pass")
                    .attachment(dst)
                    .pipeline(LoVisualRenderPipelines.WET_SURFACE)
                    .uniform("WetSurface", WetSurfaceUniforms.get())
                    .sampler("u_Texture", src, PostProcessManager.getSampler())
                    .sampler("u_MainDepth", mainDepth, PostProcessManager.getSampler())
                    .end();
        } catch (Throwable t) {
            wetSurfaceDepthSupported = false;
            return false;
        }

        return true;
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.AFTER_POST_PROCESS;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null || mc.gameRenderer == null) return;
        if (mc.gameRenderer.mainCamera() == null) return;
        if (onlyWhenRaining.get() && !mc.level.isRaining()) {
            clearAll();
            return;
        }

        Camera camera = mc.gameRenderer.mainCamera();

        long now = System.nanoTime();
        if (lastFrameNanos == 0L) lastFrameNanos = now;
        float dt = (float) ((now - lastFrameNanos) / 1_000_000_000.0);
        lastFrameNanos = now;
        dt = Mth.clamp(dt, 0.0f, 0.05f);
        clock += dt;

        Vec3 cam = camera.position();

        updateDrops(cam, dt);
        updateSplashes(dt);
        updateDroplets(dt);
        updateMist(cam, dt);
        updateLightning(dt);

        if (drops.isEmpty() && splashList.isEmpty() && dropletList.isEmpty() && mistList.isEmpty()) return;

        int colorArgb = resolveColorArgb();
        float r = ((colorArgb >>> 16) & 0xFF) / 255f;
        float g = ((colorArgb >>> 8) & 0xFF) / 255f;
        float b = (colorArgb & 0xFF) / 255f;
        float[] rgb = {r, g, b};

        float ambient = ambientLight(camera);
        float brightnessMul = Mth.clamp(ambient + flash * 0.9f, 0.0f, 1.65f);
        float alphaMul = opacity.get() * brightnessMul;

        Quaternionf camRot = RenderState.cameraRotation;
        float camYaw = camera.yRot();

        MeshBuilder blendMesh = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED, Renderer3D.DepthMode.MAIN);
        if (blendMesh != null) {
            if (groundMist.get()) drawMist(blendMesh, camRot, rgb, alphaMul);
            drawStreaks(blendMesh, camYaw, rgb, alphaMul, false);
            if (splashes.get()) drawRipples(blendMesh, rgb, alphaMul);
        }

        MeshBuilder additiveMesh = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE_DEPTH, Renderer3D.DepthMode.PRE_DEPTH);
        if (additiveMesh != null) {
            drawStreaks(additiveMesh, camYaw, rgb, alphaMul, true);
            if (splashes.get()) {
                drawCrowns(additiveMesh, camYaw, rgb, alphaMul);
                if (droplets.get()) drawDroplets(additiveMesh, camYaw, rgb, alphaMul);
            }
        }
    }

    private static int packColor(float r, float g, float b, float a) {
        int ai = Mth.clamp(Math.round(a * 255.0f), 0, 255);
        int ri = Mth.clamp(Math.round(r * 255.0f), 0, 255);
        int gi = Mth.clamp(Math.round(g * 255.0f), 0, 255);
        int bi = Mth.clamp(Math.round(b * 255.0f), 0, 255);
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private static Vector3f rotated(Quaternionf rot, float x, float y, float z) {
        return new Vector3f(x, y, z).rotate(rot);
    }

    private void drawStreaks(MeshBuilder mesh, float camYaw, float[] rgb, float alphaMul, boolean highlight) {
        if (drops.isEmpty()) return;

        float sizeMul = dropSize.get();
        float r = radius.get();
        float fadeStart = r * 0.62f;

        for (int i = 0, size = drops.size(); i < size; i++) {
            Drop d = drops.get(i);

            double speed = Math.sqrt(d.vx * d.vx + d.vy * d.vy + d.vz * d.vz);
            if (speed < 0.0001) continue;

            float length = (float) Mth.clamp((float) (speed * 0.055 * sizeMul), 0.16f, 2.6f);
            float width = (float) (0.014 + 0.019 * d.mass) * sizeMul * (highlight ? 0.45f : 1.0f);

            float yawRad = (float) Math.toRadians(camYaw);
            double rightX = Math.cos(yawRad);
            double rightZ = Math.sin(yawRad);
            double horizontal = d.vx * rightX + d.vz * rightZ;
            float tilt = (float) Math.toDegrees(Math.atan2(horizontal, Math.abs(d.vy) + 0.0001));
            tilt = Mth.clamp(tilt, -55.0f, 55.0f);

            Quaternionf orient = new Quaternionf()
                    .rotateY((float) Math.toRadians(-camYaw))
                    .rotateZ((float) Math.toRadians(-tilt));

            float half = width * 0.5f;
            float top = length * 0.5f;
            float bottom = -length * 0.5f;

            float headAlpha = alphaMul * d.brightness * (highlight ? 0.30f : 0.42f);
            if (headAlpha <= 0.006f) continue;
            headAlpha = Math.min(headAlpha, 0.95f);
            float tailAlpha = 0.0f;
            float headWidth = half;
            float tailWidth = half * 0.35f;

            Vector3f p1 = rotated(orient, -tailWidth, top, 0.0f);
            Vector3f p2 = rotated(orient, tailWidth, top, 0.0f);
            Vector3f p3 = rotated(orient, headWidth, bottom, 0.0f);
            Vector3f p4 = rotated(orient, -headWidth, bottom, 0.0f);

            mesh.ensureQuadCapacity();
            int i1 = mesh.vec3(d.x + p1.x, d.y + p1.y, d.z + p1.z).color(packColorObj(rgb, tailAlpha)).next();
            int i2 = mesh.vec3(d.x + p2.x, d.y + p2.y, d.z + p2.z).color(packColorObj(rgb, tailAlpha)).next();
            int i3 = mesh.vec3(d.x + p3.x, d.y + p3.y, d.z + p3.z).color(packColorObj(rgb, headAlpha)).next();
            int i4 = mesh.vec3(d.x + p4.x, d.y + p4.y, d.z + p4.z).color(packColorObj(rgb, headAlpha)).next();
            mesh.quad(i1, i2, i3, i4);
        }
    }

    private void drawRipples(MeshBuilder mesh, float[] rgb, float alphaMul) {
        if (splashList.isEmpty()) return;

        for (int i = 0, size = splashList.size(); i < size; i++) {
            Splash s = splashList.get(i);
            float progress = Mth.clamp(s.life / s.maxLife, 0.0f, 1.0f);
            float eased = 1.0f - (1.0f - progress) * (1.0f - progress);

            float outer = s.scale * (0.18f + eased * (s.water ? 1.35f : 0.85f));
            float inner = outer * (0.55f + 0.35f * eased);
            float alpha = alphaMul * (1.0f - progress) * (s.water ? 0.5f : 0.38f);
            if (alpha <= 0.01f) continue;

            for (int seg = 0; seg < RIPPLE_SEGMENTS; seg++) {
                double a0 = (Math.PI * 2.0 / RIPPLE_SEGMENTS) * seg;
                double a1 = (Math.PI * 2.0 / RIPPLE_SEGMENTS) * (seg + 1);

                float ix0 = (float) (Math.cos(a0) * inner);
                float iz0 = (float) (Math.sin(a0) * inner);
                float ix1 = (float) (Math.cos(a1) * inner);
                float iz1 = (float) (Math.sin(a1) * inner);
                float ox0 = (float) (Math.cos(a0) * outer);
                float oz0 = (float) (Math.sin(a0) * outer);
                float ox1 = (float) (Math.cos(a1) * outer);
                float oz1 = (float) (Math.sin(a1) * outer);

                mesh.ensureQuadCapacity();
                int i1 = mesh.vec3(s.x + ix0, s.y, s.z + iz0).color(packColorObj(rgb, alpha)).next();
                int i2 = mesh.vec3(s.x + ix1, s.y, s.z + iz1).color(packColorObj(rgb, alpha)).next();
                int i3 = mesh.vec3(s.x + ox1, s.y, s.z + oz1).color(packColorObj(rgb, 0.0f)).next();
                int i4 = mesh.vec3(s.x + ox0, s.y, s.z + oz0).color(packColorObj(rgb, 0.0f)).next();
                mesh.quad(i1, i2, i3, i4);
            }
        }
    }

    private void drawCrowns(MeshBuilder mesh, float camYaw, float[] rgb, float alphaMul) {
        if (splashList.isEmpty()) return;

        Quaternionf orient = new Quaternionf().rotateY((float) Math.toRadians(-camYaw));

        for (int i = 0, size = splashList.size(); i < size; i++) {
            Splash s = splashList.get(i);
            float progress = Mth.clamp(s.life / s.maxLife, 0.0f, 1.0f);
            if (progress > 0.55f) continue;

            float local = progress / 0.55f;
            float height = s.scale * (0.55f * (float) Math.sin(local * Math.PI));
            float alpha = alphaMul * (1.0f - local) * 0.32f;
            if (alpha <= 0.01f || height <= 0.005f) continue;

            float base = s.scale * 0.16f;
            Vector3f p1 = rotated(orient, -base * 0.25f, height, 0.0f);
            Vector3f p2 = rotated(orient, base * 0.25f, height, 0.0f);
            Vector3f p3 = rotated(orient, base, 0.0f, 0.0f);
            Vector3f p4 = rotated(orient, -base, 0.0f, 0.0f);

            mesh.ensureQuadCapacity();
            int i1 = mesh.vec3(s.x + p1.x, s.y + p1.y, s.z + p1.z).color(packColorObj(rgb, 0.0f)).next();
            int i2 = mesh.vec3(s.x + p2.x, s.y + p2.y, s.z + p2.z).color(packColorObj(rgb, 0.0f)).next();
            int i3 = mesh.vec3(s.x + p3.x, s.y + p3.y, s.z + p3.z).color(packColorObj(rgb, alpha)).next();
            int i4 = mesh.vec3(s.x + p4.x, s.y + p4.y, s.z + p4.z).color(packColorObj(rgb, alpha)).next();
            mesh.quad(i1, i2, i3, i4);
        }
    }

    private void drawDroplets(MeshBuilder mesh, float camYaw, float[] rgb, float alphaMul) {
        if (dropletList.isEmpty()) return;

        float sizeMul = dropSize.get();
        float yawRad = (float) Math.toRadians(camYaw);

        for (int i = 0, size = dropletList.size(); i < size; i++) {
            Droplet d = dropletList.get(i);
            float progress = Mth.clamp(d.life / d.maxLife, 0.0f, 1.0f);
            float alpha = alphaMul * (1.0f - progress) * 0.5f;
            if (alpha <= 0.01f) continue;

            double speed = Math.sqrt(d.vx * d.vx + d.vy * d.vy + d.vz * d.vz);
            float length = (float) Mth.clamp((float) (speed * 0.03 * sizeMul), 0.04f, 0.4f);
            float width = 0.016f * sizeMul;

            double horizontal = d.vx * Math.cos(yawRad) + d.vz * Math.sin(yawRad);
            float tilt = (float) Math.toDegrees(Math.atan2(horizontal, -d.vy + 0.0001));

            Quaternionf orient = new Quaternionf()
                    .rotateY((float) Math.toRadians(-camYaw))
                    .rotateZ((float) Math.toRadians(-tilt));

            Vector3f p1 = rotated(orient, -width * 0.4f, length * 0.5f, 0.0f);
            Vector3f p2 = rotated(orient, width * 0.4f, length * 0.5f, 0.0f);
            Vector3f p3 = rotated(orient, width, -length * 0.5f, 0.0f);
            Vector3f p4 = rotated(orient, -width, -length * 0.5f, 0.0f);

            mesh.ensureQuadCapacity();
            int i1 = mesh.vec3(d.x + p1.x, d.y + p1.y, d.z + p1.z).color(packColorObj(rgb, 0.0f)).next();
            int i2 = mesh.vec3(d.x + p2.x, d.y + p2.y, d.z + p2.z).color(packColorObj(rgb, 0.0f)).next();
            int i3 = mesh.vec3(d.x + p3.x, d.y + p3.y, d.z + p3.z).color(packColorObj(rgb, alpha)).next();
            int i4 = mesh.vec3(d.x + p4.x, d.y + p4.y, d.z + p4.z).color(packColorObj(rgb, alpha)).next();
            mesh.quad(i1, i2, i3, i4);
        }
    }

    private void drawMist(MeshBuilder mesh, Quaternionf camRot, float[] rgb, float alphaMul) {
        if (mistList.isEmpty()) return;

        for (int i = 0, size = mistList.size(); i < size; i++) {
            Mist mist = mistList.get(i);
            float progress = Mth.clamp(mist.life / mist.maxLife, 0.0f, 1.0f);
            float fade = (float) Math.sin(progress * Math.PI);
            float alpha = alphaMul * fade * 0.05f;
            if (alpha <= 0.004f) continue;

            float half = mist.size * 0.5f;
            float top = mist.size * 0.42f;

            Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(camRot);
            Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(camRot);

            double bx = mist.x, by = mist.y, bz = mist.z;

            mesh.ensureQuadCapacity();
            int i1 = mesh.vec3(bx - right.x * half, by - right.y * half + up.y * top, bz - right.z * half).color(packColorObj(rgb, 0.0f)).next();
            int i2 = mesh.vec3(bx + right.x * half, by + right.y * half + up.y * top, bz + right.z * half).color(packColorObj(rgb, 0.0f)).next();
            int i3 = mesh.vec3(bx + right.x * half, by + right.y * half, bz + right.z * half).color(packColorObj(rgb, alpha)).next();
            int i4 = mesh.vec3(bx - right.x * half, by - right.y * half, bz - right.z * half).color(packColorObj(rgb, alpha)).next();
            mesh.quad(i1, i2, i3, i4);
        }
    }

    private dev.loki.lovisual.render.engine.color.RenderColor packColorObj(float[] rgb, float a) {
        return new dev.loki.lovisual.render.engine.color.RenderColor(packColor(rgb[0], rgb[1], rgb[2], a));
    }

    public enum Preset implements EnumValue.IdProvider {
        DRIZZLE("drizzle"),
        RAIN("rain"),
        DOWNPOUR("downpour"),
        STORM("storm");

        private final String id;

        Preset(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    public enum ColorMode implements EnumValue.IdProvider {
        REALISTIC("realistic"),
        THEME("theme"),
        CUSTOM("custom");

        private final String id;

        ColorMode(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    public enum ReflectionQuality implements EnumValue.IdProvider {
        PERFORMANCE("performance"),
        BALANCED("balanced"),
        HIGH("high");

        private final String id;

        ReflectionQuality(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }
    }

    private static final class Drop {
        double x, y, z;
        double vx, vy, vz;
        double mass = 1.0;
        double ground;
        float phase;
        float brightness = 1.0f;
        boolean dead;
    }

    private static final class Droplet {
        double x, y, z;
        double vx, vy, vz;
        float life;
        float maxLife = 0.4f;
    }

    private static final class Splash {
        double x, y, z;
        float life;
        float maxLife = 0.4f;
        float scale = 0.4f;
        boolean water;
    }

    private static final class Mist {
        double x, y, z;
        double vx, vz;
        float size = 3.0f;
        float life;
        float maxLife = 4.0f;
    }
}
