package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics.preview;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Native "pick a picture" dialog plus the extension check shared with drag-and-drop. */
public final class PreviewFileDialog {
    private static final List<String> EXTENSIONS = List.of("png", "jpg", "jpeg", "bmp", "tga", "gif");

    private PreviewFileDialog() {
    }

    public static boolean isPicture(Path path) {
        if (path == null || !Files.isRegularFile(path)) return false;
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot >= 0 && EXTENSIONS.contains(name.substring(dot + 1));
    }

    /** Blocks until the dialog closes; null when cancelled or the choice is not a picture. */
    public static Path choose(String title) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(EXTENSIONS.size());
            for (String ext : EXTENSIONS) filters.put(stack.UTF8("*." + ext));
            filters.flip();
            String picked = TinyFileDialogs.tinyfd_openFileDialog(title, "", filters, "Pictures", false);
            if (picked == null || picked.isBlank()) return null;
            Path path = Path.of(picked);
            return isPicture(path) ? path : null;
        }
    }
}
