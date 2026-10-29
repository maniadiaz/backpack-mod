package com.example.examplemod;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

public final class BackpackTemplateTests {
    static void register(DeferredRegister<Consumer<GameTestHelper>> functions) {
        functions.register("template_recipes",()->BackpackTemplateTests::recipes);
        functions.register("template_permissions",()->BackpackTemplateTests::permissions);
        functions.register("paged_inventory",()->BackpackTemplateTests::pages);
        functions.register("portable_crafting",()->BackpackTemplateTests::crafting);
        functions.register("portable_furnaces",()->BackpackTemplateTests::furnaces);
        functions.register("auto_feeding",()->BackpackTemplateTests::feeding);
        functions.register("arrow_supply",()->BackpackTemplateTests::arrows);
        functions.register("placed_backpack",()->BackpackTemplateTests::placement);
        functions.register("menu_wire_format",()->BackpackTemplateTests::wire);
        functions.register("module_clicks",()->BackpackTemplateTests::moduleClicks);
        functions.register("fuel_menu_cycles",()->BackpackTemplateTests::fuelMenuCycles);
    }
    private static Player player(GameTestHelper helper) {
        var player=helper.makeMockPlayer(GameType.SURVIVAL);
        var pos=helper.absolutePos(new BlockPos(1,1,1));player.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);return player;
    }
    private static BackpackState equipped(Player player,BackpackLevel level) {
        player.setItemSlot(EquipmentSlot.BODY,new ItemStack(ExampleMod.backpack(level)));
        return BackpackRuntime.equipped(player);
    }
    private static void install(BackpackState state,Template... templates) {
        for(Template template:templates)state.installed.setItem(template.ordinal(),new ItemStack(template.item()));
    }
    private static BackpackMenu menu(Player player,BackpackState state) {
        return new BackpackMenu(5,player.getInventory(),state,null,null,-1,false);
    }
    public static void moduleClicks(GameTestHelper helper) {
        var player=player(helper);var state=equipped(player,BackpackLevel.NETHERITE);
        install(state,Template.values());var menu=menu(player,state);
        for(int station=0;station<3;station++) {
            menu.clickMenuButton(player,station+2);
            long visible=menu.slots.stream().filter(slot->slot.container==state.workshop && slot.isActive()).count();
            helper.assertTrue(visible==3,"Cooking view must expose exactly input, fuel and output; got "+visible);
            int base=menu.workshopSlotStart()+station*3;
            Item input=station==2?Items.BEEF:Items.RAW_IRON;
            menu.setCarried(new ItemStack(input));menu.clicked(base,0,ContainerInput.PICKUP,player);
            helper.assertTrue(state.workshop.getItem(station*3).is(input),"Cooking input rejected a menu click");
            menu.setCarried(new ItemStack(Items.COAL));menu.clicked(base+1,0,ContainerInput.PICKUP,player);
            helper.assertTrue(state.workshop.getItem(station*3+1).is(Items.COAL),"Fuel rejected a menu click");
        }
        menu.clickMenuButton(player,5);
        helper.assertTrue(menu.slots.stream().filter(slot->slot.container==state.workshop && slot.isActive()).count()==4,"Food view must expose four slots");
        for(int number=0;number<BackpackState.FOOD_SLOTS;number++) {
            int food=BackpackState.foodSlot(number);
            menu.setCarried(new ItemStack(Items.APPLE,64));menu.clicked(menu.workshopSlotStart()+food,0,ContainerInput.PICKUP,player);
            helper.assertTrue(state.workshop.getItem(food).getCount()==64 && menu.getCarried().isEmpty(),"Food slot rejected a full stack: "+number);
            helper.assertTrue(!menu.slots.get(menu.workshopSlotStart()+food).mayPlace(new ItemStack(Items.STONE)),"Food slot accepts stone");
        }
        menu.clickMenuButton(player,6);
        helper.assertTrue(menu.slots.get(menu.workshopSlotStart()+BackpackState.ARROWS).isActive(),"Arrow slot is hidden");
        menu.setCarried(new ItemStack(Items.ARROW,64));menu.clicked(menu.workshopSlotStart()+BackpackState.ARROWS,0,ContainerInput.PICKUP,player);
        helper.assertTrue(state.workshop.getItem(BackpackState.ARROWS).getCount()==64,"Arrow slot rejected ammunition");
        menu.clickMenuButton(player,7);
        helper.assertTrue(menu.slots.stream().filter(slot->slot.container==state.workshop && slot.isActive()).count()==9,"Shulker view has extra/missing slots");
        for(int view=0;view<=7;view++) {
            menu.clickMenuButton(player,view);
            var positions=new java.util.HashSet<String>();
            for(var slot:menu.slots)if(slot.isActive()) {
                helper.assertTrue(positions.add(slot.x+":"+slot.y),"Visible slots overlap in view "+view);
                helper.assertTrue(slot.x>=0 && slot.x+16<=menu.layout().width() && slot.y>=0 && slot.y+16<=menu.layout().height(),"Slot lies outside menu bounds");
            }
        }
        menu.removed(player);helper.succeed();
    }
    public static void fuelMenuCycles(GameTestHelper helper) {
        Item[] fuels={Items.COAL,Items.CHARCOAL,Items.OAK_PLANKS,Items.LAVA_BUCKET};
        for(int station=0;station<3;station++)for(Item fuel:fuels) {
            var player=player(helper);var state=equipped(player,BackpackLevel.IRON);
            install(state,Template.values()[station+1]);var menu=menu(player,state);menu.clickMenuButton(player,station+2);
            int base=menu.workshopSlotStart()+station*3;
            menu.setCarried(new ItemStack(station==2?Items.BEEF:Items.RAW_IRON));menu.clicked(base,0,ContainerInput.PICKUP,player);
            for(int tick=0;tick<220;tick++)BackpackMachines.tick(helper.getLevel(),player.blockPosition(),state);
            helper.assertTrue(state.workshop.getItem(station*3+2).isEmpty(),"Menu furnace cooks without fuel");
            menu.setCarried(new ItemStack(Items.STONE));menu.clicked(base+1,0,ContainerInput.PICKUP,player);
            helper.assertTrue(state.workshop.getItem(station*3+1).isEmpty(),"Fuel slot accepts stone");
            menu.setCarried(new ItemStack(fuel));menu.clicked(base+1,0,ContainerInput.PICKUP,player);
            helper.assertTrue(menu.getCarried().isEmpty(),"Menu rejects valid fuel: "+fuel);
            for(int tick=0;tick<220;tick++)BackpackMachines.tick(helper.getLevel(),player.blockPosition(),state);
            menu.clicked(base+2,0,ContainerInput.PICKUP,player);
            helper.assertTrue(menu.getCarried().is(station==2?Items.COOKED_BEEF:Items.IRON_INGOT),"Cannot cook and collect from menu: "+station+" / "+fuel);
            if(fuel==Items.LAVA_BUCKET)helper.assertTrue(state.workshop.getItem(station*3+1).is(Items.BUCKET),"Lava lost its bucket");
            menu.setCarried(ItemStack.EMPTY);menu.removed(player);
        }
        helper.succeed();
    }
    public static void recipes(GameTestHelper helper) {
        Item[] centers={Items.CRAFTING_TABLE,Items.FURNACE,Items.BLAST_FURNACE,Items.SMOKER,Items.WOODEN_HOE,Items.ARROW,Items.ENDER_CHEST};
        for(Template template:Template.values()) {
            var grid=NonNullList.withSize(9,ItemStack.EMPTY);
            for(int i=0;i<9;i++)grid.set(i,new ItemStack(i==4?centers[template.ordinal()]:i%2==0?Items.OAK_PLANKS:Items.BIRCH_PLANKS));
            var input=CraftingInput.of(3,3,grid);
            var found=helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING,input,helper.getLevel());
            helper.assertTrue(found.isPresent(),"Missing template recipe: "+template);
            ItemStack result=found.orElseThrow().value().assemble(input);
            helper.assertTrue(result.is(template.item()) && result.getCount()==1,"Wrong template output: "+template);
            grid.set(0,ItemStack.EMPTY);
            helper.assertTrue(!found.orElseThrow().value().matches(CraftingInput.of(3,3,grid),helper.getLevel()),"Incomplete wood ring accepted");
        }
        helper.succeed();
    }
    public static void wire(GameTestHelper helper) {
        for(BackpackLevel level:BackpackLevel.values()) {
            var player=player(helper);var state=equipped(player,level);install(state,Template.CRAFTING);
            var server=menu(player,state);
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                buffer.writeVarInt(level.ordinal());buffer.writeVarInt(2);buffer.writeVarInt(-1);
                var client=BackpackMenu.client(5,player.getInventory(),buffer);
                helper.assertTrue(server.slots.size()==client.slots.size() && server.pageCount()==client.pageCount(),"Client/server menu layouts differ: "+level);
                for(int i=0;i<server.slots.size();i++) {
                    var a=server.slots.get(i);var b=client.slots.get(i);
                    helper.assertTrue(a.x==b.x && a.y==b.y,"Client/server slot positions differ");
                    b.set(a.getItem().copy());
                }
                helper.assertTrue(client.installed(Template.CRAFTING),"Installed templates do not synchronize to the client menu");
                server.removed(player);
            } finally { buffer.release(); }
        }
        helper.succeed();
    }
    public static void permissions(GameTestHelper helper) {
        for(BackpackLevel level:BackpackLevel.values()) {
            var player=player(helper);var state=equipped(player,level);var menu=menu(player,state);
            for(Template template:Template.values()) {
                var slot=menu.slots.get(menu.templatesSlotStart()+template.ordinal());
                helper.assertTrue(slot.mayPlace(new ItemStack(template.item()))==template.allowed(level),"Wrong template level gate: "+level+" / "+template);
                helper.assertTrue(!slot.mayPlace(new ItemStack(Items.DIAMOND)),"Non-template accepted in template panel");
                menu.setCarried(new ItemStack(template.item()));
                menu.clicked(slot.index,0,ContainerInput.PICKUP,player);
                helper.assertTrue(state.active(template)==template.allowed(level),"Installing template bypassed level gate");
                helper.assertTrue(menu.clickMenuButton(player,template.ordinal()+1)==template.allowed(level),"Opening template bypassed level gate");
                menu.setCarried(ItemStack.EMPTY);
            }
            if(level==BackpackLevel.NETHERITE) {
                helper.assertTrue(menu.slots.get(menu.workshopSlotStart()+11).mayPlace(new ItemStack(Items.SHULKER_BOX)),"Shulker compartment rejects boxes");
                for(int i=11;i<20;i++) {
                    var slot=menu.slots.get(menu.workshopSlotStart()+i);
                    helper.assertTrue(slot.getMaxStackSize()==1,"Shulker compartment can stack boxes");
                    helper.assertTrue(!slot.mayPlace(new ItemStack(Items.DIAMOND)),"Shulker compartment accepts other items");
                    state.workshop.setItem(i,new ItemStack(Items.SHULKER_BOX));
                }
            }
            helper.assertTrue(!menu.slots.get(menu.storageSlotStart()).mayPlace(new ItemStack(Items.SHULKER_BOX)),"Normal storage bypasses the nine-shulker limit");
            state.workshop.setItem(BackpackState.FOOD,new ItemStack(Items.APPLE,8));
            menu.removed(player);
            var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,helper.getLevel().registryAccess());
            var copy=ItemStack.CODEC.parse(ops,ItemStack.CODEC.encodeStart(ops,state.bag).getOrThrow()).getOrThrow();
            var restored=new BackpackState(copy,null);
            helper.assertTrue(restored.workshop.getItem(BackpackState.FOOD).getCount()==8,"Template contents did not persist");
            for(Template template:Template.values())helper.assertTrue(restored.active(template)==template.allowed(level),"Installed template did not persist");
            if(level!=BackpackLevel.NETHERITE) {
                ItemStack upgraded=copy.transmuteCopy(ExampleMod.backpack(BackpackLevel.values()[level.ordinal()+1]),1);
                var next=new BackpackState(upgraded,null);
                helper.assertTrue(next.workshop.getItem(BackpackState.FOOD).getCount()==8,"Upgrade lost module contents");
                helper.assertTrue(next.active(Template.CRAFTING),"Upgrade lost installed template");
            }
        }
        helper.succeed();
    }
    public static void pages(GameTestHelper helper) {
        int[] capacities={9,18,27,63,90,117};
        for(BackpackLevel level:BackpackLevel.values()) {
            var player=player(helper);var state=equipped(player,level);var menu=menu(player,state);
            helper.assertTrue(level.slots()==capacities[level.ordinal()],"Wrong confirmed capacity: "+level);
            helper.assertTrue(menu.slots.stream().filter(slot->slot.container==state.storage && slot.isActive()).count()==level.slots(),"Storage contains missing/extra active slots");
            int[] columns={9,9,9,21,15,13};
            helper.assertTrue(menu.layout().columns()==columns[level.ordinal()] && menu.layout().rows()*menu.layout().columns()==level.slots(),"Grid must have complete rows and the confirmed dimensions");
            var points=new java.util.HashSet<String>();
            for(int i=0;i<level.slots();i++) {
                var slot=menu.slots.get(menu.storageSlotStart()+i);
                helper.assertTrue(points.add(slot.x+":"+slot.y),"Storage slots overlap: "+level);
                helper.assertTrue(slot.x==BackpackLayout.GRID_X+(i%menu.layout().columns())*18 && slot.y==BackpackLayout.GRID_Y+(i/menu.layout().columns())*18,"Storage is not a regular rectangle");
            }
            if(menu.pageCount()>1) {
                int hidden=menu.storageSlotStart()+54;
                menu.setCarried(new ItemStack(Items.DIAMOND));menu.clicked(hidden,0,ContainerInput.PICKUP,player);
                helper.assertTrue(state.storage.getItem(54).isEmpty(),"Hidden-page slot accepted a click");menu.setCarried(ItemStack.EMPTY);
            }
            for(int i=1;i<menu.pageCount();i++)helper.assertTrue(menu.clickMenuButton(player,101),"Cannot go to next storage page");
            helper.assertTrue(menu.page()==menu.pageCount()-1,"Wrong final page");
            int last=menu.storageSlotStart()+level.slots()-1;
            helper.assertTrue(menu.slots.get(last).isActive(),"Final storage slot is inaccessible");
            menu.setCarried(new ItemStack(Items.DIAMOND,19));menu.clicked(last,0,ContainerInput.PICKUP,player);menu.removed(player);
            var saved=state.bag.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).itemCopies().toList();
            helper.assertTrue(saved.get(level.slots()-1).getCount()==19,"Last-page contents were not saved");
        }
        helper.succeed();
    }
    public static void crafting(GameTestHelper helper) {
        var player=player(helper);var state=equipped(player,BackpackLevel.LEATHER);install(state,Template.CRAFTING);
        var menu=menu(player,state);menu.clickMenuButton(player,1);
        menu.slots.get(1).setByPlayer(new ItemStack(Items.OAK_LOG));
        helper.assertTrue(menu.getResultSlot().getItem().is(Items.OAK_PLANKS),"Portable crafting has no recipe result");
        menu.clicked(0,0,ContainerInput.PICKUP,player);
        helper.assertTrue(menu.getCarried().is(Items.OAK_PLANKS) && menu.getCarried().getCount()==4,"Wrong crafted stack");
        helper.assertTrue(menu.slots.get(1).getItem().isEmpty(),"Crafting did not consume its ingredient");
        menu.setCarried(ItemStack.EMPTY);menu.slots.get(1).setByPlayer(new ItemStack(Items.COBBLESTONE,7));
        state.installed.setItem(Template.CRAFTING.ordinal(),ItemStack.EMPTY);menu.removed(player);
        var restored=new BackpackState(state.bag.copy(),null);
        helper.assertTrue(restored.crafting.getItem(0).getCount()==7,"Removing crafting template lost its stored inputs");
        helper.succeed();
    }
    public static void furnaces(GameTestHelper helper) {
        var player=player(helper);var state=equipped(player,BackpackLevel.IRON);
        install(state,Template.FURNACE,Template.BLAST_FURNACE,Template.SMOKER);
        Item[] inputs={Items.RAW_IRON,Items.IRON_ORE,Items.BEEF},outputs={Items.IRON_INGOT,Items.IRON_INGOT,Items.COOKED_BEEF};
        for(int i=0;i<3;i++) {state.workshop.setItem(i*3,new ItemStack(inputs[i]));state.workshop.setItem(i*3+1,new ItemStack(Items.COAL));}
        for(int tick=0;tick<230;tick++)BackpackMachines.tick(helper.getLevel(),player.blockPosition(),state);
        for(int i=0;i<3;i++) {
            helper.assertTrue(state.workshop.getItem(i*3+2).is(outputs[i]),"Cooking template failed: "+i);
            helper.assertTrue(state.workshop.getItem(i*3).isEmpty() && state.workshop.getItem(i*3+1).isEmpty(),"Cooking did not consume input and fuel: "+i);
        }
        var empty=new BackpackState(new ItemStack(ExampleMod.IRON_BACKPACK.get()),null);install(empty,Template.FURNACE);
        empty.workshop.setItem(0,new ItemStack(Items.RAW_GOLD));
        for(int tick=0;tick<210;tick++)BackpackMachines.tick(helper.getLevel(),player.blockPosition(),empty);
        helper.assertTrue(empty.workshop.getItem(2).isEmpty() && empty.workshop.getItem(0).getCount()==1,"Furnace cooked without fuel");
        empty.workshop.setItem(1,new ItemStack(Items.LAVA_BUCKET));BackpackMachines.tick(helper.getLevel(),player.blockPosition(),empty);
        helper.assertTrue(empty.workshop.getItem(1).is(Items.BUCKET),"Fuel container remainder was lost");
        empty.flush();var restored=new BackpackState(empty.bag.copy(),null);
        helper.assertTrue(restored.burn[0]==empty.burn[0] && restored.cook[0]==empty.cook[0],"Cooking progress did not persist");
        helper.succeed();
    }
    public static void feeding(GameTestHelper helper) {
        var player=player(helper);var state=equipped(player,BackpackLevel.GOLD);install(state,Template.FEEDING);
        state.workshop.setItem(BackpackState.FOOD,new ItemStack(Items.APPLE,3));player.getFoodData().setFoodLevel(10);
        int nutrition=state.workshop.getItem(BackpackState.FOOD).get(DataComponents.FOOD).nutrition();
        helper.assertTrue(BackpackMachines.feed(player,state),"Feeding template did not consume food");
        helper.assertTrue(player.getFoodData().getFoodLevel()==10+nutrition && state.workshop.getItem(BackpackState.FOOD).getCount()==2,"Feeding did not restore hunger or consumed the wrong amount");
        state.workshop.setItem(BackpackState.FOOD,ItemStack.EMPTY);
        state.workshop.setItem(BackpackState.foodSlot(3),new ItemStack(Items.APPLE,4));
        player.getFoodData().setFoodLevel(10);
        helper.assertTrue(BackpackMachines.feed(player,state) && state.workshop.getItem(BackpackState.foodSlot(3)).getCount()==3,"Feeding cannot use the fourth food slot");
        var restoredFood=new BackpackState(state.bag.copy(),null);
        helper.assertTrue(restoredFood.workshop.getItem(BackpackState.foodSlot(3)).getCount()==3,"Additional food slot did not persist");
        state.workshop.setItem(BackpackState.FOOD,new ItemStack(Items.APPLE,2));
        player.getFoodData().setFoodLevel(20);
        helper.assertTrue(!BackpackMachines.feed(player,state) && state.workshop.getItem(BackpackState.FOOD).getCount()==2,"Feeding wastes food at full hunger");
        player.getFoodData().setFoodLevel(10);state.installed.setItem(Template.FEEDING.ordinal(),ItemStack.EMPTY);
        helper.assertTrue(!BackpackMachines.feed(player,state),"Feeding works without its template");
        helper.succeed();
    }
    public static void arrows(GameTestHelper helper) {
        var player=player(helper);var state=equipped(player,BackpackLevel.DIAMOND);install(state,Template.ARROWS);
        state.workshop.setItem(BackpackState.ARROWS,new ItemStack(Items.ARROW,7));
        var bow=new ItemStack(Items.BOW);player.setItemInHand(InteractionHand.MAIN_HAND,bow);
        helper.assertTrue(player.getProjectile(bow)==state.workshop.getItem(BackpackState.ARROWS),"Bow cannot find backpack ammunition");
        var item=(BowItem)bow.getItem();
        helper.assertTrue(item.releaseUsing(bow,helper.getLevel(),player,item.getUseDuration(bow,player)-20),"Bow did not shoot with backpack ammunition");
        helper.assertTrue(state.workshop.getItem(BackpackState.ARROWS).getCount()==6,"Bow consumed the wrong number of backpack arrows");
        var saved=state.bag.getOrDefault(BackpackContent.WORKSHOP.get(),ItemContainerContents.EMPTY).itemCopies().toList();
        helper.assertTrue(saved.get(BackpackState.ARROWS).getCount()==6,"Fired ammunition did not persist immediately");
        state.installed.setItem(Template.ARROWS.ordinal(),ItemStack.EMPTY);
        helper.assertTrue(player.getProjectile(bow).isEmpty(),"Arrow supply works without its template");
        helper.succeed();
    }
    public static void placement(GameTestHelper helper) {
        var world=helper.getLevel();var player=player(helper);var state=equipped(player,BackpackLevel.NETHERITE);
        install(state,Template.CRAFTING,Template.SHULKER);state.storage.setItem(116,new ItemStack(Items.DIAMOND,23));
        state.workshop.setItem(BackpackState.SHULKERS,new ItemStack(Items.SHULKER_BOX));state.flush();
        ItemStack expected=state.bag.copy();player.setItemSlot(EquipmentSlot.BODY,ItemStack.EMPTY);player.setItemInHand(InteractionHand.MAIN_HAND,state.bag);
        var support=helper.absolutePos(new BlockPos(1,0,1));var pos=support.above();
        var hit=new BlockHitResult(Vec3.atCenterOf(support).add(0,0.5,0),Direction.UP,support,false);
        world.setBlock(support,Blocks.AIR.defaultBlockState(),3);
        helper.assertTrue(PlacedBackpackBlock.place(new UseOnContext(player,InteractionHand.MAIN_HAND,hit))==net.minecraft.world.InteractionResult.FAIL,"Backpack can be placed without solid support");
        world.setBlock(support,Blocks.STONE.defaultBlockState(),3);
        helper.assertTrue(PlacedBackpackBlock.place(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"Backpack cannot be placed on stone");
        helper.assertTrue(player.getMainHandItem().isEmpty(),"Placing a backpack did not move it out of the hand");
        var entity=(PlacedBackpackEntity)world.getBlockEntity(pos);
        helper.assertTrue(ItemStack.matches(expected,entity.backpack()),"Placing lost backpack data");
        var open=new BackpackMenu(7,player.getInventory(),entity.contents(),null,entity,-1,false);
        player.containerMenu=open;
        helper.assertTrue(open.stillValid(player),"Placed backpack menu is invalid next to its owner");
        player.setShiftKeyDown(true);
        world.getBlockState(pos).useWithoutItem(world,player,new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false));
        helper.assertTrue(world.getBlockEntity(pos)==entity,"Backpack can be picked up while its menu is in use");
        player.setShiftKeyDown(false);open.removed(player);player.containerMenu=player.inventoryMenu;
        var restored=new PlacedBackpackEntity(pos,world.getBlockState(pos));
        restored.loadWithComponents(TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,world.registryAccess(),entity.saveWithoutMetadata(world.registryAccess())));
        helper.assertTrue(ItemStack.matches(expected,restored.backpack()),"Placed backpack save/load lost data");
        player.setShiftKeyDown(true);
        world.getBlockState(pos).useWithoutItem(world,player,new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false));
        helper.assertTrue(world.getBlockState(pos).isAir() && ItemStack.matches(expected,player.getMainHandItem()),"Picking up placed backpack lost contents or duplicated it");
        player.setShiftKeyDown(false);PlacedBackpackBlock.place(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
        var area=new net.minecraft.world.phys.AABB(pos).inflate(3);
        int before=world.getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().getItem() instanceof BackpackItem).size();
        world.setBlock(support,Blocks.AIR.defaultBlockState(),3);
        var drops=world.getEntitiesOfClass(ItemEntity.class,area,e->e.getItem().getItem() instanceof BackpackItem);
        helper.assertTrue(world.getBlockState(pos).isAir() && drops.size()==before+1,"Breaking support must drop exactly one backpack");
        helper.assertTrue(drops.stream().anyMatch(e->ItemStack.matches(e.getItem(),expected)),"Dropped backpack lost stored contents");
        helper.succeed();
    }
}
