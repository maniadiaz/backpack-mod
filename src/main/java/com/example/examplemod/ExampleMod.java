package com.example.examplemod;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.EnumMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;

@Mod(ExampleMod.MODID)
public final class ExampleMod {
    public static final String MODID = "backpack_mod";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    private static final EnumMap<BackpackLevel, RegistryObject<Item>> BACKPACKS = new EnumMap<>(BackpackLevel.class);
    public static final RegistryObject<Item> BACKPACK = registerBackpack(BackpackLevel.LEATHER);
    public static final RegistryObject<Item> STONE_BACKPACK = registerBackpack(BackpackLevel.STONE);
    public static final RegistryObject<Item> IRON_BACKPACK = registerBackpack(BackpackLevel.IRON);
    public static final RegistryObject<Item> GOLD_BACKPACK = registerBackpack(BackpackLevel.GOLD);
    public static final RegistryObject<Item> DIAMOND_BACKPACK = registerBackpack(BackpackLevel.DIAMOND);
    public static final RegistryObject<Item> NETHERITE_BACKPACK = registerBackpack(BackpackLevel.NETHERITE);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final RegistryObject<CreativeModeTab> BACKPACK_TAB = TABS.register("backpacks", () -> CreativeModeTab.builder()
        .title(Component.translatable("itemGroup.backpack_mod"))
        .icon(() -> BACKPACK.get().getDefaultInstance())
        .displayItems((parameters, output) -> {
            BACKPACKS.values().forEach(entry -> output.accept(entry.get()));
            BackpackContent.TEMPLATES.values().forEach(entry -> output.accept(entry.get()));
        }).build());

    private static RegistryObject<Item> registerBackpack(BackpackLevel level) {
        RegistryObject<Item> entry = ITEMS.register(level.itemId(), () -> new BackpackItem(
            new Item.Properties().setId(ITEMS.key(level.itemId())).stacksTo(1), level));
        BACKPACKS.put(level, entry);
        return entry;
    }

    public static Item backpack(BackpackLevel level) { return BACKPACKS.get(level).get(); }

    public ExampleMod(FMLJavaModLoadingContext context) {
        BackpackContent.register(context.getModBusGroup());
        ITEMS.register(context.getModBusGroup());
        TABS.register(context.getModBusGroup());
        BackpackRecipes.SERIALIZERS.register(context.getModBusGroup());
        BackpackGameTests.register(context.getModBusGroup());
        BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
                BACKPACKS.values().forEach(event::accept);BackpackContent.TEMPLATES.values().forEach(event::accept);
            }
        });
        MobBackpacks.register();
        EquippedBackpack.register();
        BackpackNetwork.register();
        BackpackRuntime.register();
    }
}
