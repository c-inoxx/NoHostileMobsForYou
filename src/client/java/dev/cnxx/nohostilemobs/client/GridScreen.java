package dev.cnxx.nohostilemobs.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

abstract class GridScreen<T> extends Screen {
    private static final int BUTTON_W = 130;
    private static final int BUTTON_H = 20;
    private static final int GAP = 4;
    private static final int ROW = 22;
    private static final int TOP = 38;
    private static final int MAX_COLUMNS = 4;

    protected final Screen parent;
    private List<T> entries;
    private int page = 0;
    private int pageCount = 1;
    private int pageTextX, pageTextY;

    protected GridScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    protected abstract List<T> createEntries();

    protected abstract Component label(T entry);

    protected abstract void onSelect(T entry);

    protected abstract Component hint();

    protected ItemStack icon(T entry) {
        return ItemStack.EMPTY;
    }

    protected boolean hasActionRow() {
        return false;
    }

    protected void addActionButtons(int left, int y, int width) {}

    @Override
    protected void init() {
        if (entries == null) entries = createEntries();

        int columns = Mth.clamp((width - 20 + GAP) / (BUTTON_W + GAP), 1, MAX_COLUMNS);
        int gridWidth = columns * BUTTON_W + (columns - 1) * GAP;
        int left = (width - gridWidth) / 2;

        int navY = height - 28;
        int actionY = navY - 24;
        int gridBottom = (hasActionRow() ? actionY : navY) - 6;
        int rows = Math.max(1, (gridBottom - TOP) / ROW);
        int perPage = rows * columns;

        pageCount = Math.max(1, (entries.size() + perPage - 1) / perPage);
        page = Mth.clamp(page, 0, pageCount - 1);

        int start = page * perPage;
        int end = Math.min(entries.size(), start + perPage);
        for (int i = start; i < end; i++) {
            T entry = entries.get(i);
            int slot = i - start;
            int x = left + (slot % columns) * (BUTTON_W + GAP);
            int y = TOP + (slot / columns) * ROW;
            addRenderableWidget(new IconButton(x, y, BUTTON_W, BUTTON_H, label(entry), icon(entry), b -> {
                onSelect(entry);
                rebuildWidgets();
            }));
        }

        if (hasActionRow()) addActionButtons(left, actionY, gridWidth);

        addRenderableWidget(Button.builder(Component.literal("<"), b -> { page--; rebuildWidgets(); })
                .bounds(left, navY, 20, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { page++; rebuildWidgets(); })
                .bounds(left + 24, navY, 20, 20).build()).active = page < pageCount - 1;
        pageTextX = left + 52;
        pageTextY = navY + 6;

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(left + gridWidth - 100, navY, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(font, title, width / 2, 10, 0xFFFFFFFF);
        graphics.centeredText(font, hint().copy().withStyle(ChatFormatting.GRAY), width / 2, 22, 0xFFFFFFFF);
        graphics.text(font, Component.translatable("nohostilemobs.screen.page", page + 1, pageCount),
                pageTextX, pageTextY, 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        NoHostileMobsClient.saveAndSync();
    }
}
