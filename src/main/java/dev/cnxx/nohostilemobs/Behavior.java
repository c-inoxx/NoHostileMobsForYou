package dev.cnxx.nohostilemobs;

import java.util.Locale;

public enum Behavior {
    REPEL,
    REMOVE;

    public String translationKey() {
        return "nohostilemobs.behavior." + name().toLowerCase(Locale.ROOT);
    }

    public Behavior next() {
        Behavior[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
