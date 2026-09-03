package dev.loki.lovisual.features.cosmetic;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

public final class CosmeticTextureSlot {
    private final int index;
    private final Identifier id;
    private int width;
    private int height;
    private final String fileName;

    private boolean loaded;
    private JsonObject textureJson;

    CosmeticTextureSlot(int index, Identifier id, int width, int height, String fileName) {
        this.index = index;
        this.id = id;
        this.width = Math.max(width, 1);
        this.height = Math.max(height, 1);
        this.fileName = fileName;
    }

    public int getIndex() {
        return index;
    }

    public Identifier getId() {
        return id;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isLoaded() {
        return loaded;
    }

    void markLoaded() {
        loaded = true;
    }

    static CosmeticTextureSlot fromJson(int index, String modelKey, JsonObject texture) {
        int w = texture.has("width") ? texture.get("width").getAsInt() : 64;
        int h = texture.has("height") ? texture.get("height").getAsInt() : 64;
        String name = texture.has("name") ? texture.get("name").getAsString() : "tex" + index;
        Identifier id = Identifier.fromNamespaceAndPath(
                "lovisual",
                "dynamic/cosmetic/" + modelKey + "/tex/" + index
        );
        CosmeticTextureSlot slot = new CosmeticTextureSlot(index, id, w, h, name);
        slot.textureJson = texture;
        return slot;
    }

    void reset() {
        loaded = false;
    }

    /**
     * Attempts to load this texture slot from, in order: the cosmetic's external folder
     * (file name, then outfits/&lt;file&gt;, then a generic texture.png fallback), or an
     * inline base64 "source" embedded in the .bbmodel JSON.
     */
    void load(Minecraft mc, File externalDir, String resourcePath) {
        if (loaded || mc == null || mc.getTextureManager() == null) return;

        try {
            if (externalDir != null) {
                if (tryFile(mc, new File(externalDir, fileName))) return;
                if (tryFile(mc, new File(externalDir, "outfits/" + fileName))) return;
                if (tryFile(mc, new File(externalDir, "texture.png"))) return;
            }

            if (textureJson != null && textureJson.has("source")) {
                String source = textureJson.get("source").getAsString();
                if (source.startsWith("data:image") && source.contains(",")) {
                    byte[] bytes = Base64.getDecoder().decode(source.substring(source.indexOf(',') + 1));
                    try (InputStream is = new ByteArrayInputStream(bytes)) {
                        register(mc, is);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private boolean tryFile(Minecraft mc, File file) throws IOException {
        if (!file.exists()) return false;
        try (InputStream is = new FileInputStream(file)) {
            register(mc, is);
            return true;
        }
    }

    private void register(Minecraft mc, InputStream is) throws IOException {
        NativeImage image = NativeImage.read(is);
        width = Math.max(image.getWidth(), 1);
        height = Math.max(image.getHeight(), 1);
        DynamicTexture texture = new DynamicTexture(() -> "lovisual_cosmetic_" + index, image);
        mc.getTextureManager().register(id, texture);
        loaded = true;
    }
}
