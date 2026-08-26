# Shine — Fabric 1.20.1 port status

## Rendering and Iris compatibility

The active bloom pipeline uses the Fabric 1.20.1 legacy `RenderTarget`/`PostChain` implementation. Selective source capture begins at world-render start, while the bloom PostChain is now executed at `WorldRenderEvents.END`, after the world has finished rendering. This prevents the effect from being applied prematurely before entities and terrain have contributed to the final frame.

Iris compatibility remains optional and reflective. Shine yields only when Iris explicitly confirms that a shader pack is active. If Iris is installed but its optional API is unavailable or cannot be queried, Shine no longer disables bloom; it continues with the legacy pipeline and logs a warning. This preserves the user-enabled bloom feature rather than treating an unverified Iris installation as a hard disable condition.

## Original visual baseline

The port now loads the bundled original `assets/shine/defaults/shine.json` through `BloomConfig.Data.defaults()`, rather than relying only on reduced hardcoded defaults. The schema includes the original extended bloom controls: `strength`, `threshold`, `radius`, `tinyRadius`, `broadRadius`, `radiusScaleVersion`, `blurPassCount`, `bloomDistance`, `highlightClamp`, `softKnee`, light/entity/particle source strengths, source/state/entity/particle overrides and source radius profiles.

The original baseline values are preserved, including strength 8.0, radius 500.0, tiny radius 90.0, broad radius 700.0, bloom distance 256.0, highlight clamp 0.28, soft knee 0.2, sculk 500.0 and torch 499.0. Legacy `blockStrengthOverrides` remains supported as an alias.

Selective bloom attachment lifecycle now records the originally bound framebuffer and both draw-buffer slots before enabling `COLOR_ATTACHMENT1`, restores them on detach, and temporarily rebinds the original framebuffer if a Vanilla/Sodium callback changed the current binding. Failed framebuffer completeness checks also roll back the attachment immediately.

The legacy 1.20.1 PostChain now receives dynamic BloomConfig values before processing. Runtime uniform binding is defensive: if the PostChain accessor is unavailable or a pass has no EffectInstance, Shine logs at most one warning and preserves the JSON defaults instead of crashing or disabling the bloom pipeline. `Threshold`, `Strength`, `SourceStrengthScale`, `HighlightClamp`, `SoftKnee`, `MaxDistance`, `DistanceFadeRange` and `Radius` are applied through the mapped `PostPass`/`EffectInstance` API. `blurPassCount` controls the active blur pairs, while `tinyRadius`, `radius` and `broadRadius` select the corresponding blur profile. The active runtime chain therefore responds to visual settings instead of only reading the saved JSON baseline.

The fast blur shader preserves the configured effective radius while bounding each pass to 13 texture fetches, preventing Shine's large radius range from turning into an unbounded per-pixel loop on 1.20.1 hardware.

All missing original `assets/shine` visual resources remain merged: particle/world ambience JSON, core/entity/particle shaders, bloom profile/downsample/upsample resources, sky, atmosphere, rimlight and effect textures. The automated resource audit reports 116 valid JSON files, 1,443 asset files and zero missing shader/import errors. A shader program audit also validates all 17 JSON-defined shader programs, their vertex/fragment companions, and explicit location 1 declarations for bloomColor; the audit currently reports zero errors. The shader overlay ownership audit is now wired into Gradle `check` and confirms 11 visual entity overlays, one particle overlay and the Iris ownership guard with zero conflicts. The five service-only variants (`armor_entity_glint`, `entity_decal`, `entity_glint`, `entity_glint_direct` and `entity_shadow`) remain the exact vanilla 1.20.1 shaders and do not write bloomColor, preventing glint/shadow/decal artifacts. The patch and restore tools are policy-aware and were executed twice with identical results, proving that repeated resource adaptation does not reintroduce bloom into service passes. It now also validates every local `shine:` texture referenced by the 84 particle definitions; vanilla `minecraft:` sprites are correctly treated as game-provided assets.

