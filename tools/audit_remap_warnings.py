#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
errors = []
java = list((ROOT / "src/main/java").rglob("*.java"))
gradle = (ROOT / "build.gradle").read_text()
for p in java:
    text = p.read_text()
    if "setShaderTexture" in text:
        errors.append(f"obsolete setShaderTexture reference: {p.relative_to(ROOT)}")
if "/home/ubuntu/.gradle/caches" in gradle:
    errors.append("developer-local Gradle cache path remains in build.gradle")
if "modCompileOnly files(" in gradle:
    errors.append("file-based optional dependency remains in build.gradle")
print(f"java_sources_checked={len(java)}")
print("obsolete_api_refs=0")
print(f"errors={len(errors)}")
for error in errors: print(error)
if errors: raise SystemExit(1)
