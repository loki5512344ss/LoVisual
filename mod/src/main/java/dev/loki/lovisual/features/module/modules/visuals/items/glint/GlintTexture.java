package dev.loki.lovisual.features.module.modules.visuals.items.glint;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Rewrites the two vanilla glint textures in place with a restyled copy of the pack's original image.
 * Remembers which texture object it painted so a resource reload (which re-uploads the originals) is detected.
 */
final class GlintTexture {
    /** What to paint onto a glint texture; a null paint means "leave it vanilla". */
    record Paint(String style, int primary, int secondary, float strength, float coverage) {
    }

    private final Map<AbstractTexture, Paint> painted = new IdentityHashMap<>();

    /** Applies {@code items} to the item/entity glint and {@code armor} to the armor glint. */
    void sync(Paint items, Paint armor) {
        sync(ItemFeatureRenderer.ENCHANTED_GLINT_ITEM, items);
        sync(ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR, armor);
    }

    void forget() {
        painted.clear();
    }

    private void sync(Identifier id, Paint paint) {
        Minecraft mc = Minecraft.getInstance();
        AbstractTexture texture = mc.getTextureManager().getTexture(id);
        if (!(texture instanceof ReloadableTexture reloadable) || texture.getTexture() == null) return;
        if (java.util.Objects.equals(painted.get(texture), paint)) return;
        if (upload(mc, reloadable, paint)) {
            if (paint == null) painted.remove(texture);
            else painted.put(texture, paint);
        }
    }

    private static boolean upload(Minecraft mc, ReloadableTexture texture, Paint paint) {
        Optional<Resource> resource = mc.getResourceManager().getResource(texture.resourceId());
        if (resource.isEmpty()) return false;
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            if (paint != null) repaint(image, paint);
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture.getTexture(), image);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void repaint(NativeImage image, Paint paint) {
        int width = image.getWidth();
        int height = image.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setPixel(x, y, GlintTint.pixel(image.getPixel(x, y), (x + 0.5f) / width, (y + 0.5f) / height,
                        paint.style(), paint.primary(), paint.secondary(), paint.strength(), paint.coverage()));
            }
        }
    }
}
