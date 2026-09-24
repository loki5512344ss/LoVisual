package dev.loki.lovisual.mixins.render.text;

import com.google.gson.JsonParseException;
import net.minecraft.client.gui.font.FontManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FontManager.class)
public abstract class FontManagerMixin {

    @Redirect(
            method = "loadResourceStack",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;[Ljava/lang/Object;)V"
            )
    )
    private static void lovisual$maybeSuppressFontLoadWarn(
            Logger logger,
            String format,
            Object[] args
    ) {
        if (args != null && args.length > 0) {
            Object last = args[args.length - 1];
            if (last instanceof JsonParseException jsonParseException) {
                String message = jsonParseException.getMessage();
                if (message != null && message.contains("No key providers")) {
                    return;
                }
            }
        }

        logger.warn(format, args);
    }
}

