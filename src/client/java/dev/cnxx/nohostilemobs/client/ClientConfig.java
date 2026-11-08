package dev.cnxx.nohostilemobs.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.cnxx.nohostilemobs.Behavior;
import dev.cnxx.nohostilemobs.NoHostileMobs;
import dev.cnxx.nohostilemobs.ProtectionSettings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class ClientConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("nohostilemobs.json");

    public boolean enabled = false;
    public Behavior behavior = Behavior.REPEL;
    public float radius = ProtectionSettings.DEFAULT_RADIUS;
    public float strength = ProtectionSettings.DEFAULT_STRENGTH;
    public boolean blockSpawns = true;
    public boolean removeProjectiles = true;
    public Set<String> excluded = defaultExcluded();

    public boolean showRing = true;
    public String ringParticle = RingParticles.DEFAULT_ID;
    public boolean showHud = true;

    public static Set<String> defaultExcluded() {
        Set<String> ids = new TreeSet<>();
        ProtectionSettings.DEFAULT_EXCLUDED.forEach(id -> ids.add(id.toString()));
        return ids;
    }

    public static ClientConfig load() {
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                ClientConfig config = GSON.fromJson(reader, ClientConfig.class);
                if (config != null) return config.fixNulls();
            } catch (Exception e) {
                NoHostileMobs.LOGGER.warn("Could not read {}, using defaults", FILE, e);
                return new ClientConfig(); // keep the broken file until the user saves
            }
        }
        ClientConfig config = new ClientConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            NoHostileMobs.LOGGER.warn("Could not save {}", FILE, e);
        }
    }

    public boolean isExcluded(Identifier id) {
        return excluded.contains(id.toString());
    }

    public void toggleExcluded(Identifier id) {
        if (!excluded.remove(id.toString())) excluded.add(id.toString());
    }

    public ProtectionSettings toSettings() {
        Set<Identifier> ids = new LinkedHashSet<>();
        for (String s : excluded) {
            Identifier id = Identifier.tryParse(s);
            if (id != null) ids.add(id);
        }
        return new ProtectionSettings(enabled, behavior, radius, strength, blockSpawns, removeProjectiles, ids)
                .sanitized();
    }

    // Gson leaves fields null if they're missing from the file.
    private ClientConfig fixNulls() {
        behavior = Objects.requireNonNullElse(behavior, Behavior.REPEL);
        ringParticle = Objects.requireNonNullElse(ringParticle, RingParticles.DEFAULT_ID);
        if (excluded == null) {
            excluded = defaultExcluded();
        } else {
            Set<String> cleaned = new TreeSet<>();
            excluded.stream().filter(Objects::nonNull).forEach(cleaned::add);
            excluded = cleaned;
        }
        ProtectionSettings clamped = toSettings();
        radius = clamped.radius();
        strength = clamped.strength();
        return this;
    }
}
