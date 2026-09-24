package dev.loki.lovisual.features.module.modules.visuals.player;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.relations.EntityFilters;
import dev.loki.lovisual.features.relations.PlayerRelations;
import dev.loki.lovisual.features.relations.category.CategoryService;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

final class CrosshairColorHelper {

    private static final int DEFAULT_COLOR = 0xFFFFFFFF;
    private static final float COLOR_SMOOTH_SPEED = 14.0f;

    private final Crosshair host;
    private int animatedColor = DEFAULT_COLOR;
    private boolean colorInitialized;

    CrosshairColorHelper(Crosshair host) {
        this.host = host;
    }

    void reset() {
        colorInitialized = false;
    }

    int resolveCrosshairTargetColor() {
        if (!host.highlightTarget.get()) return DEFAULT_COLOR;
        Minecraft mc = Minecraft.getInstance();
        HitResult hit = mc.hitResult;
        if (!(hit instanceof EntityHitResult ehr)) return DEFAULT_COLOR;
        Entity entity = ehr.getEntity();
        if (entity == null) return DEFAULT_COLOR;
        return resolveEntityColor(entity);
    }

    int smoothCrosshairColor(int targetColor) {
        if (!colorInitialized) {
            animatedColor = targetColor;
            colorInitialized = true;
            return animatedColor;
        }
        float t = AnimationUtility.clamp01(AnimationUtility.deltaTime() * COLOR_SMOOTH_SPEED);
        animatedColor = HudRenderUtil.mixColor(animatedColor, targetColor, t);
        if (colorDistance(animatedColor, targetColor) <= 2) animatedColor = targetColor;
        return animatedColor;
    }

    private static int colorDistance(int a, int b) {
        int dr = Math.abs(((a >>> 16) & 0xFF) - ((b >>> 16) & 0xFF));
        int dg = Math.abs(((a >>> 8) & 0xFF) - ((b >>> 8) & 0xFF));
        int db = Math.abs((a & 0xFF) - (b & 0xFF));
        int da = Math.abs(((a >>> 24) & 0xFF) - ((b >>> 24) & 0xFF));
        return dr + dg + db + da;
    }

    private int resolveEntityColor(Entity entity) {
        if (entity instanceof Player player) return CategoryService.getColor(player);
        PlayerRelations rel = PlayerRelations.get();
        boolean friendly = isFriendlyEntity(entity);
        return friendly ? rel.colorFriend() : rel.colorEnemy();
    }

    private boolean isFriendlyEntity(Entity entity) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (id != null && EntityFilters.get().isIgnoredEntity(id.toString())) return true;
        return entity.getType().getCategory() != MobCategory.MONSTER;
    }

    int defaultColor() {
        return DEFAULT_COLOR;
    }
}
