from pathlib import Path
from PIL import Image

root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/shine/textures/particle'
errors = []
count = 0
max_dimension = 0
for path in root.rglob('*.png'):
    with Image.open(path) as image:
        count += 1
        max_dimension = max(max_dimension, *image.size)
        if max(image.size) > 256:
            errors.append(f'oversized_particle_texture={path.relative_to(root)}:{image.size[0]}x{image.size[1]}')
print(f'particle_textures={count}')
print(f'max_dimension={max_dimension}')
print(f'errors={len(errors)}')
for error in errors:
    print(error)
raise SystemExit(bool(errors))
