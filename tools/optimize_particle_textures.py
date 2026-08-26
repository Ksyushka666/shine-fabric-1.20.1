from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/shine/textures/particle'
changed = 0
for path in ROOT.rglob('*.png'):
    with Image.open(path) as source:
        width, height = source.size
        longest = max(width, height)
        if longest <= 256:
            continue
        scale = 256 / longest
        target = (max(1, round(width * scale)), max(1, round(height * scale)))
        image = source.convert('RGBA').resize(target, Image.Resampling.LANCZOS)
        image.save(path, optimize=True)
        changed += 1
        print(f'{path.relative_to(ROOT)}: {width}x{height} -> {target[0]}x{target[1]}')
print(f'optimized={changed}')
