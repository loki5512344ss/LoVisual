package dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorSolver.CachedThrow;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorSolver.SolveResult;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.model.DisplayState;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedTriangulatorHudPanel;

import java.util.List;
import java.util.Locale;

import static dev.loki.lovisual.features.theme.Theme.theme;
import dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator.TriangulatorSolver;

public final class TriangulatorView {
    private static final long RESET_FLASH_MS = 3000L;

    private int uiHeaderLeft;
    private int uiHeaderRight;
    private int uiBodyLeft;
    private int uiBodyRight;
    private int uiOutline;
    private int uiText;
    private int uiMuted;
    private int uiCounter;
    private int uiTitleText;
    private int uiDivider;
    private int uiBlurTint;
    private int uiSuccess;
    private int uiWarn;
    private int uiDanger;

    public void update(boolean themeMode, int alpha, int bg, int bg2, int stroke, int text, int muted) {
        if (themeMode) {
            int window = HudRenderUtil.setAlpha(theme().windowBg(), alpha);
            int header = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().windowHeader(), theme().surface(), 0.18f),
                    Math.min(255, alpha + 14)
            );
            int surface = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().surface(), theme().windowBg(), 0.22f),
                    Math.min(255, alpha + 6)
            );
            int deep = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().surface(), theme().windowHeader(), 0.42f),
                    Math.min(255, alpha + 18)
            );

            uiHeaderLeft = HudRenderUtil.mixColor(header, window, 0.22f);
            uiHeaderRight = HudRenderUtil.mixColor(surface, header, 0.52f);
            uiBodyLeft = HudRenderUtil.mixColor(window, surface, 0.24f);
            uiBodyRight = HudRenderUtil.mixColor(deep, surface, 0.18f);
            uiOutline = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().windowStroke(), theme().strokeSoft(), 0.18f),
                    Math.min(255, Math.max(182, alpha + 36))
            );
            uiText = theme().textPrimary();
            uiTitleText = HudRenderUtil.mixColor(theme().textPrimary(), theme().accent(), 0.14f);
            uiMuted = HudRenderUtil.mixColor(theme().textMuted(), theme().textPrimary(), 0.16f);
            uiCounter = HudRenderUtil.mixColor(theme().accent(), theme().textPrimary(), 0.38f);
            uiDivider = HudRenderUtil.scaleAlpha(uiMuted, 0.68f);
            uiBlurTint = HudRenderUtil.mixColor(
                    HudRenderUtil.mixColor(uiHeaderRight, uiBodyRight, 0.5f),
                    theme().accent(),
                    0.14f
            );
        } else {
            uiHeaderLeft = bg;
            uiHeaderRight = bg2;
            uiBodyLeft = HudRenderUtil.mixColor(bg, bg2, 0.15f);
            uiBodyRight = bg2;
            uiOutline = stroke;
            uiText = text;
            uiTitleText = uiText;
            uiMuted = muted;
            uiCounter = HudRenderUtil.mixColor(text, 0xFFE1E1FF, 0.20f);
            uiDivider = HudRenderUtil.scaleAlpha(uiMuted, 0.55f);
            uiBlurTint = HudRenderUtil.mixColor(uiHeaderLeft, uiBodyRight, 0.5f);
        }

        uiSuccess = HudRenderUtil.mixColor(uiCounter, 0xFF55FF93, 0.55f);
        uiWarn = HudRenderUtil.mixColor(uiCounter, 0xFFFFC85A, 0.55f);
        uiDanger = HudRenderUtil.mixColor(uiCounter, 0xFFFF6A6A, 0.62f);
    }

    public ScriptedTriangulatorHudPanel.Palette palette() {
        return new ScriptedTriangulatorHudPanel.Palette(
                uiHeaderLeft, uiHeaderRight, uiBodyLeft, uiBodyRight,
                uiOutline, uiText, uiMuted, uiCounter,
                uiTitleText, uiDivider, uiBlurTint,
                uiSuccess, uiWarn, uiDanger
        );
    }

    public int text() {
        return uiText;
    }

    public int muted() {
        return uiMuted;
    }

    public int counter() {
        return uiCounter;
    }

    public int danger() {
        return uiDanger;
    }

    public float blurStrength(boolean themeMode, int blurAlpha) {
        return Math.min(1.0f, (blurAlpha / 255.0f) * (themeMode ? 1.15f : 1.0f));
    }

    public DisplayState status(List<CachedThrow> throwsToShow,
                               SolveResult result,
                               boolean preview,
                               boolean trackingEye,
                               String resetReason,
                               long resetMs,
                               boolean nether) {
        long now = System.currentTimeMillis();
        if (throwsToShow.isEmpty()) {
            if (!resetReason.isEmpty() && now - resetMs <= RESET_FLASH_MS) {
                return new DisplayState(resetReason, "Cache cleared", "Waiting for a new eye", "", uiDanger);
            }
            if (trackingEye) {
                return new DisplayState("Tracking eye", "Stabilizing trajectory", "Need one clean capture", "", uiWarn);
            }
            if (preview) {
                return new DisplayState("Solved", TriangulatorSolver.formatCoordinateLine(1288.0, -412.0, nether),
                        TriangulatorSolver.formatResultLine(1288.0, -412.0, 5.2, nether), "88%", uiSuccess);
            }
            return new DisplayState("Waiting for eye", "Throw an eye of ender", "Second clean eye unlocks result", "", uiMuted);
        }

        if (throwsToShow.size() == 1) {
            CachedThrow first = throwsToShow.get(0);
            return new DisplayState(
                    trackingEye ? "Tracking next eye" : "Need second eye",
                    String.format(Locale.ROOT, "Q %s  Stability %.3f", TriangulatorSolver.formatPercent(first.quality()), first.stability()),
                    "Need another angle for solve",
                    "",
                    uiWarn
            );
        }

        if (result == null || !result.hasSolution()) {
            return new DisplayState(
                    "Angles too close",
                    String.format(Locale.ROOT, "Best pair %.2f deg", result == null ? 0.0 : result.minPairAngleDeg()),
                    "Move farther, then throw again",
                    "",
                    uiWarn
            );
        }

        if (!result.ready()) {
            return new DisplayState(
                    "Low correlation",
                    TriangulatorSolver.formatCoordinateLine(result.x(), result.z(), nether),
                    String.format(Locale.ROOT, "%s  Pair %.2f deg",
                            TriangulatorSolver.formatResultLine(result.x(), result.z(), result.rmse(), nether), result.minPairAngleDeg()),
                    TriangulatorSolver.formatPercent(result.confidence()),
                    uiWarn
            );
        }

        return new DisplayState(
                "Solved",
                TriangulatorSolver.formatCoordinateLine(result.x(), result.z(), nether),
                TriangulatorSolver.formatResultLine(result.x(), result.z(), result.rmse(), nether),
                TriangulatorSolver.formatPercent(result.confidence()),
                uiSuccess
        );
    }

    public int markerColor(CachedThrow cachedThrow, SolveResult result, int index, int total, double autoResetDistance) {
        if (result == null || !result.hasSolution()) {
            return index == total - 1 ? uiWarn : uiCounter;
        }
        double error = TriangulatorSolver.distanceToLine(result.x(), result.z(), cachedThrow);
        if (error <= 8.0) {
            return uiSuccess;
        }
        if (error <= autoResetDistance * 0.5) {
            return uiWarn;
        }
        return uiDanger;
    }
}
