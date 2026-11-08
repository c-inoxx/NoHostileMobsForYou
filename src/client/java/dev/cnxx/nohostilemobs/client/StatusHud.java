package dev.cnxx.nohostilemobs.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class StatusHud implements HudElement {
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !NoHostileMobsClient.config().showHud) return;
        graphics.text(mc.font, NoHostileMobsClient.statusMessage(), 4, 4, 0xFFFFFFFF, true);
    }
}
