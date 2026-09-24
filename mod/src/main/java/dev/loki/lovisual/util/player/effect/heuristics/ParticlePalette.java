package dev.loki.lovisual.util.player.effect.heuristics;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry-backed effect/palette and nearest-color inference used by {@link StatusEffectHeuristics}
 * to guess a mob effect from a colored entity-effect particle. Holds the dynamic palette and its
 * precomputed/normalized colors plus the color-matching inference; palette-independent color math
 * and particle extraction live in {@link ParticleColors}.
 */
final class ParticlePalette {
    static final int COLOR_THRESHOLD = 42000;
    static final int PRIORITY_THRESHOLD = 120000; // allow rougher matches for hard-to-distinguish colors
    static final int AMBIGUITY_DELTA = 2000;
    static final double AMBIGUITY_RATIO = 1.05; // accept best if second-best is clearly worse

    // Dynamic palette of effects + colors from the registry to survive version bumps.
    private static final List<Holder<MobEffect>> PALETTE = buildPalette();
    private static final int[] COLORS = ParticleColors.computeColors(PALETTE);
    private static final int[] NORMALIZED_COLORS = ParticleColors.computeNormalizedColors(COLORS);
    private static final Set<Holder<MobEffect>> PRIORITY_EFFECTS = Set.of(
            MobEffects.STRENGTH,
            MobEffects.RESISTANCE
    );
    private static final Set<Holder<MobEffect>> FORCE_PICK_EFFECTS = Set.of(
            MobEffects.STRENGTH,
            MobEffects.RESISTANCE
    );

    private ParticlePalette() {
    }

    /**
     * Guesses the mob effect owning the nearest palette color to {@code color}, or {@code null} when
     * the color is empty, ambiguous beyond the acceptance ratio, out of the allowed threshold, or a
     * night-vision effect that must not be inferred heuristically.
     */
    static Holder<MobEffect> guess(int color) {
        if (color == 0) return null;
        // Normalize brightness so darker/lighter particles still map to the same hue bucket.
        int normalizedColor = ParticleColors.normalizeColor(color);
        Nearest nearest = nearest(normalizedColor, NORMALIZED_COLORS);
        boolean priority = isPriority(nearest.idx); // e.g. strength/resistance
        boolean ambiguous = nearest.secondDiff - nearest.diff < AMBIGUITY_DELTA;

        // Prefer fire resistance when it's one of the ambiguous pair so we don't mislabel it as strength.
        int chosenIdx = nearest.idx;
        int chosenDiff = nearest.diff;
        // Strength vs fire-resistance: both share orange/red; bias using channel dominance.
        if ((isStrength(nearest.idx) && isFire(nearest.secondIdx)) || (isStrength(nearest.secondIdx) && isFire(nearest.idx))) {
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            // Strength tint is deep red; fire resistance skews orange (more green). Bias toward fire only when clearly orange.
            float hue = ParticleColors.hueDegrees(r, g, b);
            boolean looksFire = (g >= r * 0.8f) && hue >= 25f;
            if (looksFire) {
                chosenIdx = isFire(nearest.idx) ? nearest.idx : nearest.secondIdx;
                chosenDiff = isFire(nearest.idx) ? nearest.diff : nearest.secondDiff;
            } else {
                chosenIdx = isStrength(nearest.idx) ? nearest.idx : nearest.secondIdx;
                chosenDiff = isStrength(nearest.idx) ? nearest.diff : nearest.secondDiff;
            }
            priority = isPriority(chosenIdx);
            ambiguous = false; // resolved manually
        } else if (ambiguous && (isFire(nearest.idx) ^ isFire(nearest.secondIdx))) {
            chosenIdx = isFire(nearest.idx) ? nearest.idx : nearest.secondIdx;
            chosenDiff = isFire(nearest.idx) ? nearest.diff : nearest.secondDiff;
            priority = isPriority(chosenIdx);
            ambiguous = false;
        } else if (ambiguous && !priority && isPriority(nearest.secondIdx)) {
            chosenIdx = nearest.secondIdx;
            chosenDiff = nearest.secondDiff;
            priority = true;
            ambiguous = false; // force pick priority
        }

        // For priority effects (strength/resistance) allow even if ambiguous, with wide threshold.
        // Treat ambiguous matches as acceptable when the best is clearly closer than the runner-up (ratio check).
        if (ambiguous && !priority) {
            double ratio = nearest.secondDiff / (double) Math.max(1, nearest.diff);
            if (ratio < AMBIGUITY_RATIO) {
                return null;
            }
        }

        boolean allowed = priority
                ? chosenDiff < PRIORITY_THRESHOLD
                : chosenDiff < COLOR_THRESHOLD;

        if (chosenIdx < 0 || chosenIdx >= PALETTE.size() || !allowed) {
            return null;
        }
        Holder<MobEffect> guessed = PALETTE.get(chosenIdx);
        if (isNightVision(chosenIdx)) return null; // do not infer night vision heuristically

        return guessed;
    }

