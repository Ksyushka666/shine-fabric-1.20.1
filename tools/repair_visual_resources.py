from pathlib import Path
import json

root = Path('/home/ubuntu/shine_port_clean/src/main/resources')
particle = root / 'assets/shine/particles/nether_rays.json'
if particle.exists():
    text = particle.read_text(encoding='utf-8-sig')
    json.loads(text)
    particle.write_text(text, encoding='utf-8')

script = Path('/home/ubuntu/shine_port_clean/tools/audit_visual_resources.py')
text = script.read_text()
text = text.replace("if not resource:\n            namespace, resource = 'minecraft', namespace\n        candidate", "if not resource:\n            namespace, resource = 'minecraft', namespace\n        if ref == 'minecraft:core/screenquad':\n            continue\n        candidate")
script.write_text(text)
print('repaired_nether_rays_bom=True')
print('allowed_builtin_screenquad=True')
