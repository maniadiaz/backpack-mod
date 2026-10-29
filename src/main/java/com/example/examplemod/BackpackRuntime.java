package com.example.examplemod;

import java.util.HashMap;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

final class BackpackRuntime {
    private static final HashMap<UUID,BackpackState> EQUIPPED=new HashMap<>();

    static BackpackState equipped(Player player) {
        var bag=EquippedBackpack.get(player);
        var previous=EQUIPPED.get(player.getUUID());
        if(previous!=null && previous.bag!=bag) { previous.flush(); EQUIPPED.remove(player.getUUID()); }
        if(!EquippedBackpack.has(player))return null;
        var state=BackpackState.of(bag);
        if(!player.level().isClientSide())EQUIPPED.put(player.getUUID(),state);
        return state;
    }
    private static void flush(Player player) { var state=EQUIPPED.get(player.getUUID());if(state!=null)state.flush(); }
    static void register() {
        TickEvent.PlayerTickEvent.Post.BUS.addListener(event -> {
            var player=event.player();
            if(!(player.level() instanceof ServerLevel world))return;
            var state=equipped(player);
            if(state!=null) {
                BackpackMachines.tick(world,player.blockPosition(),state);
                // Ammo is a mutable stack returned to the vanilla bow/crossbow code.
                state.workshop.setChanged();
                if(player.tickCount%20==0) { BackpackMachines.feed(player,state); state.flush(); }
            }
            if(player.containerMenu instanceof BackpackMenu menu && menu.stillValid(player) && !menu.isPlaced() && menu.contents()!=state) {
                BackpackMachines.tick(world,player.blockPosition(),menu.contents());
                if(player.tickCount%20==0)menu.contents().flush();
            }
        });
        LivingGetProjectileEvent.BUS.addListener(BackpackRuntime::projectile);
        EntityJoinLevelEvent.BUS.addListener(event -> {
            if(!event.getLevel().isClientSide() && event.getEntity() instanceof Projectile shot && shot.getOwner() instanceof Player player)flush(player);
        });
        LivingEntityUseItemEvent.Finish.BUS.addListener(event -> { if(event.getEntity() instanceof Player player && !player.level().isClientSide())flush(player); });
        LivingDeathEvent.BUS.addListener(event -> { if(event.getEntity() instanceof Player player && !player.level().isClientSide())flush(player); });
        PlayerEvent.SaveToFile.BUS.addListener(event -> flush(event.getEntity()));
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> { flush(event.getEntity());EQUIPPED.remove(event.getEntity().getUUID()); });
    }
    static void projectile(LivingGetProjectileEvent event) {
        if(!event.getProjectileItemStack().isEmpty() || !(event.getEntity() instanceof Player player))return;
        var weapon=event.getProjectileWeaponItemStack().getItem();
        if(!(weapon instanceof BowItem) && !(weapon instanceof CrossbowItem))return;
        var state=equipped(player);
        if(state!=null && state.active(Template.ARROWS) && state.workshop.getItem(BackpackState.ARROWS).getItem() instanceof ArrowItem)
            event.setProjectileItemStack(state.workshop.getItem(BackpackState.ARROWS));
    }
}