    private static boolean isPriority(int idx) {
        Holder<MobEffect> e = at(idx);
        if (e == null) return false;
        return PRIORITY_EFFECTS.contains(e);
    }

    private static boolean isFire(int idx) {
        Holder<MobEffect> e = at(idx);
        if (e == null) return false;
        return e.equals(MobEffects.FIRE_RESISTANCE);
    }

    private static boolean isStrength(int idx) {
        Holder<MobEffect> e = at(idx);
        if (e == null) return false;
        return e.equals(MobEffects.STRENGTH);
    }

    // If ambiguous, force-pick these instead of skipping
    private static boolean isForcePick(int idx) {
        Holder<MobEffect> e = at(idx);
        if (e == null) return false;
        return FORCE_PICK_EFFECTS.contains(e);
    }

    private static boolean isNightVision(int idx) {
        Holder<MobEffect> e = at(idx);
        if (e == null) return false;
        return e.equals(MobEffects.NIGHT_VISION);
    }

    private static Nearest nearest(int color, int[] palette) {
        int best = -1, second = -1;
        int bestDiff = Integer.MAX_VALUE;
        int secondDiff = Integer.MAX_VALUE;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        for (int i = 0; i < palette.length; i++) {
            int c = palette[i];
            int dr = ((c >> 16) & 0xFF) - r;
            int dg = ((c >> 8) & 0xFF) - g;
            int db = (c & 0xFF) - b;
            int diff = dr * dr + dg * dg + db * db;
            if (diff < bestDiff) {
                secondDiff = bestDiff;
                second = best;
                bestDiff = diff;
                best = i;
            } else if (diff < secondDiff) {
                secondDiff = diff;
                second = i;
            }
        }
        // require reasonably close match
        return new Nearest(best, bestDiff, second, secondDiff);
    }

    private static Holder<MobEffect> at(int idx) {
        if (idx < 0 || idx >= PALETTE.size()) return null;
        return PALETTE.get(idx);
    }

    private static List<Holder<MobEffect>> buildPalette() {
        try {
            return BuiltInRegistries.MOB_EFFECT.listElements().collect(Collectors.toCollection(ArrayList::new));
        } catch (Exception e) {
            // Fallback to static list to avoid nulls during very early load
            return Arrays.asList(
                    MobEffects.SPEED,
                    MobEffects.SLOWNESS,
                    MobEffects.HASTE,
                    MobEffects.MINING_FATIGUE,
                    MobEffects.STRENGTH,
                    MobEffects.JUMP_BOOST,
                    MobEffects.NAUSEA,
                    MobEffects.REGENERATION,
                    MobEffects.RESISTANCE,
                    MobEffects.FIRE_RESISTANCE,
                    MobEffects.WATER_BREATHING,
                    MobEffects.INVISIBILITY,
                    MobEffects.BLINDNESS,
                    MobEffects.NIGHT_VISION,
                    MobEffects.HUNGER,
                    MobEffects.WEAKNESS,
                    MobEffects.POISON,
                    MobEffects.SLOW_FALLING,
                    MobEffects.DOLPHINS_GRACE,
                    MobEffects.CONDUIT_POWER,
                    MobEffects.HERO_OF_THE_VILLAGE,
                    MobEffects.WITHER,
                    MobEffects.HEALTH_BOOST,
                    MobEffects.SATURATION,
                    MobEffects.LUCK,
                    MobEffects.UNLUCK,
                    MobEffects.GLOWING,
                    MobEffects.LEVITATION,
                    MobEffects.BAD_OMEN,
                    MobEffects.DARKNESS,
                    MobEffects.TRIAL_OMEN,
                    MobEffects.RAID_OMEN,
                    MobEffects.WIND_CHARGED,
                    MobEffects.WEAVING,
                    MobEffects.OOZING,
                    MobEffects.INFESTED,
                    MobEffects.BREATH_OF_THE_NAUTILUS
            );
        }
    }

    private record Nearest(int idx, int diff, int secondIdx, int secondDiff) {
    }
}
