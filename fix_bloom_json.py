import json
from pathlib import Path

path = Path('/home/ubuntu/shine_port_clean/src/main/resources/assets/shine/post_effect/bloom_poc.json')
data = json.loads(path.read_text())
extract = data['passes'][0]['uniforms']
for item in extract:
    if item['name'] == 'Scale':
        item['name'] = 'SourceStrengthScale'
    elif item['name'] == 'SourceMode':
        item['name'] = 'DistanceFadeRange'
        item['value'] = 16.0
composite = data['passes'][-1]['uniforms']
existing = {item['name'] for item in composite}
for name, value in [('Weight0', 1.0), ('Weight1', 0.65), ('Weight2', 0.35), ('Weight3', 0.2), ('Weight4', 0.1), ('Weight5', 0.05), ('DistanceFadeRange', 16.0)]:
    if name not in existing:
        composite.append({'name': name, 'type': 'float', 'value': value})
path.write_text(json.dumps(data, indent=2) + '\n')
