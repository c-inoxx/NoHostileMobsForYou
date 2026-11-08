package dev.cnxx.nohostilemobs.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.cnxx.nohostilemobs.NoHostileMobs;
import dev.cnxx.nohostilemobs.SettingsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ServerboundPlayChannelEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;

public class NoHostileMobsClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(NoHostileMobs.id("main"));
    private static final int RING_INTERVAL_TICKS = 5;
    // The server's channel list can arrive after joining.
    private static final int NOTICE_DELAY_TICKS = 40;

    private static ClientConfig config;
    private static boolean available = false;
    private static int noticeCountdown = 0;
    private static boolean pendingSync = false;
    private static int tickCounter = 0;
    // The chat screen closes after a command runs, so /nm settings opens the screen a tick later.
    private static boolean openSettingsNextTick = false;

    private KeyMapping toggleKey;
    private KeyMapping settingsKey;

    @Override
    public void onInitializeClient() {
        config = ClientConfig.load();

        toggleKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.nohostilemobs.toggle", InputConstants.KEY_K, CATEGORY));
        settingsKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.nohostilemobs.settings", InputConstants.KEY_J, CATEGORY));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            available = ClientPlayNetworking.canSend(SettingsPayload.TYPE);
            if (available) {
                pendingSync = true;
            } else {
                noticeCountdown = NOTICE_DELAY_TICKS;
            }
        });
        // Can fire before JOIN, when sending isn't possible yet, so the tick does the sync.
        ServerboundPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (!available && channels.contains(SettingsPayload.TYPE.id())) {
                available = true;
                noticeCountdown = 0;
                pendingSync = true;
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            available = false;
            pendingSync = false;
            noticeCountdown = 0;
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> NmCommands.register(dispatcher));
        HudElementRegistry.addLast(NoHostileMobs.id("status"), new StatusHud());
    }

    public static ClientConfig config() {
        return config;
    }

    public static void openSettingsNextTick() {
        openSettingsNextTick = true;
    }

    public static boolean isAvailable() {
        return available;
    }

    public static void saveAndSync() {
        config.save();
        sync();
    }

    private static void sync() {
        if (available) {
            ClientPlayNetworking.send(new SettingsPayload(config.toSettings()));
        }
    }

    private void onTick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return;

        if (pendingSync) {
            pendingSync = false;
            sync();
        }

        if (noticeCountdown > 0 && --noticeCountdown == 0 && !available && config.enabled) {
            player.sendSystemMessage(Component.translatable("nohostilemobs.message.unavailable")
                    .withStyle(ChatFormatting.GOLD));
        }

        while (toggleKey.consumeClick()) {
            config.enabled = !config.enabled;
            saveAndSync();
            player.sendOverlayMessage(statusMessage());
        }
        while (settingsKey.consumeClick()) {
            client.gui.setScreen(new ProtectionScreen(null));
        }
        if (openSettingsNextTick) {
            openSettingsNextTick = false;
            client.gui.setScreen(new ProtectionScreen(null));
        }

        if (++tickCounter % RING_INTERVAL_TICKS == 0 && config.enabled && config.showRing && available) {
            drawRing(client, player);
        }
    }

    public static Component statusMessage() {
        if (!config.enabled) {
            return Component.translatable("nohostilemobs.message.off").withStyle(ChatFormatting.RED);
        }
        if (!available) {
            return Component.translatable("nohostilemobs.message.unavailable_short").withStyle(ChatFormatting.GOLD);
        }
        return Component.translatable("nohostilemobs.message.on",
                Component.translatable(config.behavior.translationKey()),
                Math.round(config.radius)).withStyle(ChatFormatting.GREEN);
    }

    private static void drawRing(Minecraft client, LocalPlayer player) {
        if (client.level == null) return;
        double r = config.radius;
        int points = Math.clamp(Math.round(r * 6), 24, 120);
        double y = player.getY() + 0.1;
        SimpleParticleType particle = RingParticles.resolve(config.ringParticle);
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2 * i / points;
            client.level.addParticle(particle,
                    player.getX() + r * Math.cos(angle), y, player.getZ() + r * Math.sin(angle), 0, 0, 0);
        }
    }
}
