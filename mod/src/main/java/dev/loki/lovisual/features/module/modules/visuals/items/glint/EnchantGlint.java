package dev.loki.lovisual.features.module.modules.visuals.items.glint;

import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.modules.visuals.items.glint.style.GlintMotion;
import dev.loki.lovisual.features.module.modules.visuals.items.glint.style.GlintStyles;
import net.minecraft.util.Util;
import org.joml.Matrix4f;

/**
 * Enchantment glint on items and armor: eleven styles (Rainbow, Solid, Gradient, Pulse, Fire, Galaxy, Hologram,
 * Glitch, Sparkle, Aurora, Hidden), colours, strength, coverage, scroll speed and stripe scale. The look is a
 * repaint of the textures; speed, scale and the animated styles' motion go through GlintTextureMixin.
 */
@ModuleInfo(
        id = "enchantglint",
        displayName = "Enchant Glint",
        aliases = {"enchantglint", "glint"},
        category = ModuleCategory.VISUALS
)
public class EnchantGlint extends Module {
    private final ModeValue style = modeSetting("enchantGlintStyle", "style", GlintTint.RAINBOW,
            GlintTint.RAINBOW, GlintTint.SOLID, GlintTint.GRADIENT, GlintStyles.PULSE, GlintStyles.FIRE,
            GlintStyles.GALAXY, GlintStyles.HOLOGRAM, GlintStyles.GLITCH, GlintStyles.SPARKLE, GlintStyles.AURORA,
            GlintTint.HIDDEN);
    private final RGBColorValue color =
            visibleWhen(colorNoAlpha("enchantGlintColor", "color", "#FF4FD8"), this::usesPrimary);
    private final RGBColorValue secondColor =
            visibleWhen(colorNoAlpha("enchantGlintColor2", "color2", "#4F9BFF"), this::usesSecondary);
    private final NumberValue<Float> strength = num("enchantGlintStrength", "strength", 1.5f, 0.2f, 3.0f);
    private final NumberValue<Float> coverage = num("enchantGlintCoverage", "coverage", 0.35f, 0.0f, 1.0f);
    private final NumberValue<Float> speed = num("enchantGlintSpeed", "speed", 1.0f, 0.1f, 4.0f);
    private final NumberValue<Float> scale = num("enchantGlintScale", "scale", 1.0f, 0.2f, 4.0f);
    private final BooleanValue items = bool("enchantGlintItems", "items", true);
    private final BooleanValue armor = bool("enchantGlintArmor", "armor", true);

    private final GlintTexture texture = new GlintTexture();

    private boolean usesPrimary() {
        String current = style.get();
        return !GlintTint.RAINBOW.equals(current) && !GlintTint.HIDDEN.equals(current);
    }

    private boolean usesSecondary() {
        return GlintTint.GRADIENT.equals(style.get()) || GlintStyles.usesSecondary(style.get());
    }

    @Override
    public void onTick() {
        GlintTexture.Paint paint = new GlintTexture.Paint(style.get(),
                color.getArgb() & 0xFFFFFF, secondColor.getArgb() & 0xFFFFFF, strength.get(), coverage.get());
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

    /** Animated texture matrix of the current style, or null while the style keeps the vanilla motion. */
    public Matrix4f motion(float scale) {
        if (!isEnabled()) return null;
        return GlintMotion.matrix(style.get(), Util.getMillis() * 0.001f * speed.get(), scale);
    }

    public float scaleFactor() {
        return isEnabled() ? scale.get() : 1.0f;
    }
}
