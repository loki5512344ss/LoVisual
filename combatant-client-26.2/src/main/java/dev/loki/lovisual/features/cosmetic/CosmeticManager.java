package dev.loki.lovisual.features.cosmetic;

/**
 * Holds which cosmetic (model/pet/accessory) the local user currently has equipped.
 * Rendering and animation live in separate classes (not part of this core pass).
 */
public final class CosmeticManager {
    private static final CosmeticManager INSTANCE = new CosmeticManager();

    private CosmeticModel activeModel;
    private CosmeticModel activePet;
    private CosmeticModel activeAccessory;

    private CosmeticManager() {
    }

    public static CosmeticManager get() {
        return INSTANCE;
    }

    /**
     * Reloads the catalog from disk, re-resolving the currently equipped cosmetics
     * (by id) against the freshly loaded models.
     */
    public void init() {
        String modelId = activeModel != null ? activeModel.getId() : null;
        String petId = activePet != null ? activePet.getId() : null;
        String accessoryId = activeAccessory != null ? activeAccessory.getId() : null;

        for (CosmeticModel model : CosmeticRepository.get().all()) {
            model.resetTexture();
        }
        CosmeticRepository.get().reload();

        CosmeticRepository repo = CosmeticRepository.get();
        activeModel = modelId != null ? repo.get(modelId) : null;
        activePet = petId != null ? repo.get(petId) : null;
        activeAccessory = accessoryId != null ? repo.get(accessoryId) : null;
    }

    public CosmeticModel getActive(CosmeticType type) {
        return switch (type) {
            case PET -> activePet;
            case ACCESSORY -> activeAccessory;
            case MODEL -> activeModel;
        };
    }

    public void setActive(CosmeticType type, CosmeticModel model) {
        if (model != null) {
            CosmeticModel resolved = CosmeticRepository.get().get(model.getId());
            if (resolved != null) {
                model = resolved;
            }
        }
        switch (type) {
            case PET -> activePet = model;
            case ACCESSORY -> activeAccessory = model;
            case MODEL -> activeModel = model;
        }
    }

    public void clear(CosmeticType type) {
        setActive(type, null);
    }

    public boolean hasAnyActive() {
        return activeModel != null || activePet != null || activeAccessory != null;
    }

    public boolean isActive() {
        return activeModel != null;
    }
}
