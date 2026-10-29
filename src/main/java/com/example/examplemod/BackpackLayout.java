package com.example.examplemod;

/** One layout shared by server slots, client slots and drawing. */
final class BackpackLayout {
    static final int GRID_X=52, GRID_Y=38;
    private final BackpackLevel level;
    BackpackLayout(BackpackLevel level){this.level=level;}
    int columns(){return switch(level){case GOLD -> 21;case DIAMOND -> 15;case NETHERITE -> 13;default -> 9;};}
    int rows(){return (level.slots()+columns()-1)/columns();}
    int gridWidth(){return columns()*18;}
    int center(){return GRID_X+gridWidth()/2;}
    int playerX(){return center()-81;}
    int playerY(){return GRID_Y+Math.max(6,rows())*18+22;}
    int width(){return gridWidth()+104;}
    int height(){return playerY()+76;}
    int templateX(Template template){return template.ordinal()<4?28:GRID_X+gridWidth()+6;}
    int templateY(Template template){return GRID_Y+(template.ordinal()<4?template.ordinal():template.ordinal()-4)*24;}
    int buttonX(Template template){return template.ordinal()<4?4:GRID_X+gridWidth()+28;}
    int workshopX(int slot){
        if(slot<9)return center()+(slot%3==2?28:-44);
        if(BackpackState.foodSlotIndex(slot))return center()-35+(slot==9?0:slot-19)*18;
        if(slot==BackpackState.ARROWS)return center()-8;
        return center()-26+((slot-BackpackState.SHULKERS)%3)*18;
    }
    int workshopY(int slot){
        if(slot<9)return slot%3==1?92:56;
        if(BackpackState.foodSlotIndex(slot) || slot==BackpackState.ARROWS)return 68;
        return 50+((slot-BackpackState.SHULKERS)/3)*18;
    }
}
