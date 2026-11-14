from PIL import Image, ImageDraw

S = 512  # supersampled canvas
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

def poly(points, color):
    d.polygon([(int(x * S / 128), int(y * S / 128)) for x, y in points], fill=color)

OUTLINE = (10, 34, 31, 255)
BASE = (47, 211, 192, 255)
LIGHT = (122, 240, 226, 255)
DARK = (23, 168, 147, 255)

top, left, right, bottom = (64, 16), (18, 54), (110, 54), (64, 116)
facet = (64, 62)

poly([top, right, bottom, left], OUTLINE)
# shrink the outline away by drawing the gem slightly smaller
poly([(64, 24), (25, 55), (64, 108), (103, 55)], BASE)
poly([(64, 24), (103, 55), (64, 62), (25, 55)], LIGHT)   # top facet
poly([(64, 63), (103, 56), (64, 107), (25, 56)], DARK)    # bottom facet

img = img.resize((128, 128), Image.LANCZOS)
img.save("src/main/resources/logo.png")
print("icon written")
