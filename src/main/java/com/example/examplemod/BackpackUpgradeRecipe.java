package com.example.examplemod;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

/** Retains vanilla recipe-book support while copying the backpack's complete data patch. */
public record BackpackUpgradeRecipe(ShapedRecipe base) implements CraftingRecipe {
    @Override public boolean matches(CraftingInput input, Level world) {
        if (input.width() != 3 || input.height() != 3 || !base.matches(input, world)) return false;
        ItemStack center = input.getItem(4);
        ItemStack result = base.assemble(input);
        return center.getItem() instanceof BackpackItem before && result.getItem() instanceof BackpackItem after
            && after.level().ordinal() == before.level().ordinal() + 1;
    }

    @Override public ItemStack assemble(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3 || !(input.getItem(4).getItem() instanceof BackpackItem)) return ItemStack.EMPTY;
        BackpackState.flushIfLoaded(input.getItem(4));
        return input.getItem(4).transmuteCopy(base.assemble(input).getItem(), 1);
    }
    @Override public CraftingBookCategory category() { return base.category(); }
    @Override public boolean showNotification() { return base.showNotification(); }
    @Override public String group() { return base.group(); }
    @Override public PlacementInfo placementInfo() { return base.placementInfo(); }
    @Override public List<RecipeDisplay> display() { return base.display(); }
    @Override public RecipeSerializer<BackpackUpgradeRecipe> getSerializer() { return BackpackRecipes.UPGRADE.get(); }
}
