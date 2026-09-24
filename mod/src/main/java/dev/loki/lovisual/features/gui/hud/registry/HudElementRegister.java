package dev.loki.lovisual.features.gui.hud.registry;

import dev.loki.lovisual.events.meta.UsedImplicitly;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@UsedImplicitly
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HudElementRegister {
    int order() default 0;
}
