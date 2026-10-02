package dev.loki.lovisual.features.module.modules.visuals.models.mesh;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.features.module.modules.visuals.models.mesh.blockbench.BbModelParser;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Verity: a free-form ball mesh read from its Blockbench file on first use, with the face painted on the
 * texture (the avatar ships one texture per mood). Drawn floating at the player's position.
 */
public final class VerityMesh {
    private static final String MODEL = "/assets/lovisual/models/verity/verity.bbmodel";
    /** Where the ball sits on the player, in entity model space (y grows downwards). */
    private static final float CENTER_Y = 0.75f;
    /** Blockbench units are sixteenths of a block; this also gives the ball a player's presence. */
    private static final float UNIT = 2.2f / 16.0f;

    private float[] triangles;

    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay,
                       Identifier texture, float scale) {
        float[] data = geometry();
        if (data.length == 0) return;
        pose.pushPose();
        pose.translate(0.0f, CENTER_Y, 0.0f);
        pose.scale(scale, scale, scale);
        MeshVertices.submit(pose, collector, texture, data, light, overlay);
        pose.popPose();
    }

    private float[] geometry() {
        if (triangles == null) triangles = load();
        return triangles;
    }

    private static float[] load() {
        try (InputStream stream = VerityMesh.class.getResourceAsStream(MODEL)) {
            if (stream == null) return new float[0];
            return BbModelParser.parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8), UNIT);
        } catch (IOException | RuntimeException e) {
            return new float[0];
        }
    }
}
