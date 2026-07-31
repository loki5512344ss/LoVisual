/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat;

import com.mojang.blaze3d.platform.InputConstants;
import dev.loki.lovisual.features.gui.hud.draggable.impl.BetterChat;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * Mouse-button dispatch over the chat surface: scrollbar drag, suggest rows, search icon,
 * context menu and click/selection logic. Delegates textual actions to {@link BetterChatContextMenuHandler}.
 */
final class BetterChatMouseClickHandler {
    private BetterChatMouseClickHandler() {
    }

    static boolean onMouseButton(double mx, double my, int button, boolean pressed) {
        Minecraft mc = Minecraft.getInstance();
        if (!BetterChat.isActive()) return false;
        if (!(ClientScreen.current() instanceof ChatScreen)) return false;

        BetterChatModel.MousePos mouse = BetterChatInputHandler.resolveMouse(mx, my);
        double fx = mouse.fx();
        double fy = mouse.fy();
        boolean leftUp = button == GLFW.GLFW_MOUSE_BUTTON_LEFT && !pressed;

        boolean overSuggestWindow = BetterChatSuggestionState.isInsideSuggestWindow(fx, fy, mouse.rawX(), mouse.rawY());

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && BetterChatRenderer.sbEnabled && BetterChatDrawUtil.isScrollbarHovered(fx, fy)) {
            if (pressed) {
                BetterChatRenderer.draggingScrollbar = true;
                BetterChatRenderer.sbDragOffset = (fy >= BetterChatRenderer.sbThumbY && fy <= BetterChatRenderer.sbThumbY + BetterChatRenderer.sbThumbH)
                        ? (float) fy - BetterChatRenderer.sbThumbY
                        : BetterChatRenderer.sbThumbH * 0.5f;
                BetterChatDrawUtil.scrollToPosition(fy);
            } else {
                BetterChatRenderer.draggingScrollbar = false;
            }
            return true;
        }
        if (BetterChatRenderer.draggingScrollbar) {
            if (leftUp) BetterChatRenderer.draggingScrollbar = false;
            return true;
        }

        float sTop = BetterChatRenderer.suggestRowsY;
        boolean overSuggestRows = BetterChatSuggestionState.isInsideSuggestRows(fx, fy, mouse.rawX(), mouse.rawY());

        if (BetterChatRenderer.suggestDraggingScrollbar) {
            if (leftUp) BetterChatRenderer.suggestDraggingScrollbar = false;
            return true;
        }

        if (BetterChatRenderer.suggestHasScrollbar && BetterChatSuggestionState.isInsideSuggestScrollbar(fx, fy)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (pressed) {
                    BetterChatRenderer.suggestDraggingScrollbar = true;
                    BetterChatSuggestionState.scrollSuggestToPosition(fy);
                }
            }
            return true;
        }

        if (overSuggestRows) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && pressed) {
                int row = (int) ((fy - sTop) / BetterChatRenderer.suggestItemH);
                row = Mth.clamp(row, 0, Math.max(0, BetterChatRenderer.suggestVisible - 1));
                int idx = Mth.clamp(BetterChatRenderer.suggestStart + row, 0, Math.max(0, BetterChatRenderer.suggestTotal - 1));
                BetterChatSuggestionState.applySuggestion(idx);
                return true;
            }
            return true;
        }

        if (overSuggestWindow) {
            return true;
        }

        if (BetterChatRenderer.searchHit) {
            boolean inside = fx >= BetterChatRenderer.searchRectX && fx <= BetterChatRenderer.searchRectX + BetterChatRenderer.searchRectW && fy >= BetterChatRenderer.searchRectY && fy <= BetterChatRenderer.searchRectY + BetterChatRenderer.searchRectH;
            if (inside && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (pressed) {
                    BetterChatSuggestionState.toggleSearchMode();
                }
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && pressed) {
                BetterChatSearch.setActive(false);
            }
        }

        if (BetterChatRenderer.contextMenu.open()) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                if (pressed && BetterChatRenderer.contextMenu.contains(fx, fy)) {
                    BetterChatContextMenuModel.ContextMenu.MenuEntry entry = BetterChatRenderer.contextMenu.pick(fy);
                    if (entry != null) entry.action().run();
                }
                BetterChatRenderer.contextMenu = BetterChatContextMenuModel.ContextMenu.closed();
                return true;
            }
        }

        BetterChatModel.PickResult pick = BetterChatRenderer.frame.pick(fx, fy);
        if (pick == null) {
            if (pressed && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                BetterChatRenderer.selection.clear();
                BetterChatRenderer.selecting = false;
                BetterChatRenderer.leftPending = false;
            }
            if (!pressed && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                BetterChatRenderer.selecting = false;
            }
            BetterChatRenderer.contextMenu = BetterChatContextMenuModel.ContextMenu.closed();
            return false;
        }

        boolean ctrlDown = InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(mc.getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (pressed) {
                BetterChatRenderer.leftPending = true;
                BetterChatRenderer.leftDownFx = fx;
                BetterChatRenderer.leftDownFy = fy;
                BetterChatRenderer.leftDownMsgIndex = pick.line().messageIndex();
                BetterChatRenderer.leftDownCharIndex = pick.glyph().charIndex();
                BetterChatRenderer.leftDownStyle = pick.glyph().style();

                BetterChatRenderer.selecting = false;
                BetterChatRenderer.contextMenu = BetterChatContextMenuModel.ContextMenu.closed();
                return true;
            } else {
                if (BetterChatRenderer.leftPending) {
                    BetterChatRenderer.leftPending = false;

                    if (BetterChatRenderer.selecting) {
                        BetterChatRenderer.selecting = false;
                        return true;
                    }

                    boolean handled = BetterChatContextMenuHandler.handleClickEvent(BetterChatRenderer.leftDownStyle, ctrlDown);
                    if (!handled && ctrlDown) {
                        handled = BetterChatContextMenuHandler.tryPrefillTellFromClick(BetterChatRenderer.leftDownMsgIndex, BetterChatRenderer.leftDownCharIndex);
                    }
                    if (!handled) BetterChatRenderer.selection.clear();
                    return true;
                }
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (pressed) {
                BetterChatContextMenuHandler.openContextMenu(pick, mx, my);
            }
        }
        return false;
    }
}