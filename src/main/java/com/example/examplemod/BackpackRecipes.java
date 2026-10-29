package com.example.examplemod;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

final class BackpackRecipes {
    static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ExampleMod.MODID);
    static final RegistryObject<RecipeSerializer<BackpackUpgradeRecipe>> UPGRADE = SERIALIZERS.register("backpack_upgrade", () -> new RecipeSerializer<>(
        ShapedRecipe.MAP_CODEC.xmap(BackpackUpgradeRecipe::new, BackpackUpgradeRecipe::base),
        ShapedRecipe.STREAM_CODEC.map(BackpackUpgradeRecipe::new, BackpackUpgradeRecipe::base)));
}
