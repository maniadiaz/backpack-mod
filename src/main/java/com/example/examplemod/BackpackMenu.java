package com.example.examplemod;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.crafting.RecipeType;

/** Portable storage and installed workshop templates share one server-authoritative menu. */
public final class BackpackMenu extends AbstractCraftingMenu {
    static final int PAGE_SIZE=117;
    private final BackpackLayout layout;
    private final BackpackState state;
    private final Player player;
    private final InteractionHand hand;
    private final int lockedHotbar;
    private final PlacedBackpackEntity placed;
    private final boolean client;
    private final SimpleContainerData data=new SimpleContainerData(8);
    private final int templatesStart,workStart,playerStart;
    private boolean initializing=true;

    static BackpackMenu client(int id,Inventory inventory,FriendlyByteBuf buffer) {
        int number=Math.clamp(buffer.readVarInt(),0,5);
        int mode=buffer.readVarInt(),locked=buffer.readVarInt();
        ItemStack fake=new ItemStack(ExampleMod.backpack(BackpackLevel.values()[number]));
        return new BackpackMenu(id,inventory,new BackpackState(fake,null),mode==0?InteractionHand.MAIN_HAND:mode==1?InteractionHand.OFF_HAND:null,null,locked,true);
    }

    static void open(Player player,ItemStack bag,InteractionHand hand,PlacedBackpackEntity placed) {
        if(!(player instanceof ServerPlayer server) || player.isSpectator() || bag.isEmpty())return;
        BackpackState state=placed==null?BackpackState.of(bag):placed.contents();
        if(state==null)return;
        if(state.viewer!=null && state.viewer.player.containerMenu==state.viewer) {
            player.sendOverlayMessage(Component.translatable("message.backpack_mod.in_use"));return;
        }
        int mode=placed!=null?3:hand==null?2:hand==InteractionHand.MAIN_HAND?0:1;
        int locked=hand==null?-1:hand==InteractionHand.MAIN_HAND?player.getInventory().getSelectedSlot():40;
        server.openMenu(new SimpleMenuProvider((id,inventory,owner)->new BackpackMenu(id,inventory,state,hand,placed,locked,false),
            Component.translatable("container.backpack_mod.backpack",bag.getHoverName())),buffer->{
                buffer.writeVarInt(state.level.ordinal());buffer.writeVarInt(mode);buffer.writeVarInt(locked);
            });
    }

    // Also used by development GameTests with an isolated storage inventory.
    BackpackMenu(int id,Inventory inventory,SimpleContainer storage,ItemStack bag,InteractionHand hand) {
        this(id,inventory,new BackpackState(bag,storage),hand,null,
            hand==null?-1:hand==InteractionHand.MAIN_HAND?inventory.getSelectedSlot():40,false);
    }

