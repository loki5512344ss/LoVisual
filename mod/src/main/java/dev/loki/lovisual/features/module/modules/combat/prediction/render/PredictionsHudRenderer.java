package dev.loki.lovisual.features.module.modules.combat.prediction;

import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.Kind;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.TimerPlate;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.TrajectoryResult;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.helpers.clip.ScreenProjection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

public final class PredictionsHudRenderer {
    private PredictionsHudRenderer() {}

    private static final float PLATE_RADIUS = 1.7f;
    private static final float PLATE_TEXT_SCALE = 0.68f;
    private static final float PLATE_TEXT_PADDING = 24.0f;
    private static final float PLATE_HEIGHT = 16.0f;
    private static final float TNT_FUSE_TICKS = 80.0f;
    private static final float PLATE_BG_OFFSET_X = 2.0f;
    private static final float PLATE_BG_OFFSET_Y = 0.0f;
    private static final float PLATE_BG_SHRINK = 3.0f;
    private static final float PLATE_ICON_SIZE = 13.0f;
    private static final float PLATE_ICON_OFFSET_X = 4.0f;
    private static final float PLATE_ICON_TEXT_GAP = 4.0f;
    private static final float PLATE_TEXT_OFFSET_Y = 0.0f;
    private static final int PLATE_SHADOW = 0xAA000000;
    private static final float PLATE_SHADOW_BLUR = 3.6f;
    private static final float PLATE_SHADOW_INNER_ALPHA = 0.18f;

    public static void drawTimerPlateForResult(Predictions facade, Renderer2D renderer, TextRenderer textRenderer,
                                               TrajectoryResult res, float sx, float sy) {
        double seconds = res.flightTicks() / 20.0;
        if (seconds <= 0.0) return;
        ItemStack iconStack = useItemIcon(res.kind()) ? resolveTimerItem(res.kind()) : ItemStack.EMPTY;
        Identifier iconId = iconStack.isEmpty() ? resolveTimerIcon(res.kind()) : null;
        drawTimerPlate(facade, renderer, textRenderer, formatTime(seconds), sx, sy, res.argb(), iconStack, iconId);
    }

    public static void drawTntTimers(Predictions facade, Renderer2D renderer, TextRenderer textRenderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;
        if (world == null) return;
        int plateColor = facade.getTntColorValue().getArgb();
        for (Entity entity : world.entitiesForRendering()) {
            if (!(entity instanceof PrimedTnt tnt) || !tnt.isAlive() || tnt.isRemoved()) continue;
            int fuse = tnt.getFuse();
            if (fuse < 0) continue;
            Vec3 anchor = tnt.getPosition(tickDelta).add(0.0, tnt.getBbHeight() + 0.5, 0.0);
            Vec3 screen = ScreenProjection.worldToScreen(anchor, tickDelta);
            if (screen == null) continue;
            double seconds = Math.max(0.0, (fuse - tickDelta) / 20.0);
            drawTimerPlate(facade, renderer, textRenderer, formatTime(seconds),
                    (float) screen.x, (float) screen.y, plateColor, facade.getTntTimerIcon(), null);
            if (facade.isTntBarEnabled()) drawFuseBar(renderer, (float) screen.x, (float) screen.y, (fuse - tickDelta) / TNT_FUSE_TICKS, plateColor);
        }
    }

    private static void drawFuseBar(Renderer2D renderer, float cx, float plateY, float progress, int argb) {
        float w = 30.0f;
        float x = cx - w * 0.5f;
        float y = plateY + PLATE_HEIGHT + 2.0f;
        renderer.roundedRect(x, y, w, 2.5f, 1.2f, 0xA0000000);
        float fill = w * Math.max(0.0f, Math.min(1.0f, progress));
        if (fill > 1.0f) renderer.roundedRect(x, y, fill, 2.5f, 1.2f, argb | 0xFF000000);
    }

