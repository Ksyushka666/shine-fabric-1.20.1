#!/usr/bin/env python3
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAR = next(p for p in (ROOT / "build/libs").glob("*.jar") if not p.name.endswith("-sources.jar"))
SOURCES = next((ROOT / "build/libs").glob("*-sources.jar"))
errors = []
for path, forbidden in ((JAR, (".java", ".log", ".tmp", "audit", "PortPipelineTest")), (SOURCES, (".log", ".tmp", "audit"))):
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        for name in names:
            if any(token.lower() in name.lower() for token in forbidden):
                errors.append(f"forbidden entry in {path.name}: {name}")
        if path == JAR:
            metadata = archive.read("fabric.mod.json").decode("utf-8")
            if re.search(r"\$\{|TODO|REPLACE_ME", metadata):
                errors.append("unresolved metadata placeholder")
            if "shine.mixins.json" not in names:
                errors.append("active mixin config missing")

print("mod_jar_clean=PASS" if not errors else "mod_jar_clean=FAIL")
print(f"jar_entries={len(zipfile.ZipFile(JAR).namelist())}")
print(f"source_jar_entries={len(zipfile.ZipFile(SOURCES).namelist())}")
print(f"errors={len(errors)}")
for error in errors: print(error)
if errors: raise SystemExit(1)
