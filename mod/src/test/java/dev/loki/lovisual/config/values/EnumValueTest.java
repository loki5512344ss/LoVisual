package dev.loki.lovisual.config.values;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import dev.loki.lovisual.config.values.mode.EnumValue;

public final class EnumValueTest {

    private enum Speed implements EnumValue.IdProvider, EnumValue.AliasProvider {
        FAST("fast-id", List.of("f", "turbo")),
        SLOW("slow-id", List.of());

        private final String id;
        private final List<String> aliases;

        Speed(String id, List<String> aliases) {
            this.id = id;
            this.aliases = aliases;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public List<String> aliases() {
            return aliases;
        }
    }

    private enum Plain {
        ON,
        OFF
    }

    @Test
    void idProviderUsedForJsonAndLookup() {
        EnumValue<Speed> value = new EnumValue<>("e", Speed.FAST, Speed.FAST, Speed.SLOW);

        assertEquals("fast-id", value.getId());
        assertEquals("fast-id", value.toJson());
        assertEquals(List.of("fast-id", "slow-id"), value.getOptions());
    }

    @Test
    void setByIdIsCaseInsensitive() {
        EnumValue<Speed> value = new EnumValue<>("e", Speed.FAST, Speed.FAST, Speed.SLOW);

        value.setById("SLOW-ID");
        assertEquals(Speed.SLOW, value.get());
    }

    @Test
    void aliasesResolveToConstants() {
        EnumValue<Speed> value = new EnumValue<>("e", Speed.FAST, Speed.FAST, Speed.SLOW);

        value.setById("turbo");
        assertEquals(Speed.FAST, value.get());

        value.setById("f");
        assertEquals(Speed.FAST, value.get());
    }

    @Test
    void unknownIdKeepsCurrentValue() {
        EnumValue<Speed> value = new EnumValue<>("e", Speed.SLOW, Speed.FAST, Speed.SLOW);

        value.setById("nope");
        value.setById(null);
        assertEquals(Speed.SLOW, value.get());
    }

    @Test
    void fromJsonDelegatesToSetById() {
        EnumValue<Speed> value = new EnumValue<>("e", Speed.FAST, Speed.FAST, Speed.SLOW);

        value.fromJson("SLOW-ID");
        assertEquals(Speed.SLOW, value.get());

        value.fromJson(42);
        value.fromJson(null);
        assertEquals(Speed.SLOW, value.get());
    }

    @Test
    void defaultFallsBackToFirstOptionWhenAbsent() {
        EnumValue<Speed> value = new EnumValue<>("e", Speed.FAST, Speed.SLOW);

        assertEquals(Speed.SLOW, value.get());
    }

    @Test
    void plainEnumUsesLowercaseNameAsId() {
        EnumValue<Plain> value = new EnumValue<>("p", Plain.ON, Plain.class);

        assertEquals("on", value.getId());
        assertEquals("on", value.toJson());

        value.setById("OFF");
        assertEquals("off", value.toJson());
    }
}
