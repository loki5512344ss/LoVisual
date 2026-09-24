package dev.loki.lovisual.mixins.security.text;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import dev.loki.lovisual.features.security.FilteredTranslatableTextContent;

@Mixin(TranslatableContents.class)
public abstract class TranslatableTextContentMixin {

    @WrapOperation(method = "decompose", at = @At(value = "INVOKE", target = "Lnet/minecraft/locale/Language;getOrDefault(Ljava/lang/String;)Ljava/lang/String;"))
    private String lovisual$backdoor$resolveNoFallback(Language language, String key, Operation<String> original) {
        if ((Object) this instanceof FilteredTranslatableTextContent filtered && filtered.getResolved() != null) {
            return filtered.getResolved();
        }
        return original.call(language, key);
    }

    @WrapOperation(method = "decompose", at = @At(value = "INVOKE", target = "Lnet/minecraft/locale/Language;getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"))
    private String lovisual$backdoor$resolveFallback(Language language, String key, String fallback, Operation<String> original) {
        if ((Object) this instanceof FilteredTranslatableTextContent filtered && filtered.getResolved() != null) {
            return filtered.getResolved();
        }
        return original.call(language, key, fallback);
    }
}
