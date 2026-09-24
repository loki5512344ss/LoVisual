package dev.loki.lovisual.config.values.primitive;
import dev.loki.lovisual.config.values.collection.ConfigValue;

public class BooleanValue extends ConfigValue<Boolean> {

    public BooleanValue(String name, boolean def) {
        super(name, def);
    }

    @Override
    public Object toJson() {
        return value;
    }

    @Override
    public void fromJson(Object json) {
        if (json instanceof Boolean b)
            value = b;
    }
}
