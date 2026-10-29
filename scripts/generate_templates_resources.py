"""Recipes and original pixel art for the portable workshop expansion."""
from pathlib import Path
import json
from generate_backpack_assets import ROOT, png, rect, DARK, LEATHER, LIGHT, GOLD
PROJECT=ROOT.parents[4]
DATA=ROOT.parent.parent / "data/backpack_mod"
def write(path, data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
levels=["backpack","stone_backpack","iron_backpack","gold_backpack","diamond_backpack","netherite_backpack"]
materials=["stone","iron_ingot","gold_ingot","diamond","netherite_ingot"]
for i,material in enumerate(materials,1):
    write(DATA/"recipe"/(levels[i]+".json"), {"type":"backpack_mod:backpack_upgrade","category":"equipment","pattern":[" M ","MBM"," M "],"key":{"M":"minecraft:"+material,"B":"backpack_mod:"+levels[i-1]},"result":{"id":"backpack_mod:"+levels[i],"count":1}})
templates=[
    ("crafting_template","mesa de crafteo","Crafting","minecraft:crafting_table",0,(157,100,54,255)),
    ("furnace_template","horno","Furnace","minecraft:furnace",1,(115,119,124,255)),
    ("blast_furnace_template","alto horno","Blast Furnace","minecraft:blast_furnace",2,(192,199,203,255)),
    ("smoker_template","ahumador","Smoker","minecraft:smoker",2,(106,80,53,255)),
    ("feeding_template","comida","Feeding","#minecraft:hoes",3,(199,65,49,255)),
    ("arrow_template","flechas","Arrow","minecraft:arrow",4,(196,193,170,255)),
    ("shulker_template","cajas de shulker","Shulker","minecraft:ender_chest",5,(144,100,170,255))]
for index,(name,es,en,ingredient,minimum,color) in enumerate(templates):
    write(DATA/"recipe"/(name+".json"),{"type":"minecraft:crafting_shaped","category":"equipment","pattern":["WWW","WCW","WWW"],"key":{"W":"#minecraft:planks","C":ingredient},"result":{"id":"backpack_mod:"+name,"count":1}})
    write(DATA/"advancement/recipes"/(name+".json"),{"parent":"minecraft:recipes/root","criteria":{"has_wood":{"trigger":"minecraft:inventory_changed","conditions":{"items":[{"items":"#minecraft:planks"}]}}},"requirements":[["has_wood"]],"rewards":{"recipes":["backpack_mod:"+name]}})
    icon=[[(0,0,0,0) for x in range(32)] for y in range(32)]
    rect(icon,5,2,27,30,(85,64,44,255));rect(icon,6,3,26,29,(223,204,160,255))
    rect(icon,8,5,24,7,(178,151,107,255));rect(icon,8,26,24,27,(178,151,107,255))
    rect(icon,10,10,23,23,DARK);rect(icon,11,11,22,22,color)
    if index==0:
        for q in (14,18):rect(icon,q,11,q+1,22,DARK);rect(icon,11,q,22,q+1,DARK)
    elif index in (1,2,3):
        rect(icon,13,15,20,20,(31,32,34,255));rect(icon,14,19,19,21,(226,129,49,255))
        if index==2:rect(icon,12,11,13,22,DARK);rect(icon,20,11,21,22,DARK)
        if index==3:rect(icon,14,8,19,12,(82,70,61,255))
    elif index==4:
        rect(icon,12,13,21,21,color);rect(icon,14,11,20,14,color);rect(icon,17,8,19,12,(67,110,55,255))
    elif index==5:
        rect(icon,15,10,18,23,(117,84,50,255));rect(icon,12,10,21,13,(224,228,221,255));rect(icon,14,8,19,11,(224,228,221,255));rect(icon,12,21,21,24,(215,210,179,255))
    else:
        rect(icon,10,11,23,14,(181,143,205,255));rect(icon,12,18,21,20,DARK);rect(icon,14,17,19,21,(90,184,169,255))
    png(ROOT/"textures/item"/(name+".png"),icon)
    write(ROOT/"items"/(name+".json"),{"model":{"type":"minecraft:model","model":"backpack_mod:item/"+name}})
    write(ROOT/"models/item"/(name+".json"),{"parent":"minecraft:item/generated","textures":{"layer0":"backpack_mod:item/"+name}})
    empty=[]
    for y in range(16):
        row=[]
        for x in range(16):
            c=icon[y*2][x*2];v=int((c[0]+c[1]+c[2])/3)
            row.append((v,v,v,180) if c[3] else (0,0,0,0))
        empty.append(row)
    png(ROOT/"textures/gui/sprites/container/slot"/(name+".png"),empty)
    for lang in ["es_es","es_mx","en_us"]:
        path=ROOT/"lang"/(lang+".json");text=json.loads(path.read_text(encoding="utf-8"))
        text["item.backpack_mod."+name]=en+" Template" if lang=="en_us" else "Plantilla de "+es
        write(path,text)
colors=[None,(116,120,124,255),(211,221,226,255),(242,194,55,255),(72,220,211,255),(77,64,76,255)]
variants={}
for level,item in enumerate(levels):
    for face,rotation in [("south",0),("west",90),("north",180),("east",270)]:
        variants[f"facing={face},level={level}"]={"model":"backpack_mod:block/"+item,"y":rotation}
    textures={}
    for surface in ["front","side","top","back","pocket","strap"]:
        pixels=[[(138+(x*3+y)%9,85+(x+y*2)%8,45,255) for x in range(16)] for y in range(16)]
        if surface=="strap":rect(pixels,0,0,16,16,DARK)
        else:
            rect(pixels,0,0,16,2,colors[level] or LIGHT)
            for x in range(1,16,3):pixels[14][x]=(210,163,102,255)
        if surface=="front":
            rect(pixels,0,0,16,4,colors[level] or LIGHT);rect(pixels,7,3,10,8,DARK);rect(pixels,7,4,10,7,GOLD)
        if surface=="back":rect(pixels,3,0,5,16,DARK);rect(pixels,11,0,13,16,DARK)
        if surface=="pocket":rect(pixels,0,2,16,4,DARK);rect(pixels,7,2,9,5,GOLD)
        png(ROOT/"textures/block"/(item+"_"+surface+".png"),pixels)
        textures[surface]="backpack_mod:block/"+item+"_"+surface
    textures["particle"]=textures["side"]
    def cube(a,b,texture):return {"from":a,"to":b,"faces":{face:{"uv":[0,0,16,16],"texture":"#"+texture} for face in ["north","south","east","west","up","down"]}}
    body=cube([3,0,4],[13,12,10],"side");body["faces"]["south"]["texture"]="#front";body["faces"]["north"]["texture"]="#back"
    parts=[body,cube([2.5,10.5,3.5],[13.5,13,10.5],"top"),cube([4,2,10],[12,7,12],"pocket"),cube([1,2,5],[3,8,9],"side"),cube([13,2,5],[15,8,9],"side"),cube([6,13,6],[7,15,7],"strap"),cube([9,13,6],[10,15,7],"strap"),cube([6,14,6],[10,15,7],"strap")]
    write(ROOT/"models/block"/(item+".json"),{"parent":"minecraft:block/block","textures":textures,"elements":parts})
write(ROOT/"blockstates/placed_backpack.json",{"variants":variants})
write(DATA/"loot_table/blocks/placed_backpack.json",{"type":"minecraft:block","pools":[]})
for lang in ["es_es","es_mx","en_us"]:
    path=ROOT/"lang"/(lang+".json");text=json.loads(path.read_text(encoding="utf-8"));english=lang=="en_us"
    text.update({
        "block.backpack_mod.placed_backpack":"Backpack" if english else "Mochila",
        "message.backpack_mod.in_use":"This backpack is being used by another player." if english else "Otro jugador está usando esta mochila.",
        "item.backpack_mod.template_hint":"Backpack template · minimum level %s" if english else "Plantilla para mochila · nivel mínimo %s",
        "screen.backpack_mod.storage":"Storage" if english else "Inventario",
        "screen.backpack_mod.input":"Input" if english else "Entrada",
        "screen.backpack_mod.fuel":"Fuel" if english else "Combustible",
        "screen.backpack_mod.output":"Output" if english else "Salida",
        "screen.backpack_mod.title":"Backpack \u00b7 Lv. %s \u00b7 %s" if english else "Mochila \u00b7 Nv. %s \u00b7 %s",
        "screen.backpack_mod.templates":"Templates" if english else "Plantillas",
        "screen.backpack_mod.template_slot":"%s · Level %s" if english else "%s · Desde nivel %s",
        "screen.backpack_mod.hint.5":"4 food stacks \u00b7 Automatic feeding" if english else "4 pilas de comida \u00b7 Alimentaci\u00f3n autom\u00e1tica",
        "screen.backpack_mod.hint.6":"Arrows for bow and crossbow" if english else "Flechas para arco y ballesta",
        "screen.backpack_mod.hint.7":"Up to 9 shulker boxes" if english else "Máximo 9 cajas de shulker"})
    for i,entry in enumerate(templates,1):text["screen.backpack_mod.module."+str(i)]=entry[2] if english else entry[1].capitalize()
    write(path,text)
