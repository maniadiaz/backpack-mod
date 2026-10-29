package com.example.examplemod;

import com.mojang.serialization.JsonOps;
import java.util.HashSet;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameType;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;

/** Registered explicitly through the vanilla test-function registry. */
@GameTestNamespace(ExampleMod.MODID)
public final class BackpackGameTests {
    public static void register(net.minecraftforge.eventbus.api.bus.BusGroup bus) {
        var functions = net.minecraftforge.registries.DeferredRegister.<java.util.function.Consumer<GameTestHelper>>create(
            net.minecraft.core.registries.Registries.TEST_FUNCTION, ExampleMod.MODID);
        functions.register("loot_and_persistence", () -> BackpackGameTests::lootAndPersistence);
        functions.register("carrier_selection", () -> BackpackGameTests::carrierSelection);
        functions.register("menu_guards_and_saving", () -> BackpackGameTests::menuGuardsAndSaving);
        functions.register("death_drops", () -> BackpackGameTests::deathDrops);
        functions.register("creeper_explosion", () -> BackpackGameTests::creeperExplosion);
        functions.register("equipped_slot_and_storage", () -> BackpackGameTests::equippedSlotAndStorage);
        functions.register("equipped_death_and_clone", () -> BackpackGameTests::equippedDeathAndClone);
        functions.register("creative_slot_validation", () -> BackpackGameTests::creativeSlotValidation);
        functions.register("crafting_and_upgrades", () -> BackpackGameTests::craftingAndUpgrades);
        functions.register("all_level_capacities", () -> BackpackGameTests::allLevelCapacities);
        BackpackTemplateTests.register(functions);
        functions.register(bus);
    }
    @GameTest
    public static void craftingAndUpgrades(GameTestHelper helper) {
        var manager = helper.getLevel().recipeAccess();
        var type = net.minecraft.world.item.crafting.RecipeType.CRAFTING;
        var grid = net.minecraft.core.NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < 9; i++) grid.set(i, new ItemStack(i == 4 ? Items.CHEST : Items.LEATHER));
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
        var basic = manager.getRecipeFor(type, input, helper.getLevel());
        helper.assertTrue(basic.isPresent(), "Leather/chest recipe from the crafting screenshot is missing");
        helper.assertTrue(basic.orElseThrow().value().assemble(input).is(ExampleMod.BACKPACK.get()), "Base recipe returns the wrong item");
        grid.set(0, ItemStack.EMPTY);
        helper.assertTrue(manager.getRecipeFor(type, net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid), helper.getLevel()).isEmpty(), "Base recipe accepts seven leather");
        ItemStack bag = MobBackpacks.createLoot(helper.getLevel(), RandomSource.create(86));
        bag.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Mi mochila con objetos"));
        bag.set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(0x3592C4));
        for (int number = 1; number < BackpackLevel.values().length; number++) {
            BackpackLevel level = BackpackLevel.values()[number];
            grid = net.minecraft.core.NonNullList.withSize(9, ItemStack.EMPTY);
            for (int corner : new int[]{1,3,5,7}) grid.set(corner, new ItemStack(level.material()));
            grid.set(4, bag);
            input = net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid);
            var recipe = manager.getRecipeFor(type, input, helper.getLevel());
            helper.assertTrue(recipe.isPresent(), "Missing loaded upgrade recipe: " + level);
            ItemStack result = recipe.orElseThrow().value().assemble(input);
            helper.assertTrue(ItemStack.matches(result, bag.transmuteCopy(ExampleMod.backpack(level),1)), "Upgrade lost contents, name or color: " + level);
            helper.assertTrue(bag.getCount() == 1, "Recipe preview consumed the input backpack");
            grid.set(0, new ItemStack(level.material()));
            helper.assertTrue(manager.getRecipeFor(type, net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid),helper.getLevel()).isEmpty(), "Upgrade accepted extra ingredients");
            grid.set(0,ItemStack.EMPTY);
            grid.set(1,ItemStack.EMPTY);
            helper.assertTrue(manager.getRecipeFor(type, net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid),helper.getLevel()).isEmpty(), "Upgrade accepted only three cross materials");
            grid.set(1,new ItemStack(level.material()));
            grid.set(4,result);
            helper.assertTrue(manager.getRecipeFor(type, net.minecraft.world.item.crafting.CraftingInput.of(3,3,grid),helper.getLevel()).isEmpty(), "Upgrade accepted the wrong source level");
            bag = result;
        }
        helper.assertTrue(BackpackLevel.of(bag) == BackpackLevel.NETHERITE, "Upgrade chain did not reach netherite");
        helper.succeed();
    }

    @GameTest
    public static void allLevelCapacities(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var seen = new java.util.HashSet<net.minecraft.world.item.Item>();
        for (BackpackLevel level : BackpackLevel.values()) {
            ItemStack bag = new ItemStack(ExampleMod.backpack(level));
            helper.assertTrue(seen.add(bag.getItem()), "Two levels share one item");
            player.setItemInHand(InteractionHand.MAIN_HAND, bag);
            var contents = new SimpleContainer(level.slots());
            var menu = new BackpackMenu(3, player.getInventory(), contents, bag, InteractionHand.MAIN_HAND);
            helper.assertTrue(menu.getRowCount() == new BackpackLayout(level).rows() && menu.getContainer().getContainerSize() == level.slots(), "Wrong capacity: " + level);
            contents.setItem(level.slots()-1, new ItemStack(Items.DIAMOND, 2));
            menu.removed(player);
            var saved = bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).itemCopies().toList();
            helper.assertTrue(saved.size() == level.slots() && saved.get(level.slots()-1).getCount() == 2, "Last inventory slot was lost: " + level);
            helper.assertTrue(!menu.slots.get(menu.storageSlotStart()).mayPlace(new ItemStack(ExampleMod.DIAMOND_BACKPACK.get())), "Nested higher-tier backpack accepted");
        }
        helper.succeed();
    }

    @GameTest
    public static void equippedSlotAndStorage(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        EquippedBackpack.attach(player);
        int size = player.inventoryMenu.slots.size();
        EquippedBackpack.attach(player);
        helper.assertTrue(size == 47 && player.inventoryMenu.slots.size() == size, "Equipment slot attached more than once");
        var slot = player.inventoryMenu.slots.get(46);
        helper.assertTrue(slot.x == EquippedBackpack.X && slot.y == EquippedBackpack.Y, "Wrong equipment slot position");
        helper.assertTrue(!slot.mayPlace(new ItemStack(Items.DIAMOND)), "Equipment slot accepts non-backpacks");
        var bag = MobBackpacks.createLoot(helper.getLevel(), RandomSource.create(72));
        var expectedBag = bag.copy();
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        player.inventoryMenu.setCarried(bag);
        player.inventoryMenu.clicked(46, 0, ContainerInput.PICKUP, player);
        bag = EquippedBackpack.get(player);
        helper.assertTrue(ItemStack.matches(bag, expectedBag) && player.inventoryMenu.getCarried().isEmpty(), "Backpack was not equipped");
        helper.assertTrue(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "Backpack replaced armor");
        var contents = new SimpleContainer(9);
        var menu = new BackpackMenu(2, player.getInventory(), contents, bag, null);
        helper.assertTrue(menu.stillValid(player) && player.getMainHandItem().isEmpty(), "Equipped backpack requires a held item");
        contents.setItem(0, new ItemStack(Items.DIAMOND, 3));
        menu.removed(player);
        var saved = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, player.registryAccess());
        player.saveWithoutId(saved);
        var restored = helper.makeMockPlayer(GameType.SURVIVAL);
        restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, player.registryAccess(), saved.buildResult()));
        helper.assertTrue(ItemStack.matches(bag, EquippedBackpack.get(restored)), "Equipped contents lost during player save/load");
        var beforeUnequip = bag.copy();
        player.inventoryMenu.clicked(46, 0, ContainerInput.PICKUP, player);
        helper.assertTrue(EquippedBackpack.get(player).isEmpty() && ItemStack.matches(player.inventoryMenu.getCarried(), beforeUnequip), "Backpack was not unequipped");
        helper.assertTrue(!menu.stillValid(player), "Unequipped backpack menu remains valid");
        helper.succeed();
    }

    @GameTest
    public static void equippedDeathAndClone(GameTestHelper helper) {
        var oldPlayer = helper.makeMockPlayer(GameType.SURVIVAL);
        var bag = MobBackpacks.createLoot(helper.getLevel(), RandomSource.create(16));
        oldPlayer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.BODY, bag);
        var rules = helper.getLevel().getGameRules();
        var rule = net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY;
        boolean previous = rules.get(rule);
        try {
            rules.set(rule, true, helper.getLevel().getServer());
            var kept = helper.makeMockPlayer(GameType.SURVIVAL);
            net.minecraftforge.event.entity.player.PlayerEvent.Clone.BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(kept, oldPlayer, true));
            helper.assertTrue(ItemStack.matches(bag, EquippedBackpack.get(kept)), "keepInventory did not preserve equipped backpack");
            rules.set(rule, false, helper.getLevel().getServer());
            var respawned = helper.makeMockPlayer(GameType.SURVIVAL);
            net.minecraftforge.event.entity.player.PlayerEvent.Clone.BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(respawned, oldPlayer, true));
            helper.assertTrue(EquippedBackpack.get(respawned).isEmpty(), "Death clone duplicated the dropped backpack");
            var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
            oldPlayer.setPos(pos.getX(), pos.getY(), pos.getZ());
            var area = oldPlayer.getBoundingBox().inflate(3);
            int before = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
                e -> e.getItem().is(ExampleMod.BACKPACK.get())).size();
            oldPlayer.getInventory().dropAll();
            int after = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
                e -> e.getItem().is(ExampleMod.BACKPACK.get())).size();
            helper.assertTrue(after == before + 1 && EquippedBackpack.get(oldPlayer).isEmpty(), "Equipped backpack must drop exactly once");
        } finally {
            rules.set(rule, previous, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest
    public static void creativeSlotValidation(GameTestHelper helper) {
        var survival = helper.makeMockPlayer(GameType.SURVIVAL);
        var bag = MobBackpacks.createLoot(helper.getLevel(), RandomSource.create(18));
        EquippedBackpack.applyCreativeChange(survival, bag);
        helper.assertTrue(EquippedBackpack.get(survival).isEmpty(), "Survival client injected creative backpack data");
        var creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;
        EquippedBackpack.applyCreativeChange(creative, new ItemStack(Items.DIAMOND));
        helper.assertTrue(EquippedBackpack.get(creative).isEmpty(), "Creative slot accepted a non-backpack");
        EquippedBackpack.applyCreativeChange(creative, bag);
        helper.assertTrue(ItemStack.matches(bag, EquippedBackpack.get(creative)), "Creative backpack contents changed");
        EquippedBackpack.applyCreativeChange(creative, ItemStack.EMPTY);
        helper.assertTrue(EquippedBackpack.get(creative).isEmpty(), "Creative unequip failed");
        helper.succeed();
    }

    @GameTest
    public static void lootAndPersistence(GameTestHelper helper) {
        var variants = new HashSet<String>();
        var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        for (int seed = 0; seed < 256; seed++) {
            ItemStack bag = MobBackpacks.createLoot(helper.getLevel(), RandomSource.create(seed));
            var contents = bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).itemCopies().toList();
            helper.assertTrue(contents.size() >= 4 && contents.size() <= BackpackLevel.LEATHER.slots(), "Loot must fit the backpack");
            helper.assertTrue(contents.stream().anyMatch(s -> s.is(Items.ENCHANTED_BOOK) && EnchantmentHelper.hasAnyEnchantments(s)), "Missing enchanted book");
            var encoded = ItemStack.CODEC.encodeStart(ops, bag).getOrThrow();
            ItemStack restored = ItemStack.CODEC.parse(ops, encoded).getOrThrow();
            helper.assertTrue(ItemStack.isSameItemSameComponents(bag, restored), "Backpack contents changed during serialization");
            variants.add(encoded.toString());
        }
        helper.assertTrue(variants.size() > 200, "Loot should vary between random seeds");
        helper.succeed();
    }

    @GameTest
    public static void carrierSelection(GameTestHelper helper) {
        var zombie = helper.spawn(EntityTypes.ZOMBIE, new net.minecraft.core.BlockPos(1, 1, 1));
        var skeleton = helper.spawn(EntityTypes.SKELETON, new net.minecraft.core.BlockPos(1, 1, 1));
        var creeper = helper.spawn(EntityTypes.CREEPER, new net.minecraft.core.BlockPos(1, 1, 1));
        var cow = helper.spawn(EntityTypes.COW, new net.minecraft.core.BlockPos(1, 1, 1));
        for (var entity : java.util.List.of(zombie, skeleton, creeper)) {
            entity.setUUID(new UUID(0, 1));
            helper.assertTrue(MobBackpacks.isCarrier(entity), "Eligible carrier not selected");
            entity.setUUID(new UUID(0, 99));
            helper.assertTrue(!MobBackpacks.isCarrier(entity), "Non-carrier selected");
        }
        cow.setUUID(new UUID(0, 1));
        helper.assertTrue(!MobBackpacks.isCarrier(cow), "Other mobs must not carry backpacks");
        helper.succeed();
    }

    @GameTest
    public static void deathDrops(GameTestHelper helper) {
        int uuid = 10;
        for (var type : java.util.List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.CREEPER)) {
            var mob = helper.spawn(type, new net.minecraft.core.BlockPos(1, 1, 1));
            mob.setUUID(new UUID(0, uuid++));
            var area = mob.getBoundingBox().inflate(2);
            long before = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
                e -> e.getItem().is(ExampleMod.BACKPACK.get())).size();
            mob.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000);
            long after = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
                e -> e.getItem().is(ExampleMod.BACKPACK.get())).size();
            helper.assertTrue(after == before + 1, "Carrier must drop exactly one backpack: " + type);
        }
        var noBag = helper.spawn(EntityTypes.ZOMBIE, new net.minecraft.core.BlockPos(1, 1, 1));
        noBag.setUUID(new UUID(0, 99));
        var area = noBag.getBoundingBox().inflate(2);
        int before = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
            e -> e.getItem().is(ExampleMod.BACKPACK.get())).size();
        noBag.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 1000);
        int after = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
            e -> e.getItem().is(ExampleMod.BACKPACK.get())).size();
        helper.assertTrue(after == before, "A non-carrier dropped a backpack");
        helper.succeed();
    }

    @GameTest
    public static void creeperExplosion(GameTestHelper helper) {
        var creeper = helper.spawn(EntityTypes.CREEPER, new net.minecraft.core.BlockPos(1, 1, 1));
        creeper.setUUID(new UUID(0, 17));
        creeper.setNoAi(true);
        var area = creeper.getBoundingBox().inflate(3);
        creeper.ignite();
        helper.runAfterDelay(45, () -> {
            var bags = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area,
                e -> e.getItem().is(ExampleMod.BACKPACK.get()));
            helper.assertTrue(creeper.isRemoved() && bags.size() == 1, "Exploding creeper must drop one surviving backpack");
            helper.succeed();
        });
    }

    @GameTest
    public static void menuGuardsAndSaving(GameTestHelper helper) {
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var bag = new ItemStack(ExampleMod.BACKPACK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, bag);
        var contents = new SimpleContainer(BackpackLevel.LEATHER.slots());
        var menu = new BackpackMenu(1, player.getInventory(), contents, bag, InteractionHand.MAIN_HAND);
        int heldSlot = menu.playerSlotStart() + 27 + player.getInventory().getSelectedSlot();
        menu.clicked(heldSlot, 0, ContainerInput.THROW, player);
        menu.clicked(heldSlot, 0, ContainerInput.QUICK_MOVE, player);
        menu.clicked(menu.storageSlotStart(), player.getInventory().getSelectedSlot(), ContainerInput.SWAP, player);
        helper.assertTrue(player.getMainHandItem() == bag && contents.isEmpty(), "Open backpack was moved");
        helper.assertTrue(!menu.slots.get(menu.storageSlotStart()).mayPlace(new ItemStack(ExampleMod.BACKPACK.get())), "Nested backpack accepted");
        contents.setItem(0, new ItemStack(Items.DIAMOND, 3));
        menu.removed(player);
        helper.assertTrue(bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne().getCount() == 3, "Contents were not saved");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.assertTrue(!menu.stillValid(player), "Menu stays valid after losing backpack");
        helper.succeed();
    }
}
