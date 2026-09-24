
package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.relations;

import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.features.module.modules.misc.DefineTarget;
import dev.loki.lovisual.features.relations.PlayerRelations;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class RelationsState {
    final RelationPlayerCardComponent cardComponent = new RelationPlayerCardComponent();
    final OnlineRelationPlayerPickerComponent onlinePicker;
    final List<CardEntryHit> cardHits = new ArrayList<>();

    RelationTab tab = RelationTab.FRIENDS;
    ActiveField activeField = ActiveField.NONE;
    String playerInput = "";
    String statusMessage;
    long statusUntilMs;

    Rect friendsPill = new Rect(0f, 0f, 0f, 0f);
    Rect enemiesPill = new Rect(0f, 0f, 0f, 0f);
    Rect staffPill = new Rect(0f, 0f, 0f, 0f);
    Rect playerInputRect = new Rect(0f, 0f, 0f, 0f);
    Rect addTypedButton = new Rect(0f, 0f, 0f, 0f);
    Rect reservedPickerButton = new Rect(0f, 0f, 0f, 0f);

    float scroll;
    float smoothedScroll;
    boolean scrollbarVisible;
    boolean draggingScrollbar;
    float scrollbarX;
    float scrollbarY;
    float scrollbarW;
    float scrollbarH;
    float scrollbarThumbY;
    float scrollbarThumbH;
    float scrollbarMaxScroll;
    float scrollbarDragOffset;
    float listX;
    float listY;
    float listW;
    float listH;
    float modeAnim;
    float friendsHoverAnim;
    float enemiesHoverAnim;
    float staffHoverAnim;
    float addHoverAnim;
    float pickerHoverAnim;
    boolean movementInputBlocked;

    final long STATUS_DURATION_MS = 2200L;
    final String I18N = "clickgui.settings.relations.";

    RelationsState() {
        onlinePicker = new OnlineRelationPlayerPickerComponent();
    }

    void bindStatusSink() {
        onlinePicker.setStatusSink(this::setStatus);
    }

    void resetScroll() {
        scroll = 0f;
        smoothedScroll = 0f;
        draggingScrollbar = false;
        activeField = ActiveField.NONE;
        onlinePicker.close();
        onlinePicker.resetScroll();
        movementInputBlocked = false;
    }

    boolean isMovementInputBlocked() {
        return movementInputBlocked;
    }

    void updateMovementBlocked() {
        movementInputBlocked = activeField != ActiveField.NONE || onlinePicker.blocksMovementInput();
    }

    void setStatus(String message) {
        statusMessage = message;
        statusUntilMs = System.currentTimeMillis() + STATUS_DURATION_MS;
    }

    void clearStatus() {
        statusMessage = null;
        statusUntilMs = 0L;
    }

    String tr(String key, String fallback, Object... args) {
        return ClickGuiI18n.tr(I18N + key, fallback, args);
    }

    java.util.List<String> filteredEntries(java.util.List<String> entries) {
        if (!dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch.isActive() || dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch.getText().isBlank()) return entries;
        String q = dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch.getText().toLowerCase(java.util.Locale.ROOT);
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String entry : entries) {
            if (entry != null && entry.toLowerCase(java.util.Locale.ROOT).contains(q)) out.add(entry);
        }
        return out;
    }

    enum ActiveField {
        NONE,
        PLAYER
    }

    enum RelationTab {
        FRIENDS,
        ENEMIES,
        STAFF;

        List<String> entries() {
            PlayerRelations rel = PlayerRelations.get();
            return switch (this) {
                case FRIENDS -> sorted(rel.getFriends());
                case ENEMIES -> sorted(rel.getEnemies());
                case STAFF -> sorted(rel.getStaff());
            };
        }

        boolean add(String name) {
            PlayerRelations rel = PlayerRelations.get();
            boolean changed = switch (this) {
                case FRIENDS -> rel.addFriend(name);
                case ENEMIES -> rel.addEnemy(name);
                case STAFF -> rel.addStaff(name);
            };
            if (changed) rel.save();
            return changed;
        }

        boolean remove(String name) {
            PlayerRelations rel = PlayerRelations.get();
            boolean changed = switch (this) {
                case FRIENDS -> rel.removeFriend(name);
                case ENEMIES -> rel.removeEnemy(name);
                case STAFF -> rel.removeStaff(name);
            };
            if (changed) rel.save();
            return changed;
        }

        DefineTarget.RelationTargetMode pickerMode() {
            return switch (this) {
                case FRIENDS -> DefineTarget.RelationTargetMode.FRIEND;
                case ENEMIES -> DefineTarget.RelationTargetMode.ENEMY;
                case STAFF -> DefineTarget.RelationTargetMode.STAFF;
            };
        }

        List<String> sorted(Set<String> src) {
            List<String> out = new ArrayList<>();
            if (src != null) {
                for (String value : src) {
                    if (value != null && !value.isBlank()) out.add(value);
                }
            }
            out.sort(String.CASE_INSENSITIVE_ORDER);
            return out;
        }
    }

    record CardEntryHit(String name, RelationPlayerCardComponent.CardHit hit) {}

    record Rect(float x, float y, float w, float h) {
        boolean contains(float mx, float my) {
            return ClickGuiMath.insideRect(mx, my, x, y, w, h);
        }
    }
}
