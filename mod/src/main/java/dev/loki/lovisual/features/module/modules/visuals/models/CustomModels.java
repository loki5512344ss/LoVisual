package dev.loki.lovisual.features.module.modules.visuals.models;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.modules.visuals.models.mesh.ChickenMesh;
import dev.loki.lovisual.features.module.modules.visuals.models.mesh.RabbitModel;
import dev.loki.lovisual.features.module.modules.visuals.models.mesh.VerityMesh;
import dev.loki.lovisual.features.relations.category.CategoryService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws chosen players with a model of their own instead of their skin. Only this client sees it: nothing about
 * the players changes and nothing is sent anywhere. The choice is made per player at render-state extraction
 * (AvatarCustomModelMixin) and the replacement happens where the model is submitted (LivingEntityCustomModelMixin).
 */
@ModuleInfo(
        id = "custommodels",
        displayName = "Custom Models",
        aliases = {"custommodels", "models"},
        category = ModuleCategory.ACCESSORIES
)
public class CustomModels extends Module {
    public static final String CHICKEN = "Chicken";
    public static final String VERITY = "Verity";
    public static final String RABBIT = "Rabbit";

    private static final String[] FACES = {"Normal", "Talking", "Disturbing", "Disturbing talking",
            "Disturbing yelling", "Bad", "Very bad"};
    private static final String[] FACE_FILES = {"verity", "veritytalk", "veritydisturbing", "veritydisturbingtalk",
            "veritydisturbingyell", "veritybad", "verityverybad"};
    private static final int MAX_RABBITS = 128;

    private final ModeValue model = modeSetting("customModelsModel", "model", CHICKEN, CHICKEN, VERITY, RABBIT);
    private final BooleanValue self = bool("customModelsSelf", "self", true);
    private final BooleanValue friends = bool("customModelsFriends", "friends", false);
    private final BooleanValue others = bool("customModelsOthers", "others", false);
    private final BooleanValue hand = visibleWhen(bool("customModelsHand", "hand", true),
            () -> CHICKEN.equals(model.get()) && self.get());
    private final NumberValue<Float> size = visibleWhen(num("customModelsSize", "size", 1.0f, 0.3f, 3.0f),
            () -> VERITY.equals(model.get()));
    private final ModeValue face = visibleWhen(modeSetting("customModelsFace", "face", FACES[0], FACES),
            () -> VERITY.equals(model.get()));

    private final ChickenMesh chicken = new ChickenMesh();
    private final VerityMesh verity = new VerityMesh();
    private final Map<Integer, RabbitModel> rabbits = new HashMap<>();

    @Override
    public void onDisable() {
        rabbits.clear();
    }

    /** The model this player is to be drawn with, or null to leave them as they are. */
    public String modelFor(Avatar avatar) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || avatar == null || mc.player == null) return null;
        boolean wanted;
        if (avatar.getUUID().equals(mc.player.getUUID())) {
            wanted = self.get();
        } else if (avatar instanceof Player player && CategoryService.isFriend(player)) {
            wanted = friends.get();
        } else {
            wanted = others.get();
        }
        return wanted ? model.get() : null;
    }

    /** Whether the hand the player looks at is drawn as the model's arm rather than their own. */
    public boolean handFromModel() {
        return isEnabled() && CHICKEN.equals(model.get()) && self.get() && hand.get();
    }

    public void submitHand(PoseStack pose, SubmitNodeCollector collector, int light, boolean right) {
        chicken.submitArm(pose, collector, light, right);
    }

    /** Draws {@code custom} in place of the player model; false when the name is not one of ours. */
    public boolean submit(String custom, Model<?> vanilla, AvatarRenderState state, PoseStack pose,
                          SubmitNodeCollector collector, int light, int overlay, int color, int outlineColor) {
        if (RABBIT.equals(custom) && vanilla instanceof PlayerModel player) {
            if (rabbits.size() > MAX_RABBITS) rabbits.clear();
            rabbits.computeIfAbsent(state.id, id -> new RabbitModel())
                    .submit(player, state, pose, collector, light, overlay, color, outlineColor);
        } else if (VERITY.equals(custom)) {
            verity.submit(pose, collector, light, overlay, verityTexture(), size.get());
        } else if (CHICKEN.equals(custom)) {
            chicken.submit(state, pose, collector, light, overlay);
        } else {
            return false;
        }
        return true;
    }

    /** The face texture chosen for Verity. */
    Identifier verityTexture() {
        return Identifier.fromNamespaceAndPath("lovisual", "models/verity/" + faceFile(face.get()) + ".png");
    }

    static String faceFile(String faceName) {
        for (int i = 0; i < FACES.length; i++) {
            if (FACES[i].equals(faceName)) return FACE_FILES[i];
        }
        return FACE_FILES[0];
    }
}
