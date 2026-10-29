package com.example.examplemod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MODID, value = Dist.CLIENT)
public final class BackpackControls {
    private static ItemStack creativeSnapshot = ItemStack.EMPTY;
    private static boolean creativeInventory;
    private static final KeyMapping OPEN = new KeyMapping("key.backpack_mod.open", InputConstants.KEY_B,
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ExampleMod.MODID, "backpack")));

    @SubscribeEvent public static void registerKeys(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event) { EquippedBackpack.attach(event.getPlayer()); }
    @SubscribeEvent public static void clonePlayer(ClientPlayerNetworkEvent.Clone event) { EquippedBackpack.attach(event.getNewPlayer()); }
    @SubscribeEvent public static void initScreen(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof InventoryScreen && Minecraft.getInstance().player != null) {
            EquippedBackpack.attach(Minecraft.getInstance().player);
        }
        if (Minecraft.getInstance().player != null) creativeSnapshot = EquippedBackpack.get(Minecraft.getInstance().player).copy();
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        boolean creative = client.player != null && client.gui.screen() instanceof CreativeModeInventoryScreen;
        if (creative) {
            ItemStack current = EquippedBackpack.get(client.player);
            if (creativeInventory && !ItemStack.matches(current, creativeSnapshot)) BackpackNetwork.creativeChange(current);
            creativeSnapshot = current.copy();
        }
        creativeInventory = creative;
        while (OPEN.consumeClick()) {
            if (client.player != null && client.gui.screen() == null) BackpackNetwork.requestOpen();
        }
    }

    private static void flushCreative(Screen screen) {
        var player = Minecraft.getInstance().player;
        if (!(screen instanceof CreativeModeInventoryScreen) || player == null) return;
        ItemStack current = EquippedBackpack.get(player);
        if (!ItemStack.matches(current, creativeSnapshot)) {
            BackpackNetwork.creativeChange(current);
            creativeSnapshot = current.copy();
        }
    }

    @SubscribeEvent public static void afterClick(ScreenEvent.MouseButtonPressed.Post event) { flushCreative(event.getScreen()); }
    @SubscribeEvent public static void afterRelease(ScreenEvent.MouseButtonReleased.Post event) { flushCreative(event.getScreen()); }
    @SubscribeEvent public static void afterKey(ScreenEvent.KeyPressed.Post event) { flushCreative(event.getScreen()); }
    @SubscribeEvent public static void closing(ScreenEvent.Closing event) { flushCreative(event.getScreen()); }

    private static boolean isBackpackScreen(Screen screen) {
        return screen.getTitle().getContents() instanceof TranslatableContents contents
            && contents.getKey().equals("container.backpack_mod.backpack");
    }

    @SubscribeEvent public static boolean screenMouse(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!OPEN.matchesMouse(event.getInfo()) || !isBackpackScreen(event.getScreen())) return false;
        var player = Minecraft.getInstance().player;
        if (player == null) return false;
        player.closeContainer();
        return true;
    }

    @SubscribeEvent public static boolean screenKey(ScreenEvent.KeyPressed.Pre event) {
        if (!OPEN.matches(event.getInfo())) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return false;
        if (isBackpackScreen(event.getScreen())) {
            client.player.closeContainer();
            return true;
        }
        if (event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof CreativeModeInventoryScreen) {
            flushCreative(event.getScreen());
            if (!client.player.containerMenu.getCarried().isEmpty()) {
                client.player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.backpack_mod.cursor_busy"));
                return true;
            }
            BackpackNetwork.requestOpen();
            return true;
        }
        return false;
    }

    @SubscribeEvent public static void slotBackground(ContainerScreenEvent.Render.Background event) {
        var screen = event.getContainerScreen();
        int slotX;
        int slotY;
        if (screen instanceof InventoryScreen) {
            slotX = EquippedBackpack.X;
            slotY = EquippedBackpack.Y;
        } else if (screen instanceof CreativeModeInventoryScreen) {
            var equipped = screen.getMenu().slots.stream().filter(slot -> slot.container instanceof EquippedBackpack).findFirst();
            if (equipped.isEmpty()) return; // Only show in creative's player-inventory tab.
            equipped.get().x = slotX = 144;
            equipped.get().y = slotY = 20;
        } else return;
        int x = screen.getGuiLeft() + slotX - 1;
        int y = screen.getGuiTop() + slotY - 1;
        var graphics = event.getGuiGraphics();
        graphics.fill(x, y, x + 18, y + 18, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }
}
