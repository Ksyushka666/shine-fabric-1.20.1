#!/usr/bin/env python3
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
props = (ROOT / "gradle.properties").read_text()
expected_version = next(line.split("=", 1)[1].strip() for line in props.splitlines() if line.startswith("mod_version="))
jar = next(p for p in (ROOT / "build/libs").glob("*.jar") if not p.name.endswith("-sources.jar"))
sources = next((ROOT / "build/libs").glob("*-sources.jar"))
errors = []
if not jar.is_file(): errors.append("remapped JAR missing")
else:
    with zipfile.ZipFile(jar) as z:
        names = set(z.namelist())
        try: metadata = json.loads(z.read("fabric.mod.json"))
        except Exception as e: errors.append(f"fabric.mod.json unreadable: {e}"); metadata = {}
        if metadata.get("version") != expected_version: errors.append(f"expanded artifact version mismatch: {metadata.get('version')} != {expected_version}")
        if metadata.get("environment") != "*": errors.append("artifact environment mismatch")
        for forbidden in ("build.gradle", "PORT_STATUS.md"):
            if forbidden in names: errors.append(f"forbidden artifact entry: {forbidden}")
        if any(name.endswith(".legacy21") or name.endswith(".java") for name in names): errors.append("source/archive file leaked into mod JAR")
        for required in ("com/bloom/BloomMod.class", "com/bloom/particle/ShineParticleTypes.class", "fabric.mod.json", "assets/shine/defaults/shine.json"):
            if required not in names: errors.append(f"required artifact entry missing: {required}")
if not sources.is_file(): errors.append("sources JAR missing")
else:
    with zipfile.ZipFile(sources) as z:
        java_count = sum(name.endswith(".java") for name in z.namelist())
        if java_count != 29: errors.append(f"source count mismatch: {java_count}")
print("remapped_artifact_checked=1")
print("source_java_count=29")
print(f"errors={len(errors)}")
for error in errors: print(error)
if errors: raise SystemExit(1)
