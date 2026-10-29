package com.example.examplemod;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** BODY is saved/synchronized by vanilla and is independent of the chest armor slot. */
public final class EquippedBackpack implements Container {
    public static final int X = 26;
    public static final int Y = 62;
    private final Player player;

    private EquippedBackpack(Player player) { this.player = player; }

    public static ItemStack get(Player player) { return player.getItemBySlot(EquipmentSlot.BODY); }
    public static boolean has(Player player) { return get(player).getItem() instanceof BackpackItem; }

    static void applyCreativeChange(Player player, ItemStack stack) {
        // Mirrors vanilla creative inventory editing; survival clients cannot supply item data.
        if (!player.hasInfiniteMaterials() || !player.isAlive()) return;
        if (!stack.isEmpty() && (!(stack.getItem() instanceof BackpackItem) || stack.getCount() != 1)) return;
        player.setItemSlot(EquipmentSlot.BODY, stack.copy());
        player.inventoryMenu.broadcastChanges();
    }

    public static void attach(Player player) {
        if (player.inventoryMenu.slots.stream().anyMatch(slot -> slot.container instanceof EquippedBackpack)) return;
        player.inventoryMenu.addSlot(new Slot(new EquippedBackpack(player), 0, X, Y) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof BackpackItem; }
            @Override public int getMaxStackSize() { return 1; }
            @Override public Identifier getNoItemIcon() {
                return Identifier.fromNamespaceAndPath(ExampleMod.MODID, "container/slot/backpack");
            }
        });
    }

    public static void register() {
        EntityJoinLevelEvent.BUS.addListener(event -> { if (event.getEntity() instanceof Player player) attach(player); });
        PlayerEvent.LoadFromFile.BUS.addListener(event -> attach(event.getEntity()));
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> attach(event.getEntity()));
        PlayerEvent.Clone.BUS.addListener(event -> {
            Player player = event.getEntity();
            BackpackState.flushIfLoaded(get(event.getOriginal()));
            attach(player);
            if (!event.isWasDeath() || player.level() instanceof ServerLevel level && level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
                player.setItemSlot(EquipmentSlot.BODY, get(event.getOriginal()).copy());
            }
        });
    }

    public static void open(Player player) {
        if (player.isSpectator() || !player.isAlive()) return;
        if (player.containerMenu instanceof BackpackMenu) return;
        if (!player.containerMenu.getCarried().isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.backpack_mod.cursor_busy"));
            return;
        }
        if (!has(player)) {
            player.sendOverlayMessage(Component.translatable("message.backpack_mod.no_backpack"));
            return;
        }
        BackpackItem.open(player, get(player), null);
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return get(player).isEmpty(); }
    @Override public ItemStack getItem(int slot) { return slot == 0 ? get(player) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int count) {
        BackpackState.flushIfLoaded(get(player));
        ItemStack removed = getItem(slot).split(count);
        if (slot == 0 && get(player).isEmpty()) player.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
        setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        BackpackState.flushIfLoaded(get(player));
        ItemStack previous = getItem(slot);
        if (slot == 0) player.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
        return previous;
    }
    @Override public void setItem(int slot, ItemStack stack) { if (slot == 0) player.setItemSlot(EquipmentSlot.BODY, stack); }
    @Override public void setChanged() { player.getInventory().setChanged(); }
    @Override public boolean stillValid(Player player) { return this.player == player && player.isAlive(); }
    @Override public void clearContent() { player.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY); }
}
