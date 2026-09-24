package dev.loki.lovisual.util.block.scan;

import dev.loki.lovisual.config.values.mode.EnumValue;

import java.util.List;

public enum BlockScanMode implements EnumValue.IdProvider, EnumValue.AliasProvider {
    LOS("los", "los_unlocked"),
    UNRESTRICTED("unrestricted", "aggressive");

    private final String id;
    private final List<String> aliases;

    BlockScanMode(String id, String... aliases) {
        this.id = id;
        this.aliases = List.of(aliases);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public List<String> aliases() {
        return aliases;
    }
}
