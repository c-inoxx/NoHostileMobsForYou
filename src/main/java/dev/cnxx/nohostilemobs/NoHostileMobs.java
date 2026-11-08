package dev.cnxx.nohostilemobs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoHostileMobs implements ModInitializer {
    public static final String MOD_ID = "nohostilemobs";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(SettingsPayload.TYPE, SettingsPayload.CODEC);

        // Fabric calls this on the server thread.
        ServerPlayNetworking.registerGlobalReceiver(SettingsPayload.TYPE, (payload, context) ->
                ProtectionManager.set(context.player().getUUID(), payload.settings().sanitized()));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ProtectionManager.remove(handler.getPlayer().getUUID()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ProtectionManager.clear());

        ServerTickEvents.END_LEVEL_TICK.register(ProtectionLogic::tickLevel);

        LOGGER.info("No Hostile Mobs For You loaded");
    }
}
