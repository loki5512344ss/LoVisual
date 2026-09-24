package dev.loki.lovisual.render.engine.text.msdf.atlas;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.textures.FilterMode;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.system.MemoryUtil;
import dev.loki.lovisual.render.engine.texture.Texture;
import dev.loki.lovisual.render.engine.text.font.FontUtils;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderSystem;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Загрузка MSDF-атласа (msdf-bmfont JSON + PNG) из ресурсов и заливка в GPU-текстуру.
 */
final class MsdfAtlasLoader {
    private static volatile String lastError = "";

    private MsdfAtlasLoader() {
    }

    static String lastError() {
        return lastError;
    }

    private static void setLastError(String reason) {
        lastError = reason != null ? reason : "";
    }

    static @Nullable MsdfAtlas load(FontInfo info) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            setLastError("mc null");
            return null;
        }
        ResourceManager rm = mc.getResourceManager();

        Identifier jsonId = FontUtils.msdfResource(info, "json");
        Identifier pngId = FontUtils.msdfResource(info, "png");
        if (jsonId == null || pngId == null) {
            setLastError("msdf ids missing");
            return null;
        }

        JsonObject root;
        try (InputStream in = rm.getResource(jsonId).orElseThrow().open()) {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception ignored) {
            setLastError("msdf json missing/invalid: " + jsonId);
            return null;
        }

        BufferedImage image;
        try (InputStream in = rm.getResource(pngId).orElseThrow().open()) {
            image = ImageIO.read(in);
            if (image == null) return null;
        } catch (Exception ignored) {
            setLastError("msdf png missing/invalid: " + pngId);
            return null;
        }

        JsonObject atlasObj = root.has("atlas") ? root.getAsJsonObject("atlas") : null;
        float distanceRange = MsdfJson.getFloat(atlasObj, "distanceRange", 4f);
        int atlasWidth = MsdfJson.getInt(atlasObj, "width", image.getWidth());
        int atlasHeight = MsdfJson.getInt(atlasObj, "height", image.getHeight());
        if (atlasWidth <= 0) atlasWidth = image.getWidth();
        if (atlasHeight <= 0) atlasHeight = image.getHeight();
        String yOrigin = MsdfJson.getString(atlasObj, "yOrigin", "bottom");
        boolean yOriginBottom = !"top".equalsIgnoreCase(yOrigin);

        JsonObject metricsObj = root.has("metrics") ? root.getAsJsonObject("metrics") : null;
        float emSize = MsdfJson.getFloat(metricsObj, "emSize", 1f);
        float lineHeight = MsdfJson.getFloat(metricsObj, "lineHeight", emSize);
        float ascender = MsdfJson.getFloat(metricsObj, "ascender", lineHeight);
        if (ascender < 0f) ascender = -ascender;
        if (lineHeight <= 0f) lineHeight = 1f;

        Int2ObjectOpenHashMap<MsdfGlyph> glyphs = readGlyphs(root, atlasWidth, atlasHeight, yOriginBottom);

        MsdfGlyph fallback = glyphs.get(32);
        if (fallback == null) fallback = glyphs.get('?');

        Texture texture = uploadTexture(image);
        if (texture == null) {
            setLastError("msdf texture upload failed: " + pngId);
            return null;
        }

        if (glyphs.isEmpty()) {
            texture.close();
            setLastError("msdf glyphs empty: " + jsonId);
            return null;
        }

        setLastError("");
        TextRenderSystem.glyphAtlases().registerPage("msdf:" + jsonId, texture, atlasWidth, atlasHeight, true);
        TextRenderSystem.glyphAtlases().onDirtyRectUpload(glyphs.size(), atlasWidth * atlasHeight * 4);
        return new MsdfAtlas(texture, distanceRange, atlasWidth, atlasHeight, lineHeight, ascender, glyphs, fallback);
    }

    private static Int2ObjectOpenHashMap<MsdfGlyph> readGlyphs(JsonObject root, int atlasWidth, int atlasHeight, boolean yOriginBottom) {
        Int2ObjectOpenHashMap<MsdfGlyph> glyphs = new Int2ObjectOpenHashMap<>();
        JsonArray glyphArray = root.has("glyphs") ? root.getAsJsonArray("glyphs") : null;
        if (glyphArray == null) return glyphs;

        float invW = 1f / atlasWidth;
        float invH = 1f / atlasHeight;
        for (JsonElement el : glyphArray) {
            if (!el.isJsonObject()) continue;
            JsonObject g = el.getAsJsonObject();

            int codePoint = MsdfJson.readUnicode(g.get("unicode"));
            if (codePoint < 0) continue;

            float advance = MsdfJson.getFloat(g, "advance", 0f);
            MsdfGlyph.Bounds plane = MsdfJson.readBounds(g.has("planeBounds") ? g.getAsJsonObject("planeBounds") : null);
            MsdfGlyph.Bounds atlas = MsdfJson.readBounds(g.has("atlasBounds") ? g.getAsJsonObject("atlasBounds") : null);

            MsdfGlyph glyph = MsdfGlyph.from(advance, plane, atlas, yOriginBottom, atlasWidth, atlasHeight, invW, invH);
            glyphs.put(codePoint, glyph);
        }
        return glyphs;
    }

    private static @Nullable Texture uploadTexture(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int[] pixels = new int[w * h];
        image.getRGB(0, 0, w, h, pixels, 0, w);

        ByteBuffer buffer = MemoryUtil.memAlloc(w * h * 4);
        try {
            for (int argb : pixels) {
                buffer.put((byte) ((argb >> 16) & 0xFF));
                buffer.put((byte) ((argb >> 8) & 0xFF));
                buffer.put((byte) (argb & 0xFF));
                buffer.put((byte) ((argb >> 24) & 0xFF));
            }
            buffer.flip();

            Texture texture = new Texture(w, h, GpuFormat.RGBA8_UNORM, FilterMode.LINEAR, FilterMode.LINEAR);
            texture.upload(buffer);
            return texture;
        } catch (Exception ignored) {
            return null;
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }
}
