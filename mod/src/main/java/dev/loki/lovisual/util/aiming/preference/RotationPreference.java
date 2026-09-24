package dev.loki.lovisual.util.aiming.preference;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.aiming.data.Rotation;

import java.util.Comparator;

public interface RotationPreference extends Comparator<Rotation> {

    Vec3 getPreferredSpotOnBox(AABB box, Vec3 eyes, double range);
}
