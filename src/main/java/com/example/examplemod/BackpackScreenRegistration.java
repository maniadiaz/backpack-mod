package com.example.examplemod;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=ExampleMod.MODID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class BackpackScreenRegistration {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(()->MenuScreens.register(BackpackContent.MENU.get(),BackpackScreen::new));
    }
}
