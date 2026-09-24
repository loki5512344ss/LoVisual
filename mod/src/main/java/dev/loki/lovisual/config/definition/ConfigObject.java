package dev.loki.lovisual.config.definition;

import dev.loki.lovisual.config.values.collection.ConfigValue;

import java.util.List;

public interface ConfigObject {
    List<ConfigValue<?>> getConfigValues();
}
