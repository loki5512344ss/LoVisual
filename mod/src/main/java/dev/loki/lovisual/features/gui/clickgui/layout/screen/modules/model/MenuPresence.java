package dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model;

/**
 * Snapshot of the local ClickGUI for GUI presence: hovered column, module row under the cursor (255 = none),
 * whether that column shows a module's settings, search state, cursor position normalised to the panels' bounds
 * (0..1), left button and list scroll (0..255).
 */
public record MenuPresence(int category, int moduleIndex, boolean settingsOpen, boolean searching,
                           float x, float y, boolean pressed, int scroll) {
}
