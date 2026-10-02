package dev.loki.lovisual.features.module.modules.visuals.items.glint;

import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;

/**
 * Enchantment glint on items and armor: style (Rainbow, Solid, Gradient, Hidden), colours, strength, scroll
 * speed and stripe scale. Speed and scale go through GlintTextureMixin, the look by repainting the textures.
 */
@ModuleInfo(
        id = "enchantglint",
        displayName = "Enchant Glint",
        aliases = {"enchantglint", "glint"},
        category = ModuleCategory.VISUALS
)
public class EnchantGlint extends Module {
    private final ModeValue style = modeSetting("enchantGlintStyle", "style", GlintTint.RAINBOW,
            GlintTint.RAINBOW, GlintTint.SOLID, GlintTint.GRADIENT, GlintTint.HIDDEN);
    private final RGBColorValue color =
            visibleWhen(colorNoAlpha("enchantGlintColor", "color", "#FF4FD8"), this::usesPrimary);
    private final RGBColorValue secondColor =
            visibleWhen(colorNoAlpha("enchantGlintColor2", "color2", "#4F9BFF"), this::usesSecondary);
    private final NumberValue<Float> strength = num("enchantGlintStrength", "strength", 1.5f, 0.2f, 3.0f);
    private final NumberValue<Float> speed = num("enchantGlintSpeed", "speed", 1.0f, 0.1f, 4.0f);
    private final NumberValue<Float> scale = num("enchantGlintScale", "scale", 1.0f, 0.2f, 4.0f);
    private final BooleanValue items = bool("enchantGlintItems", "items", true);
    private final BooleanValue armor = bool("enchantGlintArmor", "armor", true);

    private final GlintTexture texture = new GlintTexture();

    private boolean usesPrimary() {
        return GlintTint.SOLID.equals(style.get()) || GlintTint.GRADIENT.equals(style.get());
    }

    private boolean usesSecondary() {
        return GlintTint.GRADIENT.equals(style.get());
    }

    @Override
    public void onTick() {
        GlintTexture.Paint paint = new GlintTexture.Paint(style.get(),
                color.getArgb() & 0xFFFFFF, secondColor.getArgb() & 0xFFFFFF, strength.get());
        texture.sync(items.get() ? paint : null, armor.get() ? paint : null);
    }

    @Override
    public void onDisable() {
        texture.sync(null, null);
        texture.forget();
    }

    public float speedFactor() {
        return isEnabled() ? speed.get() : 1.0f;
    }

    public float scaleFactor() {
        return isEnabled() ? scale.get() : 1.0f;
    }
}
