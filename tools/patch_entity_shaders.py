from pathlib import Path
import json
import subprocess
import zipfile

ROOT = Path(__file__).parents[1]
MC = Path.home() / '.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar'
OUT = ROOT / 'src/main/resources'
pattern = 'assets/minecraft/shaders/core/'
service = {'rendertype_armor_entity_glint', 'rendertype_entity_decal', 'rendertype_entity_glint', 'rendertype_entity_glint_direct', 'rendertype_entity_shadow'}
with zipfile.ZipFile(MC) as archive:
    files = [n for n in archive.namelist() if n.startswith(pattern) and 'entity' in Path(n).name and n.endswith(('.json', '.vsh', '.fsh'))]
    files = [n for n in files if Path(n).stem not in service]
    for name in files:
        target = OUT / name
        target.parent.mkdir(parents=True, exist_ok=True)
        data = archive.read(name)
        if name.endswith('.fsh'):
            text = data.decode('utf-8')
            if 'out vec4 fragColor;' not in text or 'bloomColor' in text:
                target.write_text(text, encoding='utf-8')
                continue
            text = text.replace('uniform sampler2D Sampler0;', 'uniform sampler2D Sampler0;\nuniform float ShineEntityStrength;', 1)
            text = text.replace('out vec4 fragColor;', 'out vec4 fragColor;\nlayout(location = 1) out vec4 bloomColor;', 1)
            end = text.rfind('}')
            text = text[:end] + '    bloomColor = vec4(fragColor.rgb * fragColor.a, ShineEntityStrength);\n' + text[end:]
            target.write_text(text, encoding='utf-8')
        elif name.endswith('.json'):
            obj = json.loads(data.decode('utf-8'))
            uniforms = obj.setdefault('uniforms', [])
            if not any(u.get('name') == 'ShineEntityStrength' for u in uniforms):
                uniforms.append({'name': 'ShineEntityStrength', 'type': 'float', 'count': 1, 'values': [0.0]})
            target.write_text(json.dumps(obj, indent=4) + '\n', encoding='utf-8')
        else:
            target.write_bytes(data)

# Vanilla entity vertex shaders import these shared includes at runtime; bundle the exact 1.20.1 files.
for include in ('fog.glsl', 'light.glsl'):
    name = f'assets/minecraft/shaders/include/{include}'
    with zipfile.ZipFile(MC) as archive:
        (OUT / name).parent.mkdir(parents=True, exist_ok=True)
        (OUT / name).write_bytes(archive.read(name))
print(f'patched_entity_shader_files={len(files)}')
