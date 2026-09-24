package dev.loki.lovisual.config.i18n;

import java.util.List;

public interface ConfigValueAliasProvider {
    List<String> getLegacyValueNames(String currentValueName);
}
