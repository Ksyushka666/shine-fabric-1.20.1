#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
config = json.loads((ROOT / "src/main/resources/shine.mixins.json").read_text())
errors = []
client = config.get("client", [])
common = config.get("mixins", [])
if not client:
    errors.append("active mixin config has no client section")
if common:
    errors.append(f"unexpected common mixins: {common}")
for name in client:
    if name.startswith("../"):
        errors.append(f"mixin escapes client package: {name}")
plugin = config.get("plugin", "")
if plugin != "com.bloom.mixin.BloomMixinPlugin":
    errors.append("mixin plugin declaration mismatch")
source = (ROOT / "src/main/java/com/bloom/mixin/BloomMixinPlugin.java").read_text()
if "isSupportedSodiumVersion" not in source:
    errors.append("Sodium compatibility predicate missing")
if "return false" not in source:
    errors.append("optional mixin fallback missing")
print(f"client_mixins={len(client)}")
print(f"common_mixins={len(common)}")
print(f"errors={len(errors)}")
for error in errors: print(error)
if errors: raise SystemExit(1)
