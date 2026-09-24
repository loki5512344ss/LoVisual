package dev.loki.lovisual.render.engine.renderer.ui.runtime.render.bridge;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import net.minecraft.resources.Identifier;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.asset.registry.UiAssetKind;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.asset.UiAssetRef;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiBounds;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.theme.UiColor;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.style.UiStyle;
import dev.loki.lovisual.render.engine.svg.parser.SvgRegistry;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.helpers.util.GuiSpriteBatch;
import dev.loki.lovisual.render.helpers.player.PlayerHeadRenderer;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.render.context.UiRenderContext;

public final class UiImageRendererBridge {
    private static Identifier identifier(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Identifier.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void render(UiNode node, UiAssetRef asset, UiRenderContext context) {
        if (node == null || asset == null || context == null) return;
        UiBounds rawBounds = node.bounds();
        UiBounds bounds = new UiBounds(
                node.props().number("renderX", rawBounds.x()),
                node.props().number("renderY", rawBounds.y()),
                Math.max(0.0f, node.props().number("renderWidth", rawBounds.width())),
                Math.max(0.0f, node.props().number("renderHeight", rawBounds.height()))
        );
        UiStyle style = node.style();
        String explicitTint = node.props().string("tint", "");
        int tint = UiColor.parse(explicitTint, style.textColor() != null ? style.textColor() : 0xFFFFFFFF);
        if ((tint >>> 24) == 0) return;
        if (asset.kind() == UiAssetKind.SVG) {
            Identifier svgId = SvgRegistry.resolve(asset.getId());
            if (svgId == null) return;
            SvgRenderOptions options = explicitTint == null || explicitTint.isBlank()
                    ? SvgRenderOptions.DEFAULT
                    : SvgRenderOptions.overrideColor(tint);
            context.renderer().svg(svgId, bounds.x(), bounds.y(), bounds.width(), bounds.height(), options);
            return;
        }

        Identifier id = identifier(asset.getId());
        if (id == null) return;

        if (asset.kind() == UiAssetKind.GUI_SPRITE) {
            GuiSpriteBatch.draw(id, bounds.x(), bounds.y(), bounds.width(), bounds.height(), tint);
            return;
        }
        if (asset.kind() == UiAssetKind.PLAYER_HEAD) {
            PlayerHeadRenderer.drawRounded(
                    context.drawContext(),
                    bounds.x(),
                    bounds.y(),
                    Math.min(bounds.width(), bounds.height()),
                    style.radius(),
                    id,
                    new RenderColor(tint),
                    node.props().bool("secondLayer", true),
                    null,
                    0.0f,
                    false
            );
            return;
        }
        boolean mask = node.props().bool("mask", false) || node.props().bool("alphaMask", false);
        if (mask) {
            Renderer2D.TEXTURE.roundedTexMaskRect(
                    bounds.x(),
                    bounds.y(),
                    bounds.width(),
                    bounds.height(),
                    style.radius(),
                    tint,
                    id
            );
            return;
        }
        Renderer2D.TEXTURE.roundedTexRect(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                style.radius(),
                tint,
                id
        );
    }
}
