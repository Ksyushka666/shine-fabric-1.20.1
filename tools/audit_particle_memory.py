from pathlib import Path
import struct

root = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/shine/textures/particle'
errors = []
count = 0
max_dimension = 0
png_signature = b'\x89PNG\r\n\x1a\n'
for path in root.rglob('*.png'):
    data = path.read_bytes()
    if len(data) < 24 or data[:8] != png_signature or data[12:16] != b'IHDR':
        errors.append(f'invalid_png_header={path.relative_to(root)}')
        continue
    width, height = struct.unpack('>II', data[16:24])
    count += 1
    max_dimension = max(max_dimension, width, height)
    if max(width, height) > 256:
        errors.append(f'oversized_particle_texture={path.relative_to(root)}:{width}x{height}')
print(f'particle_textures={count}')
print(f'max_dimension={max_dimension}')
print(f'errors={len(errors)}')
for error in errors:
    print(error)
raise SystemExit(bool(errors))
