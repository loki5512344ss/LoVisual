package dev.loki.lovisual.features.module.modules.misc.sound;

import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.util.wav.engine.CustomSoundEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

import java.util.Locale;

/** Plays our own sound when a totem of undying pops, optionally silencing the vanilla one. */
@ModuleInfo(
        id = "totemsound",
        displayName = "Totem Sound",
        aliases = {"totemsound"},
        category = ModuleCategory.MISC
)
public class TotemSound extends Module {
    private static final String VANILLA_TOTEM_SOUND = "minecraft:item.totem.use";
    private static final String EVERYONE = "Everyone";
    private static final String SELF = "Self";
    private static final String OTHERS = "Others";

    private final ModeValue sound = modeSetting("totemSoundSound", "sound", "Bell",
            "Bell", "Bonk", "Bubble", "Crime", "Critical");
    private final ModeValue trigger = modeSetting("totemSoundTrigger", "trigger", EVERYONE,
            EVERYONE, SELF, OTHERS);
    private final NumberValue<Float> volume = num("totemSoundVolume", "volume", 1.0f, 0.0f, 1.0f);
    private final NumberValue<Float> pitch = num("totemSoundPitch", "pitch", 1.0f, 0.5f, 2.0f);
    private final BooleanValue muteVanilla = bool("totemSoundMuteVanilla", "mute_vanilla", false);

    /** Called on every totem pop packet; {@code entity} is the one that popped. */
    public void onTotemPop(Entity entity) {
        if (!isEnabled() || entity == null) return;
        if (!matches(trigger.get(), entity == Minecraft.getInstance().player)) return;
        Identifier id = Identifier.fromNamespaceAndPath("lovisual",
                "sounds/hitsounds/" + sound.get().toLowerCase(Locale.ROOT) + ".wav");
        CustomSoundEngine.get().play(id, volume.get(), pitch.get(), false, true);
    }

    /** Volume multiplier for the sound engine: 0 for the vanilla totem sound when muted, else 1. */
    public float vanillaScale(String soundEventId) {
        return isEnabled() && muteVanilla.get() && VANILLA_TOTEM_SOUND.equals(soundEventId) ? 0.0f : 1.0f;
    }

    /** Pure decision: does this trigger mode cover the popping entity? */
    static boolean matches(String trigger, boolean isSelf) {
        return switch (trigger) {
            case SELF -> isSelf;
            case OTHERS -> !isSelf;
            default -> true;
        };
    }
}