The YACL screen exposes the extended Shine 3.0 controls for `tinyRadius`, `radius`, `broadRadius`, blur pass count, bloom distance, threshold, highlight clamp, soft knee, default light/non-light source strength and entity/particle source strengths. All newly added controls have English locale entries, avoiding raw translation keys in the configuration UI. These controls bind directly to the copied `BloomConfig.Data` and are persisted through the existing save callback. BloomConfig sanitization rejects non-finite numeric values and drops invalid `ResourceLocation` override keys before they reach selection/runtime code. `BloomSelection` now resolves block/state and fluid overrides as well as entity texture and particle identifiers at runtime, with finite clamped fallbacks for each category. Client hooks around `EntityRenderer.render` and `ParticleEngine.render` push and restore the selected entity/particle strength during their 1.20.1 render lifecycles, making the values available to compatible source-output paths without loading client classes on the common entrypoint. The vanilla 1.20.1 particle shader is explicitly wired to `layout(location = 1) bloomColor`, with `ShineParticleStrength` supplied by the ShaderInstance hook; the required vanilla `fog.glsl` include is bundled and audited. All 11 visual vanilla entity fragment shader variants are likewise bundled from the 1.20.1 client resources, declare `layout(location = 1) bloomColor` and `ShineEntityStrength`, and receive the texture-specific strength from the EntityRenderer hook. The matching `fog.glsl` and `light.glsl` includes are bundled for deterministic shader loading.

The original experimental baseline is now loaded by `ExperimentalConfigManager` from `assets/shine/defaults/experimental.json`, with a user override at `config/shine/experimental.json`. Nested JSON values are merged safely, and failures fall back to bundled defaults without touching the bloom configuration or disabling selective bloom. The manager is initialized only on the client entrypoint. A client resource-reload listener now shuts down the active PostChain and source framebuffer before resources are rebuilt, then reloads the experimental state, preventing stale GPU targets after resource-pack changes. Its registration is guarded by a client-side idempotency flag, so repeated initializer calls cannot accumulate duplicate reload callbacks. The client entrypoint now also has a general initialization guard, covering tick, world-render and stopping callback registration as well as the reload listener. `BloomPostProcessor.shutdown()` resets Iris-disable and uniform-warning state in addition to closing the PostChain and source target.

All 84 transferred particle definition JSON files are registered through Fabric 1.20.1 `FabricParticleTypes.simple()`. Common registration is isolated in the server-safe `ShineParticleTypes` class and called from `BloomMod`; client-only texture-sheet factories remain in `ShineParticleRegistry` and are installed from `BloomClient`. The client factory registration is idempotent, preventing duplicate providers during repeated initialization or reload paths. The generic provider uses each definition's particle sprite set and translucent particle sheet, ensuring the transferred particle resources have a valid runtime registration path without loading client classes from the common entrypoint.

## Build and tests

`gradle clean check build` passes with Gradle 8.12 and Fabric Loom 1.10.2. `PortPipelineTest` passes and now verifies PostChain bindings, shader uniforms, framebuffer cleanup, Iris fallback behavior, END event ordering, disabled legacy mixin exclusion, original visual defaults, all 84 particle definitions, experimental config initialization, dynamic PostChain uniform binding, complete YACL radius/source-strength controls with localization coverage, robust config sanitization, block/state/fluid/entity/particle source override lookup, EntityRenderer/ParticleEngine selection hooks, particle and entity shader output integration with Iris ownership guard and service render-type exclusions, complete shader program resource audit, repeatable service render-type policy, defensive accessor/pass handling, radius-profile and blur-pass mapping, bounded blur sampling, Sodium 0.5.x version gating, conditional mixin application, idempotent Sodium shader transformation with varying guards and selective fragment-output binding, common registry server safety, idempotent factory registration and framebuffer/draw-buffer restoration across Vanilla/Sodium callbacks, client resource-reload cleanup and PostChain recreation, idempotent reload listener registration, repeated client initializer protection and lifecycle flag reset. The ambience audit confirms the transferred world-ambience particle definitions remain registered and that disabled 1.21 GPU/FrameGraph terrain markers are not active in the 1.20.1 mixin config. `visualResourceAudit`, `shaderOverlayAudit`, `remapJar` and `remapSourcesJar` also pass. The integrated audit reports `particle_definitions=84` with zero missing local sprite references and zero missing shader imports.

## Runtime qualification

