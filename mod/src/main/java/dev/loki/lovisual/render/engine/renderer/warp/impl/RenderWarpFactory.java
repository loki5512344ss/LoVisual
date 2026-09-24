package dev.loki.lovisual.render.engine.renderer.warp;

/**
 * Static construction entry points for {@link RenderWarp}: corner warps, mouse tilt and
 * yaw/pitch/roll perspective. Produces immutable warp values via the package-private
 * {@link RenderWarp} constructor, delegating the trigonometry to {@link RenderWarpGeometry}.
 */
public final class RenderWarpFactory {
    private RenderWarpFactory() {
    }

    public static RenderWarp corners(float x,
                                     float y,
                                     float width,
                                     float height,
                                     float tlX,
                                     float tlY,
                                     float trX,
                                     float trY,
                                     float brX,
                                     float brY,
                                     float blX,
                                     float blY) {
        return corners(x, y, width, height, tlX, tlY, trX, trY, brX, brY, blX, blY, 1.0f);
    }

    public static RenderWarp corners(float x,
                                     float y,
                                     float width,
                                     float height,
                                     float tlX,
                                     float tlY,
                                     float trX,
                                     float trY,
                                     float brX,
                                     float brY,
                                     float blX,
                                     float blY,
                                     float strength) {
        if (Math.abs(width) <= 0.0001f || Math.abs(height) <= 0.0001f || Math.abs(strength) <= 0.0001f) {
            return RenderWarp.IDENTITY;
        }
        return new RenderWarp(true, x, y, width, height,
                tlX, tlY, trX, trY, brX, brY, blX, blY, RenderWarpGeometry.clampStrength(strength));
    }

    public static RenderWarp tilt(float x,
                                  float y,
                                  float width,
                                  float height,
                                  float strength,
                                  float hover,
                                  float mouseX,
                                  float mouseY) {
        if (Math.abs(width) <= 0.0001f || Math.abs(height) <= 0.0001f) {
            return RenderWarp.IDENTITY;
        }

        float cx = x + width * 0.5f;
        float cy = y + height * 0.5f;
        float dx = RenderWarpGeometry.clamp((mouseX - cx) / width, -1.0f, 1.0f);
        float dy = RenderWarpGeometry.clamp((mouseY - cy) / height, -1.0f, 1.0f);
        float amount = Math.max(0.0f, hover) * strength;
        if (Math.abs(amount) <= 0.0001f) {
            return RenderWarp.IDENTITY;
        }

        return angles(x, y, width, height, -dx * amount, dy * amount, 0.0f,
                3.5f, 1.0f, 1.0f + Math.max(0.0f, hover) * 0.025f);
    }

    public static RenderWarp angles(float x,
                                    float y,
                                    float width,
                                    float height,
                                    float yawDeg,
                                    float pitchDeg,
                                    float rollDeg) {
        return angles(x, y, width, height, yawDeg, pitchDeg, rollDeg, 3.5f, 1.0f, 1.0f);
    }

    public static RenderWarp perspective(float x,
                                         float y,
                                         float width,
                                         float height,
                                         float yawDeg,
                                         float pitchDeg,
                                         float rollDeg,
                                         float depth,
                                         float perspective,
                                         float scale) {
        return angles(x, y, width, height, yawDeg, pitchDeg, rollDeg, depth, perspective, scale);
    }

    public static RenderWarp angles(float x,
                                    float y,
                                    float width,
                                    float height,
                                    float yawDeg,
                                    float pitchDeg,
                                    float rollDeg,
                                    float depth,
                                    float perspective,
                                    float scale) {
        if (Math.abs(width) <= 0.0001f || Math.abs(height) <= 0.0001f) {
            return RenderWarp.IDENTITY;
        }
        if (Math.abs(yawDeg) <= 0.0001f
                && Math.abs(pitchDeg) <= 0.0001f
                && Math.abs(rollDeg) <= 0.0001f
                && Math.abs(scale - 1.0f) <= 0.0001f) {
            return RenderWarp.IDENTITY;
        }

        float cx = x + width * 0.5f;
        float cy = y + height * 0.5f;
        float cameraDistance = Math.max(1.0f, Math.max(Math.abs(width), Math.abs(height)) * Math.max(0.75f, depth));
        float persp = Math.max(0.0f, perspective);
        float s = Math.max(0.05f, scale);

        float[] tl = RenderWarpGeometry.projectCorner(-width * 0.5f, -height * 0.5f, yawDeg, pitchDeg, rollDeg, cameraDistance, persp, s, cx, cy);
        float[] tr = RenderWarpGeometry.projectCorner(width * 0.5f, -height * 0.5f, yawDeg, pitchDeg, rollDeg, cameraDistance, persp, s, cx, cy);
        float[] br = RenderWarpGeometry.projectCorner(width * 0.5f, height * 0.5f, yawDeg, pitchDeg, rollDeg, cameraDistance, persp, s, cx, cy);
        float[] bl = RenderWarpGeometry.projectCorner(-width * 0.5f, height * 0.5f, yawDeg, pitchDeg, rollDeg, cameraDistance, persp, s, cx, cy);

        return corners(
                x, y, width, height,
                tl[0] - x, tl[1] - y,
                tr[0] - (x + width), tr[1] - y,
                br[0] - (x + width), br[1] - (y + height),
                bl[0] - x, bl[1] - (y + height),
                1.0f
        );
    }
}
