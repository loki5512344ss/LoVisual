package dev.loki.lovisual.config.io;

import java.util.List;
import dev.loki.lovisual.config.definition.SettingDef;

/**
 * Provides GUI-neutral setting definitions.
 */
public interface SettingDefProvider {
    List<SettingDef> getSettingDefs();
}
