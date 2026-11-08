package dev.cnxx.nohostilemobs.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExclusionScreen extends GridScreen<EntityType<?>> {
    private final ClientConfig config = NoHostileMobsClient.config();

    public ExclusionScreen(Screen parent) {
        super(Component.translatable("nohostilemobs.screen.excluded.title"), parent);
    }

    @Override
    protected List<EntityType<?>> createEntries() {
        return hostileTypes();
    }

    public static List<EntityType<?>> hostileTypes() {
        // getBaseClass() always returns Entity in 26.x, so use the spawn category instead.
        List<EntityType<?>> types = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.getCategory() == MobCategory.MONSTER) types.add(type);
        }
        types.sort(Comparator.comparing(t -> t.getDescription().getString()));
        return types;
    }

    @Override
    protected Component label(EntityType<?> type) {
        return config.isExcluded(EntityType.getKey(type))
                ? Component.literal("✔ ").append(type.getDescription()).withStyle(ChatFormatting.GREEN)
                : type.getDescription().copy().withStyle(ChatFormatting.GRAY);
    }

    @Override
    protected ItemStack icon(EntityType<?> type) {
        return SpawnEggItem.byId(type).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void onSelect(EntityType<?> type) {
        config.toggleExcluded(EntityType.getKey(type));
    }

    @Override
    protected Component hint() {
        return Component.translatable("nohostilemobs.screen.excluded.hint");
    }

    @Override
    protected boolean hasActionRow() {
        return true;
    }

    @Override
    protected void addActionButtons(int left, int y, int width) {
        int w = (width - 8) / 3;
        addRenderableWidget(Button.builder(Component.translatable("nohostilemobs.option.select_all"), b -> {
            hostileTypes().forEach(t -> config.excluded.add(EntityType.getKey(t).toString()));
            rebuildWidgets();
        }).bounds(left, y, w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("nohostilemobs.option.deselect_all"), b -> {
            hostileTypes().forEach(t -> config.excluded.remove(EntityType.getKey(t).toString()));
            rebuildWidgets();
        }).bounds(left + w + 4, y, w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("nohostilemobs.option.reset_excluded"), b -> {
            config.excluded = ClientConfig.defaultExcluded();
            rebuildWidgets();
        }).bounds(left + 2 * (w + 4), y, width - 2 * (w + 4), 20).build());
    }
}
