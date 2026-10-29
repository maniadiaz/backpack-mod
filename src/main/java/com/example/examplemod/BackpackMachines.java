package com.example.examplemod;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import net.minecraft.world.phys.Vec3;

final class BackpackMachines {
    static void tick(ServerLevel level, BlockPos position, BackpackState state) {
        station(level,position,state,0,Template.FURNACE,RecipeType.SMELTING);
        station(level,position,state,1,Template.BLAST_FURNACE,RecipeType.BLASTING);
        station(level,position,state,2,Template.SMOKER,RecipeType.SMOKING);
    }

    private static <T extends AbstractCookingRecipe> void station(ServerLevel world, BlockPos pos, BackpackState state, int station, Template template, RecipeType<T> type) {
        if (!state.active(template)) return;
        int base=station*3;
        ItemStack input=state.workshop.getItem(base), fuel=state.workshop.getItem(base+1), output=state.workshop.getItem(base+2);
        if (state.burn[station]>0) state.burn[station]--;
        var found=world.recipeAccess().getRecipeFor(type,new SingleRecipeInput(input),world);
        String id=found.map(r -> r.id().identifier().toString()).orElse("");
        if (!id.equals(state.recipe[station])) { state.cook[station]=0; state.recipe[station]=id; }
        if (found.isEmpty()) { state.cook[station]=0; return; }
        T recipe=found.orElseThrow().value();
        ItemStack result=recipe.assemble(new SingleRecipeInput(input));
        boolean room=!result.isEmpty() && (output.isEmpty() || ItemStack.isSameItemSameComponents(output,result)
            && output.getCount()+result.getCount()<=output.getMaxStackSize());
        if (!room) { state.cook[station]=0; return; }
        if (state.burn[station]<=0 && !fuel.isEmpty()) {
            LootContext context=context(world,pos,station,state);
            int vanilla=ResolvableInt.getFromItem(fuel,DataComponents.COOKING_FUEL,CookingFuel::burnTime,context,0);
            int duration=net.minecraftforge.event.ForgeEventFactory.getItemBurnTime(fuel,vanilla,type);
            if (duration>0) {
                state.burn[station]=state.totalBurn[station]=duration;
                state.speed[station]=Math.max(0.001F,ResolvableFloat.getFromItem(fuel,DataComponents.COOKING_FUEL,CookingFuel::speedMultiplier,context,1));
                var remainder=fuel.getCraftingRemainder();
                fuel.shrink(1);
                if(fuel.isEmpty() && remainder!=null)state.workshop.setItem(base+1,remainder.create());
                else state.workshop.setChanged();
                state.flush();
            }
        }
        state.totalCook[station]=Math.max(1,(int)Math.ceil(recipe.cookingTime()/state.speed[station]));
        if (state.burn[station]>0) {
            if (++state.cook[station]>=state.totalCook[station]) {
                if(output.isEmpty())state.workshop.setItem(base+2,result.copy());
                else { output.grow(result.getCount()); state.workshop.setChanged(); }
                if(input.is(net.minecraft.world.item.Items.WET_SPONGE) && state.workshop.getItem(base+1).is(net.minecraft.world.item.Items.BUCKET))
                    state.workshop.setItem(base+1,new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET));
                input.shrink(1); state.workshop.setChanged();
                state.cook[station]=0; state.xp[station]+=recipe.experience(); state.flush();
            }
        } else state.cook[station]=Math.max(0,state.cook[station]-2);
    }

    private static LootContext context(ServerLevel world, BlockPos pos, int station, BackpackState state) {
        BlockEntity entity=switch(station) {
            case 1 -> new BlastFurnaceBlockEntity(pos,Blocks.BLAST_FURNACE.defaultBlockState());
            case 2 -> new SmokerBlockEntity(pos,Blocks.SMOKER.defaultBlockState());
            default -> new FurnaceBlockEntity(pos,Blocks.FURNACE.defaultBlockState());
        };
        entity.setLevel(world);
        // This entity is only a fuel-resolution context. It is never placed or ticked in the world.
        var container=new SimpleContainer(state.workshop.getItem(station*3),state.workshop.getItem(station*3+1),state.workshop.getItem(station*3+2));
        return new LootContext.Builder(new LootParams.Builder(world)
            .withParameter(LootContextParams.BLOCK_STATE,entity.getBlockState())
            .withParameter(LootContextParams.BLOCK_ENTITY,entity)
            .withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(pos))
            .withParameter(LootContextParams.CONTAINER,container).create(LootContextParamSets.CONTAINER_PROCESS)).create(Optional.empty());
    }

    static boolean feed(Player player, BackpackState state) {
        if(!state.active(Template.FEEDING) || !player.isAlive() || player.isSpectator() || player.hasInfiniteMaterials()
            || !player.getFoodData().needsFood())return false;
        for(int number=0;number<BackpackState.FOOD_SLOTS;number++) {
            int slot=BackpackState.foodSlot(number);
            ItemStack food=state.workshop.getItem(slot);
            if(food.isEmpty() || !food.has(DataComponents.FOOD) || !food.has(DataComponents.CONSUMABLE))continue;
            ItemStack result=food.finishUsingItem(player.level(),player);
            state.workshop.setItem(slot,result);
            state.flush();return true;
        }
        return false;
    }

    static void claimExperience(Player player, BackpackState state, int station) {
        if(player.level().isClientSide())return;
        float value=state.xp[station]; int amount=(int)value;
        if(player.getRandom().nextFloat()<value-amount)amount++;
        if(amount>0)player.giveExperiencePoints(amount);
        state.xp[station]=0; state.flush();
    }
}
