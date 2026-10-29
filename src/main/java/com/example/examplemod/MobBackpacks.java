package com.example.examplemod;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.level.ExplosionEvent;

public final class MobBackpacks {
    private static final String DROPPED = "backpack_mod:dropped";
    private static final String EXPLODED = "backpack_mod:exploded";
    private static final List<Item> FOOD = List.of(Items.APPLE, Items.CARROT, Items.BREAD, Items.BAKED_POTATO, Items.COOKED_BEEF);
    // Repeated entries are weights: iron is more common than diamonds.
    private static final List<Item> MINERALS = List.of(Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT,
        Items.GOLD_INGOT, Items.GOLD_INGOT, Items.COAL, Items.LAPIS_LAZULI, Items.REDSTONE, Items.DIAMOND);

    private MobBackpacks() {}

    public static boolean isCarrier(Entity entity) {
        return (entity.getType() == EntityTypes.ZOMBIE || entity.getType() == EntityTypes.SKELETON || entity.getType() == EntityTypes.CREEPER)
            && Math.floorMod(entity.getUUID().hashCode(), 100) < Config.CARRIER_PERCENT;
    }

    public static void register() {
        LivingDropsEvent.BUS.addListener(MobBackpacks::onDrops);
        // A creeper discards itself after exploding, bypassing LivingDropsEvent.
        ExplosionEvent.Detonate.BUS.addListener(event -> {
            if (event.getExplosion().getDirectSourceEntity() instanceof Creeper creeper && isCarrier(creeper)) {
                creeper.addTag(EXPLODED);
            }
        });
        EntityLeaveLevelEvent.BUS.addListener(MobBackpacks::onLeave);
    }

    private static void onDrops(LivingDropsEvent event) {
        var mob = event.getEntity();
        if (!(mob.level() instanceof ServerLevel level) || !level.getGameRules().get(GameRules.MOB_DROPS)
            || !isCarrier(mob) || mob.entityTags().contains(DROPPED)) return;
        ItemStack backpack = createLoot(level, mob.getRandom());
        mob.addTag(DROPPED);
        event.getDrops().add(new ItemEntity(level, mob.getX(), mob.getY() + 0.5, mob.getZ(), backpack));
    }

    private static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Creeper creeper)
            || creeper.getRemovalReason() != Entity.RemovalReason.DISCARDED || !creeper.entityTags().contains(EXPLODED)
            || creeper.entityTags().contains(DROPPED) || !level.getGameRules().get(GameRules.MOB_DROPS)) return;
        creeper.addTag(DROPPED);
        // Spawn after the explosion, so its own blast does not destroy the backpack.
        level.addFreshEntity(new ItemEntity(level, creeper.getX(), creeper.getY() + 0.5, creeper.getZ(), createLoot(level, creeper.getRandom())));
    }

    public static ItemStack createLoot(ServerLevel level, RandomSource random) {
        List<ItemStack> contents = new ArrayList<>();
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        var enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getRandom(random)
            .orElseThrow(() -> new IllegalStateException("No enchantments available for backpack loot"));
        int min = enchantment.value().getMinLevel();
        book.enchant(enchantment, min + random.nextInt(enchantment.value().getMaxLevel() - min + 1));
        contents.add(book);
        contents.add(roll(FOOD, Config.MIN_FOOD, Config.MAX_FOOD, random));
        contents.add(roll(MINERALS, Config.MIN_MINERALS, Config.MAX_MINERALS, random));
        int extra = Config.MIN_EXTRA_ROLLS + random.nextInt(Config.MAX_EXTRA_ROLLS - Config.MIN_EXTRA_ROLLS + 1);
        for (int i = 0; i < extra; i++) {
            boolean food = random.nextBoolean();
            contents.add(roll(food ? FOOD : MINERALS, food ? Config.MIN_FOOD : Config.MIN_MINERALS,
                food ? Config.MAX_FOOD : Config.MAX_MINERALS, random));
        }
        ItemStack backpack = new ItemStack(ExampleMod.BACKPACK.get());
        backpack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return backpack;
    }

    private static ItemStack roll(List<Item> pool, int min, int max, RandomSource random) {
        Item item = pool.get(random.nextInt(pool.size()));
        int count = item == Items.DIAMOND ? 1 : min + random.nextInt(max - min + 1);
        return new ItemStack(item, count);
    }
}

