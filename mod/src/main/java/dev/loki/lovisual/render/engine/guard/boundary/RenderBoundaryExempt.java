package dev.loki.lovisual.render.engine.guard.boundary;
import dev.loki.lovisual.render.engine.guard.LegacyRenderPath;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a deliberate exception to the post-RHI boundary rules.
 * <p>
 * Every usage should be temporary or isolated. New renderer features should not add this annotation unless the code is
 * a backend/profiler/interop implementation.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface RenderBoundaryExempt {
    public LegacyRenderPath value();

    public String reason();
}
