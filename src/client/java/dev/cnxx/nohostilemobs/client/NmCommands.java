package dev.cnxx.nohostilemobs.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.cnxx.nohostilemobs.Behavior;
import dev.cnxx.nohostilemobs.ProtectionSettings;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

final class NmCommands {
    private NmCommands() {}

    static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        ClientConfig config = NoHostileMobsClient.config();

        dispatcher.register(literal("nm")
                .executes(NmCommands::status)
                .then(literal("on").executes(ctx -> setEnabled(ctx, true)))
                .then(literal("off").executes(ctx -> setEnabled(ctx, false)))
                .then(literal("toggle").executes(ctx -> setEnabled(ctx, !config.enabled)))
                .then(behavior())
                .then(literal("radius").then(argument("blocks",
                        FloatArgumentType.floatArg(ProtectionSettings.MIN_RADIUS, ProtectionSettings.MAX_RADIUS))
                        .executes(ctx -> {
                            config.radius = Math.round(FloatArgumentType.getFloat(ctx, "blocks"));
                            return changed(ctx, Component.translatable("nohostilemobs.option.radius",
                                    Math.round(config.radius)));
                        })))
                .then(literal("strength").then(argument("value",
                        FloatArgumentType.floatArg(ProtectionSettings.MIN_STRENGTH, ProtectionSettings.MAX_STRENGTH))
                        .executes(ctx -> {
                            config.strength = Math.round(FloatArgumentType.getFloat(ctx, "value") * 10) / 10f;
                            return changed(ctx, Component.translatable("nohostilemobs.option.strength",
                                    String.format(Locale.ROOT, "%.1f", config.strength)));
                        })))
                .then(onOff("spawns", "block_spawns", () -> config.blockSpawns, v -> config.blockSpawns = v))
                .then(onOff("projectiles", "projectiles", () -> config.removeProjectiles,
                        v -> config.removeProjectiles = v))
                .then(onOff("ring", "show_ring", () -> config.showRing, v -> config.showRing = v))
                .then(onOff("hud", "show_hud", () -> config.showHud, v -> config.showHud = v))
                .then(literal("particle").then(argument("particle", IdentifierArgument.id())
                        .suggests((ctx, builder) ->
                                SharedSuggestionProvider.suggestResource(RingParticles.selectable(), builder))
                        .executes(NmCommands::setParticle)))
                .then(literal("exclude").then(argument("mob", IdentifierArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                ExclusionScreen.hostileTypes().stream().map(EntityType::getKey)
                                        .filter(id -> !config.isExcluded(id)), builder))
                        .executes(ctx -> setExcluded(ctx, true))))
                .then(literal("include").then(argument("mob", IdentifierArgument.id())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                config.excluded.stream().map(Identifier::tryParse)
                                        .filter(Objects::nonNull), builder))
                        .executes(ctx -> setExcluded(ctx, false))))
                .then(literal("excluded").executes(NmCommands::listExcluded))
                .then(literal("settings").executes(ctx -> {
                    NoHostileMobsClient.openSettingsNextTick();
                    return Command.SINGLE_SUCCESS;
                })));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> behavior() {
        LiteralArgumentBuilder<FabricClientCommandSource> node = literal("behavior");
        for (Behavior b : Behavior.values()) {
            node.then(literal(b.name().toLowerCase(Locale.ROOT)).executes(ctx -> {
                NoHostileMobsClient.config().behavior = b;
                return changed(ctx, Component.translatable("nohostilemobs.option.behavior",
                        Component.translatable(b.translationKey())));
            }));
        }
        return node;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> onOff(
            String name, String optionKey, BooleanSupplier getter, Consumer<Boolean> setter) {
        return literal(name)
                .then(literal("on").executes(ctx -> { setter.accept(true); return changed(ctx, toggleLabel(optionKey, true)); }))
                .then(literal("off").executes(ctx -> { setter.accept(false); return changed(ctx, toggleLabel(optionKey, false)); }))
                .executes(ctx -> {
                    feedback(ctx, toggleLabel(optionKey, getter.getAsBoolean()));
                    return Command.SINGLE_SUCCESS;
                });
    }

    private static int status(CommandContext<FabricClientCommandSource> ctx) {
        ClientConfig c = NoHostileMobsClient.config();
        Identifier particleId = Identifier.tryParse(c.ringParticle);
        feedback(ctx, Component.translatable("nohostilemobs.command.status.header").withStyle(ChatFormatting.BOLD));
        line(ctx, NoHostileMobsClient.statusMessage());
        line(ctx, Component.translatable("nohostilemobs.option.behavior", Component.translatable(c.behavior.translationKey())));
        line(ctx, Component.translatable("nohostilemobs.option.radius", Math.round(c.radius)));
        line(ctx, Component.translatable("nohostilemobs.option.strength", String.format(Locale.ROOT, "%.1f", c.strength)));
        line(ctx, toggleLabel("block_spawns", c.blockSpawns));
        line(ctx, toggleLabel("projectiles", c.removeProjectiles));
        line(ctx, toggleLabel("show_ring", c.showRing));
        line(ctx, Component.translatable("nohostilemobs.option.particle",
                particleId != null ? RingParticles.displayName(particleId) : Component.literal(c.ringParticle)));
        line(ctx, toggleLabel("show_hud", c.showHud));
        line(ctx, Component.translatable("nohostilemobs.option.excluded", c.excluded.size()));
        return Command.SINGLE_SUCCESS;
    }

    private static int setEnabled(CommandContext<FabricClientCommandSource> ctx, boolean enabled) {
        NoHostileMobsClient.config().enabled = enabled;
        return changed(ctx, NoHostileMobsClient.statusMessage());
    }

    private static int setParticle(CommandContext<FabricClientCommandSource> ctx) {
        Identifier id = ctx.getArgument("particle", Identifier.class);
        if (!RingParticles.selectable().contains(id)) {
            ctx.getSource().sendError(Component.translatable("nohostilemobs.command.error.particle", id.toString()));
            return 0;
        }
        NoHostileMobsClient.config().ringParticle = id.toString();
        return changed(ctx, Component.translatable("nohostilemobs.option.particle", RingParticles.displayName(id)));
    }

    private static int setExcluded(CommandContext<FabricClientCommandSource> ctx, boolean exclude) {
        ClientConfig config = NoHostileMobsClient.config();
        Identifier id = ctx.getArgument("mob", Identifier.class);
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) {
            ctx.getSource().sendError(Component.translatable("nohostilemobs.command.error.mob", id.toString()));
            return 0;
        }
        Component name = type.getDescription();
        if (exclude == config.isExcluded(id)) {
            ctx.getSource().sendError(Component.translatable(exclude
                    ? "nohostilemobs.command.error.already_excluded"
                    : "nohostilemobs.command.error.not_excluded", name));
            return 0;
        }
        config.toggleExcluded(id);
        return changed(ctx, Component.translatable(exclude
                ? "nohostilemobs.command.excluded" : "nohostilemobs.command.included", name));
    }

    private static int listExcluded(CommandContext<FabricClientCommandSource> ctx) {
        ClientConfig config = NoHostileMobsClient.config();
        List<Component> names = config.excluded.stream()
                .map(Identifier::tryParse)
                .filter(Objects::nonNull)
                .map(id -> BuiltInRegistries.ENTITY_TYPE.getOptional(id)
                        .<Component>map(EntityType::getDescription)
                        .orElse(Component.literal(id.toString())))
                .sorted(Comparator.comparing(Component::getString))
                .toList();
        feedback(ctx, Component.translatable("nohostilemobs.option.excluded", names.size()).withStyle(ChatFormatting.BOLD));
        if (names.isEmpty()) {
            line(ctx, Component.translatable("nohostilemobs.command.excluded.none"));
        } else {
            names.forEach(n -> line(ctx, n));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int changed(CommandContext<FabricClientCommandSource> ctx, Component message) {
        NoHostileMobsClient.saveAndSync();
        feedback(ctx, message);
        return Command.SINGLE_SUCCESS;
    }

    private static void feedback(CommandContext<FabricClientCommandSource> ctx, Component message) {
        ctx.getSource().sendFeedback(prefix().append(message));
    }

    private static void line(CommandContext<FabricClientCommandSource> ctx, Component message) {
        ctx.getSource().sendFeedback(Component.literal("  ").append(message));
    }

    private static MutableComponent prefix() {
        return Component.literal("[NM] ").withStyle(ChatFormatting.DARK_AQUA);
    }

    private static Component toggleLabel(String optionKey, boolean value) {
        return Component.translatable("nohostilemobs.option." + optionKey,
                value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }
}