The current active rendering path is adapted for Fabric 1.20.1 and Sodium 0.5.11. Entity and particle shader overlays are guarded against Iris ownership: when Iris confirms an active shader pack, Shine leaves Iris-owned ShaderInstance uniforms untouched rather than mutating a foreign program. Reflection failures continue safely without crashing the client, and the Vanilla/Sodium legacy path remains available when no shader pack owns the frame. `BloomMixinPlugin` now applies Sodium compatibility mixins only when Sodium is present and its metadata version is in the supported 0.5.x line; absent or newer incompatible Sodium builds leave the Vanilla path active instead of attempting unsafe targets. `SodiumShaderLoaderMixin` now transforms vertex/fragment sources only when all required 0.5.x template markers are present; partial or already-transformed sources are returned unchanged, preventing duplicate outputs and broken varying linkage. The `ShaderChunkRenderer` redirect binds the additional `bloomColor` output only alongside the primary `fragColor` binding and preserves unrelated fragment outputs. The expanded original 1.21 visual resources are preserved and validated as resources. A graphical client smoke test remains unavailable in the sandbox because Minecraft asset CDN downloads are blocked; local testing is still required with and without Sodium 0.5.11 and Iris.


## Latest continuation: framebuffer ownership safety

The selective bloom attachment bridge now records the pre-existing `GL_COLOR_ATTACHMENT1` texture before installing Shine's source texture. On framebuffer completeness failure, the original attachment is restored instead of being unconditionally detached. On normal detach, the original texture and draw-buffer configuration are restored even when Vanilla or Sodium changed the currently bound framebuffer between callbacks. `PortPipelineTest` now verifies this ownership-preserving path alongside the existing framebuffer cleanup checks.

The latest `gradle clean check build` completed successfully. The graphical client smoke test remains a separate runtime qualification step because the sandbox cannot launch a fully asset-backed Minecraft client.


## Latest continuation: Sodium shader transformation verification

The Sodium 0.5.11 jar was inspected directly. Its `ShaderLoader.getShaderSource(ResourceLocation)` is static, `ShaderChunkRenderer.begin/end(TerrainRenderPass)` are protected lifecycle methods, and the opaque shader resources contain the exact 0.5.x markers used by Shine. Regression fixtures now invoke the private vertex and fragment transformers reflectively and verify that the complete templates are transformed, a second transformation is a no-op, and incomplete templates remain unchanged. This guards against silent shader drift and duplicate varying/output declarations.

The latest `gradle clean check build` completed successfully with `PortPipelineTest: PASS`, zero shader overlay audit errors, and zero visual resource audit errors.


## Latest continuation: world disconnect cleanup

A Fabric 1.20.1 `ClientPlayConnectionEvents.DISCONNECT` hook now calls `BloomPostProcessor.shutdown()`. This releases the PostChain, detaches/restores any source framebuffer attachment, destroys the selective source target and resets runtime flags immediately when leaving a world. The existing `CLIENT_STOPPING` hook remains as a final client-level safety net. The lifecycle regression suite verifies both cleanup paths.

The latest `gradle clean check build` completed successfully with all automated audits passing.


## Latest continuation: dedicated-server safety hardening

The common `BloomMod` and `ShineParticleTypes` paths were audited for client-only dependencies. The common registry contains no Minecraft client, OpenGL, Fabric client API, YACL, Sodium or Iris references. Particle registration is now partial-failure-safe: each identifier is checked independently, already registered entries are reused, and an exception during a later entry does not make an incomplete non-empty map suppress future retries. `all()` returns an unmodifiable view, preventing external code from corrupting common registration state.

`PortPipelineTest` now verifies these server-safety and registry invariants. The latest `gradle clean check build` completed successfully with all audits passing.


## Latest continuation: configuration baseline safety

The BloomConfig runtime baseline now passes through the same sanitizer during static initialization, missing-config creation and malformed-config fallback. This prevents bundled Shine 3.0 defaults from bypassing finite-value, range and override-map validation before the first client tick. Regression coverage verifies both initialization and fallback paths, while YACL edits continue to operate on the sanitized mutable runtime copy.

The latest `gradle clean check build` completed successfully with `PortPipelineTest: PASS`, zero shader overlay audit errors and zero visual resource audit errors.


## Latest continuation: PostChain resource graph audit

A reproducible `postChainGraphAudit` now parses the active 1.20.1 `bloom_poc.json`, resolves every fragment shader, verifies that each declared sampler and uniform exists in the corresponding GLSL source, and confirms that non-main output targets are declared. The audit is integrated into Gradle `check`. It reports 8 passes, 4 targets and zero errors, preventing future resource or binding drift from reaching runtime unnoticed.

