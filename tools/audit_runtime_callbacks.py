#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
post = (ROOT / "src/main/java/com/bloom/client/render/BloomPostProcessor.java").read_text()
source = (ROOT / "src/main/java/com/bloom/client/render/BloomSourceRenderer.java").read_text()
client = (ROOT / "src/main/java/com/bloom/client/BloomClient.java").read_text()
errors = []
checks = {
    "disabled bloom clears source": "BloomSourceRenderer.reset()",
    "Iris ownership skip clears chain": "closeRuntimeChain();\n            BloomSourceRenderer.reset();",
    "PostChain process failure closes chain": "chainFailureLogged = true;\n            }\n            closeRuntimeChain();",
    "PostChain creation failure nulls chain": "runtimeChain = null;\n            return null;",
    "GL viewport restored": "GL11.glViewport(previousViewport[0], previousViewport[1], previousViewport[2], previousViewport[3]);",
    "blend state restored": "if (previousBlend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();",
    "depth state restored": "if (previousDepth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();",
    "source prepare catches runtime errors": "catch (RuntimeException exception)",
    "source prepare destroys failed target": "sourceTarget.destroyBuffers();",
    "source frame reset before prepare": "BloomSelectionState.reset();",
    "world start callback registered": "WorldRenderEvents.START.register(BloomPostProcessor::prepareSourceIfEnabled)",
    "world end callback registered": "WorldRenderEvents.END.register(BloomPostProcessor::renderIfEnabled)",
}
for name, marker in checks.items():
    haystack = client if "callback" in name else ((source + "\n" + post) if "disabled bloom" in name else (source if ("source" in name or "frame" in name) else post))
    if marker not in haystack:
        errors.append(name)
print(f"runtime_callback_checks={len(checks)}")
print(f"errors={len(errors)}")
for error in errors: print(f"missing: {error}")
if errors: raise SystemExit(1)
