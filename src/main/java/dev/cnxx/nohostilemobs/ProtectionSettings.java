package dev.cnxx.nohostilemobs;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.Set;

/** Comes from the client, so the server always calls {@link #sanitized()} before using it. */
public record ProtectionSettings(
        boolean enabled,
        Behavior behavior,
        float radius,
        float strength,
        boolean blockSpawns,
        boolean removeProjectiles,
        Set<Identifier> excluded
) {
    public static final float MIN_RADIUS = 2f;
    public static final float MAX_RADIUS = 32f;
    public static final float DEFAULT_RADIUS = 8f;

    public static final float MIN_STRENGTH = 0.2f;
    public static final float MAX_STRENGTH = 3f;
    public static final float DEFAULT_STRENGTH = 1f;

    // Keeps clients from sending huge packets.
    public static final int MAX_EXCLUDED = 512;

    // Removing bosses can break their fights.
    public static final Set<Identifier> DEFAULT_EXCLUDED = Set.of(
            Identifier.withDefaultNamespace("ender_dragon"),
            Identifier.withDefaultNamespace("wither"),
            Identifier.withDefaultNamespace("warden"),
            Identifier.withDefaultNamespace("elder_guardian")
    );

    public ProtectionSettings sanitized() {
        float r = Float.isFinite(radius) ? Mth.clamp(radius, MIN_RADIUS, MAX_RADIUS) : DEFAULT_RADIUS;
        float s = Float.isFinite(strength) ? Mth.clamp(strength, MIN_STRENGTH, MAX_STRENGTH) : DEFAULT_STRENGTH;
        Set<Identifier> ex = excluded == null ? Set.of()
                : Set.copyOf(excluded.stream().limit(MAX_EXCLUDED).toList());
        return new ProtectionSettings(enabled, behavior == null ? Behavior.REPEL : behavior,
                r, s, blockSpawns, removeProjectiles, ex);
    }
}
