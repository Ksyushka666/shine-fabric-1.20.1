#!/usr/bin/env python3
import json
import subprocess
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAR = next(p for p in (ROOT / "build/libs").glob("*.jar") if not p.name.endswith("-sources.jar"))
errors = []

metadata = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text())
if metadata.get("environment") != "*":
    errors.append("metadata environment is not universal")
if "com.bloom.BloomMod" not in metadata.get("entrypoints", {}).get("main", []):
    errors.append("common main entrypoint missing")
for required in ("fabricloader", "minecraft", "java", "fabric-api"):
    if required not in metadata.get("depends", {}):
        errors.append(f"required dependency missing: {required}")
for optional in ("yet_another_config_lib_v3", "sodium", "iris"):
    if optional not in metadata.get("suggests", {}):
        errors.append(f"optional suggestion missing: {optional}")

with zipfile.ZipFile(JAR) as archive:
    names = set(archive.namelist())
    for required in ("com/bloom/BloomMod.class", "com/bloom/particle/ShineParticleTypes.class", "fabric.mod.json"):
        if required not in names:
            errors.append(f"required artifact entry missing: {required}")

for class_name in ("com.bloom.BloomMod", "com.bloom.particle.ShineParticleTypes"):
    result = subprocess.run(["javap", "-classpath", str(JAR), "-verbose", class_name], capture_output=True, text=True, check=False)
    if result.returncode != 0:
        errors.append(f"unable to inspect {class_name}")
        continue
    for forbidden in ("net/minecraft/client", "com/mojang/blaze3d", "net/irisshaders", "me/jellysquid", "dev/isxander", "com/terraformersmc"):
        if forbidden in result.stdout:
            errors.append(f"common class {class_name} references client/optional symbol {forbidden}")

print(f"common_classes_checked=2")
print(f"errors={len(errors)}")
for error in errors:
    print(error)
if errors:
    raise SystemExit(1)
