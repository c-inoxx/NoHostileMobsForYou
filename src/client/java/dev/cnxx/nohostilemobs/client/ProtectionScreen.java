package dev.cnxx.nohostilemobs.client;

import dev.cnxx.nohostilemobs.Behavior;
import dev.cnxx.nohostilemobs.ProtectionSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public class ProtectionScreen extends Screen {
    private static final int WIDTH = 210;
    private static final int ROW = 24;

    private final Screen parent;
    private final ClientConfig config = NoHostileMobsClient.config();

    public ProtectionScreen(Screen parent) {
        super(Component.translatable("nohostilemobs.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = width / 2 - WIDTH / 2;
        int y = Math.max(36, height / 2 - ROW * 5);

        addRenderableWidget(toggle("enabled", () -> config.enabled, v -> config.enabled = v, x, y, WIDTH));
        y += ROW;

        addRenderableWidget(Button.builder(behaviorLabel(), b -> {
            config.behavior = config.behavior.next();
            rebuildWidgets(); // the strength slider is only active for REPEL
        }).bounds(x, y, WIDTH, 20).build());
        y += ROW;

        addRenderableWidget(new RangeSlider(x, y, WIDTH,
                ProtectionSettings.MIN_RADIUS, ProtectionSettings.MAX_RADIUS, 1, config.radius,
                v -> config.radius = (float) v,
                v -> Component.translatable("nohostilemobs.option.radius", (int) Math.round(v))));
        y += ROW;

        RangeSlider strength = new RangeSlider(x, y, WIDTH,
                ProtectionSettings.MIN_STRENGTH, ProtectionSettings.MAX_STRENGTH, 0.1, config.strength,
                v -> config.strength = (float) v,
                v -> Component.translatable("nohostilemobs.option.strength", String.format("%.1f", v)));
        strength.active = config.behavior == Behavior.REPEL;
        addRenderableWidget(strength);
        y += ROW;

        addRenderableWidget(toggle("block_spawns", () -> config.blockSpawns,
                v -> config.blockSpawns = v, x, y, WIDTH));
        y += ROW;
        addRenderableWidget(toggle("projectiles", () -> config.removeProjectiles,
                v -> config.removeProjectiles = v, x, y, WIDTH));
        y += ROW;

        int half = (WIDTH - 4) / 2;
        addRenderableWidget(toggle("show_ring", () -> config.showRing, v -> config.showRing = v, x, y, half));
        addRenderableWidget(toggle("show_hud", () -> config.showHud, v -> config.showHud = v,
                x + half + 4, y, half));
        y += ROW;

        Identifier particleId = Identifier.tryParse(config.ringParticle);
        Component particleName = particleId != null ? RingParticles.displayName(particleId)
                : Component.literal(config.ringParticle);
        addRenderableWidget(Button.builder(
                Component.translatable("nohostilemobs.option.particle", particleName),
                b -> minecraft.gui.setScreen(new ParticleScreen(this))
        ).bounds(x, y, half, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("nohostilemobs.option.excluded", config.excluded.size()),
                b -> minecraft.gui.setScreen(new ExclusionScreen(this))
        ).bounds(x + half + 4, y, half, 20).build());
        y += ROW + 8;

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(x, y, WIDTH, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
        if (minecraft.player != null && !NoHostileMobsClient.isAvailable()) {
            graphics.centeredText(font, Component.translatable("nohostilemobs.message.unavailable_short")
                    .withStyle(ChatFormatting.GOLD), width / 2, 24, 0xFFFFFFFF);
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        NoHostileMobsClient.saveAndSync();
    }

    private Component behaviorLabel() {
        return Component.translatable("nohostilemobs.option.behavior",
                Component.translatable(config.behavior.translationKey()));
    }

    private static Button toggle(String key, BooleanSupplier getter, Consumer<Boolean> setter, int x, int y, int w) {
        return Button.builder(toggleLabel(key, getter.getAsBoolean()), b -> {
            setter.accept(!getter.getAsBoolean());
            b.setMessage(toggleLabel(key, getter.getAsBoolean()));
        }).bounds(x, y, w, 20).build();
    }

    private static Component toggleLabel(String key, boolean value) {
        return Component.translatable("nohostilemobs.option." + key,
                value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    private static final class RangeSlider extends AbstractSliderButton {
        private final double min, max, step;
        private final DoubleConsumer onChange;
        private final DoubleFunction<Component> label;

        RangeSlider(int x, int y, int w, double min, double max, double step, double current,
                    DoubleConsumer onChange, DoubleFunction<Component> label) {
            super(x, y, w, 20, Component.empty(), (Mth.clamp(current, min, max) - min) / (max - min));
            this.min = min;
            this.max = max;
            this.step = step;
            this.onChange = onChange;
            this.label = label;
            updateMessage();
        }

        private double actual() {
            double raw = min + value * (max - min);
            return Mth.clamp(Math.round(raw / step) * step, min, max);
        }

        @Override
        protected void updateMessage() {
            setMessage(label.apply(actual()));
        }

        @Override
        protected void applyValue() {
            onChange.accept(actual());
        }
    }
}
