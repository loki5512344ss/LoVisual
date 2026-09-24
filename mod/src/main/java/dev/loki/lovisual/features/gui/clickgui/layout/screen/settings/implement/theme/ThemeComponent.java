package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.theme;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Settings screen component that lists every installed theme as a card. Owns the
 * hit rectangles used for click routing, the per-card selection animation and a
 * shared {@link ThemeScrollState}; card visuals themselves live in
 * {@link ThemeCardRenderer}.
 */
public final class ThemeComponent {

    private final List<Hit> hits = new ArrayList<>();
    private final Map<String, Float> swatchHoverByKey = new HashMap<>();
    private final Map<String, Float> selectedAnimByTheme = new HashMap<>();
    private final Set<String> liveThemeIds = new HashSet<>();
    private final ThemeScrollState scrollState = new ThemeScrollState();

    /** Reset both raw and smoothed scroll (called when the panel is reopened). */
    public void resetScroll() {
        scrollState.reset();
    }

    /** Wheel-scroll handler; ignored unless the cursor is inside the card area. */
    public void scroll(float mx, float my, float menuX, float menuY, float menuW, float menuH, double amount, float scale) {
        float x = menuX + 31f * scale;
        float y = menuY + 33f * scale;
        float w = menuW - 42f * scale;
        float h = menuH - 39f * scale;
        if (!ClickGuiMath.insideRect(mx, my, x, y, w, h)) return;
        scrollState.addDelta((float) (amount * 22f));
    }

    /** Primary-button press routed to the scrollbar thumb. Returns true if consumed. */
    public boolean mousePressedScrollbar(float mx, float my, int button) {
        return scrollState.press(mx, my, button);
    }

    public void mouseReleased(int button) {
        scrollState.release(button);
    }

    /** Translate a click on a previously-recorded hit rect into a {@link ThemeAction}. */
    public ThemeAction click(float mx, float my, int button) {
        if (scrollState.hovered(mx, my)) return null;
        if (button != 0 && button != 1) return null;
        for (Hit hit : hits) {
            if (!ClickGuiMath.insideRect(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) continue;
            if (hit.deleteButton()) return button == 0 && hit.custom() ? new ThemeAction(ThemeActionType.DELETE, hit.themeId()) : null;
            if (hit.addCard()) return button == 0 ? new ThemeAction(ThemeActionType.ADD, null) : null;
            if (hit.themeId() == null || hit.themeId().isBlank()) return null;
            if (button == 1 && hit.custom()) return new ThemeAction(ThemeActionType.EDIT, hit.themeId());
            if (button == 0) return new ThemeAction(ThemeActionType.SELECT, hit.themeId());
        }
        return null;
    }

    /** Layout + paint the theme grid for one frame. */
    public void render(float menuX, float menuY, float menuW, float menuH, float mx, float my, float scale) {
        hits.clear();

        SettingsGuiPalette palette = SettingsGuiPalette.current();
        updateSelectedAnimations(Theme.themes());
        List<Themes.ThemeEntry> entries = ThemeCardRenderer.filteredEntries();
        boolean includeAddCard = ThemeCardRenderer.shouldShowAddCard();
        int cardsTotal = entries.size() + (includeAddCard ? 1 : 0);
        if (cardsTotal <= 0) return;

        float areaX = menuX + 31f * scale;
        float areaY = menuY + 33f * scale;
        float areaW = menuW - 42f * scale;
        float areaH = menuH - 39f * scale;

        int columns = 2;
        float gap = 6f * scale;
        float cardW = (areaW - gap * (columns - 1)) / columns;
        float cardH = 46f * scale;
        float stepY = cardH + gap;

        int rows = (cardsTotal + columns - 1) / columns;
        float contentH = rows * stepY - gap;
        float maxScroll = Math.max(0f, contentH - areaH);

        scrollState.updateMetrics(menuX, menuY, menuH, areaH, contentH, maxScroll, scale);
        if (scrollState.dragging()) scrollState.scrollToMouse(my);
        scrollState.smooth();
        scrollState.updateMetrics(menuX, menuY, menuH, areaH, contentH, maxScroll, scale);
        float smoothedScroll = scrollState.smoothed();

        boolean clipped = ScissorFunction.pushRaw(areaX, areaY, areaW, areaH);
        int idx = 0;
        for (Themes.ThemeEntry entry : entries) {
            float x = areaX + (idx % columns) * (cardW + gap);
            float y = areaY + (idx / columns) * stepY + smoothedScroll;
            idx++;
            if (y + cardH < areaY || y > areaY + areaH) continue;
            boolean custom = !entry.builtin();
            ThemeCardRenderer.drawThemeCard(entry, x, y, cardW, cardH, mx, my, scale, palette, selectedAnim(entry.getId()), swatchHoverByKey);
            if (custom) {
                float ds = ThemeCardRenderer.deleteButtonSize(scale);
                hits.add(new Hit(ThemeCardRenderer.deleteButtonX(x, cardW, scale), ThemeCardRenderer.deleteButtonY(y, scale), ds, ds, entry.getId(), false, true, true));
            }
            hits.add(new Hit(x, y, cardW, cardH, entry.getId(), false, custom, false));
        }

        if (includeAddCard) {
            float x = areaX + (idx % columns) * (cardW + gap);
            float y = areaY + (idx / columns) * stepY + smoothedScroll;
            if (!(y + cardH < areaY || y > areaY + areaH)) {
                ThemeCardRenderer.drawAddCard(x, y, cardW, cardH, scale, palette);
                hits.add(new Hit(x, y, cardW, cardH, null, true, false, false));
            }
        }
        if (clipped) ScissorFunction.pop();
        scrollState.render(scale, palette);
    }

    /** Approach / snap every theme's selection value towards 0 or 1 and drop stale ids. */
    private void updateSelectedAnimations(List<Themes.ThemeEntry> entries) {
        String currentId = Theme.currentId();
        float dt = AnimationUtility.deltaTime();
        liveThemeIds.clear();
        for (Themes.ThemeEntry entry : entries) {
            String id = entry.getId();
            liveThemeIds.add(id);
            float target = id.equals(currentId) ? 1f : 0f;
            float value = selectedAnimByTheme.getOrDefault(id, target);
            value = AnimationUtility.approach(value, target, dt, target > value ? 11.5f : 9.5f);
            value = AnimationUtility.snap(value, target, 0.002f);
            selectedAnimByTheme.put(id, value);
        }
        selectedAnimByTheme.keySet().removeIf(id -> !liveThemeIds.contains(id));
        // Chip keys look like "<themeId>#<index>"; drop the ones whose owning theme vanished
        // so renamed/deleted cards cannot leave entries accumulating in the map forever.
        swatchHoverByKey.keySet().removeIf(key -> {
            int hash = key.lastIndexOf('#');
            return hash < 0 || !liveThemeIds.contains(key.substring(0, hash));
        });
    }

    private float selectedAnim(String themeId) {
        return selectedAnimByTheme.getOrDefault(themeId, themeId.equals(Theme.currentId()) ? 1f : 0f);
    }

    /** Kinds of interaction the caller can respond to. */
    public enum ThemeActionType {
        SELECT,
        ADD,
        EDIT,
        DELETE
    }

    /** Result of a click on one of the recorded hit rects. */
    public record ThemeAction(ThemeActionType type, String themeId) {
    }

    /** Screen rectangle registered by {@link #render} for the next {@link #click} call. */
    private record Hit(float x, float y, float w, float h, String themeId, boolean addCard, boolean custom, boolean deleteButton) {
    }
}