The latest `gradle clean check build` completed successfully with all automated audits passing.


## Latest continuation: resize lifecycle safety

The source target preparation path is now exception-safe during window resize and framebuffer recreation. If `TextureTarget` creation, resize or clear fails, Shine logs the failure, destroys the damaged target, resets its dimensions and clears `preparedThisFrame`, preventing stale GPU handles from reaching attachment or PostChain processing. The PostProcessor also rejects null or non-positive main target dimensions before creating or processing a PostChain. Regression checks cover both invalid dimensions and failed source-target preparation paths.

The latest `gradle clean check build` completed successfully. The PostChain graph audit reports 8 passes, 4 targets and zero errors.


## Latest continuation: entity and particle selection state isolation

`BloomSelectionState` now exposes a frame/world-boundary reset that clears all four thread-local channels: block, fluid, entity and particle strength. `BloomSourceRenderer.prepareSource()` invokes this reset before each world-render frame, and the existing full reset path invokes it during disconnect, resource reload and client shutdown. This prevents a render exception or unbalanced callback from carrying an entity/particle strength into a later frame or world. The entity and particle mixins continue to restore their immediate previous values for nested rendering, while the frame reset provides a defensive boundary against exceptional control flow.

The latest `gradle clean check build` completed successfully with all automated audits passing.


## Latest continuation: crash-safe configuration persistence

BloomConfig saving now writes JSON to `shine.json.tmp` first and replaces the live file with an atomic move where the filesystem supports it, falling back to a regular replace when atomic moves are unavailable. Failed saves clean up the temporary file. This prevents an interrupted YACL save or client shutdown from leaving a truncated configuration that would require fallback defaults on the next launch. Regression coverage verifies the temporary-file and atomic-move path.

The latest `gradle clean check build` completed successfully. The PostChain graph audit reports 8 passes, 4 targets and zero errors; shader overlay and visual resource audits also pass.


## Latest continuation: Mixin target coverage

The mapped Fabric Loom 1.20.1 client jar was inspected directly. Confirmed targets include `EntityRenderer.render`, `ParticleEngine.render`, `LevelRenderer.renderChunkLayer`, `ShaderInstance.apply`, `ModelBlockRenderer.putQuadData` and `LiquidBlockRenderer.tesselate`. The four core Vanilla selection/render mixins for EntityRenderer, ParticleEngine, LevelRenderer and ShaderInstance now use fail-fast injection annotations instead of `require=0`, so a future 1.20.1 mapping drift cannot silently disable core selective bloom. Optional Sodium compatibility mixins retain tolerant injection behavior because Sodium is an optional dependency and are gated by the 0.5.x plugin check.

The latest `gradle clean check build` completed successfully with `PortPipelineTest: PASS`, PostChain graph audit at 8 passes/4 targets/0 errors, shader overlay audit at 0 errors and visual resource audit at 116 JSON/1,443 assets/0 errors.


## Latest continuation: Vanilla terrain/fluid source encoding

The mapped Fabric 1.20.1 bytecode confirms that `LiquidBlockRenderer.tesselate` contains exactly three calls to its private `getLightColor` helper; the three ordinal redirects in Shine therefore cover all vanilla fluid light samples. Terrain `ModelBlockRenderer.putQuadData` and Indigo quad paths preserve vanilla light data while encoding the selected source strength into the bloom channel.

`BloomSourceEncoding` now handles non-finite source strengths explicitly, converting NaN and infinity to zero before packed-light or material-bit encoding. Regression coverage verifies the finite guard and all three fluid redirect ordinals.

The latest `gradle clean check build` completed successfully with all audits passing.


## Latest continuation: Fabric metadata and optional dependency matrix

Fabric metadata now explicitly documents Sodium 0.5.x and Iris as optional suggestions while retaining YACL as a required dependency for the complete configuration UI. The common `main` entrypoint remains separate from the client entrypoint, and the Mod Menu entrypoint remains optional. The Mixin plugin applies Sodium mixins only when Sodium is loaded and its version starts with `0.5.`, while Vanilla mixins remain unconditional.

`PortPipelineTest` now verifies the metadata entrypoints, required YACL dependency and optional Sodium/Iris declarations. The latest `gradle clean check build` completed successfully with all audits passing.


## Latest continuation: optional YACL/server compatibility

