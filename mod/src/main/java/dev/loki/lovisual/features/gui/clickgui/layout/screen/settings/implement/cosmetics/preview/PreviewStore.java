package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics.preview;

import com.mojang.blaze3d.platform.NativeImage;
import dev.loki.lovisual.util.logging.DebugLog;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** The player's own card pictures: one PNG plus one crop per cosmetic, under config/lovisual/cosmetics/previews. */
public final class PreviewStore {
    public record Preview(Identifier texture, int width, int height, PreviewCrop crop) {
    }

    private static final Preview NONE = new Preview(null, 0, 0, null);
    private static final String NAMESPACE = "lovisual";

    private final Map<String, Preview> cache = new HashMap<>();

    /** The card picture for a cosmetic, or null when the player has not set one. */
    public Preview get(String id) {
        Preview cached = cache.computeIfAbsent(id, this::load);
        return cached == NONE ? null : cached;
    }

    /** Copies the picture in as PNG, stores the crop and refreshes the texture. */
    public boolean save(String id, Path source, PreviewCrop crop) {
        try (InputStream in = Files.newInputStream(source); NativeImage image = NativeImage.read(in)) {
            Files.createDirectories(dir());
            image.writeToFile(png(id));
            Files.writeString(cropFile(id), crop.serialize());
        } catch (IOException | RuntimeException e) {
            DebugLog.errorOnce("cosmetic-preview-save-" + id, "[Cosmetics] Failed to save preview", e);
            return false;
        }
        reload(id);
        return true;
    }

    /** Stores a new crop for the picture already saved. */
    public void saveCrop(String id, PreviewCrop crop) {
        try {
            Files.writeString(cropFile(id), crop.serialize());
        } catch (IOException e) {
            DebugLog.errorOnce("cosmetic-preview-crop-" + id, "[Cosmetics] Failed to save crop", e);
            return;
        }
        reload(id);
    }

    public void clear(String id) {
        try {
            Files.deleteIfExists(png(id));
            Files.deleteIfExists(cropFile(id));
        } catch (IOException e) {
            DebugLog.errorOnce("cosmetic-preview-clear-" + id, "[Cosmetics] Failed to remove preview", e);
        }
        release(id);
        cache.put(id, NONE);
    }

    public Path pictureOf(String id) {
        return png(id);
    }

    private void reload(String id) {
        release(id);
        cache.remove(id);
    }

    private void release(String id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) mc.getTextureManager().release(textureId(id));
    }

    private Preview load(String id) {
        Path file = png(id);
        if (!Files.isRegularFile(file)) return NONE;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return NONE;
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            int w = image.getWidth();
            int h = image.getHeight();
            Identifier texture = textureId(id);
            mc.getTextureManager().register(texture, new DynamicTexture(() -> "lovisual_cosmetic_" + id, image));
            return new Preview(texture, w, h, readCrop(id, w, h));
        } catch (IOException | RuntimeException e) {
            DebugLog.errorOnce("cosmetic-preview-load-" + id, "[Cosmetics] Failed to load preview", e);
            return NONE;
        }
    }

    private PreviewCrop readCrop(String id, int w, int h) {
        try {
            Path file = cropFile(id);
            PreviewCrop crop = Files.isRegularFile(file) ? PreviewCrop.parse(Files.readString(file)) : null;
            return crop != null ? crop : PreviewCrop.centered(w, h);
        } catch (IOException e) {
            return PreviewCrop.centered(w, h);
        }
    }

    private static Identifier textureId(String id) {
        return Identifier.fromNamespaceAndPath(NAMESPACE, "cosmetic_preview/" + id);
    }

    private static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("lovisual").resolve("cosmetics").resolve("previews");
    }

    private static Path png(String id) {
        return dir().resolve(id + ".png");
    }

    private static Path cropFile(String id) {
        return dir().resolve(id + ".crop");
    }
}
