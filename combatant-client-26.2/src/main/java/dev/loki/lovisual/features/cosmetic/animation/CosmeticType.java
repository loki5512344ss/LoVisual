package dev.loki.lovisual.features.cosmetic.animation;

public enum CosmeticType {
    MODEL("model", "Модель"),
    PET("pet", "Питомец"),
    ACCESSORY("accessory", "Аксессуар");

    private final String id;
    private final String displayName;

    CosmeticType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CosmeticType fromJson(String raw) {
        if (raw == null || raw.isBlank()) return MODEL;
        return switch (raw.toLowerCase()) {
            case "pet", "pets", "питомец", "питомцы" -> PET;
            case "accessory", "accessories", "item", "items", "аксессуар", "аксессуары" -> ACCESSORY;
            default -> MODEL;
        };
    }
}
