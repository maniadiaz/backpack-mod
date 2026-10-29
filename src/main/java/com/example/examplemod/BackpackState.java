package com.example.examplemod;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.stream.IntStream;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;

/** All portable inventories are persisted inside the same backpack stack. */
public final class BackpackState {
    private static final WeakHashMap<ItemStack, WeakReference<BackpackState>> CACHE = new WeakHashMap<>();
    public static final int FOOD = 9, ARROWS = 10, SHULKERS = 11;
    static final int WORKSHOP_SIZE = 23, FOOD_SLOTS = 4;
    // Append food slots so existing arrows and shulker boxes retain their saved indices.
    static int foodSlot(int number) { return number == 0 ? FOOD : 19 + number; }
    static boolean foodSlotIndex(int index) { return index == FOOD || index >= 20 && index < WORKSHOP_SIZE; }
    final ItemStack bag;
    final BackpackLevel level;
    final SimpleContainer storage;
    final SimpleContainer installed;
    final SimpleContainer workshop;
    final SimpleContainer crafting;
    final int[] burn = new int[3], totalBurn = new int[3], cook = new int[3], totalCook = new int[]{200,100,100};
    final float[] speed = new float[]{1,1,1}, xp = new float[3];
    final String[] recipe = new String[]{"","",""};
    BackpackMenu viewer;
    Runnable changed = () -> {};
    private boolean loading = true;

    static BackpackState of(ItemStack bag) {
        var reference = CACHE.get(bag);
        BackpackState state = reference == null ? null : reference.get();
        if (state == null) { state = new BackpackState(bag, null); CACHE.put(bag, new WeakReference<>(state)); }
        return state;
    }

    BackpackState(ItemStack bag, SimpleContainer override) {
        this.bag = bag;
        level = BackpackLevel.of(bag);
        storage = override == null ? inventory(level.slots(), DataComponents.CONTAINER) : override;
        installed = inventory(7, BackpackContent.INSTALLED.get());
        workshop = inventory(WORKSHOP_SIZE, BackpackContent.WORKSHOP.get());
        crafting = inventory(9, BackpackContent.CRAFTING.get());
        var tag = bag.getOrDefault(BackpackContent.MACHINES.get(), CustomData.EMPTY).copyTag();
        for (int i=0; i<3; i++) {
            burn[i] = tag.getIntOr("burn"+i,0); totalBurn[i] = tag.getIntOr("totalBurn"+i,0);
            cook[i] = tag.getIntOr("cook"+i,0); totalCook[i] = tag.getIntOr("totalCook"+i, i == 0 ? 200 : 100);
            speed[i] = tag.getFloatOr("speed"+i,1); xp[i] = tag.getFloatOr("xp"+i,0);
            recipe[i] = tag.getStringOr("recipe"+i, "");
        }
        loading = false;
    }

    private SimpleContainer inventory(int size, DataComponentType<ItemContainerContents> component) {
        SimpleContainer container = new SimpleContainer(size) {
            @Override public void setChanged() { if (!loading) { persist(this, component); changed.run(); } }
            @Override public ItemStack removeItemNoUpdate(int slot) { var result=super.removeItemNoUpdate(slot); setChanged(); return result; }
        };
        var saved = bag.getOrDefault(component, ItemContainerContents.EMPTY).itemCopies().toList();
        for (int i=0;i<Math.min(size,saved.size());i++) container.setItem(i,saved.get(i));
        return container;
    }

    boolean active(Template template) { return template.allowed(level) && template.matches(installed.getItem(template.ordinal())); }
    static boolean shulker(ItemStack stack) { return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof ShulkerBoxBlock; }
    static boolean normalItem(ItemStack stack) { return !(stack.getItem() instanceof BackpackItem) && !shulker(stack); }

    private void persist(SimpleContainer container, DataComponentType<ItemContainerContents> component) {
        ItemContainerContents value = ItemContainerContents.fromItems(IntStream.range(0,container.getContainerSize()).mapToObj(container::getItem).toList());
        if (!value.equals(bag.getOrDefault(component,ItemContainerContents.EMPTY))) bag.set(component,value);
    }

    void flush() {
        persist(storage,DataComponents.CONTAINER); persist(installed,BackpackContent.INSTALLED.get());
        persist(workshop,BackpackContent.WORKSHOP.get()); persist(crafting,BackpackContent.CRAFTING.get());
        CustomData.update(BackpackContent.MACHINES.get(),bag,tag -> {
            for (int i=0;i<3;i++) {
                tag.putInt("burn"+i,burn[i]);tag.putInt("totalBurn"+i,totalBurn[i]);tag.putInt("cook"+i,cook[i]);
                tag.putInt("totalCook"+i,totalCook[i]);tag.putFloat("speed"+i,speed[i]);tag.putFloat("xp"+i,xp[i]);tag.putString("recipe"+i,recipe[i]);
            }
        });
        changed.run();
    }

    static void flushIfLoaded(ItemStack stack) {
        var reference=CACHE.get(stack); var state=reference==null?null:reference.get(); if(state!=null)state.flush();
    }
}
