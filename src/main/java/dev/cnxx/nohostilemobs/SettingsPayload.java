package dev.cnxx.nohostilemobs;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

public record SettingsPayload(ProtectionSettings settings) implements CustomPacketPayload {
    public static final Type<SettingsPayload> TYPE = new Type<>(NoHostileMobs.id("settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> CODEC =
            CustomPacketPayload.codec(SettingsPayload::write, SettingsPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        ProtectionSettings s = settings;
        buf.writeBoolean(s.enabled());
        buf.writeVarInt(s.behavior().ordinal());
        buf.writeFloat(s.radius());
        buf.writeFloat(s.strength());
        buf.writeBoolean(s.blockSpawns());
        buf.writeBoolean(s.removeProjectiles());
        int count = Math.min(s.excluded().size(), ProtectionSettings.MAX_EXCLUDED);
        buf.writeVarInt(count);
        s.excluded().stream().limit(count).forEach(buf::writeIdentifier);
    }

    private static SettingsPayload read(RegistryFriendlyByteBuf buf) {
        boolean enabled = buf.readBoolean();
        int behaviorId = buf.readVarInt();
        Behavior[] behaviors = Behavior.values();
        if (behaviorId < 0 || behaviorId >= behaviors.length) {
            throw new DecoderException("Unknown behavior: " + behaviorId);
        }
        Behavior behavior = behaviors[behaviorId];
        float radius = buf.readFloat();
        float strength = buf.readFloat();
        boolean blockSpawns = buf.readBoolean();
        boolean removeProjectiles = buf.readBoolean();
        int count = buf.readVarInt();
        if (count < 0 || count > ProtectionSettings.MAX_EXCLUDED) {
            throw new DecoderException("Too many excluded mobs: " + count);
        }
        Set<Identifier> excluded = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            excluded.add(buf.readIdentifier());
        }
        return new SettingsPayload(new ProtectionSettings(enabled, behavior, radius, strength,
                blockSpawns, removeProjectiles, excluded));
    }

    @Override
    public Type<SettingsPayload> type() {
        return TYPE;
    }
}
