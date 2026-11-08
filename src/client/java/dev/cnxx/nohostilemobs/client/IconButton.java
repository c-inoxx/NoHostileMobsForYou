package dev.cnxx.nohostilemobs.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class IconButton extends Button.Plain {
    private static final int ICON_SPACE = 20;

    private final ItemStack icon;

    IconButton(int x, int y, int width, int height, Component message, ItemStack icon, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.icon = icon;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (icon.isEmpty()) {
            super.extractContents(graphics, mouseX, mouseY, delta);
            return;
        }
        extractDefaultSprite(graphics);
        graphics.fakeItem(icon, getX() + 2, getY() + (getHeight() - 16) / 2);
        // The margin is applied on both sides, keeping the label centered but clear of the icon.
        extractScrollingStringOverContents(
                graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE),
                getMessage(), ICON_SPACE);
    }
}
