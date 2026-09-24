package dev.loki.lovisual.util.aiming;

public final class RestrictedSingleUseAction {

    private final Runnable action;
    private boolean used;

    public RestrictedSingleUseAction(Runnable action) {
        this.action = action;
    }

    public void invoke() {
        if (used) return;
        used = true;
        if (action != null) {
            action.run();
        }
    }
}
