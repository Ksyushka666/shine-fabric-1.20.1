#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "src/main/java"
errors = []

client = (JAVA / "com/bloom/client/BloomClient.java").read_text()
particles = (JAVA / "com/bloom/client/particle/ShineParticleRegistry.java").read_text()
menu = (JAVA / "com/bloom/client/config/BloomModMenuIntegration.java").read_text()

for name, text in (("BloomClient", client), ("ShineParticleRegistry", particles)):
    for forbidden in ("com.terraformersmc.modmenu", "dev.isxander.yacl", "net.irisshaders", "me.jellysquid", "net.caffeinemc"):
        if forbidden in text:
            errors.append(f"{name} directly references optional UI/compat symbol {forbidden}")
if "isModLoaded(\"yet_another_config_lib_v3\")" not in menu or "return parent -> parent" not in menu:
    errors.append("Mod Menu integration lacks no-YACL fallback")
if "ShineParticleRegistry.registerFactories()" not in client:
    errors.append("client particle provider registration missing")
if "ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)" not in client:
    errors.append("client resource reload registration missing")

print("clean_client_paths_checked=3")
print(f"errors={len(errors)}")
for error in errors:
    print(error)
if errors:
    raise SystemExit(1)
