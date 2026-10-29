package com.example.examplemod;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

public final class BackpackItem extends Item {
    private final BackpackLevel level;
    public BackpackItem(Properties properties, BackpackLevel level) { super(properties); this.level = level; }
    public BackpackLevel level() { return level; }

    @Override
    public boolean canFitInsideContainerItems() { return false; }

    @Override
    public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return PlacedBackpackBlock.place(context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack backpack = player.getItemInHand(hand);
            if (player.isShiftKeyDown() && EquippedBackpack.get(player).isEmpty()) {
                player.setItemSlot(EquipmentSlot.BODY, backpack);
                player.setItemInHand(hand, ItemStack.EMPTY);
            } else {
                open(player, backpack, hand);
            }
        }
        return InteractionResult.SUCCESS;
    }

    static void open(Player player, ItemStack backpack, InteractionHand hand) {
        BackpackMenu.open(player,backpack,hand,null);
    }
}
