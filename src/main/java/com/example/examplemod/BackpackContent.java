package com.example.examplemod;

import java.util.EnumMap;
import java.util.Set;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BackpackContent {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ExampleMod.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ExampleMod.MODID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ExampleMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ExampleMod.MODID);
    public static final RegistryObject<DataComponentType<ItemContainerContents>> INSTALLED = inventory("templates");
    public static final RegistryObject<DataComponentType<ItemContainerContents>> WORKSHOP = inventory("workshop");
    public static final RegistryObject<DataComponentType<ItemContainerContents>> CRAFTING = inventory("crafting_grid");
    public static final RegistryObject<DataComponentType<CustomData>> MACHINES = COMPONENTS.register("machines", () -> DataComponentType.<CustomData>builder().persistent(CustomData.CODEC).build());
    public static final EnumMap<Template, RegistryObject<Item>> TEMPLATES = new EnumMap<>(Template.class);
    public static final RegistryObject<MenuType<BackpackMenu>> MENU = MENUS.register("backpack", () -> IForgeMenuType.create(BackpackMenu::client));
    public static final RegistryObject<Block> PLACED = BLOCKS.register("placed_backpack", () -> new PlacedBackpackBlock(
        BlockBehaviour.Properties.of().setId(BLOCKS.key("placed_backpack")).strength(0.8F).noOcclusion()));
    public static final RegistryObject<BlockEntityType<PlacedBackpackEntity>> PLACED_ENTITY = ENTITIES.register("placed_backpack",
        () -> new BlockEntityType<>(PlacedBackpackEntity::new, Set.of(PLACED.get())));

    static {
        for (Template template : Template.values()) {
            TEMPLATES.put(template, ExampleMod.ITEMS.register(template.id(), () -> new TemplateItem(new Item.Properties().setId(ExampleMod.ITEMS.key(template.id())).stacksTo(16),template)));
        }
    }
    private static RegistryObject<DataComponentType<ItemContainerContents>> inventory(String id) {
        return COMPONENTS.register(id, () -> DataComponentType.<ItemContainerContents>builder()
            .persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).build());
    }
    public static void register(BusGroup bus) { COMPONENTS.register(bus); MENUS.register(bus); BLOCKS.register(bus); ENTITIES.register(bus); }
}
