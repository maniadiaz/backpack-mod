package com.example.examplemod;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum BackpackLevel {
    LEATHER(9, "backpack", Items.LEATHER), STONE(18, "stone_backpack", Items.STONE),
    IRON(27, "iron_backpack", Items.IRON_INGOT), GOLD(63, "gold_backpack", Items.GOLD_INGOT),
    DIAMOND(90, "diamond_backpack", Items.DIAMOND), NETHERITE(117, "netherite_backpack", Items.NETHERITE_INGOT);

    private final int slots;
    private final String itemId;
    private final Item material;
    BackpackLevel(int slots, String itemId, Item material) { this.slots = slots; this.itemId = itemId; this.material = material; }
    public int slots() { return slots; }
    public int rows() { return slots / 9; }
    public String itemId() { return itemId; }
    public Item material() { return material; }
    public static BackpackLevel of(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof BackpackItem backpack ? backpack.level() : LEATHER;
    }
}
