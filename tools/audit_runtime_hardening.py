#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src/main/java/com/bloom"
errors = []
legacy = list(SRC.rglob("*.legacy21"))
# These files are intentionally renamed out of the Java source set and retained
# only as migration references; Gradle compiles *.java, not *.legacy21.
common_files = [p for p in SRC.rglob("*.java") if "/client/" not in p.as_posix() and "/mixin/client/" not in p.as_posix()]
client_tokens = ("net.minecraft.client", "com.mojang.blaze3d", "RenderSystem", "MinecraftClient")
for p in common_files:
    text = p.read_text()
    for token in client_tokens:
        if token in text:
            errors.append(f"client token {token} in common source: {p.relative_to(ROOT)}")
client = (SRC / "client/BloomClient.java").read_text()
processor = (SRC / "client/render/BloomPostProcessor.java").read_text()
source = (SRC / "client/render/BloomSourceRenderer.java").read_text()
required_hooks = (
    "ClientPlayConnectionEvents.DISCONNECT",
    "ClientLifecycleEvents.CLIENT_STOPPING",
    "WorldRenderEvents.START.register",
    "WorldRenderEvents.END.register",
    "BloomPostProcessor.shutdown()",
)
for hook in required_hooks:
    if hook not in client: errors.append(f"lifecycle hook missing: {hook}")
if "BloomSelectionState.reset()" not in source: errors.append("frame-boundary selection reset missing")
if "closeRuntimeChain()" not in processor or "runtimeChain = null" not in processor: errors.append("postprocessor chain cleanup missing")
print(f"common_sources_checked={len(common_files)}")
print(f"legacy_sources_archived={len(legacy)}")
print(f"errors={len(errors)}")
for error in errors: print(error)
if errors: raise SystemExit(1)
