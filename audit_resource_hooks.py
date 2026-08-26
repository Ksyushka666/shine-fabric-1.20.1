#!/usr/bin/env python3
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/shine"
JAVA = ROOT / "src/main/java/com/bloom"
errors = []
particle_json = sorted((ASSETS / "particles").glob("*.json"))
registry = (JAVA / "particle/ShineParticleTypes.java").read_text()
client_registry = (JAVA / "client/particle/ShineParticleRegistry.java").read_text()
post = (JAVA / "client/render/BloomPostProcessor.java").read_text()
client = (JAVA / "client/BloomClient.java").read_text()
if len(particle_json) != 84: errors.append(f"particle definition count mismatch: {len(particle_json)}")
for path in particle_json:
    particle_id = path.stem
    if f'"{particle_id}"' not in registry: errors.append(f"particle type not registered: {particle_id}")
if "for (SimpleParticleType type : types.values())" not in client_registry: errors.append("generic particle factory loop missing")
if not (ROOT / "src/main/resources/assets/shine/post_effect/bloom_poc.json").is_file(): errors.append("active bloom PostChain JSON missing")
if "BloomPostProcessor::prepareSourceIfEnabled" not in client or "BloomPostProcessor::renderIfEnabled" not in client: errors.append("active bloom world hooks missing")
if "bloom_poc" not in post: errors.append("PostProcessor does not reference active bloom_poc chain")
for required in ("defaults/shine.json", "defaults/experimental.json"):
    if not (ASSETS / required).is_file(): errors.append(f"bundled baseline missing: {required}")
print(f"particle_definitions={len(particle_json)}")
print("post_chain_resources=1")
print("world_render_hooks=2")
print(f"errors={len(errors)}")
for error in errors: print(error)
if errors: raise SystemExit(1)
