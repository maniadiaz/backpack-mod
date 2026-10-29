package com.example.examplemod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
    private final Button[] tabs=new Button[8];
    private final BackpackLayout layout;
    private float fit=1;
    public BackpackScreen(BackpackMenu menu,Inventory inventory,Component title) {
        super(menu,inventory,title,menu.layout().width(),menu.layout().height());
        layout=menu.layout();inventoryLabelX=layout.playerX();inventoryLabelY=layout.playerY()-12;
    }
    @Override protected void init() {
        // Scale this screen only; retain the player's global GUI scale preference.
        width=minecraft.getWindow().getGuiScaledWidth();height=minecraft.getWindow().getGuiScaledHeight();
        fit=Math.min(1F,Math.min((float)width/(imageWidth+8),(float)height/(imageHeight+8)));
        width=Math.round(width/fit);height=Math.round(height/fit);
        super.init();
        tabs[0]=addRenderableWidget(Button.builder(Component.translatable("screen.backpack_mod.storage"),button->send(0))
            .bounds(leftPos+layout.center()-28,topPos+18,56,16).build());
        String[] labels={"C","H","A","S","Co","Fl","Sh"};
        for(Template template:Template.values()) {
            int view=template.ordinal()+1;
            tabs[view]=addRenderableWidget(Button.builder(Component.literal(labels[template.ordinal()]),button->send(view))
                .bounds(leftPos+layout.buttonX(template),topPos+layout.templateY(template)-1,20,18)
                .tooltip(Tooltip.create(Component.translatable("item.backpack_mod."+template.id()))).build());
        }
        updateButtons();
    }
    private void send(int action) { if(minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,action); }
    private void updateButtons() {
        for(int i=0;i<8;i++)tabs[i].active=(i==0 || menu.installed(Template.values()[i-1])) && i!=menu.view();
    }
    @Override protected void containerTick(){super.containerTick();updateButtons();}
    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick) {
        graphics.pose().pushMatrix();graphics.pose().scale(fit,fit);
        super.extractRenderState(graphics,(int)(mouseX/fit),(int)(mouseY/fit),partialTick);
        graphics.pose().popMatrix();
    }
    private MouseButtonEvent scaled(MouseButtonEvent event){return new MouseButtonEvent(event.x()/fit,event.y()/fit,event.buttonInfo());}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){return super.mouseClicked(scaled(event),doubleClick);}
    @Override public boolean mouseReleased(MouseButtonEvent event){return super.mouseReleased(scaled(event));}
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){return super.mouseDragged(scaled(event),dx/fit,dy/fit);}
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy){return super.mouseScrolled(x/fit,y/fit,dx,dy);}
    @Override public void mouseMoved(double x,double y){super.mouseMoved(x/fit,y/fit);}
    @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick) {
        graphics.pose().pushMatrix();graphics.pose().scale(fit,fit);
        drawSlotBackground(graphics);
        graphics.pose().popMatrix();
    }
    private void drawSlotBackground(GuiGraphicsExtractor graphics) {
        // No panel, dimming or blur: only individual slot frames cover the world.
        for(var slot:menu.slots)if(slot.isActive())slotFrame(graphics,leftPos+slot.x-1,topPos+slot.y-1,false);
        for(Template template:Template.values())if(!template.allowed(menu.level())) {
            int x=leftPos+layout.templateX(template),y=topPos+layout.templateY(template);
            slotFrame(graphics,x-1,y-1,true);
            graphics.text(font,Component.literal("×"),x+5,y+4,0xFFBBBBBB,true);
        }
        int center=leftPos+layout.center(),y=topPos;
        if(menu.view()>=2 && menu.view()<=4) {
            int station=menu.view()-2;
            graphics.fill(center-10,y+62,center+18,y+70,0x88505050);
            graphics.fill(center-10,y+62,center-10+28*menu.progress(station)/100,y+70,0xFFE3B84A);
            graphics.fill(center-42,y+76,center-30,y+89,0x88505050);
            int height=13*menu.burning(station)/100;
            graphics.fill(center-42,y+89-height,center-30,y+89,0xFFDF7635);
            graphics.text(font,Component.translatable("screen.backpack_mod.input"),center-72,y+44,0xFFFFFFFF,true);
            graphics.text(font,Component.translatable("screen.backpack_mod.fuel"),center-72,y+114,0xFFFFFFFF,true);
            graphics.text(font,Component.translatable("screen.backpack_mod.output"),center+20,y+44,0xFFFFFFFF,true);
        }
        if(menu.view()==1)graphics.text(font,Component.literal("→"),center+8,y+72,0xFFFFFFFF,true);
    }
    private static void slotFrame(GuiGraphicsExtractor graphics,int x,int y,boolean blocked) {
        graphics.fill(x,y,x+18,y+18,blocked?0x80505050:0xB0CCCCCC);
        graphics.fill(x+1,y+1,x+17,y+17,blocked?0xB0202020:0xB0353535);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics,int mouseX,int mouseY) {
        graphics.text(font,Component.translatable("screen.backpack_mod.title",menu.level().ordinal(),menu.level().slots()),BackpackLayout.GRID_X,5,0xFFFFFFFF,true);
        graphics.text(font,playerInventoryTitle,layout.playerX(),layout.playerY()-12,0xFFFFFFFF,true);
        if(menu.view()!=0) {
            graphics.text(font,Component.translatable("screen.backpack_mod.module."+menu.view()),BackpackLayout.GRID_X,26,0xFFFFFFFF,true);
            if(menu.view()>=5)graphics.text(font,Component.translatable("screen.backpack_mod.hint."+menu.view()),BackpackLayout.GRID_X,132,0xFFFFFFFF,true);
        }
    }
    @Override protected void extractTooltip(GuiGraphicsExtractor graphics,int mouseX,int mouseY) {
        super.extractTooltip(graphics,(int)(mouseX*fit),(int)(mouseY*fit));
        for(Template template:Template.values()) {
            int x=leftPos+layout.templateX(template),y=topPos+layout.templateY(template);
            if(mouseX>=x && mouseX<x+16 && mouseY>=y && mouseY<y+16 && !menu.installed(template))
                graphics.setTooltipForNextFrame(Component.translatable("screen.backpack_mod.template_slot",
                    Component.translatable("item.backpack_mod."+template.id()),template.minimumLevel()),(int)(mouseX*fit),(int)(mouseY*fit));
        }
    }
}
