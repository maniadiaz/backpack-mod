"""Original pixel art for Backpack-mod. Standard library only."""
from pathlib import Path
import json, struct, zlib
ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/backpack_mod"

def png(path, pixels):
    path.parent.mkdir(parents=True, exist_ok=True)
    w, h = len(pixels[0]), len(pixels)
    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)
    raw = b"".join(b"\0" + bytes(v for color in row for v in color) for row in pixels)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))

def rect(p, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            p[y][x] = color

DARK = (66, 39, 29, 255)
LEATHER = (145, 88, 47, 255)
LIGHT = (177, 116, 64, 255)
STITCH = (207, 159, 99, 255)
GOLD = (226, 183, 73, 255)
texture = [[(132 + (x * 7 + y * 3) % 11, 79 + (x + y * 5) % 8, 43, 255) for x in range(64)] for y in range(64)]
for x0, y0, x1, y1 in [(0,0,24,14), (0,16,14,22), (24,0,52,8), (0,32,10,41)]:
    rect(texture,x0,y0,x1,y0+1,DARK)
    rect(texture,x0,y1-1,x1,y1,DARK)
    for x in range(x0+2,x1-1,3):
        texture[y1-2][x] = STITCH
rect(texture,24,16,36,30,DARK)
rect(texture,25,16,26,30,LIGHT)
rect(texture,24,32,44,40,DARK)
rect(texture,48,16,55,21,GOLD)
rect(texture,50,17,52,19,DARK)
png(ROOT / "textures/entity/backpack.png", texture)
icon = [[(0,0,0,0) for x in range(32)] for y in range(32)]
rect(icon,12,1,20,7,DARK); rect(icon,14,3,18,6,(0,0,0,0))
rect(icon,3,16,8,28,DARK); rect(icon,4,17,7,26,LEATHER)
rect(icon,24,16,29,28,DARK); rect(icon,25,17,28,26,LEATHER)
rect(icon,6,6,26,30,DARK); rect(icon,7,7,25,29,LEATHER)
rect(icon,8,7,24,14,LIGHT); rect(icon,8,14,24,16,DARK)
rect(icon,10,20,22,28,DARK); rect(icon,11,21,21,27,LIGHT)
rect(icon,14,12,18,19,DARK); rect(icon,14,13,18,17,GOLD)
rect(icon,15,14,17,16,DARK)
for x in range(8,25,3):
    icon[27][x] = STITCH
for y in range(17,26,3):
    icon[y][8] = STITCH; icon[y][23] = STITCH
png(ROOT / "textures/item/backpack.png", icon)
# Muted silhouette displayed by the empty equipment slot.
empty = []
for y in range(16):
    row=[]
    for x in range(16):
        c=icon[y*2][x*2]
        row.append((205,205,205,210) if c[3] else (0,0,0,0))
    empty.append(row)
png(ROOT / "textures/gui/sprites/container/slot/backpack.png", empty)
for lang in ["es_es", "es_mx", "en_us"]:
    path = ROOT / "lang" / (lang + ".json")
    data = json.loads(path.read_text(encoding="utf-8"))
    data.update({
        "key.backpack_mod.open": "Open / close equipped backpack" if lang == "en_us" else "Abrir / cerrar mochila equipada",
        "key.category.backpack_mod.backpack": "Backpack-mod",
        "container.backpack_mod.backpack": "%s",
        "message.backpack_mod.cursor_busy": "Put the cursor item away before opening your backpack." if lang == "en_us" else "Guarda primero el objeto que llevas en el cursor.",
        "message.backpack_mod.no_backpack": "Equip a backpack in your backpack slot first." if lang == "en_us" else "Primero equipa una mochila en su slot del inventario."
    })
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

# Six material tiers share the leather design and have distinct reinforcements.
levels = [
    ("backpack", "cuero", "Leather", None),
    ("stone_backpack", "piedra", "Stone", (116, 120, 124, 255)),
    ("iron_backpack", "hierro", "Iron", (211, 221, 226, 255)),
    ("gold_backpack", "oro", "Gold", (242, 194, 55, 255)),
    ("diamond_backpack", "diamante", "Diamond", (72, 220, 211, 255)),
    ("netherite_backpack", "netherite", "Netherite", (77, 64, 76, 255)),
]
for number, (item_id, spanish, english, color) in enumerate(levels):
    tier_icon = [row[:] for row in icon]
    tier_texture = [row[:] for row in texture]
    if color:
        rect(tier_icon,7,7,25,10,color)
        rect(tier_icon,7,27,10,29,color)
        rect(tier_icon,22,27,25,29,color)
        rect(tier_texture,24,0,52,8,color)
        rect(tier_texture,0,12,24,14,color)
    png(ROOT / ("textures/item/"+item_id+".png"), tier_icon)
    png(ROOT / ("textures/entity/"+item_id+".png"), tier_texture)
    for relative, data in {
        "items/"+item_id+".json": {"model":{"type":"minecraft:model","model":"backpack_mod:item/"+item_id}},
        "models/item/"+item_id+".json": {"parent":"minecraft:item/generated","textures":{"layer0":"backpack_mod:item/"+item_id}}
    }.items():
        path=ROOT/relative
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_text(json.dumps(data,indent=2)+"\n",encoding="utf-8")
    for lang in ["es_es","es_mx","en_us"]:
        path=ROOT/"lang"/(lang+".json")
        data=json.loads(path.read_text(encoding="utf-8"))
        data["item.backpack_mod."+item_id] = (english+" Backpack (level "+str(number)+")") if lang=="en_us" else ("Mochila de "+spanish+" (nivel "+str(number)+")")
        data["itemGroup.backpack_mod"] = "Backpack-mod"
        path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
