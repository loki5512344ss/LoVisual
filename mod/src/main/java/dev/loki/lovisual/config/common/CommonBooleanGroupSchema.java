package dev.loki.lovisual.config.common;

import java.util.Map;
import dev.loki.lovisual.config.common.CommonSettingSchema;

public interface CommonBooleanGroupSchema extends CommonSettingSchema {
    Map<String, Boolean> defaults();
}
