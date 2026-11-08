package dev.cnxx.nohostilemobs.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class ParticleScreen extends GridScreen<Identifier> {
    private final ClientConfig config = NoHostileMobsClient.config();

    public ParticleScreen(Screen parent) {
        super(Component.translatable("nohostilemobs.screen.particle.title"), parent);
    }

    @Override
    protected List<Identifier> createEntries() {
        return RingParticles.selectable();
    }

    @Override
    protected Component label(Identifier id) {
        boolean selected = id.toString().equals(config.ringParticle);
        return selected
                ? Component.literal("● ").append(RingParticles.displayName(id)).withStyle(ChatFormatting.GREEN)
                : RingParticles.displayName(id);
    }

    @Override
    protected void onSelect(Identifier id) {
        config.ringParticle = id.toString();
    }

    @Override
    protected Component hint() {
        return Component.translatable("nohostilemobs.screen.particle.hint");
    }
}
