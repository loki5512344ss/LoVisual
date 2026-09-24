package dev.loki.lovisual.render.invisible;

/**
 * Marker injected into entity render states when InvisESP reveals
 * an invisible player this frame.
 */
public interface SeeInvisiblesState {
    public boolean lovisual$isSeeInvisRevealed();

    public void lovisual$setSeeInvisRevealed(boolean revealed);
}
