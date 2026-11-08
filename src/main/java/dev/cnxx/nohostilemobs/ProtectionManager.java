package dev.cnxx.nohostilemobs;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ProtectionManager {
    private static final Map<UUID, ProtectionSettings> SETTINGS = new ConcurrentHashMap<>();

    private ProtectionManager() {}

    public static void set(UUID player, ProtectionSettings settings) {
        SETTINGS.put(player, settings);
    }

    public static ProtectionSettings getActive(UUID player) {
        ProtectionSettings s = SETTINGS.get(player);
        return s != null && s.enabled() ? s : null;
    }

    public static boolean hasAnyActive() {
        for (ProtectionSettings s : SETTINGS.values()) {
            if (s.enabled()) return true;
        }
        return false;
    }

    public static void remove(UUID player) {
        SETTINGS.remove(player);
    }

    public static void clear() {
        SETTINGS.clear();
    }
}
