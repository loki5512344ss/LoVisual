package dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.render;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.mixininterface.entity.IRoundedHitbox;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/**
 * Геометрия/спрайты кнопок: скруглённый hitbox, отрисовка GUI-спрайтов.
 */
public final class ButtonSprites {
    private ButtonSprites() {
    }

    public static void markRounded(AbstractWidget widget, float radius) {
        if (widget instanceof IRoundedHitbox rounded) {
            rounded.lovisual$setRoundedHitbox(radius);
        }
    }

    public static int forceOpaque(int argb) {
        return (argb & 0x00FFFFFF) | 0xFF000000;
    }

    public static void drawGuiSprite(Identifier spriteId, int x, int y, int w, int h, float alpha) {
        if (spriteId == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        TextureAtlas atlas = mc.getAtlasManager().getAtlasOrThrow(AtlasIds.GUI);
        if (atlas == null) return;
        TextureAtlasSprite sprite = atlas.getSprite(spriteId);
        if (sprite == null) return;
        Identifier atlasId = atlas.location();
        if (atlasId == null) return;

        float scale = ViewportContext.getScaleFactor();
        float xb = x * scale;
        float yb = y * scale;
        float wb = w * scale;
        float hb = h * scale;
        int argb = HudRenderUtil.scaleAlpha(0xFFFFFFFF, alpha);

        ViewportContext.beginUnscaled(null);
        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(xb, yb, wb, hb, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), argb);
        Renderer2D.TEXTURE.end();
        Renderer2D.TEXTURE.render(atlasId);
        ViewportContext.end(null);
    }
}