    BackpackMenu(int id,Inventory inventory,BackpackState state,InteractionHand hand,PlacedBackpackEntity placed,int locked,boolean client) {
        super(BackpackContent.MENU.get(),id,3,3);
        this.state=state;this.player=inventory.player;this.hand=hand;this.placed=placed;this.lockedHotbar=locked;this.client=client;
        layout=new BackpackLayout(state.level);
        addSlot(new ResultSlot(player,craftSlots,resultSlots,0,layout.center()+28,68) {
            @Override public boolean isActive(){return view()==1 && installed(Template.CRAFTING);}
            @Override public boolean mayPickup(Player p){return isActive();}
        });
        for(int i=0;i<9;i++) {
            addSlot(new Slot(craftSlots,i,layout.center()-62+(i%3)*18,50+(i/3)*18) {
                @Override public boolean isActive(){return view()==1 && installed(Template.CRAFTING);}
                @Override public boolean mayPlace(ItemStack stack){return BackpackState.normalItem(stack);}
            });
        }
        for(int i=0;i<state.level.slots();i++) {
            addSlot(new Slot(state.storage,i,BackpackLayout.GRID_X+(i%layout.columns())*18,BackpackLayout.GRID_Y+(i/layout.columns())*18) {
                @Override public boolean isActive(){return view()==0;}
                @Override public boolean mayPlace(ItemStack stack){return BackpackState.normalItem(stack);}
            });
        }
        templatesStart=slots.size();
        for(Template template:Template.values()) {
            addSlot(new Slot(state.installed,template.ordinal(),layout.templateX(template),layout.templateY(template)) {
                @Override public boolean isActive(){return template.allowed(state.level);}
                @Override public boolean mayPlace(ItemStack stack){return isActive() && template.matches(stack);}
                @Override public int getMaxStackSize(){return 1;}
                @Override public net.minecraft.resources.Identifier getNoItemIcon(){return net.minecraft.resources.Identifier.fromNamespaceAndPath(ExampleMod.MODID,"container/slot/"+template.id());}
            });
        }
        workStart=slots.size();
        for(int i=0;i<BackpackState.WORKSHOP_SIZE;i++) {
            final int workIndex=i;
            addSlot(new Slot(state.workshop,i,layout.workshopX(i),layout.workshopY(i)) {
                private Template template(){return workIndex<9?Template.values()[1+workIndex/3]:BackpackState.foodSlotIndex(workIndex)?Template.FEEDING:workIndex==BackpackState.ARROWS?Template.ARROWS:Template.SHULKER;}
                @Override public boolean isActive(){return view()==template().ordinal()+1 && installed(template());}
                @Override public boolean mayPlace(ItemStack stack){
                    if(workIndex>=BackpackState.SHULKERS && workIndex<20)return BackpackState.shulker(stack);
                    if(!BackpackState.normalItem(stack))return false;
                    if(BackpackState.foodSlotIndex(workIndex))return stack.has(DataComponents.FOOD) && stack.has(DataComponents.CONSUMABLE);
                    if(workIndex==BackpackState.ARROWS)return stack.getItem() instanceof ArrowItem;
                    if(workIndex%3==2)return false;
                    return workIndex%3!=1 || stack.has(DataComponents.COOKING_FUEL);
                }
                @Override public int getMaxStackSize(){return workIndex>=BackpackState.SHULKERS && workIndex<20?1:super.getMaxStackSize();}
                @Override public void onTake(Player player,ItemStack stack){
                    super.onTake(player,stack);
                    if(workIndex<9 && workIndex%3==2)BackpackMachines.claimExperience(player,state,workIndex/3);
                }
            });
        }
        playerStart=slots.size();
        addStandardInventorySlots(inventory,layout.playerX(),layout.playerY());
        for(int i=playerStart;i<slots.size();i++) {
            Slot old=slots.get(i);
            if(old.getContainerSlot()!=locked)continue;
            var replacement=new Slot(old.container,old.getContainerSlot(),old.x,old.y) {
                @Override public boolean mayPickup(Player player){return false;}
                @Override public boolean mayPlace(ItemStack stack){return false;}
            };
            replacement.index=i;slots.set(i,replacement);
        }
        addDataSlots(data);
        for(int i=0;i<9;i++)craftSlots.setItem(i,state.crafting.getItem(i).copy());
        initializing=false;
        if(!client)state.viewer=this;
        recalculate();
    }

    public int view(){return data.get(0);}
    public int page(){return data.get(1);}
    public int pageCount(){return 1;}
    public BackpackLevel level(){return state.level;}
    public boolean installed(Template template){return state.active(template);}
    public int progress(int station){return data.get(2+station);}
    public int burning(int station){return data.get(5+station);}
    int storageSlotStart(){return 10;}
    int playerSlotStart(){return playerStart;}
    int templatesSlotStart(){return templatesStart;}
    int workshopSlotStart(){return workStart;}
    SimpleContainer getContainer(){return state.storage;}
    int getRowCount(){return layout.rows();}
    BackpackLayout layout(){return layout;}
    BackpackState contents(){return state;}
    boolean isPlaced(){return placed!=null;}