    public static void drawTimerPlate(Predictions facade, Renderer2D renderer, TextRenderer textRenderer,
                                      String txt, float sx, float sy, int argb,
                                      ItemStack iconStack, Identifier iconId) {
        if (txt == null || txt.isEmpty()) return;
        TextRenderer tr = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, textRenderer);
        float textW = (float) tr.getWidth(txt) * PLATE_TEXT_SCALE;
        float plateW = textW + PLATE_TEXT_PADDING;
        float plateX = sx - plateW * 0.5f;
        float plateY = sy;
        float bgX = plateX + PLATE_BG_OFFSET_X;
        float bgY = plateY + PLATE_BG_OFFSET_Y;
        float bgW = plateW - PLATE_BG_SHRINK;
        int plateShadow = argb != 0 ? (argb & 0x00FFFFFF) | (PLATE_SHADOW & 0xFF000000) : PLATE_SHADOW;
        renderer.roundedRectSoftShadow(bgX, bgY, bgW, PLATE_HEIGHT, PLATE_RADIUS, PLATE_SHADOW_BLUR, PLATE_SHADOW_INNER_ALPHA, plateShadow);
        float iconX = plateX + PLATE_ICON_OFFSET_X;
        float iconY = plateY + (PLATE_HEIGHT - PLATE_ICON_SIZE) * 0.5f;
        float iconScale = PLATE_ICON_SIZE / 16.0f;
        if (iconStack == null || iconStack.isEmpty()) {
            iconStack = ItemStack.EMPTY;
            drawTimerIcon(iconId, iconX, iconY, PLATE_ICON_SIZE);
        }
        float textX = iconX + PLATE_ICON_SIZE + PLATE_ICON_TEXT_GAP;
        float textH = (float) tr.getHeight() * PLATE_TEXT_SCALE;
        float textY = plateY + (PLATE_HEIGHT - textH) * 0.5f + PLATE_TEXT_OFFSET_Y;
        facade.getTimerPlates().add(new TimerPlate(textX, textY, txt, iconX, iconY, iconScale, iconStack));
    }

    private static void drawTimerIcon(Identifier iconId, float x, float y, float size) {
        if (iconId == null) return;
        Renderer2D.TEXTURE.roundedTexRect(x, y, size, size, 1.5f, 1.0f, 0, 0, 1, 1, 0xFFFFFFFF, iconId);
    }

    private static boolean useItemIcon(Kind kind) {
        return kind == Kind.SNOWBALL || kind == Kind.EGG || kind == Kind.FIREBALL
                || kind == Kind.FIREWORK || kind == Kind.WIND_CHARGE;
    }

    private static ItemStack resolveTimerItem(Kind kind) {
        return switch (kind) {
            case SNOWBALL -> new ItemStack(Items.SNOWBALL);
            case EGG -> new ItemStack(Items.EGG);
            case FIREBALL -> new ItemStack(Items.FIRE_CHARGE);
            case FIREWORK -> new ItemStack(Items.FIREWORK_ROCKET);
            case WIND_CHARGE -> new ItemStack(Items.WIND_CHARGE);
            default -> ItemStack.EMPTY;
        };
    }

    private static Identifier resolveTimerIcon(Kind kind) {
        return switch (kind) {
            case PEARL_ENTITY -> Identifier.fromNamespaceAndPath("lovisual", "textures/timers/pearl.png");
            case TRIDENT -> Identifier.fromNamespaceAndPath("lovisual", "textures/timers/trident.png");
            case POTION_SPLASH, POTION_LINGERING -> Identifier.fromNamespaceAndPath("lovisual", "textures/timers/potion.png");
            case ARROW -> Identifier.fromNamespaceAndPath("lovisual", "textures/timers/arrow.png");
            case FIREBALL, FIREWORK, WIND_CHARGE, SNOWBALL, EGG -> null;
        };
    }

    public static String formatTime(double seconds) {
        String s = String.format(Locale.US, "%.1f", seconds);
        s = s.replace('.', ',');
        return s + " s";
    }
}
