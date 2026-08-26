#!/usr/bin/env python3
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAR = next((ROOT / "build/libs").glob("*-sources.jar"))
source_set = {p.relative_to(ROOT / "src/main/java").as_posix() for p in (ROOT / "src/main/java").rglob("*.java")}
with zipfile.ZipFile(JAR) as archive:
    jar_set = {n for n in archive.namelist() if n.endswith(".java")}
missing = sorted(source_set - jar_set)
extra = sorted(jar_set - source_set)
print(f"source_java_count={len(source_set)}")
print(f"source_jar_java_count={len(jar_set)}")
print(f"missing_source_count={len(missing)}")
print(f"extra_source_count={len(extra)}")
for path in missing: print(f"missing={path}")
for path in extra: print(f"extra={path}")
if missing or extra: raise SystemExit(1)