YACL is now declared as an optional suggestion rather than a hard dependency, allowing dedicated servers to load Shine without installing the client configuration library. The Mod Menu integration checks `FabricLoader.isModLoaded("yet_another_config_lib_v3")` before referencing `BloomConfigScreen`; when YACL is absent it returns the parent screen unchanged. With YACL installed, the full Shine configuration UI remains available. Sodium and Iris remain optional suggestions with the existing conditional Sodium mixin and Iris ownership guards.

The latest `gradle clean check build` completed successfully with all automated audits passing.


## Latest continuation: remapped artifact parity

A reproducible `artifactParityAudit` now runs after `remapJar` and compares every file under `src/main/resources` with the final remapped JAR. It also verifies the required Fabric metadata, mixin configuration, PostChain, defaults and Vanilla shader resources. The current artifact contains all 1,445 source resources with zero missing resources; required-resource checks pass. The audit is integrated into Gradle `check`.

The latest `gradle clean check build` completed successfully with all automated audits passing.


## Latest continuation: Sodium 0.5.x version gate

The Sodium compatibility predicate is now isolated, null-safe and covered by fixtures. It accepts versions such as `0.5.11+mc1.20.1` and rejects 0.4.x, 0.6.x and malformed/null version metadata. The Mixin plugin still applies Sodium mixins only when Sodium is present and the predicate succeeds; Vanilla bloom mixins remain independent of Sodium.

The latest `gradle clean check build` completed successfully. Artifact parity confirms all 1,445 source resources are present in the remapped JAR.


## Latest continuation: configuration concurrency safety

BloomConfig now publishes its active Data reference as volatile, synchronizes set/copy/load operations, and serializes a defensive Data snapshot instead of the mutable live object. ExperimentalConfigManager uses volatile state and synchronized load/reload/enabled/snapshot methods. This keeps YACL edits, resource reload and save operations from observing or writing a partially mutated configuration.

The corrected build run reached `BUILD SUCCESSFUL`; all 15 tasks completed, including PortPipelineTest, PostChain graph audit, shader overlay audit, artifact parity and visual resource audit.


## Latest continuation: clean runtime artifact safety

A `cleanRuntimeAudit` now inspects the final remapped JAR and common class constant pools. It verifies universal metadata, the common main entrypoint, required runtime dependencies, optional suggestions, required artifact entries and the absence of client/OpenGL/Sodium/Iris/YACL/Mod Menu references from `BloomMod` and `ShineParticleTypes`. The current artifact passes with two common classes checked and zero errors.

The audit is integrated into Gradle `check`. The latest `gradle clean check build` completed successfully with all 16 tasks passing.


## Latest continuation: clean client optional loading

A `cleanClientAudit` now verifies that `BloomClient` and `ShineParticleRegistry` do not directly reference Mod Menu, YACL, Sodium or Iris classes. It also verifies that client particle providers and resource reload listeners remain registered, while the Mod Menu integration contains a no-YACL parent-screen fallback. This confirms that the main bloom pipeline can initialize on a clean Vanilla client without optional UI or compatibility libraries.

The latest `gradle clean check build` completed successfully with all 17 tasks passing. Clean client and clean runtime audits both report zero errors.


## Latest continuation: final remapped artifact metadata

A `finalMetadataAudit` now inspects the expanded `fabric.mod.json` and manifest inside the remapped JAR. It verifies mod id `shine`, resolved version `1.0.0`, universal environment, Minecraft dependency `~1.20.1`, main/client entrypoints and the Fabric Loom remapped-mixin marker. The current artifact passes with zero metadata errors.

The latest `gradle clean check build` completed successfully with all 18 tasks passing.


## Latest continuation: Iris reflection fallback safety

IrisCompat now uses volatile cached state and synchronized lazy reflection initialization. Iris API classes are resolved only when Iris is actually loaded. Missing API classes, missing methods and invocation failures remain non-fatal: Shine keeps the legacy Vanilla/Sodium bloom path active, while Iris shader-pack ownership is respected whenever Iris positively confirms an active pack.

The latest `gradle clean check build` completed successfully with all 18 tasks passing, including Iris fallback regression assertions, clean client/runtime audits and final metadata validation.


## Latest continuation: YACL visual settings parity

