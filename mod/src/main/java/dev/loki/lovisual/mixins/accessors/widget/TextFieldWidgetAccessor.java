package dev.loki.lovisual.mixins.accessors.widget;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EditBox.class)
public interface TextFieldWidgetAccessor {
    @Accessor("cursorPos")
    int lovisual$getSelectionStart();

    @Accessor("highlightPos")
    int lovisual$getSelectionEnd();
}
