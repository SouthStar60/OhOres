#!/usr/bin/env python3
"""Generates the pixel art block textures for OhOres.

The container GUI uses the vanilla furnace texture, so nothing else is generated here.

Run:  python3 tools/generate_textures.py
"""
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BLOCK_DIR = os.path.join(ROOT, "src/main/resources/assets/ohores/textures/block")

# palette
EDGE = (22, 25, 28, 255)
DARK = (42, 46, 51, 255)
MID = (60, 66, 72, 255)
LIGHT = (84, 92, 100, 255)
HILITE = (110, 120, 130, 255)
COPPER = (178, 112, 60, 255)
COPPER_D = (122, 74, 38, 255)
CYAN = (79, 214, 224, 255)
CYAN_D = (40, 140, 152, 255)
CYAN_L = (170, 245, 250, 255)


def noise_panel(size, base, seed):
    img = Image.new("RGBA", (size, size), base)
    px = img.load()
    rnd = random.Random(seed)
    for y in range(size):
        for x in range(size):
            d = rnd.randint(-6, 6)
            r, g, b, a = px[x, y]
            px[x, y] = (max(0, min(255, r + d)), max(0, min(255, g + d)), max(0, min(255, b + d)), a)
    return img


def frame(img):
    """Classic 1px block frame: light top/left, dark bottom/right."""
    px = img.load()
    w, h = img.size
    for x in range(w):
        px[x, 0] = LIGHT
        px[x, h - 1] = EDGE
    for y in range(h):
        px[0, y] = LIGHT
        px[w - 1, y] = EDGE
    return img


def rivet(draw, x, y, color=COPPER):
    draw.point((x, y), fill=color)
    draw.point((x + 1, y), fill=COPPER_D)


def make_side():
    img = noise_panel(16, MID, 1)
    frame(img)
    draw = ImageDraw.Draw(img)
    # inner panel border
    draw.rectangle([3, 3, 12, 12], outline=DARK)
    # ore flecks
    draw.point((5, 6), fill=CYAN)
    draw.point((6, 6), fill=CYAN_L)
    draw.point((6, 7), fill=CYAN_D)
    draw.point((9, 9), fill=CYAN)
    draw.point((10, 9), fill=CYAN_L)
    draw.point((10, 10), fill=CYAN_D)
    draw.point((5, 10), fill=CYAN_D)
    draw.point((10, 5), fill=CYAN_D)
    # rivets in the corners
    rivet(draw, 1, 1)
    rivet(draw, 13, 1)
    rivet(draw, 1, 13)
    rivet(draw, 13, 13)
    return img


def make_front():
    img = noise_panel(16, MID, 2)
    frame(img)
    draw = ImageDraw.Draw(img)
    rivet(draw, 1, 1)
    rivet(draw, 13, 1)
    rivet(draw, 1, 13)
    rivet(draw, 13, 13)

    # status leds
    draw.rectangle([3, 2, 12, 2], fill=DARK)
    for i, x in enumerate((4, 7, 10)):
        draw.point((x, 2), fill=CYAN if i != 1 else CYAN_L)

    # intake mouth
    draw.rectangle([3, 5, 12, 12], fill=EDGE)
    draw.rectangle([4, 6, 11, 11], fill=(10, 12, 14, 255))
    draw.rectangle([4, 6, 11, 6], fill=CYAN_D)
    draw.rectangle([4, 6, 4, 11], fill=CYAN_D)
    draw.point((6, 8), fill=CYAN)
    draw.point((7, 8), fill=CYAN_L)
    draw.point((9, 10), fill=CYAN)
    draw.point((8, 9), fill=CYAN_D)
    draw.point((5, 10), fill=CYAN_D)
    # metal teeth around the mouth
    for x in range(5, 11, 2):
        draw.point((x, 5), fill=COPPER)
        draw.point((x, 12), fill=COPPER_D)
    return img


def make_top():
    img = noise_panel(16, LIGHT, 3)
    frame(img)
    draw = ImageDraw.Draw(img)
    draw.rectangle([3, 3, 12, 12], outline=DARK)
    # cross beams
    draw.rectangle([7, 3, 8, 12], fill=COPPER)
    draw.rectangle([3, 7, 12, 8], fill=COPPER)
    draw.point((7, 7), fill=CYAN_L)
    draw.point((8, 8), fill=CYAN_D)
    rivet(draw, 1, 1)
    rivet(draw, 13, 1)
    rivet(draw, 1, 13)
    rivet(draw, 13, 13)
    return img


def make_bottom():
    img = noise_panel(16, DARK, 4)
    frame(img)
    draw = ImageDraw.Draw(img)
    for y in (4, 6, 8, 10, 12):
        draw.line([2, y, 13, y], fill=EDGE)
    draw.rectangle([6, 5, 9, 11], fill=(30, 33, 37, 255))
    rivet(draw, 1, 1)
    rivet(draw, 13, 1)
    rivet(draw, 1, 13)
    rivet(draw, 13, 13)
    return img


def main():
    os.makedirs(BLOCK_DIR, exist_ok=True)

    make_side().save(os.path.join(BLOCK_DIR, "rare_ore_extractor_side.png"))
    make_front().save(os.path.join(BLOCK_DIR, "rare_ore_extractor_front.png"))
    make_top().save(os.path.join(BLOCK_DIR, "rare_ore_extractor_top.png"))
    make_bottom().save(os.path.join(BLOCK_DIR, "rare_ore_extractor_bottom.png"))
    print("textures written")


if __name__ == "__main__":
    main()
