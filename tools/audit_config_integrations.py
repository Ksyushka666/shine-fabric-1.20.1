#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
meta = json.loads((ROOT / "src/main/resources/fabric.mod.json").read_text())
main = (ROOT / "src/main/java/com/bloom/BloomMod.java").read_text()
client = (ROOT / "src/main/java/com/bloom/client/BloomClient.java").read_text()
menu = (ROOT / "src/main/java/com/bloom/client/config/BloomModMenuIntegration.java").read_text()
config = (ROOT / "src/main/java/com/bloom/client/config/BloomConfig.java").read_text()
errors = []
if meta.get("environment") != "*": errors.append("mod metadata must remain dual-environment")
if "main" not in meta.get("entrypoints", {}) or "client" not in meta.get("entrypoints", {}): errors.append("main/client entrypoints missing")
if not any("BloomModMenuIntegration" in x for x in meta.get("entrypoints", {}).get("modmenu", [])): errors.append("optional Mod Menu entrypoint missing")
if "dev.isxander.yacl3" in main: errors.append("YACL dependency leaked into common entrypoint")
if "net.minecraft.client" in main or "com.mojang.blaze3d" in main: errors.append("client dependency leaked into common entrypoint")
if 'isModLoaded("yet_another_config_lib_v3")' not in menu: errors.append("YACL optional guard missing in Mod Menu integration")
if "BloomConfigScreen::create" not in menu: errors.append("Mod Menu does not expose YACL screen when available")
if "BloomConfig.load()" not in client or "ExperimentalConfigManager.load()" not in client: errors.append("client config initialization missing")
if "StandardCopyOption.ATOMIC_MOVE" not in config or ".tmp" not in config: errors.append("atomic config persistence missing")
print(f"config_integration_checks=10")
print(f"errors={len(errors)}")
for error in errors: print(error)
if errors: raise SystemExit(1)
