from pathlib import Path
import json
import re

root = Path(__file__).parents[1] / 'src/main/resources'
assets = root / 'assets'
errors = []
json_count = 0
for path in assets.rglob('*.json'):
    json_count += 1
    try:
        data = json.loads(path.read_text())
    except Exception as exc:
        errors.append(f'INVALID_JSON {path.relative_to(root)}: {exc}')
        continue
    text = path.read_text()
    for match in re.finditer(r'"(?:vertex_shader|fragment_shader|shader)"\s*:\s*"([^"]+)"', text):
        ref = match.group(1)
        namespace, _, resource = ref.partition(':')
        if not resource:
            namespace, resource = 'minecraft', namespace
        if ref == 'minecraft:core/screenquad':
            continue
        candidate = assets / namespace / ('shaders/' + resource + '.fsh')
        candidates = [candidate, assets / namespace / ('shaders/' + resource + '.vsh')]
        if not any(p.is_file() for p in candidates):
            errors.append(f'MISSING_SHADER {path.relative_to(root)} -> {ref}')
    for match in re.finditer(r'"(?:target|sampler_name)"\s*:\s*"([^"]+)"', text):
        value = match.group(1)
        if ':' in value or value in {'source', 'bloom_extract', 'bloom_ping', 'bloom_pong', 'minecraft:main'}:
            continue

for path in (assets / 'shine' / 'particles').glob('*.json'):
    try:
        particle_data = json.loads(path.read_text(encoding='utf-8-sig'))
    except Exception:
        continue
    textures = particle_data.get('textures', [])
    if not isinstance(textures, list) or not textures:
        errors.append(f'MISSING_PARTICLE_TEXTURES {path.relative_to(root)}')
        continue
    for texture in textures:
        if not isinstance(texture, str) or texture.startswith('#'):
            continue
        namespace, separator, resource = texture.partition(':')
        if not separator:
            namespace, resource = 'shine', texture
        # Minecraft namespace sprites are supplied by the game; validate all local Shine sprites.
        if namespace != 'shine':
            continue
        candidate = assets / namespace / 'textures/particle' / (resource + '.png')
        if not candidate.is_file():
            errors.append(f'MISSING_PARTICLE_TEXTURE {path.relative_to(root)} -> {texture}')

for path in assets.rglob('*.fsh'):
    text = path.read_text(errors='ignore')
    for match in re.finditer(r'#(?:moj_import|import)\s*[<]([^>]+)[>]', text):
        ref = match.group(1)
        if ref.startswith('minecraft:') or ref.startswith('sodium:'):
            continue
        candidates = [assets / 'shine' / ref]
        if path.is_relative_to(assets / 'minecraft'):
            candidates.extend([
                assets / 'minecraft' / 'shaders' / 'include' / ref,
                assets / 'minecraft' / 'shaders' / 'core' / ref,
            ])
        if not any(candidate.exists() for candidate in candidates):
            errors.append(f'MISSING_IMPORT {path.relative_to(root)} -> {ref}')

print(f'json_files={json_count}')
print(f'asset_files={sum(1 for _ in assets.rglob("*" ) if _.is_file())}')
print(f'errors={len(errors)}')
for error in errors[:200]:
    print(error)
if errors:
    raise SystemExit(1)
