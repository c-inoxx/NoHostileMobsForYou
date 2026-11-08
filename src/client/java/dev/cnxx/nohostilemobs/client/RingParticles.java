package dev.cnxx.nohostilemobs.client;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class RingParticles {
    static final String DEFAULT_ID = "minecraft:wax_on";

    // Huge effects, screen overlays or loud sounds when spawned 100 times.
    private static final Set<Identifier> BLOCKED = Set.of(
            Identifier.withDefaultNamespace("elder_guardian"),
            Identifier.withDefaultNamespace("explosion"),
            Identifier.withDefaultNamespace("explosion_emitter"),
            Identifier.withDefaultNamespace("gust_emitter_large"),
            Identifier.withDefaultNamespace("gust_emitter_small"),
            Identifier.withDefaultNamespace("sonic_boom")
    );

    private RingParticles() {}

    static List<Identifier> selectable() {
        List<Identifier> ids = new ArrayList<>();
        for (ParticleType<?> type : BuiltInRegistries.PARTICLE_TYPE) {
            Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(type);
            if (type instanceof SimpleParticleType && id != null && !BLOCKED.contains(id)) ids.add(id);
        }
        ids.sort(Comparator.comparing(id -> displayName(id).getString()));
        return ids;
    }

    static SimpleParticleType resolve(String configured) {
        Identifier id = configured == null ? null : Identifier.tryParse(configured);
        if (id != null && !BLOCKED.contains(id)) {
            ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.getOptional(id).orElse(null);
            if (type instanceof SimpleParticleType simple) return simple;
        }
        return ParticleTypes.WAX_ON;
    }

    // Minecraft has no particle names, so they come from our lang files.
    static Component displayName(Identifier id) {
        String key = "nohostilemobs.particle." + id.getNamespace() + "." + id.getPath();
        return Component.translatableWithFallback(key, prettify(id.getPath()));
    }

    private static String prettify(String path) {
        return Arrays.stream(path.split("_"))
                .filter(s -> !s.isEmpty())
                .map(s -> s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1))
                .collect(Collectors.joining(" "));
    }
}
