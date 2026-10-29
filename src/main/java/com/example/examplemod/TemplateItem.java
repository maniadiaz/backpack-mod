package com.example.examplemod;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

final class TemplateItem extends Item {
    private final Template template;
    TemplateItem(Properties properties,Template template){super(properties);this.template=template;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,TooltipDisplay display,Consumer<Component> output,TooltipFlag flag){
        output.accept(Component.translatable("item.backpack_mod.template_hint",template.minimumLevel()).withStyle(ChatFormatting.GRAY));
    }
}
