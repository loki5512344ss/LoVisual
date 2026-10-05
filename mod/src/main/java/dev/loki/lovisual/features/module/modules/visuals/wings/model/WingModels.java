package dev.loki.lovisual.features.module.modules.visuals.wings.model;

import dev.loki.lovisual.features.module.modules.visuals.wings.model.anim.BedrockClip;
import dev.loki.lovisual.features.module.modules.visuals.wings.model.anim.ProceduralWings;
import dev.loki.lovisual.features.module.modules.visuals.wings.model.anim.WingAnimator;
import dev.loki.lovisual.util.bedrock.geo.GeoParser;
import dev.loki.lovisual.util.logging.DebugLog;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Lazily loads wing models from {@code assets/lovisual/wings} and keeps them until the resources reload. */
public final class WingModels {
    private static final String NAMESPACE = "lovisual";

    /** A model ready to pose and draw. */
    public record Loaded(WingModelDef def, GeoParser.Model model, WingAnimator animator, Identifier texture) {
    }

    private static final Map<String, Optional<Loaded>> CACHE = new HashMap<>();

    private WingModels() {
    }

    public static Loaded get(WingModelDef def) {
        return CACHE.computeIfAbsent(def.name(), name -> Optional.ofNullable(load(def))).orElse(null);
    }

    public static void clear() {
        CACHE.clear();
    }

    private static Loaded load(WingModelDef def) {
        try {
            GeoParser.Model model = GeoParser.parse(read("wings/" + def.geometry() + ".geo.json"));
            WingAnimator animator = animator(def);
            Identifier texture = Identifier.fromNamespaceAndPath(NAMESPACE, "textures/wings/" + def.texture() + ".png");
            return new Loaded(def, model, animator, texture);
        } catch (IOException | RuntimeException e) {
            DebugLog.warn("[LoVisual] Wing model '" + def.name() + "' failed to load: " + e);
            return null;
        }
    }

    private static WingAnimator animator(WingModelDef def) throws IOException {
        return switch (def.animation()) {
            case "angel" -> ProceduralWings.ANGEL;
            case "demon" -> ProceduralWings.DEMON;
            case "dragon" -> ProceduralWings.DRAGON;
            default -> BedrockClip.parse(read("wings/" + def.animation() + ".animation.json"));
        };
    }

    private static String read(String path) throws IOException {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager()
                .getResource(Identifier.fromNamespaceAndPath(NAMESPACE, path));
        if (resource.isEmpty()) throw new IOException("missing resource " + path);
        try (BufferedReader reader = resource.get().openAsReader()) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