YACL 3.6.6 bindings are now covered for every scalar runtime visual setting: enable state, strength, threshold, radius profiles, blur pass count, bloom distance, highlight clamp, soft knee and all default source-strength controls. The screen edits a defensive copy, applies it only through the YACL save callback, persists it atomically and triggers post-processing/chunk refresh. Reopening the screen reads the current persisted configuration through `BloomConfig.copy()`.

The latest `gradle clean check build` completed successfully with all 18 tasks passing.


## Latest continuation: particle provider parity

The client particle registry now uses a volatile, synchronized registration guard and commits the guard only after all common Shine particle types have been assigned providers. If a provider registration throws, a later client initialization may retry instead of inheriting a false-complete state. Common particle registration remains idempotent and server-safe.

The latest `gradle clean check build` completed successfully with all 18 tasks passing. Particle, shader, resource, artifact, clean-runtime and metadata audits all pass.


## Latest continuation: PostChain resource failure fallback

BloomPostProcessor now treats PostChain creation and processing failures as recoverable. Missing or malformed shader resources are logged once, the runtime chain is discarded, and bloom remains disabled until the next reload/config save instead of repeatedly logging or retaining a broken chain. Runtime processing exceptions also close the chain and reset the source renderer. Failure guards are cleared on config save and client shutdown, allowing recovery after resources become available again.

The latest `gradle clean check build` completed successfully with all 18 tasks passing, including resource graph, artifact parity, clean-runtime, clean-client, metadata and regression audits.


## Latest continuation: fullscreen and graphics-mode transitions

The source and post-processing paths are resize-aware for fullscreen/windowed and graphics-mode target changes. BloomPostProcessor recreates the PostChain when main target dimensions change; BloomSourceRenderer resizes its source target to the same dimensions. The copy path captures and restores both GL read and draw framebuffer bindings in a finally block, while invalid dimensions and processing failures remain non-fatal.

The latest `gradle clean check build` completed successfully with all 18 tasks passing, including graphics transition regression assertions and all existing resource, artifact, client/runtime and metadata audits.


## Latest continuation: Fabric world-render callback ordering

A `renderCallbackAudit` now verifies exactly one Fabric `WorldRenderEvents.START` registration for source capture and exactly one `WorldRenderEvents.END` registration for bloom processing. It also verifies that START is registered before END, the prepared-source guard is present, and disconnect/client-stopping cleanup hooks remain installed. The latest `gradle clean check build` completed successfully with all 19 tasks passing.


## Latest continuation: GL state restoration

BloomPostProcessor now captures the active viewport and blend/depth enable states before source copy and PostChain processing. A finally block restores the viewport and RenderSystem blend/depth state even when the chain throws. Existing read/draw framebuffer restoration remains in copyTarget. This prevents Shine from leaking post-processing state into subsequent Vanilla or Sodium render layers.

The latest `gradle clean check build` completed successfully with all 19 tasks passing, including GL state regression assertions and all resource, artifact, client/runtime, callback and metadata audits.


## Latest continuation: source frame clear lifecycle

BloomSourceRenderer resets selection state and clears `preparedThisFrame` before every world-render source preparation. The source target is cleared to transparent before the prepared flag is committed, so an empty selective capture cannot reuse pixels from a previous frame. Unprepared or failed captures return no source target to PostChain, preventing stale bloom output.

The latest `gradle clean check build` completed successfully with all 19 tasks passing, including frame-clear, GL state, callback, resource, artifact, client/runtime and metadata audits.


## Latest continuation: source JAR parity

A `sourceParityAudit` now compares every Java source under `src/main/java` with the root-level Java entries in the remapped sources JAR. The initial audit exposed and corrected a path-assumption bug in the audit itself; the final check reports 28 project sources, 28 source-JAR entries, zero missing and zero extra entries.

The audit is integrated into Gradle `check`. The corrected `gradle clean check build` completed successfully with all 20 tasks passing.


## Latest continuation: package cleanliness

A `packageCleanlinessAudit` now verifies that the installable mod JAR contains no Java sources, build logs, audit files, temporary files or regression-test artifacts, and that metadata has no unresolved placeholders. The first run exposed a Gradle ordering issue because the audit did not wait for `remapSourcesJar`; the task dependency was corrected. The final audit passes with 1,601 mod-JAR entries and 1,598 source-JAR entries.

The latest `gradle clean check build` completed successfully with all 21 tasks passing, including source parity, package cleanliness, final metadata, artifact parity, clean client/runtime, callback, resource and rendering audits.


