package dev.loki.lovisual.features.command.core;

import dev.loki.lovisual.events.meta.UsedImplicitly;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@UsedImplicitly
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandInfo {
    public String id();

    public String[] aliases() default {};

    public String usage() default "";

    public String descriptionKey() default "";
}
