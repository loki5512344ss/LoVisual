package dev.loki.lovisual.render.engine.optimize.entity;

/**
 * Mixin-contract: per-entity storage for frozen animation captures
 * ({@link AnimFreeze.Holder}). Implemented on {@code Entity} by
 * {@code EntityAnimHolderMixin}; render code reaches it through the
 * {@code EntityRenderState} back-reference ({@code StateEntity}).
 */
public interface AnimHolder {

    AnimFreeze.Holder lovisual$anim();

    void lovisual$anim(AnimFreeze.Holder holder);
}
