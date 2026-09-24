
package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.relations;

import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.util.text.edit.ClipboardUtil;
import dev.loki.lovisual.util.text.mask.ChatNameUtil;
import org.lwjgl.glfw.GLFW;

final class RelationsInput {
    final RelationsScroll scroll = new RelationsScroll();

    boolean mousePressedScrollbar(RelationsState state, float mx, float my, int button) {
        if (state.onlinePicker.isVisible()) return state.onlinePicker.mousePressedScrollbar(mx, my, button);
        return scroll.press(state, mx, my, button);
    }

    void mouseReleased(RelationsState state, int button) {
        state.onlinePicker.mouseReleased(button);
        if (button == 0) state.draggingScrollbar = false;
    }

    void scroll(RelationsState state, float mx, float my, double amount) {
        if (state.onlinePicker.isVisible()) {
            state.onlinePicker.scroll(mx, my, amount);
            return;
        }
        if (!ClickGuiMath.insideRect(mx, my, state.listX, state.listY, state.listW, state.listH)) return;
        state.scroll += (float) (amount * 22f);
    }

    boolean click(RelationsState state, float mx, float my, int button) {
        if (button != 0) return false;
        if (state.friendsPill.contains(mx, my)) return switchTab(state, RelationsState.RelationTab.FRIENDS);
        if (state.enemiesPill.contains(mx, my)) return switchTab(state, RelationsState.RelationTab.ENEMIES);
        if (state.staffPill.contains(mx, my)) return switchTab(state, RelationsState.RelationTab.STAFF);
        if (state.playerInputRect.contains(mx, my)) {
            if (state.onlinePicker.isOpen()) {
                state.activeField = RelationsState.ActiveField.NONE;
                state.onlinePicker.focusSearch();
            } else {
                state.activeField = RelationsState.ActiveField.PLAYER;
            }
            state.updateMovementBlocked();
            state.clearStatus();
            return true;
        }
        if (state.addTypedButton.contains(mx, my)) {
            if (state.onlinePicker.isOpen()) {
                state.onlinePicker.clearSearch();
                state.onlinePicker.focusSearch();
                state.updateMovementBlocked();
                return true;
            }
            commitPlayerInput(state);
            return true;
        }
        if (state.reservedPickerButton.contains(mx, my)) {
            state.activeField = RelationsState.ActiveField.NONE;
            state.onlinePicker.toggle(state.tab.pickerMode());
            state.updateMovementBlocked();
            state.clearStatus();
            return true;
        }
        if (state.onlinePicker.isVisible() && state.onlinePicker.click(mx, my, button)) {
            state.updateMovementBlocked();
            return true;
        }
        if (state.onlinePicker.isVisible()) return false;
        for (RelationsState.CardEntryHit entryHit : state.cardHits) {
            RelationPlayerCardComponent.CardHit hit = entryHit.hit();
            if (inside(hit.delete(), mx, my)) {
                state.tab.remove(entryHit.name());
                state.setStatus(state.tr("status.removed", "Removed: %s", entryHit.name()));
                return true;
            }
            if (ClickGuiMath.insideRect(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) return true;
        }
        state.activeField = RelationsState.ActiveField.NONE;
        return false;
    }

    boolean keyPressed(RelationsState state, int keyCode, int scanCode, int modifiers) {
        if (state.onlinePicker.keyPressed(keyCode, scanCode, modifiers)) {
            state.updateMovementBlocked();
            return true;
        }
        if (state.activeField == RelationsState.ActiveField.NONE) return false;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            state.activeField = RelationsState.ActiveField.NONE;
            state.movementInputBlocked = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            commitPlayerInput(state);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            setFieldText(state, state.activeField, dropLast(fieldText(state, state.activeField)));
            return true;
        }
        if (ctrl && keyCode == GLFW.GLFW_KEY_V) {
            appendToField(state, state.activeField, ClipboardUtil.get());
            return true;
        }
        return true;
    }

    boolean charTyped(RelationsState state, char chr, int modifiers) {
        if (state.onlinePicker.charTyped(chr, modifiers)) {
            state.updateMovementBlocked();
            return true;
        }
        if (state.activeField == RelationsState.ActiveField.NONE) return false;
        if (chr >= 32 && chr != 127) appendToField(state, state.activeField, String.valueOf(chr));
        return true;
    }

    boolean switchTab(RelationsState state, RelationsState.RelationTab next) {
        if (state.tab != next) {
            state.tab = next;
            state.activeField = RelationsState.ActiveField.NONE;
            state.resetScroll();
            if (state.onlinePicker.isVisible()) state.onlinePicker.open(state.tab.pickerMode());
            state.clearStatus();
        }
        return true;
    }

    void commitPlayerInput(RelationsState state) {
        String cleaned = ChatNameUtil.normalizeNickCandidate(state.playerInput);
        if (!ChatNameUtil.isNickLike(cleaned)) {
            state.setStatus(state.tr("status.invalid_nick", "Invalid nick"));
            return;
        }
        if (state.tab.add(cleaned)) {
            state.playerInput = "";
            state.activeField = RelationsState.ActiveField.NONE;
            state.movementInputBlocked = false;
            state.setStatus(state.tr("status.added", "Added: %s", cleaned));
        } else {
            state.setStatus(state.tr("status.already_exists", "Already exists: %s", cleaned));
        }
    }

    String fieldText(RelationsState state, RelationsState.ActiveField field) {
        return switch (field) {
            case PLAYER -> state.playerInput;
            default -> "";
        };
    }

    void setFieldText(RelationsState state, RelationsState.ActiveField field, String value) {
        String next = value == null ? "" : value;
        switch (field) {
            case PLAYER -> state.playerInput = next;
            default -> {
            }
        }
    }

    void appendToField(RelationsState state, RelationsState.ActiveField field, String raw) {
        if (raw == null || raw.isEmpty()) return;
        String current = fieldText(state, field);
        int max = 32;
        String next = current + raw;
        if (next.length() > max) next = next.substring(0, max);
        setFieldText(state, field, next);
    }

    String dropLast(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, text.length() - 1);
    }

    boolean inside(RelationPlayerCardComponent.ActionHit hit, float mx, float my) {
        return hit != null && ClickGuiMath.insideRect(mx, my, hit.x(), hit.y(), hit.w(), hit.h());
    }
}
