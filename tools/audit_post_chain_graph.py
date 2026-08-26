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
    if not fragment:
        program = p.get("name", "")
        if ":" in program: program = program.split(":", 1)[1]
        program_json = RESOURCE / "shine" / "shaders" / "program" / (program + ".json")
        if program_json.is_file():
            fragment = json.loads(program_json.read_text()).get("fragment", program)
            shader = RESOURCE / "shine" / "shaders" / "program" / (fragment + ".fsh")
        else:
            shader = RESOURCE / "shine" / "shaders" / "program" / (program + ".fsh")
    else:
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
    inputs = p.get("inputs", [])
    inputs += p.get("auxtargets", [])
    for inp in inputs:
        name = inp.get("sampler_name", inp.get("name"))
        if name and name not in declared_samplers:
            errors.append(f"pass {index}: sampler {name} not declared by {fragment}")
    for uniform in p.get("uniforms", []):
        name = uniform.get("name")
        if name not in declared_uniforms:
            errors.append(f"pass {index}: uniform {name} not declared by {fragment}")
    output = p.get("output", p.get("outtarget"))
    targets = chain.get("targets", {})
    if isinstance(targets, list): targets = {name: {} for name in targets}
    if output and output != "minecraft:main" and output not in targets:
        errors.append(f"pass {index}: output target {output} is not declared")

print(f"passes={len(chain.get('passes', []))}")
print(f"targets={len(chain.get('targets', {}))}")
print(f"errors={len(errors)}")
for error in errors:
    print(error)
if errors:
    raise SystemExit(1)
