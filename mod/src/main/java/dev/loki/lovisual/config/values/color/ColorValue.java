package dev.loki.lovisual.config.values.color;

public interface ColorValue {
    String get();

    void set(String value);

    int getArgb();

    boolean supportsAlpha();

    boolean isRainbow();

    String toRainbowValue();

    String rainbowFallbackHex();
}
