package dev.loki.lovisual.features.gui.clickgui.settings.owner;

public interface TextEditorOwner {
    void applyEditorText(String rawText);

    String getEditorText();

    boolean isSingleLine();
}
