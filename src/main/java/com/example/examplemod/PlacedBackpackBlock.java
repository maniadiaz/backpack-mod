package com.example.examplemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class PlacedBackpackBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty LEVEL=IntegerProperty.create("level",0,5);
    private static final VoxelShape SHAPE=Block.box(1,0,3,15,14,13);
    public PlacedBackpackBlock(BlockBehaviour.Properties properties) {
        super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LEVEL,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING,LEVEL); }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return SHAPE; }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        var below=pos.below();var support=level.getBlockState(below);
        return support.isCollisionShapeFullBlock(level,below) && support.isFaceSturdy(level,below,Direction.UP);
    }
    @Override protected BlockState updateShape(BlockState state,LevelReader level,ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighbor,BlockState neighborState,RandomSource random) {
        return direction==Direction.DOWN && !canSurvive(state,level,pos) ? Blocks.AIR.defaultBlockState():state;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new PlacedBackpackEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState block,BlockEntityType<T> type) {
        return level.isClientSide()?null:createTickerHelper(type,BackpackContent.PLACED_ENTITY.get(),(world,pos,state,entity)->{
            var contents=entity.contents();if(contents==null)return;
            BackpackMachines.tick((ServerLevel)world,pos,contents);
            if(world.getGameTime()%20==0)contents.flush();
        });
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        return interact(level,pos,player);
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        return interact(level,pos,player);
    }
    private InteractionResult interact(Level level,BlockPos pos,Player player) {
        if(level instanceof ServerLevel && level.getBlockEntity(pos) instanceof PlacedBackpackEntity entity && entity.contents()!=null) {
            if(player.isShiftKeyDown()) {
                var viewer=entity.contents().viewer;
                if(viewer!=null && viewer.owner().containerMenu==viewer)return InteractionResult.FAIL;
                ItemStack bag=entity.take();level.removeBlock(pos,false);
                player.getInventory().add(bag);
                if(!bag.isEmpty())player.drop(bag,false,Prediction.SERVER_ONLY);
            } else BackpackMenu.open(player,entity.backpack(),null,entity);
        }
        return InteractionResult.SUCCESS;
    }
    static InteractionResult place(UseOnContext context) {
        var level=context.getLevel();var player=context.getPlayer();var pos=context.getClickedPos().above();
        if(context.getClickedFace()!=Direction.UP || player==null)return InteractionResult.PASS;
        var block=BackpackContent.PLACED.get().defaultBlockState().setValue(FACING,player.getDirection().getOpposite()).setValue(LEVEL,BackpackLevel.of(context.getItemInHand()).ordinal());
        if(!level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty() || !block.canSurvive(level,pos)
            || !player.mayUseItemAt(pos,Direction.UP,context.getItemInHand()))return InteractionResult.FAIL;
        if(!level.isClientSide()) {
            var original=context.getItemInHand();BackpackState.flushIfLoaded(original);
            if(!level.setBlock(pos,block,3))return InteractionResult.FAIL;
            if(level.getBlockEntity(pos) instanceof PlacedBackpackEntity entity) {
                entity.setBackpack(original.copyWithCount(1));original.shrink(1);
            } else { level.removeBlock(pos,false);return InteractionResult.FAIL; }
        }
        return InteractionResult.SUCCESS;
    }
}
