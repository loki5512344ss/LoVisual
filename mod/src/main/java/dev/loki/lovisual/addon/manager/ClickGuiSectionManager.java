package dev.loki.lovisual.addon.manager;

import dev.loki.lovisual.api.v0.clickgui.LoVisualClickGuiSection;
import dev.loki.lovisual.api.v0.clickgui.LoVisualClickGuiRenderContext;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.sections.ClickGuiSection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import dev.loki.lovisual.addon.core.AddonManager;

public enum ClickGuiSectionManager {
    ;

    private static final Map<String, Entry> SECTIONS = new LinkedHashMap<>();

    public static synchronized boolean register(String addonId,
                                                String sectionId,
                                                String label,
                                                LoVisualClickGuiSection section) {
        String normalizedAddonId = normalize(addonId);
        String normalizedSectionId = normalize(sectionId);
        if (normalizedAddonId.isBlank() || normalizedSectionId.isBlank() || section == null) {
            return false;
        }
        String id = normalizedAddonId + ":" + normalizedSectionId;
        if (SECTIONS.containsKey(id)) {
            return false;
        }
        String title = label == null || label.isBlank() ? normalizedSectionId : label.trim();
        SECTIONS.put(id, new Entry(
                normalizedAddonId,
                id,
                normalizedSectionId,
                title,
                new AddonSectionAdapter(section)
        ));
        return true;
    }

    public static synchronized void unregisterAddon(String addonId) {
        String normalizedAddonId = normalize(addonId);
        SECTIONS.entrySet().removeIf(entry -> {
            if (!entry.getValue().addonId().equals(normalizedAddonId)) {
                return false;
            }
            try {
                entry.getValue().section().onDeselected();
            } catch (Throwable ignored) {
            }
            return true;
        });
    }

    public static synchronized List<Entry> sections() {
        return SECTIONS.values().stream()
                .filter(entry -> AddonManager.isActive(entry.addonId()))
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record Entry(String addonId,
                        String id,
                        String localId,
                        String label,
                        ClickGuiSection section) {
    }

    private record AddonSectionAdapter(LoVisualClickGuiSection section) implements ClickGuiSection {
        @Override
        public void layout(float x, float y, float w, float h) {
            section.layout(x, y, w, h);
        }

        @Override
        public void render(float mouseX, float mouseY) {
            section.render(
                    new LoVisualClickGuiRenderContext(
                            ClickGuiRenderer.currentRenderer(),
                            ClickGuiRenderer.getInterRegular(),
                            ClickGuiRenderer.getInterMedium()
                    ),
                    mouseX,
                    mouseY
            );
        }

        @Override
        public void renderGlassPass(float alphaFactor) {
            section.renderGlassPass(alphaFactor);
        }

        @Override
        public boolean mousePressed(float mouseX, float mouseY, int button) {
            return section.mousePressed(mouseX, mouseY, button);
        }

        @Override
        public void mouseReleased(float mouseX, float mouseY, int button) {
            section.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseScrolled(float mouseX, float mouseY, double amount) {
            return section.mouseScrolled(mouseX, mouseY, amount);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return section.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public boolean charTyped(char chr, int modifiers) {
            return section.charTyped(chr, modifiers);
        }

        @Override
        public void onSelected() {
            section.onSelected();
        }

        @Override
        public void onDeselected() {
            section.onDeselected();
        }

        @Override
        public boolean isVisible() {
            return section.isVisible();
        }
    }
}
