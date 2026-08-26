#!/usr/bin/env python3
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RESOURCE = ROOT / "src/main/resources/assets"
CHAIN = RESOURCE / "shine/shaders/post/bloom_poc.json"
errors = []
chain = json.loads(CHAIN.read_text())

for index, p in enumerate(chain.get("passes", [])):
    fragment = p.get("fragment_shader", "")
    if ":" in fragment:
        namespace, path = fragment.split(":", 1)
    else:
        namespace, path = "minecraft", fragment
    shader = RESOURCE / namespace / "shaders" / (path + ".fsh")
    if not shader.is_file():
        errors.append(f"pass {index}: missing fragment shader {fragment}")
        continue
    text = shader.read_text()
    declared_samplers = set(re.findall(r"uniform\s+sampler2D\s+(\w+)", text))
    declared_uniforms = set(re.findall(r"uniform\s+(?:float|vec[234]|int|ivec[234])\s+(\w+)", text))
    for inp in p.get("inputs", []):
        name = inp.get("sampler_name")
        if name not in declared_samplers:
            errors.append(f"pass {index}: sampler {name} not declared by {fragment}")
    for uniform in p.get("uniforms", []):
        name = uniform.get("name")
        if name not in declared_uniforms:
            errors.append(f"pass {index}: uniform {name} not declared by {fragment}")
    output = p.get("output")
    if output and output != "minecraft:main" and output not in chain.get("targets", {}):
        errors.append(f"pass {index}: output target {output} is not declared")

print(f"passes={len(chain.get('passes', []))}")
print(f"targets={len(chain.get('targets', {}))}")
print(f"errors={len(errors)}")
for error in errors:
    print(error)
if errors:
    raise SystemExit(1)
