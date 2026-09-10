package dev.loki.lovisual.features.cosmetic.model;

/**
 * Where a cosmetic is worn. MODEL/PET are whole-body/companion slots (one active
 * item each); the remaining six are independent accessory slots that can all be
 * active simultaneously (e.g. a hat + wings + a belt at once).
 */
public enum CosmeticSlot {
    MODEL("model", "Модель"),
    PET("pet", "Питомец"),
    HEAD("head", "Голова"),
    SHOULDERS("shoulders", "Плечи"),
    BACK("back", "Спина"),
    WAIST("waist", "Пояс"),
    LEGS("legs", "Ноги"),
    HELD("held", "В руке");

    private final String id;
    private final String displayName;

    CosmeticSlot(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAccessory() {
        return this != MODEL && this != PET;
    }

    public static CosmeticSlot fromJson(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return switch (raw.trim().toLowerCase()) {
            case "model", "модель" -> MODEL;
            case "pet", "pets", "питомец", "питомцы" -> PET;
            case "head", "hat", "голова", "шляпа" -> HEAD;
            case "shoulders", "shoulder", "плечи" -> SHOULDERS;
            case "back", "спина" -> BACK;
            case "waist", "belt", "пояс" -> WAIST;
            case "legs", "leg", "ноги" -> LEGS;
            case "held", "hand", "рука" -> HELD;
            default -> null;
        };
    }

    /** Best-effort slot guess from the model's folder name (legacy accessories without a "slot" key). */
    public static CosmeticSlot guessFromFolderName(String folderName) {
        if (folderName == null) return HELD;
        String folder = folderName.toLowerCase();
        if (folder.contains("witch") || folder.contains("hat")) return HEAD;
        if (folder.contains("wing")) return BACK;
        if (folder.contains("wheel")) return WAIST;
        return HELD;
    }
}
