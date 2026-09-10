package dev.loki.lovisual.features.cosmetic.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import dev.loki.lovisual.features.cosmetic.animation.CosmeticBbAnimation;
import dev.loki.lovisual.features.cosmetic.animation.CosmeticTextureSlot;
import dev.loki.lovisual.features.cosmetic.animation.CosmeticType;
import dev.loki.lovisual.features.cosmetic.model.CosmeticBone;
import dev.loki.lovisual.features.cosmetic.model.CosmeticCube;
import dev.loki.lovisual.features.cosmetic.model.CosmeticSlot;

/**
 * A single cosmetic asset: a Blockbench (.bbmodel) bone/cube tree plus its texture(s),
 * loaded from a user-provided folder (avatar.json + model.bbmodel + textures).
 */
@Getter
public class CosmeticModel {
    private final String id;
    private final String name;
    private final String description;
    private final String accentColor;
    private final String folderName;
    private final CosmeticType type;
    private final CosmeticSlot slot;
    private final File externalDirectory;
    @Setter private String source = "Своя";

    private final List<CosmeticBone> rootBones = new ArrayList<>();
    private final List<CosmeticBone> renderRoots = new ArrayList<>();
    private boolean customLegsPresent;
    private boolean bowReplacementAccessory;
    private boolean swordReplacementAccessory;
    private boolean hatOnlyAccessory;
    private boolean wingsOnlyAccessory;
    private Set<String> visibleBoneUuids;
    @Setter private Set<String> animationBoneFilter;
    private final Map<String, CosmeticBone> boneMap = new HashMap<>();
    private final Map<String, CosmeticBone> boneMapByUuid = new HashMap<>();
    private final Map<String, CosmeticBbAnimation> bbAnimations = new LinkedHashMap<>();
    private final List<CosmeticTextureSlot> textureSlots = new ArrayList<>();
    private int textureWidth = 64;
    private int textureHeight = 64;
    private Identifier textureId;
    private boolean textureLoaded = false;
    private CosmeticTextureSlot fallbackSlot;

