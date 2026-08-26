#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CORE = ROOT / "src/main/resources/assets/minecraft/shaders/core"
errors = []
entity_checked = 0
particle_checked = 0
for shader in sorted(CORE.glob("*.fsh")):
    text = shader.read_text()
    if "ShineEntityStrength" in text:
        entity_checked += 1
        for marker in ("out vec4 bloomColor;", "bloomColor = vec4", "uniform float ShineEntityStrength"):
            if marker not in text: errors.append(f"entity shader missing {marker}: {shader.name}")
        if text.count("bloomColor =") != 1: errors.append(f"entity shader bloom write count != 1: {shader.name}")
        program = CORE / (shader.stem + ".json")
        if not program.is_file(): errors.append(f"entity program JSON missing: {program.name}")
        else:
            data = json.loads(program.read_text())
            uniforms = {u.get("name") for u in data.get("uniforms", [])}
            if "ShineEntityStrength" not in uniforms: errors.append(f"entity uniform missing in JSON: {program.name}")
    if "ShineParticleStrength" in text:
        particle_checked += 1
        for marker in ("out vec4 bloomColor;", "bloomColor = vec4", "uniform float ShineParticleStrength"):
            if marker not in text: errors.append(f"particle shader missing {marker}: {shader.name}")
        if text.count("bloomColor =") != 1: errors.append(f"particle shader bloom write count != 1: {shader.name}")
print(f"entity_fragment_outputs={entity_checked}")
print(f"particle_fragment_outputs={particle_checked}")
if any("layout(location" in line for shader in CORE.glob("*.fsh") for line in shader.read_text().splitlines()): errors.append("GLSL 1.50-incompatible fragment output layout qualifier")
print(f"errors={len(errors)}")
for error in errors: print(error)
if errors: raise SystemExit(1)
