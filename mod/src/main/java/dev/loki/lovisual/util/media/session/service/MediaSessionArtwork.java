package dev.loki.lovisual.util.media.session;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import dev.loki.lovisual.util.logging.DebugLog;
import java.io.ByteArrayInputStream;
import dev.loki.lovisual.util.media.info.MediaInfo;

final class MediaSessionArtwork {

    private static final Identifier ARTWORK_ID =
            Identifier.fromNamespaceAndPath("lovisual", "media_player/artwork");

    private Identifier artworkTexture;
    private int lastArtworkSize;

    Identifier texture() {
        return artworkTexture;
    }

    void clear() {
        artworkTexture = null;
        lastArtworkSize = 0;
    }

    void clearIfTrackChanged(boolean trackChanged) {
        if (trackChanged) {
            artworkTexture = null;
            lastArtworkSize = 0;
        }
    }

    void maybeUpdate(MediaInfo media, boolean trackChanged) {
        if (media == null) {
            if (trackChanged) {
                artworkTexture = null;
                lastArtworkSize = 0;
            }
            return;
        }
        byte[] artwork = media.artworkPng();
        int size = artwork != null ? artwork.length : 0;
        if (size == 0) {
            if (trackChanged) {
                artworkTexture = null;
                lastArtworkSize = 0;
            }
            return;
        }
        if (trackChanged || artworkTexture == null || size != lastArtworkSize) {
            updateArtwork(media);
            lastArtworkSize = size;
        }
    }

    private void updateArtwork(MediaInfo media) {
        Minecraft mc = Minecraft.getInstance();
        artworkTexture = null;
        if (mc == null || media == null || media.artworkPng() == null
                || media.artworkPng().length == 0) {
            return;
        }
        NativeImage image = getArtworkAsNativeImage(media);
        if (image == null) {
            return;
        }
        DynamicTexture texture = new DynamicTexture(() -> "lovisual_media_artwork", image);
        mc.getTextureManager().register(ARTWORK_ID, texture);
        artworkTexture = ARTWORK_ID;
    }

    private static NativeImage getArtworkAsNativeImage(MediaInfo mediaInfo) {
        byte[] bytes = mediaInfo.artworkPng();
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return NativeImage.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            DebugLog.errorOnce("media-artwork-native-image",
                    "[MediaSessionService] Failed to read artwork NativeImage", e);
            return null;
        }
    }
}
