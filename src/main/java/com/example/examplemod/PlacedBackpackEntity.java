package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class PlacedBackpackEntity extends BlockEntity {
    private ItemStack backpack=ItemStack.EMPTY;
    private BackpackState state;
    public PlacedBackpackEntity(BlockPos pos,BlockState block) { super(BackpackContent.PLACED_ENTITY.get(),pos,block); }
    public ItemStack backpack() { return backpack; }
    BackpackState contents() {
        if(state==null && backpack.getItem() instanceof BackpackItem) { state=BackpackState.of(backpack);state.changed=this::setChanged; }
        return state;
    }
    void setBackpack(ItemStack stack) { backpack=stack;state=null;contents();setChanged(); }
    ItemStack take() {
        if(state!=null)state.flush();
        var result=backpack;backpack=ItemStack.EMPTY;state=null;setChanged();return result;
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);if(state!=null)state.flush();output.store("backpack",ItemStack.OPTIONAL_CODEC,backpack);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);backpack=input.read("backpack",ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);state=null;
    }
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState block) {
        if(level!=null && !level.isClientSide()) {
            if(state!=null && state.viewer!=null)state.viewer.owner().closeContainer();
            Block.popResource(level,pos,take());
        }
    }
}
