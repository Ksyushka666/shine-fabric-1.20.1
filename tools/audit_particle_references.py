import json
from pathlib import Path

ROOT = Path('src/main/resources')
PARTICLES = ROOT / 'assets' / 'shine' / 'particles'
errors = []
checked = 0

for path in sorted(PARTICLES.glob('*.json')):
    checked += 1
    try:
        data = json.loads(path.read_text(encoding='utf-8-sig'))
    except Exception as exc:
        errors.append(f'{path}: invalid JSON: {exc}')
        continue
    textures = data.get('textures')
    if not isinstance(textures, list) or not textures:
        errors.append(f'{path}: missing non-empty textures array')
        continue
    for texture in textures:
        if not isinstance(texture, str):
            errors.append(f'{path}: texture entry is not a string: {texture!r}')
            continue
        if texture.startswith('#'):
            continue
        namespace, separator, relative = texture.partition(':')
        if not separator:
            namespace, relative = 'shine', texture
        if namespace != 'shine':
            # Vanilla namespace assets are supplied by Minecraft, not by the Shine JAR.
            continue
        texture_root = ROOT / 'assets' / namespace / 'textures' / 'particle'
        texture_path = texture_root / (relative + '.png')
        if not texture_path.is_file():
            errors.append(f'{path}: missing texture {texture_path}')

print(f'particle_definitions={checked}')
print(f'errors={len(errors)}')
for error in errors:
    print(error)
if errors:
    raise SystemExit(1)