    private CosmeticModel(String id, String name, String description, String accentColor,
                          String folderName, CosmeticType type, CosmeticSlot slot, File externalDirectory) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.accentColor = accentColor;
        this.folderName = folderName;
        this.type = type;
        this.slot = slot != null ? slot : defaultSlotFor(type, folderName);
        this.externalDirectory = externalDirectory;
    }

    private static CosmeticSlot defaultSlotFor(CosmeticType type, String folderName) {
        return switch (type) {
            case MODEL -> CosmeticSlot.MODEL;
            case PET -> CosmeticSlot.PET;
            case ACCESSORY -> CosmeticSlot.guessFromFolderName(folderName);
        };
    }

    public CosmeticModel withType(CosmeticType newType) {
        CosmeticModel copy = new CosmeticModel(id, name, description, accentColor, folderName, newType, null, externalDirectory);
        copy.rootBones.addAll(rootBones);
        copy.renderRoots.addAll(renderRoots);
        copy.boneMap.putAll(boneMap);
        copy.boneMapByUuid.putAll(boneMapByUuid);
        copy.bbAnimations.putAll(bbAnimations);
        copy.textureSlots.addAll(textureSlots);
        copy.textureWidth = textureWidth;
        copy.textureHeight = textureHeight;
        copy.textureId = textureId;
        copy.textureLoaded = textureLoaded;
        copy.source = source;
        copy.postProcess();
        return copy;
    }

    public void resetTexture() {
        textureLoaded = false;
        textureId = null;
        for (CosmeticTextureSlot slot : textureSlots) {
            slot.reset();
        }
    }

    public CosmeticTextureSlot getTextureSlot(int index) {
        if (!textureSlots.isEmpty()) {
            if (index >= 0 && index < textureSlots.size()) {
                return textureSlots.get(index);
            }
            return textureSlots.get(0);
        }
        if (textureId == null) return null;
        if (fallbackSlot == null) {
            fallbackSlot = new CosmeticTextureSlot(0, textureId, textureWidth, textureHeight, "fallback");
            if (textureLoaded) fallbackSlot.markLoaded();
        }
        return fallbackSlot;
    }

    /**
     * Loads a cosmetic from a user folder: {@code avatar.json} (metadata) plus a
     * {@code .bbmodel} (geometry/animation) and its texture(s).
     */
    public static CosmeticModel loadExternal(File folder, String sourceTag) {
        if (folder == null || !folder.isDirectory()) return null;
        String key = sourceTag + ":" + folder.getName().toLowerCase();
        return load(folder.getName(), folder, key, null);
    }

    private static CosmeticModel load(String folderName, File externalDir, String catalogId, CosmeticType forcedType) {
        try {
            JsonObject avatar = new JsonObject();
            File avatarFile = new File(externalDir, "avatar.json");
            if (avatarFile.exists()) {
                try (InputStreamReader reader = new InputStreamReader(new FileInputStream(avatarFile), StandardCharsets.UTF_8)) {
                    avatar = JsonParser.parseReader(reader).getAsJsonObject();
                }
            }

            String name = avatar.has("name") ? avatar.get("name").getAsString() : folderName;
            String desc = avatar.has("description") ? avatar.get("description").getAsString() : "";
            String color = avatar.has("color") ? avatar.get("color").getAsString() : "#ffffff";
            CosmeticType type = forcedType != null ? forcedType : CosmeticType.fromJson(
                    avatar.has("category") ? avatar.get("category").getAsString()
                    : avatar.has("type") ? avatar.get("type").getAsString() : null
            );

            CosmeticSlot slot = avatar.has("slot") ? CosmeticSlot.fromJson(avatar.get("slot").getAsString()) : null;
            CosmeticModel model = new CosmeticModel(catalogId, name, desc, color, folderName, type, slot, externalDir);

            InputStream modelStream = openModelStream(externalDir, folderName);
            if (modelStream == null) {
                return model;
            }

            JsonObject bbmodel;
            try (InputStreamReader reader = new InputStreamReader(modelStream, StandardCharsets.UTF_8)) {
                bbmodel = JsonParser.parseReader(reader).getAsJsonObject();
            }

            if (bbmodel.has("resolution")) {
                JsonObject res = bbmodel.getAsJsonObject("resolution");
                model.textureWidth = res.get("width").getAsInt();
                model.textureHeight = res.get("height").getAsInt();
            }

            model.textureId = Identifier.fromNamespaceAndPath(
                    "lovisual",
                    "dynamic/cosmetic/" + catalogId.replace(':', '_').replace(' ', '_')
            );
            model.loadTextureSlots(bbmodel);

            Map<String, JsonObject> elementMap = new HashMap<>();
            if (bbmodel.has("elements")) {
                for (JsonElement el : bbmodel.getAsJsonArray("elements")) {
                    JsonObject obj = el.getAsJsonObject();
                    if (obj.has("uuid")) {
                        elementMap.put(obj.get("uuid").getAsString(), obj);
                    }
                }
            }

            Map<String, JsonObject> groupMap = buildGroupMap(bbmodel);

            if (bbmodel.has("outliner")) {
                for (JsonElement el : bbmodel.getAsJsonArray("outliner")) {
                    CosmeticBone bone = parseOutliner(el, elementMap, groupMap, model.textureWidth, model.textureHeight);
                    if (bone != null) {
                        model.rootBones.add(bone);
                        model.indexBone(bone);
                    }
                }
            }

            model.bbAnimations.putAll(CosmeticBbAnimation.parseAll(bbmodel));
            model.postProcess();

            return model;
        } catch (Exception e) {
            return null;
        }
    }

    private static InputStream openModelStream(File externalDir, String folderName) throws IOException {
        File model = new File(externalDir, "model.bbmodel");
        if (!model.exists()) {
            model = new File(externalDir, folderName + ".bbmodel");
        }
        if (model.exists()) return new FileInputStream(model);
        File[] bbmodels = externalDir.listFiles((dir, name) -> name.endsWith(".bbmodel"));
        if (bbmodels != null && bbmodels.length > 0) {
            return new FileInputStream(bbmodels[0]);
        }
        return null;
    }

    private void loadTextureSlots(JsonObject bbmodel) {
        textureSlots.clear();
        if (!bbmodel.has("textures")) return;

        String modelKey = id.replace(':', '_');
        JsonArray textures = bbmodel.getAsJsonArray("textures");
        for (int i = 0; i < textures.size(); i++) {
            if (!textures.get(i).isJsonObject()) continue;
            JsonObject tex = textures.get(i).getAsJsonObject();
            textureSlots.add(CosmeticTextureSlot.fromJson(i, modelKey, tex));
        }
    }

    public void ensureTextureSlotLoaded(CosmeticTextureSlot slot) {
        if (slot == null || slot.isLoaded()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        slot.load(mc, externalDirectory, folderName);
    }

    /**
     * Loads whichever textures this model needs (per-slot bbmodel textures, or a single
     * fallback {@code texture.png}/{@code avatar.png} in the cosmetic's folder).
     */
    public void loadTexture() {
        if (textureLoaded) return;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getTextureManager() == null) return;

            boolean anyLoaded = false;
            for (CosmeticTextureSlot slot : textureSlots) {
                slot.load(mc, externalDirectory, folderName);
                if (slot.isLoaded()) anyLoaded = true;
            }

            if (!anyLoaded && externalDirectory != null) {
                String[] names = {"outfits/texture.png", "texture.png", folderName + ".png", "avatar.png"};
                for (String rel : names) {
                    File tex = new File(externalDirectory, rel);
                    if (!tex.exists()) continue;
                    try (InputStream is = new FileInputStream(tex)) {
                        registerTexture(mc, textureId, is);
                        anyLoaded = true;
                        break;
                    } catch (Exception ignored) {
                    }
                }
            } else if (!textureSlots.isEmpty() && textureSlots.get(0).isLoaded()) {
                textureId = textureSlots.get(0).getId();
                textureWidth = textureSlots.get(0).getWidth();
                textureHeight = textureSlots.get(0).getHeight();
            }

            textureLoaded = anyLoaded;
            if (!anyLoaded) {
                textureId = null;
            }
        } catch (Exception ignored) {
        }
    }

    private void registerTexture(Minecraft mc, Identifier rawId, InputStream is) throws IOException {
        NativeImage img = NativeImage.read(is);
        DynamicTexture tex = new DynamicTexture(() -> "lovisual_cosmetic", img);
        mc.getTextureManager().register(rawId, tex);
        textureId = rawId;
        textureLoaded = true;
    }

    private void indexBone(CosmeticBone bone) {
        boneMap.put(bone.getName().toLowerCase(), bone);
        boneMapByUuid.put(bone.getUuid(), bone);
        for (CosmeticBone child : bone.getChildren()) indexBone(child);
    }

    public CosmeticBone getBone(String name) {
        return boneMap.get(name.toLowerCase());
    }

    public CosmeticBone getBoneByUuid(String uuid) {
        return boneMapByUuid.get(uuid);
    }

    public boolean hasBbAnimations() {
        return !bbAnimations.isEmpty();
    }

    public Collection<CosmeticBbAnimation> getBbAnimations() {
        return bbAnimations.values();
    }

    public CosmeticBbAnimation findBbAnimation(String... candidates) {
        for (String candidate : candidates) {
            if (candidate == null) continue;
            CosmeticBbAnimation anim = bbAnimations.get(candidate.toLowerCase());
            if (anim != null) return anim;
        }
        for (String candidate : candidates) {
            if (candidate == null) continue;
            String lower = candidate.toLowerCase();
            for (Map.Entry<String, CosmeticBbAnimation> entry : bbAnimations.entrySet()) {
                if (entry.getKey().contains(lower) || lower.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    public CosmeticBbAnimation getFirstBbAnimation() {
        for (String preferred : List.of(
                "idling", "idle", "idle_arms", "idlemonsbob", "defaultstate",
                "walking", "yap", "painting", "pose", "pose2"
        )) {
            CosmeticBbAnimation anim = findBbAnimation(preferred);
            if (anim != null) return anim;
        }
        if (bbAnimations.isEmpty()) return null;
        return bbAnimations.values().iterator().next();
    }

    public List<CosmeticBone> getRenderRoots() {
        return renderRoots.isEmpty() ? rootBones : renderRoots;
    }

    public boolean hasCustomLegs() {
        return customLegsPresent;
    }

    public boolean isBowReplacementAccessory() {
        return bowReplacementAccessory;
    }

    public boolean isSwordReplacementAccessory() {
        return swordReplacementAccessory;
    }

    public boolean isBodyOnlyAccessory() {
        return type == CosmeticType.ACCESSORY && !bowReplacementAccessory && !swordReplacementAccessory;
    }

    public boolean hasRestPoseAnimation() {
        return findBbAnimation(
                "idling", "idle", "idle_arms", "idlemonsbob", "defaultstate",
                "walking", "walkingback", "flywalk"
        ) != null;
    }

    public boolean shouldSkipBone(CosmeticBone bone) {
        if (visibleBoneUuids != null) {
            return !visibleBoneUuids.contains(bone.getUuid());
        }
        String lower = bone.getName().toLowerCase();
        if (customLegsPresent && lower.contains("vanilla") && (lower.contains("leg") || lower.contains("foot"))) {
            return true;
        }
        if (lower.contains("rightarm_fp") || lower.contains("leftarm_fp")) {
            return true;
        }
        if (hatOnlyAccessory) {
            return lower.equals("body") || lower.equals("leftarm") || lower.equals("rightarm")
                    || lower.equals("leftleg") || lower.equals("rightleg") || lower.equals("root");
        }
        return false;
    }

    /**
     * Derives a standalone pet cosmetic from a single named bone subtree of a parent model
     * (e.g. turning a model's "head" bone into its own pet cosmetic).
     */
    public static CosmeticModel derivePet(CosmeticModel parent, String boneName, String petFolder, String petName) {
        if (parent == null) return null;
        CosmeticBone source = parent.findBoneDeep(boneName);
        if (source == null) return null;

        CosmeticModel pet = new CosmeticModel(
                "derived:" + petFolder.replace('/', '_'),
                petName,
                "",
                parent.getAccentColor(),
                petFolder,
                CosmeticType.PET,
                null,
                null
        );
        pet.source = parent.source;
        pet.textureWidth = parent.textureWidth;
        pet.textureHeight = parent.textureHeight;
        pet.textureId = parent.textureId;
        pet.textureSlots.addAll(parent.textureSlots);
        pet.textureLoaded = parent.textureLoaded;

        CosmeticBone root = source.deepClone();
        pet.rootBones.add(root);
        pet.renderRoots.add(root);
        pet.indexBone(root);

        Set<String> uuids = new HashSet<>();
        pet.collectUuids(root, uuids);
        pet.animationBoneFilter = uuids;

        for (Map.Entry<String, CosmeticBbAnimation> entry : parent.bbAnimations.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.contains("mons") || key.contains("mini")) {
                pet.bbAnimations.put(entry.getKey(), entry.getValue());
            }
        }
        if (pet.bbAnimations.isEmpty()) {
            pet.bbAnimations.putAll(parent.bbAnimations);
        }
        pet.postProcess();
        return pet;
    }

    private void collectUuids(CosmeticBone bone, Set<String> uuids) {
        uuids.add(bone.getUuid());
        for (CosmeticBone child : bone.getChildren()) collectUuids(child, uuids);
    }

    private CosmeticBone findBoneDeep(String name) {
        String lower = name.toLowerCase();
        for (CosmeticBone root : rootBones) {
            CosmeticBone found = findBoneDeep(root, lower);
            if (found != null) return found;
        }
        return null;
    }

    private static CosmeticBone findBoneDeep(CosmeticBone bone, String lower) {
        if (bone.getName().toLowerCase().equals(lower) || bone.getName().toLowerCase().contains(lower)) {
            return bone;
        }
        for (CosmeticBone child : bone.getChildren()) {
            CosmeticBone found = findBoneDeep(child, lower);
            if (found != null) return found;
        }
        return null;
    }

    private void postProcess() {
        customLegsPresent = boneMap.keySet().stream().anyMatch(n ->
                n.contains("leftleg") || n.contains("rightleg") || n.contains("leftpaw") || n.contains("rightpaw"))
                && boneMap.keySet().stream().noneMatch(n -> n.equals("leftlegvanilla") || n.equals("rightlegvanilla")
                || (n.contains("vanilla") && n.contains("leg")));

        String folder = folderName.toLowerCase();
        bowReplacementAccessory = type == CosmeticType.ACCESSORY && folder.contains("bow");
        swordReplacementAccessory = false;
        hatOnlyAccessory = type == CosmeticType.ACCESSORY
                && (folder.contains("witch") || folder.contains("hat")) && !bowReplacementAccessory;
        wingsOnlyAccessory = type == CosmeticType.ACCESSORY && folder.contains("wing");
        visibleBoneUuids = null;
        if (wingsOnlyAccessory) {
            visibleBoneUuids = buildWingsBoneChainUuids();
        }

        renderRoots.clear();
        for (CosmeticBone root : rootBones) {
            String n = root.getName().toLowerCase();
            if (type == CosmeticType.MODEL) {
                if (n.equals("monst3r") || n.equals("skull") || n.endsWith("_fp")) continue;
            }
            if (type == CosmeticType.PET) {
                if (n.equals("arrow") || n.equals("skull")) continue;
            }
            renderRoots.add(root);
        }
        if (renderRoots.isEmpty()) {
            renderRoots.addAll(rootBones);
        }

        applyAccessorySubtreeFilter();
    }

    private void applyAccessorySubtreeFilter() {
        if (hatOnlyAccessory) {
            CosmeticBone head = findBoneDeep("head");
            CosmeticBone hatBone = findBoneDeep("bone");
            if (head != null && hatBone != null) {
                CosmeticBone mount = new CosmeticBone(
                        head.getName(), head.getUuid(),
                        new Vector3f(head.getPivot()), new Vector3f(head.getDefaultRotation()));
                mount.getChildren().add(hatBone.deepClone());
                renderRoots.clear();
                renderRoots.add(mount);
            }
        }
    }

    private Set<String> buildWingsBoneChainUuids() {
        Set<String> keep = new HashSet<>();
        CosmeticBone wings = findBoneDeep("wings");
        if (wings == null) return keep;
        collectBoneTreeUuids(wings, keep);
        for (CosmeticBone root : rootBones) {
            markBonePathToUuid(root, wings.getUuid(), keep);
        }
        return keep;
    }

    private static void collectBoneTreeUuids(CosmeticBone bone, Set<String> uuids) {
        uuids.add(bone.getUuid());
        for (CosmeticBone child : bone.getChildren()) {
            collectBoneTreeUuids(child, uuids);
        }
    }

    private static boolean markBonePathToUuid(CosmeticBone bone, String targetUuid, Set<String> keep) {
        keep.add(bone.getUuid());
        if (bone.getUuid().equals(targetUuid)) {
            return true;
        }
        for (CosmeticBone child : bone.getChildren()) {
            if (markBonePathToUuid(child, targetUuid, keep)) {
                return true;
            }
        }
        keep.remove(bone.getUuid());
        return false;
    }

    private static Map<String, JsonObject> buildGroupMap(JsonObject bbmodel) {
        Map<String, JsonObject> map = new HashMap<>();
        if (!bbmodel.has("groups")) return map;
        for (JsonElement el : bbmodel.getAsJsonArray("groups")) {
            if (!el.isJsonObject()) continue;
            JsonObject group = el.getAsJsonObject();
            if (group.has("uuid")) {
                map.put(group.get("uuid").getAsString(), group);
            }
        }
        return map;
    }

    private static Vector3f readVec3(JsonObject obj, String key, Vector3f fallback) {
        if (obj == null || !obj.has(key)) return new Vector3f(fallback);
        JsonArray arr = obj.getAsJsonArray(key);
        return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
    }

    private static CosmeticBone parseOutliner(JsonElement element, Map<String, JsonObject> elementMap,
                                              Map<String, JsonObject> groupMap, int texW, int texH) {
        if (element.isJsonPrimitive() || !element.isJsonObject()) return null;
        JsonObject obj = element.getAsJsonObject();

        String uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : UUID.randomUUID().toString();
        JsonObject group = groupMap.get(uuid);

        String name = "bone";
        if (obj.has("name")) {
            name = obj.get("name").getAsString();
        } else if (group != null && group.has("name")) {
            name = group.get("name").getAsString();
        }

        Vector3f pivot = obj.has("origin")
                ? readVec3(obj, "origin", new Vector3f())
                : readVec3(group, "origin", new Vector3f());

        Vector3f rotation = obj.has("rotation")
                ? readVec3(obj, "rotation", new Vector3f())
                : readVec3(group, "rotation", new Vector3f());

        CosmeticBone bone = new CosmeticBone(name, uuid, pivot, rotation);

        if (obj.has("children")) {
            for (JsonElement child : obj.getAsJsonArray("children")) {
                if (child.isJsonPrimitive()) {
                    String childUuid = child.getAsString();
                    JsonObject cubeObj = elementMap.get(childUuid);
                    if (cubeObj != null) {
                        CosmeticCube cube = parseCube(cubeObj, texW, texH);
                        if (cube != null) bone.getCubes().add(cube);
                    }
                } else {
                    CosmeticBone childBone = parseOutliner(child, elementMap, groupMap, texW, texH);
                    if (childBone != null) bone.getChildren().add(childBone);
                }
            }
        }

        return bone;
    }

    private static CosmeticCube parseCube(JsonObject obj, int texW, int texH) {
        try {
            JsonArray fromArr = obj.getAsJsonArray("from");
            JsonArray toArr = obj.getAsJsonArray("to");
            Vector3f from = new Vector3f(fromArr.get(0).getAsFloat(), fromArr.get(1).getAsFloat(), fromArr.get(2).getAsFloat());
            Vector3f to = new Vector3f(toArr.get(0).getAsFloat(), toArr.get(1).getAsFloat(), toArr.get(2).getAsFloat());

            Vector3f rot = new Vector3f(0, 0, 0);
            if (obj.has("rotation")) {
                JsonArray r = obj.getAsJsonArray("rotation");
                rot.set(r.get(0).getAsFloat(), r.get(1).getAsFloat(), r.get(2).getAsFloat());
            }

            Vector3f origin = new Vector3f(0, 0, 0);
            if (obj.has("origin")) {
                JsonArray o = obj.getAsJsonArray("origin");
                origin.set(o.get(0).getAsFloat(), o.get(1).getAsFloat(), o.get(2).getAsFloat());
            }

            float inflate = obj.has("inflate") ? obj.get("inflate").getAsFloat() : 0f;

            CosmeticCube.CubeFace[] faces = new CosmeticCube.CubeFace[6];
            String[] faceNames = {"north", "east", "south", "west", "up", "down"};

            if (obj.has("faces")) {
                JsonObject facesObj = obj.getAsJsonObject("faces");
                for (int i = 0; i < faceNames.length; i++) {
                    String fn = faceNames[i];
                    if (!facesObj.has(fn)) continue;
                    JsonObject face = facesObj.getAsJsonObject(fn);
                    if (!face.has("uv") || !face.get("uv").isJsonArray()) continue;
                    JsonArray uv = face.getAsJsonArray("uv");
                    if (uv.size() < 4) continue;
                    int tex = 0;
                    if (face.has("texture") && !face.get("texture").isJsonNull()) {
                        JsonElement texEl = face.get("texture");
                        if (texEl.isJsonPrimitive() && texEl.getAsJsonPrimitive().isNumber()) {
                            tex = texEl.getAsInt();
                        }
                    }
                    float u1 = uv.get(0).getAsFloat();
                    float v1 = uv.get(1).getAsFloat();
                    float u2 = uv.get(2).getAsFloat();
                    float v2 = uv.get(3).getAsFloat();
                    if (u2 < u1) {
                        float t = u1;
                        u1 = u2;
                        u2 = t;
                    }
                    if (v2 < v1) {
                        float t = v1;
                        v1 = v2;
                        v2 = t;
                    }
                    faces[i] = new CosmeticCube.CubeFace(u1, v1, u2, v2, tex);
                }
            }

            return new CosmeticCube(from, to, rot, origin, inflate, faces, texW, texH);
        } catch (Exception e) {
            return null;
        }
    }
}
