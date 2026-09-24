package dev.loki.lovisual.config.values.color;
import dev.loki.lovisual.config.values.collection.ConfigValue;

public class KeyBindValue extends ConfigValue<String> {

    public KeyBindValue(String name, String def) {
        super(name, def == null ? "NONE" : def);
    }

    @Override
    public Object toJson() {
        return value;
    }

    @Override
    public void fromJson(Object json) {
        if (json instanceof String s)
            this.value = s.toUpperCase();
    }

    public boolean isNone() {
        return "NONE".equalsIgnoreCase(value);
    }
}