## Latest continuation: server/client mixin environment separation

A `mixinEnvironmentAudit` now verifies that the active `shine.mixins.json` uses a client-only section with no common mixins, retains the declared `BloomMixinPlugin`, and keeps Sodium selection behind the null-safe supported-version predicate. The initial audit caught an incorrect assumption in the audit tool about package-relative names; that false positive was corrected. The final environment audit passes with 13 client mixins, zero common mixins and zero errors.

The latest `gradle clean check build` completed successfully with all 22 tasks passing.


## Latest continuation: GLSL bloom output parity

A `glslOutputAudit` now checks every entity fragment overlay and the particle fragment overlay for a single `bloomColor` write, location 1 declaration, matching Shine strength uniform and corresponding JSON program uniform. The current artifact passes with 11 entity fragment outputs, one particle output and zero errors.

The audit is integrated into Gradle `check`. The latest `gradle clean check build` completed successfully with all 23 tasks passing.

## Latest continuation: runtime hardening

Added `runtimeHardeningAudit` to Gradle `check`. It verifies that common sources contain no client-only imports/tokens, that archived `.legacy21` files remain excluded from compilation, that client world-render/disconnect/stop hooks are registered, that frame-boundary selection state is reset, and that the postprocessor chain is released and nulled during cleanup.

The audit initially reported the two intentionally archived `.legacy21` migration references as errors. The audit was corrected to treat these non-`.java` files as archived references rather than compile-tree sources. The final audit passes with three common sources checked, two archived legacy references recorded, and zero errors. The subsequent full `gradle clean check build` completed successfully with all 24 tasks passing.

## Latest continuation: runtime callback safety

Added `runtimeCallbackAudit` to Gradle `check`. It verifies disabled-bloom and Iris-skip cleanup, PostChain creation/process failure cleanup, viewport/blend/depth restoration, source-target exception recovery, frame reset and world-render callback registration. The initial audit false-positive was corrected to inspect the shared PostProcessor/SourceRenderer lifecycle. Final result: 12 checks, zero errors.

The latest `gradle clean check build` completed successfully with all 25 tasks passing.

## Latest continuation: optional configuration integrations

Added `configIntegrationAudit` to Gradle `check`. It verifies dual-environment metadata, separate common/client entrypoints, optional Mod Menu registration, lazy YACL presence detection, absence of YACL/client dependencies in the common entrypoint, client-side config initialization, and atomic configuration persistence. The final audit reports 10 checks and zero errors.

The latest `gradle clean check build` completed successfully with all 26 tasks passing.

## Latest continuation: remapped artifact validation

Added `remappedArtifactAudit` to Gradle `check`. It opens the remapped mod and sources JAR directly, validates expanded Fabric metadata, confirms required common/resource entries, rejects source/archive/build files leaking into the mod JAR, and verifies the 28-file source artifact. The final audit reports zero errors.

The latest `gradle clean check build` completed successfully with all 27 tasks passing.

## Latest continuation: resource-to-hook parity

Added `resourceHookAudit` to Gradle `check`. It verifies all 84 particle definitions are represented by common registration, the generic client factory loop is active, the `post_effect/bloom_poc.json` resource exists, both world-render hooks are registered, the PostProcessor references the active chain, and both bundled configuration baselines are present. The initial audit correctly exposed path/registration assumptions in the audit itself; those were aligned with the actual 1.20.1 implementation. Final result: 84 particle definitions, one active PostChain resource, two world hooks and zero errors.

The latest `gradle clean check build` completed successfully with all 28 tasks passing.

## Public CI portability fix

The public GitHub Actions build initially failed because the runner's Gradle 9 test task treated the JavaExec-based `PortPipelineTest` as a source set with no discovered JUnit tests. The build configuration now disables only the empty JUnit `test` task while retaining `PortPipelineTest` as an explicit `check` dependency. Optional integrations use Maven coordinates instead of local sandbox cache paths. Local `gradle clean check build` passes with 27 tasks.

## Latest continuation: Loom remapping warning validation

Added `remapWarningAudit` to Gradle `check`. It confirms that no obsolete `setShaderTexture` reference or developer-local Gradle cache path remains in the source/build configuration. The observed Loom message was a non-fatal remapping diagnostic from optional dependency processing, not an active source reference. Local `gradle clean check build` passes with 28 tasks.