    @Override public Player owner(){return player;}
    @Override public boolean stillValid(Player player) {
        if(!player.isAlive() || state.bag.isEmpty())return false;
        if(client)return true;
        if(placed!=null)return !placed.isRemoved() && placed.getLevel()==player.level()
            && player.level().getBlockEntity(placed.getBlockPos())==placed && placed.backpack()==state.bag
            && player.distanceToSqr(placed.getBlockPos().getX()+0.5,placed.getBlockPos().getY()+0.5,placed.getBlockPos().getZ()+0.5)<=64;
        return (hand==null?EquippedBackpack.get(player):player.getItemInHand(hand))==state.bag;
    }
    @Override public boolean clickMenuButton(Player player,int id) {
        if(!stillValid(player))return false;
        if(id>=0 && id<=7) {
            if(id!=0 && !installed(Template.values()[id-1]))return false;
            data.set(0,id);recalculate();return true;
        }
        if(view()!=0)return false;
        if(id==100)data.set(1,Math.max(0,page()-1));
        else if(id==101)data.set(1,Math.min(pageCount()-1,page()+1));else return false;
        return true;
    }
    @Override public void clicked(int index,int button,ContainerInput input,Player player) {
        if(!stillValid(player) || index>=slots.size() || index>=0 && !slots.get(index).isActive())return;
        if(input==ContainerInput.SWAP && button==lockedHotbar)return;
        super.clicked(index,button,input,player);save();
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size() || !stillValid(player))return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if(!slot.isActive() || !slot.mayPickup(player) || !slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),before=stack.copy();
        if(index<playerStart) {
            if(!moveItemStackTo(stack,playerStart,slots.size(),true))return ItemStack.EMPTY;
            if(index==0)slot.onQuickCraft(stack,before);
        } else {
            boolean moved=false;
            for(Template template:Template.values()) {
                if(template.allowed(state.level) && template.matches(stack)) {
                    moved=moveItemStackTo(stack,templatesStart+template.ordinal(),templatesStart+template.ordinal()+1,false);break;
                }
            }
            if(!moved && view()==1 && installed(Template.CRAFTING))moved=moveItemStackTo(stack,1,10,false);
            if(!moved && view()>=2 && view()<=4 && installed(Template.values()[view()-1])) {
                int first=workStart+(view()-2)*3;
                moved=stack.has(DataComponents.COOKING_FUEL)?moveItemStackTo(stack,first+1,first+2,false):moveItemStackTo(stack,first,first+1,false);
            }
            if(!moved && view()>=5 && installed(Template.values()[view()-1])) {
                if(view()==5) {
                    for(int number=0;number<BackpackState.FOOD_SLOTS && !stack.isEmpty();number++) {
                        int first=workStart+BackpackState.foodSlot(number);
                        moved=moveItemStackTo(stack,first,first+1,false) || moved;
                    }
                } else {
                    int first=workStart+(view()==6?BackpackState.ARROWS:BackpackState.SHULKERS);
                    moved=moveItemStackTo(stack,first,view()==7?workStart+20:first+1,false);
                }
            }
            if(!moved && !moveItemStackTo(stack,10,templatesStart,false))return ItemStack.EMPTY;
        }
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(stack.getCount()==before.getCount())return ItemStack.EMPTY;
        slot.onTake(player,stack);
        if(index==0 && !stack.isEmpty())player.drop(stack,false,net.minecraft.util.Prediction.SERVER_ONLY);
        save();return before;
    }
    @Override public boolean canDragTo(Slot slot){return slot.isActive() && super.canDragTo(slot);}
    @Override public boolean canTakeItemForPickAll(ItemStack stack,Slot slot){return slot.isActive() && slot.container!=resultSlots && super.canTakeItemForPickAll(stack,slot);}
    @Override public void slotsChanged(Container container){if(!initializing){saveCraft();recalculate();}}
    private void saveCraft(){if(!client)for(int i=0;i<9;i++)state.crafting.setItem(i,craftSlots.getItem(i).copy());}
    private void recalculate() {
        if(initializing || client || !(player.level() instanceof ServerLevel world))return;
        ItemStack result=ItemStack.EMPTY;
        if(installed(Template.CRAFTING)) {
            var recipe=world.recipeAccess().getRecipeFor(RecipeType.CRAFTING,craftSlots.asCraftInput(),world);
            if(recipe.isPresent()) {
                var holder=recipe.orElseThrow();
                if(!(player instanceof ServerPlayer server) || resultSlots.setRecipeUsed(server,holder)) {
                    resultSlots.setRecipeUsed(holder);result=holder.value().assemble(craftSlots.asCraftInput());
                }
            }
        }
        resultSlots.setItem(0,result);
    }
    private void save(){
        if(client)return;
        saveCraft();
        if(view()>0 && !installed(Template.values()[view()-1]))data.set(0,0);
        recalculate();state.flush();
    }
    @Override public void broadcastChanges(){
        if(!client) {
            if(view()>0 && !installed(Template.values()[view()-1]))data.set(0,0);
            for(int i=0;i<3;i++) {
                data.set(2+i,state.cook[i]*100/Math.max(1,state.totalCook[i]));
                data.set(5+i,state.burn[i]*100/Math.max(1,state.totalBurn[i]));
            }
        }
        super.broadcastChanges();
    }
    @Override public void removed(Player player){save();if(state.viewer==this)state.viewer=null;super.removed(player);}
    @Override public Slot getResultSlot(){return slots.get(0);}
    @Override public List<Slot> getInputGridSlots(){return slots.subList(1,10);}
    @Override public RecipeBookType getRecipeBookType(){return RecipeBookType.CRAFTING;}
    @Override public RecipeBookMenu.PostPlaceAction handlePlacement(boolean maximum,boolean dropping,
        net.minecraft.world.item.crafting.RecipeHolder<?> recipe,ServerLevel world,Inventory inventory) {
        // This screen has manual crafting only. Hidden grids must never accept recipe-book packets.
        return RecipeBookMenu.PostPlaceAction.NOTHING;
    }
}
