"""Render the repository's simple geometric navigation icon set (not image editing)."""
from pathlib import Path
from PIL import Image, ImageDraw

dest = Path(__file__).resolve().parents[1] / 'frontend/src/static/icons'
dest.mkdir(parents=True, exist_ok=True)
for kind in ('home', 'plan', 'study', 'user'):
    for active, color in ((False, '#B3BECD'), (True, '#1677FF')):
        image = Image.new('RGBA', (64, 64))
        d = ImageDraw.Draw(image)
        if kind == 'home':
            d.polygon([(8,29),(32,8),(56,29),(49,29),(49,55),(15,55),(15,29)],fill=color)
            d.rounded_rectangle((27,36,37,55),2,fill='white')
        elif kind == 'plan':
            d.rounded_rectangle((12,14,52,56),5,fill=color)
            d.rounded_rectangle((23,8,41,20),4,fill=color)
            for y in (28,38,48):
                d.line((27,y,44,y),fill='white',width=3)
                d.rectangle((19,y-1,22,y+2),fill='white')
        elif kind == 'study':
            d.polygon([(8,14),(28,16),(32,21),(36,16),(56,14),(56,52),(36,54),(32,58),(28,54),(8,52)],fill=color)
            d.line((32,22,32,49),fill='white',width=3)
            d.line((16,26,25,27),fill='white',width=3)
            d.line((16,34,25,35),fill='white',width=3)
        else:
            d.ellipse((22,8,42,28),fill=color)
            d.rounded_rectangle((12,33,52,56),11,fill=color)
        image.save(dest / f'{kind}{"-active" if active else ""}.png')
