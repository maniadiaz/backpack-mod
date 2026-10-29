package com.example.examplemod;

import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public final class BackpackNetwork {
    private static final SimpleChannel CHANNEL = ChannelBuilder.named("backpack_mod:equipment").networkProtocolVersion(3).simpleChannel();
    private record Open() {}
    private record CreativeChange(ItemStack stack) {}

    public static void register() {
        CHANNEL.messageBuilder(Open.class, NetworkDirection.PLAY_TO_SERVER)
            .encoder((message, buffer) -> {})
            .decoder(buffer -> new Open())
            .consumerMainThread((message, context) -> {
                var player = context.getSender();
                if (player != null) EquippedBackpack.open(player);
            }).add();
        CHANNEL.<CreativeChange, RegistryFriendlyByteBuf>messageBuilder(CreativeChange.class, NetworkDirection.PLAY_TO_SERVER)
            .encoder((message, buffer) -> ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, message.stack()))
            .decoder(buffer -> new CreativeChange(ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer)))
            .consumerMainThread((message, context) -> {
                var player = context.getSender();
                if (player != null) EquippedBackpack.applyCreativeChange(player, message.stack());
            }).add();
        CHANNEL.build();
    }

    public static void requestOpen() { CHANNEL.send(new Open(), PacketDistributor.SERVER.noArg()); }
    public static void creativeChange(ItemStack stack) { CHANNEL.send(new CreativeChange(stack.copy()), PacketDistributor.SERVER.noArg()); }
}
