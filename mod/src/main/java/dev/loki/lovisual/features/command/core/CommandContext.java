package dev.loki.lovisual.features.command.core;

import net.minecraft.client.Minecraft;

import java.util.List;

public record CommandContext(Minecraft mc, String raw, String name, List<String> args) {

    public String arg(int idx) {
        if (idx < 0 || idx >= args.size()) return null;
        return args.get(idx);
    }
}
