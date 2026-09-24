package dev.loki.lovisual.features.module.core;

import dev.loki.lovisual.events.meta.UsedImplicitly;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;

@UsedImplicitly
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModuleInfo {
    String id();

    String displayName();

    String[] aliases() default {};

    ModuleCategory category();

    /**
     * Plain description or i18n key. Optional, but carried by Module metadata when present.
     */
    String description() default "";

    boolean enabledByDefault() default false;
}
