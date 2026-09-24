package dev.loki.lovisual.config.values.primitive;
import dev.loki.lovisual.config.values.collection.ConfigValue;

public class StringValue extends ConfigValue<String> {

    public StringValue(String name, String def) {
        super(name, def);
    }

    @Override
    public Object toJson() {
        return value;
    }

    @Override
    public void fromJson(Object json) {
        if (json instanceof String s)
            this.value = s;
    }
}
