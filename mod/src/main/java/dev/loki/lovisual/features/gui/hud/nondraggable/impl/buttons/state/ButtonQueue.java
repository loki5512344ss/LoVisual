/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */
package dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.state;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.tooltips.BetterTooltips;
import dev.loki.lovisual.mixins.accessors.widget.ClickableWidgetAccessor;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
/**
 * Очередь отложенного рендера кнопок (batch за кадр + scissor-контекст).
 */
public final class ButtonQueue {
    private static final List<RenderCall> QUEUE = new ArrayList<>();
    private static final Deque<ScissorRect> SCISSOR_STACK = new ArrayDeque<>();
    private static GuiGraphicsExtractor LAST_CTX;
    private static Screen LAST_SCREEN;
    private ButtonQueue() {
    }
    public static void beginFrame(GuiGraphicsExtractor ctx) {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc != null ? ClientScreen.current() : null;
        if (screen != LAST_SCREEN) {
            ButtonHoverState.clearAll();
            LAST_SCREEN = screen;
        }
        LAST_CTX = ctx;
        QUEUE.clear();
        SCISSOR_STACK.clear();
        BetterTooltips.beginTooltipFrame();
    }
    public static GuiGraphicsExtractor lastContext() {
        return LAST_CTX;
    }
    public static void pushScissorContext(int x1, int y1, int x2, int y2) {
        if (x2 <= x1 || y2 <= y1) return;
        SCISSOR_STACK.push(new ScissorRect(x1, y1, x2, y2));
    }

    public static void popScissorContext() {
        if (!SCISSOR_STACK.isEmpty()) {
            SCISSOR_STACK.pop();
        }
    }

    private static ScissorRect currentScissor() {
        return SCISSOR_STACK.peek();
    }

    public static void flush() {
        if (QUEUE.isEmpty()) return;
        GuiGraphicsExtractor ctx = LAST_CTX;
        if (ctx == null) {
            QUEUE.clear();
            return;
        }
        for (RenderCall call : QUEUE) {
            ScissorRect scissor = extractScissor(call);
            boolean pushed = pushScissor(scissor);
            call.render(ctx);
            if (pushed) {
                popScissor();
            }
        }
        QUEUE.clear();
    }

    private static ScissorRect extractScissor(RenderCall call) {
        if (call instanceof ButtonCall c) return c.scissor();
        if (call instanceof SliderCall c) return c.scissor();
        if (call instanceof TextIconCall c) return c.scissor();
        if (call instanceof CyclingCall c) return c.scissor();
        return null;
    }

    private static boolean pushScissor(ScissorRect scissor) {
        if (scissor == null) return false;
        float scale = ViewportContext.getScaleFactor();
        return ScissorFunction.pushScaledRect(scissor.x1, scissor.y1, scissor.x2, scissor.y2, scale);
    }

    private static void popScissor() {
        ScissorFunction.pop();
    }

    public static void captureTooltip(AbstractWidget widget, int mouseX, int mouseY) {
        if (widget == null) return;
        BetterTooltips tooltips = BetterTooltips.get();
        if (tooltips == null || !tooltips.useCustomGuiTooltips()) {
            return;
        }
        WidgetTooltipHolder state = ((ClickableWidgetAccessor) widget).lovisual$getTooltipState();
        if (state == null) return;
        Tooltip tooltip = state.get();
        if (tooltip == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        List<FormattedCharSequence> lines = tooltip.toCharSequence(mc);
        if (lines == null || lines.isEmpty()) return;
        BetterTooltips.captureTooltipOrdered(lines, DefaultTooltipPositioner.INSTANCE, mouseX, mouseY);
    }

    public static boolean hasPending() {
        return !QUEUE.isEmpty();
    }

    public static boolean hasTooltip() {
        return BetterTooltips.hasTooltip();
    }

    public static void enqueueButton(AbstractWidget widget, Component text, float hover, boolean enabled, boolean focused) {
        QUEUE.add(new ButtonCall(widget, text, hover, enabled, focused, currentScissor()));
    }

    public static void enqueueSlider(AbstractWidget widget, float value, boolean dragging, float hover) {
        QUEUE.add(new SliderCall(widget, value, dragging, hover, currentScissor()));
    }

    public static void enqueueTextIconButton(SpriteIconButton widget, boolean iconOnly, float hover, boolean enabled, boolean focused) {
        QUEUE.add(new TextIconCall(widget, iconOnly, hover, enabled, focused, currentScissor()));
    }

    public static void enqueueCyclingButton(CycleButton<?> widget, Identifier icon, boolean showLabel, float hover, boolean enabled, boolean focused) {
        QUEUE.add(new CyclingCall(widget, icon, showLabel, hover, enabled, focused, currentScissor()));
    }

    public static void renderTooltip() {
        BetterTooltips.renderTooltipWithContext(LAST_CTX);
    }

    public interface RenderCall {
        void render(GuiGraphicsExtractor ctx);
    }

    public record ButtonCall(AbstractWidget widget,
                             Component text,
                             float hover,
                             boolean enabled,
                             boolean focused,
                             ScissorRect scissor) implements RenderCall {
        @Override
        public void render(GuiGraphicsExtractor ctx) {
            dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.BetterButtons.renderButton(ctx, widget, text, hover, enabled, focused);
        }
    }

    public record SliderCall(AbstractWidget widget,
                             float value,
                             boolean dragging,
                             float hover,
                             ScissorRect scissor) implements RenderCall {
        @Override
        public void render(GuiGraphicsExtractor ctx) {
            dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.BetterButtons.renderSlider(ctx, widget, value, dragging, hover);
        }
    }

    public record TextIconCall(SpriteIconButton widget,
                               boolean iconOnly,
                               float hover,
                               boolean enabled,
                               boolean focused,
                               ScissorRect scissor) implements RenderCall {
        @Override
        public void render(GuiGraphicsExtractor ctx) {
            dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.BetterButtons.renderTextIconButton(ctx, widget, iconOnly, hover, enabled, focused);
        }
    }

    public record CyclingCall(CycleButton<?> widget,
                              Identifier icon,
                              boolean showLabel,
                              float hover,
                              boolean enabled,
                              boolean focused,
                              ScissorRect scissor) implements RenderCall {
        @Override
        public void render(GuiGraphicsExtractor ctx) {
            dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.BetterButtons.renderCyclingButton(ctx, widget, icon, showLabel, hover, enabled, focused);
        }
    }

    public record ScissorRect(int x1, int y1, int x2, int y2) {
    }
}
