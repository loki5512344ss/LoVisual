package dev.loki.lovisual.config.i18n;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opts a setting out of i18n lookup. Use this for user/content values where raw ids
 * are the intended labels, for example selectable sound ids.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DisableSettingI18n {
    boolean name() default true;

    boolean options() default true;
}
