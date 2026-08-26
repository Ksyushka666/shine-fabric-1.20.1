import json
from pathlib import Path

path = Path('/home/ubuntu/shine_port_clean/src/main/resources/assets/shine/post_effect/bloom_poc.json')
data = json.loads(path.read_text())
for p in data['passes']:
    flat = []
    for block_name, values in p.get('uniforms', {}).items():
        for item in values:
            flat.append(item)
    p['uniforms'] = flat
path.write_text(json.dumps(data, indent=2) + '\n')
