package com.example.examplemod;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

public enum Template {
    CRAFTING("crafting_template", 0), FURNACE("furnace_template", 1),
    BLAST_FURNACE("blast_furnace_template", 2), SMOKER("smoker_template", 2),
    FEEDING("feeding_template", 3), ARROWS("arrow_template", 4), SHULKER("shulker_template", 5);

    private final String id;
    private final int minimumLevel;
    Template(String id, int minimumLevel) { this.id = id; this.minimumLevel = minimumLevel; }
    public String id() { return id; }
    public int minimumLevel() { return minimumLevel; }
    public boolean allowed(BackpackLevel level) { return level.ordinal() >= minimumLevel; }
    public Item item() { return BackpackContent.TEMPLATES.get(this).get(); }
    public boolean matches(ItemStack stack) { return stack.is(item()); }
}
