#!/usr/bin/env python3
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "build/libs/shine-1.0.0.jar"
errors = []
with zipfile.ZipFile(JAR) as archive:
    metadata = json.loads(archive.read("fabric.mod.json"))
    manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8")

if metadata.get("id") != "shine": errors.append("mod id mismatch")
if metadata.get("version") in (None, "${version}") or "${" in str(metadata.get("version")): errors.append("unresolved mod version")
if metadata.get("environment") != "*": errors.append("environment is not universal")
if metadata.get("depends", {}).get("minecraft") != "~1.20.1": errors.append("Minecraft dependency is not 1.20.1")
if "com.bloom.BloomMod" not in metadata.get("entrypoints", {}).get("main", []): errors.append("main entrypoint missing")
if "com.bloom.client.BloomClient" not in metadata.get("entrypoints", {}).get("client", []): errors.append("client entrypoint missing")
if "Fabric-Minecraft-Version: 1.20.1" not in manifest: errors.append("manifest Minecraft version mismatch")
if "Fabric-Loom-Mixin-Remap-Type: mixin" not in manifest: errors.append("remapped mixin manifest marker missing")

print(f"artifact={JAR.name}")
print(f"expanded_version={metadata.get('version')}")
print(f"minecraft=1.20.1")
print(f"errors={len(errors)}")
for error in errors: print(error)
if errors: raise SystemExit(1)
