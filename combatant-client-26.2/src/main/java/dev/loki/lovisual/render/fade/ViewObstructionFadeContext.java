/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.fade;

import java.util.ArrayDeque;

public enum ViewObstructionFadeContext {
    ;
    private static final ThreadLocal<ArrayDeque<State>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    public static void push(ViewObstructionFadeState state) {
        if (state == null) {
            STACK.get().addLast(State.INACTIVE);
            return;
        }

        STACK.get().addLast(new State(
                state.lovisual$isViewObstructionFadeActive(),
                state.lovisual$getViewObstructionFadeAlpha()
        ));
    }

    /** Pushes an explicit fade state, used when combining multiple independent fade sources. */
    public static void push(boolean active, float alpha) {
        STACK.get().addLast(active ? new State(true, alpha) : State.INACTIVE);
    }

    public static void pop() {
        ArrayDeque<State> stack = STACK.get();
        if (!stack.isEmpty()) {
            stack.removeLast();
        }
        if (stack.isEmpty()) {
            STACK.remove();
        }
    }

    public static boolean isActive() {
        return current().active;
    }

    public static float alpha() {
        return current().alpha;
    }

    private static State current() {
        ArrayDeque<State> stack = STACK.get();
        return stack.isEmpty() ? State.INACTIVE : stack.peekLast();
    }

    private record State(boolean active, float alpha) {
        private static final State INACTIVE = new State(false, 1.0f);
    }
}
