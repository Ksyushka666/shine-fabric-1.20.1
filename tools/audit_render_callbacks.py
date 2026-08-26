#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
client = (ROOT / "src/main/java/com/bloom/client/BloomClient.java").read_text()
post = (ROOT / "src/main/java/com/bloom/client/render/BloomPostProcessor.java").read_text()
errors = []

if client.count("WorldRenderEvents.START.register(BloomPostProcessor::prepareSourceIfEnabled)") != 1:
    errors.append("expected exactly one START source registration")
if client.count("WorldRenderEvents.END.register(BloomPostProcessor::renderIfEnabled)") != 1:
    errors.append("expected exactly one END bloom registration")
if client.index("WorldRenderEvents.START.register") > client.index("WorldRenderEvents.END.register"):
    errors.append("START registration occurs after END registration")
if "ClientPlayConnectionEvents.DISCONNECT.register" not in client or "ClientLifecycleEvents.CLIENT_STOPPING.register" not in client:
    errors.append("lifecycle cleanup hooks missing")
if "hasPreparedSourceThisFrame" not in post or "BloomSourceRenderer.prepareSource(context)" not in post:
    errors.append("source capture/prepared-frame guard missing")
if "chain.process(0.0F)" not in post:
    errors.append("bloom processing at END path missing")

print("render_callbacks_checked=2")
print(f"errors={len(errors)}")
for error in errors: print(error)
if errors: raise SystemExit(1)
