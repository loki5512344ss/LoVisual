package dev.loki.lovisual.features.cosmetic.manager;

import java.util.EnumMap;
import java.util.Map;
import dev.loki.lovisual.features.cosmetic.manager.CosmeticRepository;
import dev.loki.lovisual.features.cosmetic.model.CosmeticModel;
import dev.loki.lovisual.features.cosmetic.model.CosmeticSlot;

/**
 * Holds which cosmetic the local user currently has equipped, per slot.
 * MODEL/PET are single-active slots; the six accessory slots (HEAD/SHOULDERS/
 * BACK/WAIST/LEGS/HELD) are independent and can all be active at once.
 * Rendering and animation live in separate classes (not part of this core pass).
 */
public final class CosmeticManager {
    private static final CosmeticManager INSTANCE = new CosmeticManager();

    private final Map<CosmeticSlot, CosmeticModel> active = new EnumMap<>(CosmeticSlot.class);

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
        Map<CosmeticSlot, String> equippedIds = new EnumMap<>(CosmeticSlot.class);
        for (Map.Entry<CosmeticSlot, CosmeticModel> entry : active.entrySet()) {
            if (entry.getValue() != null) {
                equippedIds.put(entry.getKey(), entry.getValue().getId());
            }
        }

        for (CosmeticModel model : CosmeticRepository.get().all()) {
            model.resetTexture();
        }
        CosmeticRepository.get().reload();

        active.clear();
        CosmeticRepository repo = CosmeticRepository.get();
        for (Map.Entry<CosmeticSlot, String> entry : equippedIds.entrySet()) {
            CosmeticModel resolved = repo.get(entry.getValue());
            if (resolved != null) {
                active.put(entry.getKey(), resolved);
            }
        }
    }

    public CosmeticModel getActive(CosmeticSlot slot) {
        return active.get(slot);
    }

    public void setActive(CosmeticSlot slot, CosmeticModel model) {
        if (model != null) {
            CosmeticModel resolved = CosmeticRepository.get().get(model.getId());
            if (resolved != null) {
                model = resolved;
            }
        }
        if (model == null) {
            active.remove(slot);
        } else {
            active.put(slot, model);
        }
    }

    public void clear(CosmeticSlot slot) {
        setActive(slot, null);
    }

    public boolean hasAnyActive() {
        return !active.isEmpty();
    }

    public boolean isActive() {
        return active.get(CosmeticSlot.MODEL) != null;
    }
}
