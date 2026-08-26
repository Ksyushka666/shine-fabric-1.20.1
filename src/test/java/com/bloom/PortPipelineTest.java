package com.bloom;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;

/** Lightweight build-time tests for the Fabric 1.20.1 post-effect pipeline. */
public final class PortPipelineTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path JAVA = Path.of("src/main/java");

    public static void main(String[] args) throws Exception {
        testJsonAndShaderFilesExist();
        testPostChainBindings();
        testDynamicUniformBinding();
        testShaderUniforms();
        testLifecycleCleanup();
        testNoDisabledMixinIsActive();
        testMixinOptionality();
        testSodiumShaderTransformationSafety();
        testSodiumTransformationFixtures();
        testConfigLocaleAndSanitization();
        testSourceStrengthOverrideRuntime();
        testEntityParticleHooks();
        testParticleShaderOutputPath();
        testEntityShaderOutputPath();
        testOverlayToolPolicy();
        testIrisAndEventOrdering();
        testParticleRegistryCoverage();
        testAmbienceAndLegacyIsolation();
        System.out.println("PortPipelineTest: PASS");
    }

    private static void testJsonAndShaderFilesExist() throws Exception {
        require(Files.isRegularFile(RESOURCES.resolve("assets/shine/shaders/post/bloom_poc.json")), "missing bloom PostChain JSON");
        require(Files.isRegularFile(RESOURCES.resolve("assets/shine/shaders/post/bloom_extract.fsh")), "missing extraction shader");
        require(Files.isRegularFile(RESOURCES.resolve("assets/shine/shaders/post/bloom_blur_horizontal.fsh")), "missing blur shader");
        require(Files.isRegularFile(RESOURCES.resolve("assets/shine/shaders/post/bloom_composite.fsh")), "missing composite shader");
        require(Files.isRegularFile(RESOURCES.resolve("assets/shine/defaults/experimental.json")), "missing experimental baseline");
        require(Files.isRegularFile(JAVA.resolve("com/bloom/client/config/ExperimentalConfigManager.java")), "missing experimental config manager");
        require(Files.isRegularFile(RESOURCES.resolve("assets/minecraft/shaders/core/terrain.fsh")), "missing terrain shader");
        String metadata = Files.readString(RESOURCES.resolve("fabric.mod.json"));
        require(metadata.contains("\"main\"") && metadata.contains("com.bloom.BloomMod"), "common entrypoint missing");
        require(metadata.contains("\"client\"") && metadata.contains("com.bloom.client.BloomClient"), "client entrypoint missing");
        require(metadata.contains("yet_another_config_lib_v3"), "YACL suggestion missing");
        require(metadata.contains("\"suggests\"") && metadata.indexOf("yet_another_config_lib_v3") > metadata.indexOf("\"suggests\""), "YACL must remain optional for dedicated servers");
        require(metadata.contains("\"sodium\": \"0.5.x\"") && metadata.contains("\"iris\": \"*\""), "optional Sodium/Iris metadata missing");
        String modMenu = Files.readString(JAVA.resolve("com/bloom/client/config/BloomModMenuIntegration.java"));
        require(modMenu.contains("isModLoaded(\"yet_another_config_lib_v3\")") && modMenu.contains("return parent -> parent"), "Mod Menu no-YACL fallback missing");
    }

    private static void testPostChainBindings() throws Exception {
        String json = Files.readString(RESOURCES.resolve("assets/shine/shaders/post/bloom_poc.json"));
        for (String sampler : new String[] {"DepthSampler", "SourceSampler", "InSampler", "MainSampler", "HalfSampler"}) {
            require(json.contains("\"sampler_name\": \"" + sampler + "\""), "missing sampler binding: " + sampler);
        }
        for (String uniform : new String[] {"SourceStrengthScale", "DistanceFadeRange", "Weight0", "Weight1", "Weight2", "Weight3", "Weight4", "Weight5"}) {
            require(json.contains("\"name\": \"" + uniform + "\""), "missing PostChain uniform: " + uniform);
        }
        require(json.contains("\"target\": \"source\""), "missing source target binding");
        String defaults = Files.readString(RESOURCES.resolve("assets/shine/defaults/shine.json"));
        for (String original : new String[] {"\"strength\": 8.0", "\"radius\": 500.0", "\"tinyRadius\": 90.0", "\"broadRadius\": 700.0", "\"bloomDistance\": 256.0", "\"highlightClamp\": 0.28", "\"softKnee\": 0.2", "\"minecraft:sculk\": 500.0", "\"minecraft:torch\": 499.0"}) {
            require(defaults.contains(original), "original visual default missing: " + original);
        }
    }

    private static void testDynamicUniformBinding() throws Exception {
        String processor = Files.readString(JAVA.resolve("com/bloom/client/render/BloomPostProcessor.java"));
        String accessor = Files.readString(JAVA.resolve("com/bloom/mixin/client/accessor/PostChainAccessor.java"));
        String blur = Files.readString(RESOURCES.resolve("assets/shine/shaders/post/bloom_blur_horizontal.fsh"));
        require(processor.contains("applyConfigUniforms(chain, config)"), "BloomConfig is not applied before PostChain processing");
        for (String uniform : new String[] {"Threshold", "Strength", "MaxDistance", "DistanceFadeRange", "Radius"}) {
            require(processor.contains("\"" + uniform + "\""), "dynamic bloom uniform binding missing: " + uniform);
        }
        require(accessor.contains("@Accessor(\"passes\")") && accessor.contains("shine$getPasses"), "1.20.1 PostChain passes accessor missing");
        require(processor.contains("config.tinyRadius") && processor.contains("config.radius") && processor.contains("config.broadRadius"), "bloom radius profile mapping missing");
        require(processor.contains("activeBlurPasses") && processor.contains("config.blurPassCount"), "blurPassCount mapping missing");
        require(blur.contains("uniform float Radius") && blur.contains("sampleCount = 6.0") && blur.contains("actualRadius") && blur.contains("for (int i = 1; i <= 6; ++i)"), "bounded blur sampling implementation missing");
        require(processor.contains("instanceof PostChainAccessor accessor"), "PostChain accessor fallback missing");
        require(processor.contains("if (effect == null) continue"), "null PostPass effect guard missing");
    }

    private static void testShaderUniforms() throws Exception {
        String extract = Files.readString(RESOURCES.resolve("assets/shine/shaders/post/bloom_extract.fsh"));
        String blur = Files.readString(RESOURCES.resolve("assets/shine/shaders/post/bloom_blur_horizontal.fsh"));
        String composite = Files.readString(RESOURCES.resolve("assets/shine/shaders/post/bloom_composite.fsh"));
        String terrain = Files.readString(RESOURCES.resolve("assets/minecraft/shaders/core/terrain.fsh"));
        require(extract.contains("uniform sampler2D DepthSampler") && extract.contains("uniform sampler2D SourceSampler"), "extract sampler uniforms mismatch");
        require(extract.contains("uniform float SourceStrengthScale") && extract.contains("uniform float DistanceFadeRange"), "extract scalar uniforms mismatch");
        require(blur.contains("uniform sampler2D InSampler") && blur.contains("uniform vec2 BlurDir"), "blur uniforms mismatch");
        require(composite.contains("uniform sampler2D HalfSampler") && composite.contains("uniform float Weight5"), "composite uniforms mismatch");
        require(terrain.contains("out vec4 bloomColor;") && !terrain.contains("layout(location"), "terrain bloom output missing or GLSL 1.50-incompatible");
    }

    private static void testLifecycleCleanup() throws Exception {
        String source = Files.readString(JAVA.resolve("com/bloom/client/render/BloomSourceRenderer.java"));
        String processor = Files.readString(JAVA.resolve("com/bloom/client/render/BloomPostProcessor.java"));
        String client = Files.readString(JAVA.resolve("com/bloom/client/BloomClient.java"));
        require(source.contains("destroyBuffers()"), "source RenderTarget cleanup missing");
        require(processor.contains("runtimeChain.close()"), "PostChain cleanup missing");
        require(client.contains("CLIENT_STOPPING"), "client shutdown hook missing");
        require(client.contains("ClientPlayConnectionEvents.DISCONNECT") && client.contains("BloomPostProcessor.shutdown()"), "world disconnect cleanup hook missing");
        require(client.contains("clientInitialized") && client.contains("skipping duplicate registrations"), "client initializer is not idempotent");
        require(processor.contains("irisDisabled = false") && processor.contains("uniformBindingWarningLogged = false"), "post processor shutdown does not reset lifecycle flags");
        require(source.contains("attached"), "framebuffer attachment guard missing");
        require(source.contains("glCheckFramebufferStatus"), "framebuffer completeness check missing");
        require(source.contains("GL_COLOR_ATTACHMENT1"), "selective attachment missing");
        require(source.indexOf("detachSourceFromCurrentFramebuffer()") < source.indexOf("destroyBuffers()"), "source framebuffer must detach before destruction");
        require(processor.contains("GL_READ_FRAMEBUFFER_BINDING") && processor.contains("GL_DRAW_FRAMEBUFFER_BINDING"), "copyTarget must restore GL framebuffer bindings");
        require(source.contains("attachedFramebuffer") && source.contains("previousDrawBuffer0") && source.contains("previousDrawBuffer1"), "source attachment must restore prior framebuffer draw state");
        require(source.contains("previousColorAttachment1") && source.contains("GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME"), "source attachment must preserve prior color attachment");
        require(source.contains("previousColorAttachment1, 0"), "source attachment rollback/detach must restore prior texture");
        require(source.contains("currentFramebuffer != attachedFramebuffer"), "source detach must handle framebuffer changes between callbacks");
        require(source.contains("Unable to prepare Shine selective bloom source target") && source.contains("sourceTarget.destroyBuffers()"), "source target resize failure is not exception-safe");
        require(processor.contains("main == null || main.width <= 0 || main.height <= 0") && processor.contains("if (width <= 0 || height <= 0) return null"), "PostChain resize has no invalid-dimension guard");
        String vanilla = Files.readString(JAVA.resolve("com/bloom/mixin/client/LevelRendererBloomAttachmentMixin.java"));
        String sodium = Files.readString(JAVA.resolve("com/bloom/mixin/client/sodium/SodiumShaderChunkRendererMixin.java"));
        require(vanilla.contains("attachSourceToCurrentFramebuffer") && vanilla.contains("detachSourceFromCurrentFramebuffer"), "vanilla attachment hooks are not paired");
        require(sodium.contains("attachSourceToCurrentFramebuffer") && sodium.contains("detachSourceFromCurrentFramebuffer"), "Sodium attachment hooks are not paired");
    }

    private static void testNoDisabledMixinIsActive() throws Exception {
        String mixins = Files.readString(RESOURCES.resolve("shine.mixins.json"));
        require(!mixins.contains("ChunkSectionsToRenderMixin"), "disabled 1.21 mixin is active");
        require(!mixins.contains("SectionCompilerMixin"), "disabled 1.21 mixin is active");
        require(Pattern.compile("\\\"LevelRendererBloomAttachmentMixin\\\"").matcher(mixins).find(), "vanilla attachment mixin missing");
    }

    private static void testMixinOptionality() throws Exception {
        String plugin = Files.readString(JAVA.resolve("com/bloom/mixin/BloomMixinPlugin.java"));
        require(plugin.contains("SODIUM_COMPATIBLE"), "Sodium compatibility gate missing");
        require(plugin.contains("version.startsWith(\"0.5.\")"), "Sodium 0.5.x version gate missing");
        require(plugin.contains("SODIUM_LOADED && SODIUM_COMPATIBLE"), "Sodium mixins are not conditionally applied");
        Method sodiumVersion = Class.forName("com.bloom.mixin.BloomMixinPlugin").getDeclaredMethod("isSupportedSodiumVersionString", String.class);
        sodiumVersion.setAccessible(true);
        require((Boolean) sodiumVersion.invoke(null, "0.5.11+mc1.20.1"), "Sodium 0.5.x predicate rejects supported version");
        require(!(Boolean) sodiumVersion.invoke(null, "0.6.0") && !(Boolean) sodiumVersion.invoke(null, "0.4.10") && !(Boolean) sodiumVersion.invoke(null, new Object[] {null}), "Sodium predicate accepts unsupported/null version");
    }

    private static void testSodiumShaderTransformationSafety() throws Exception {
        String loader = Files.readString(JAVA.resolve("com/bloom/mixin/client/sodium/SodiumShaderLoaderMixin.java"));
        require(loader.contains("transformVertex(shader)") && loader.contains("transformFragment(shader)"), "Sodium shader transformation helpers missing");
        require(loader.contains("if (!transformed.equals(shader)) cir.setReturnValue(transformed)"), "Sodium transformer is not idempotent-safe");
        require(loader.contains("!shader.contains(VERTEX_COLOR_DECL)") && loader.contains("!shader.contains(FRAGMENT_COLOR_DECL)"), "partial shader template guards missing");
        require(loader.contains("out float v_ShineSourceStrength") && loader.contains("in float v_ShineSourceStrength"), "Sodium varying linkage missing");
        String chunk = Files.readString(JAVA.resolve("com/bloom/mixin/client/sodium/SodiumShaderChunkRendererMixin.java"));
        require(chunk.contains("if (!\"fragColor\".equals(name))"), "Sodium output redirect must preserve non-color bindings");
        require(chunk.contains("bindFragmentData(\"bloomColor\", 1)"), "Sodium bloom output binding missing");
    }

    private static void testSodiumTransformationFixtures() throws Exception {
        Class<?> loader = Class.forName("com.bloom.mixin.client.sodium.SodiumShaderLoaderMixin");
        Method vertex = loader.getDeclaredMethod("transformVertex", String.class);
        Method fragment = loader.getDeclaredMethod("transformFragment", String.class);
        vertex.setAccessible(true);
        fragment.setAccessible(true);
        String vertexFixture = "out vec4 v_Color;\nvoid main() {\n    v_Color = _vert_color * _sample_lightmap(u_LightTex, _vert_tex_light_coord);\n}";
        String transformedVertex = (String) vertex.invoke(null, vertexFixture);
        require(transformedVertex.contains("v_ShineSourceStrength") && transformedVertex.contains("shine_decode_light_strength"), "Sodium vertex fixture was not transformed");
        require(vertex.invoke(null, transformedVertex).equals(transformedVertex), "Sodium vertex transformation is not idempotent");
        String fragmentFixture = "out vec4 fragColor; // The output fragment for the color framebuffer\nvoid main() {\n    fragColor = _linearFog(diffuseColor, v_FragDistance, u_FogColor, u_FogStart, u_FogEnd);\n}";
        String transformedFragment = (String) fragment.invoke(null, fragmentFixture);
        require(transformedFragment.contains("in float v_ShineSourceStrength") && transformedFragment.contains("out vec4 bloomColor"), "Sodium fragment fixture was not transformed");
        require(fragment.invoke(null, transformedFragment).equals(transformedFragment), "Sodium fragment transformation is not idempotent");
        String partialFixture = "out vec4 v_Color;\nvoid main() {\n    v_Color = vec4(1.0);\n}";
        require(vertex.invoke(null, partialFixture).equals(partialFixture), "partial Sodium vertex template must remain unchanged");
    }

    private static void testConfigLocaleAndSanitization() throws Exception {
        String screen = Files.readString(JAVA.resolve("com/bloom/client/config/BloomConfigScreen.java"));
        String config = Files.readString(JAVA.resolve("com/bloom/client/config/BloomConfig.java"));
        String locale = Files.readString(RESOURCES.resolve("assets/shine/lang/en_us.json"));
        for (String key : new String[] {"tiny_radius", "broad_radius", "light_source_strength", "non_light_source_strength", "entity_source_strength", "particle_source_strength"}) {
            require(screen.contains("shine.config." + key), "YACL control missing: " + key);
            require(locale.contains("shine.config." + key), "locale key missing: " + key);
        }
        require(config.contains("Double.isFinite(value)"), "non-finite config sanitization missing");
        require(config.contains("private static volatile Data data = sanitize(Data.defaults())"), "initial config baseline bypasses sanitization");
        require(config.contains("data = sanitize(Data.defaults());"), "config fallback does not sanitize bundled defaults");
        require(config.contains("StandardCopyOption.ATOMIC_MOVE") && config.contains(".tmp"), "config save is not crash-safe/atomic");
        require(config.contains("volatile Data data") && config.contains("synchronized Data copy()") && config.contains("synchronized void set"), "BloomConfig mutable state lacks concurrency guards");
        require(config.contains("synchronized void load") && config.contains("Data snapshot") && config.contains("GSON.toJson(snapshot"), "BloomConfig save/load is not snapshot-consistent");
        String experimental = Files.readString(JAVA.resolve("com/bloom/client/config/ExperimentalConfigManager.java"));
        require(experimental.contains("volatile JsonObject state") && experimental.contains("synchronized void load") && experimental.contains("synchronized JsonObject snapshot"), "experimental config state lacks concurrency guards");
        String screenSource = Files.readString(JAVA.resolve("com/bloom/client/config/BloomConfigScreen.java"));
        for (String field : new String[] {"enabled", "strength", "threshold", "radius", "tinyRadius", "broadRadius", "blurPassCount", "bloomDistance", "highlightClamp", "softKnee", "defaultLightSourceStrength", "defaultNonLightStrength", "defaultEntityTextureStrength", "defaultParticleStrength"}) {
            require(screenSource.contains("editing." + field), "YACL binding missing: " + field);
        }
        require(screenSource.contains("BloomConfig.copy()") && screenSource.contains("BloomConfig.set(editing)") && screenSource.contains("BloomConfig.save()"), "YACL save/reopen workflow is incomplete");
        require(config.contains("ResourceLocation parsed = ResourceLocation.tryParse") && config.contains("if (parsed == null) continue"), "invalid override identifier guard missing");
    }

    private static void testSourceStrengthOverrideRuntime() throws Exception {
        String selection = Files.readString(JAVA.resolve("com/bloom/client/selection/BloomSelection.java"));
        require(selection.contains("getEntitySourceStrength") && selection.contains("getParticleSourceStrength"), "entity/particle source override lookup missing");
        require(selection.contains("stateSourceStrengthOverrides") && selection.contains("getBlockSourceStrength"), "state source override lookup missing");
        require(selection.contains("Double.isFinite(value)"), "source strength finite fallback missing");
        require(selection.contains("entityTextureStrengthOverrides.get(textureId.toString())"), "entity texture override is not connected");
        require(selection.contains("particleStrengthOverrides.get(particleId.toString())"), "particle override is not connected");
        String encoding = Files.readString(JAVA.resolve("com/bloom/client/selection/BloomSourceEncoding.java"));
        require(encoding.contains("private static double clampSourceStrength") && encoding.contains("!Double.isFinite(sourceStrength)"), "terrain/fluid encoding lacks finite input guard");
        String liquid = Files.readString(JAVA.resolve("com/bloom/mixin/client/LiquidBlockRendererMixin.java"));
        require(liquid.contains("ordinal = 0") && liquid.contains("ordinal = 1") && liquid.contains("ordinal = 2"), "vanilla fluid light redirects do not cover all three calls");
    }

    private static void testEntityParticleHooks() throws Exception {
        String state = Files.readString(JAVA.resolve("com/bloom/client/selection/BloomSelectionState.java"));
        String entity = Files.readString(JAVA.resolve("com/bloom/mixin/client/EntityRendererBloomSelectionMixin.java"));
        String particle = Files.readString(JAVA.resolve("com/bloom/mixin/client/ParticleEngineBloomSelectionMixin.java"));
        String mixins = Files.readString(RESOURCES.resolve("shine.mixins.json"));
        require(state.contains("pushEntityStrength") && state.contains("popEntityStrength"), "entity selection state lifecycle missing");
        require(state.contains("pushParticleStrength") && state.contains("popParticleStrength"), "particle selection state lifecycle missing");
        require(state.contains("public static void reset()") && state.contains("BLOCK_STRENGTH.set(0.0)") && state.contains("PARTICLE_STRENGTH.set(0.0)"), "selection state frame reset missing");
        String sourceRenderer = Files.readString(JAVA.resolve("com/bloom/client/render/BloomSourceRenderer.java"));
        require(sourceRenderer.contains("BloomSelectionState.reset()"), "source lifecycle does not clear selection state");
        require(sourceRenderer.contains("preparedThisFrame = false") && sourceRenderer.contains("sourceTarget.clear(false)"), "source target is not cleared/reset at frame boundary");
        require(sourceRenderer.indexOf("preparedThisFrame = false") < sourceRenderer.indexOf("sourceTarget.clear(false)"), "prepared source flag must be committed after clear succeeds");
        require(sourceRenderer.contains("return preparedThisFrame ? sourceTarget : null"), "empty/unprepared source must not reach PostChain");
        String post = Files.readString(JAVA.resolve("com/bloom/client/render/BloomPostProcessor.java"));
        require(post.contains("chainFailureLogged") && post.contains("Unable to create Shine 1.20.1 bloom chain"), "PostChain creation fallback/log guard missing");
        require(post.contains("chain.process(0.0F)") && post.contains("closeRuntimeChain();") && post.contains("bloom chain failed during processing"), "PostChain processing failure cleanup missing");
        require(post.contains("chainFailureLogged = false") && post.contains("onConfigSaved"), "PostChain failure guard is not reset after reload/config save");
        require(post.contains("chainWidth == width && chainHeight == height") && post.contains("closeRuntimeChain();"), "PostChain does not recreate on main target resize");
        String rendererSource = Files.readString(JAVA.resolve("com/bloom/client/render/BloomSourceRenderer.java"));
        require(rendererSource.contains("sourceWidth != main.width || sourceHeight != main.height") && rendererSource.contains("sourceTarget.resize(main.width, main.height"), "source target does not follow fullscreen/resize dimensions");
        require(post.contains("GL_READ_FRAMEBUFFER_BINDING") && post.contains("GL_DRAW_FRAMEBUFFER_BINDING") && post.contains("finally"), "GL read/draw framebuffer state is not restored");
        require(post.contains("GL11.GL_VIEWPORT") && post.contains("GL11.GL_BLEND") && post.contains("GL11.GL_DEPTH_TEST"), "GL viewport/blend/depth state is not captured");
        require(post.contains("GL11.glViewport(previousViewport") && post.contains("RenderSystem.enableBlend()") && post.contains("RenderSystem.disableDepthTest()"), "GL viewport/blend/depth state is not restored");
        require(entity.contains("getTextureLocation(entity)") && entity.contains("BloomSelection.getEntitySourceStrength"), "entity texture override hook missing");
        require(particle.contains("BloomConfig.get().defaultParticleStrength"), "particle default source hook missing");
        require(mixins.contains("EntityRendererBloomSelectionMixin") && mixins.contains("ParticleEngineBloomSelectionMixin"), "entity/particle mixins are not active");
        String particleRegistry = Files.readString(JAVA.resolve("com/bloom/client/particle/ShineParticleRegistry.java"));
        require(particleRegistry.contains("volatile boolean factoriesRegistered") && particleRegistry.contains("synchronized void registerFactories"), "particle factory registration guard is not race-safe");
        require(particleRegistry.indexOf("factoriesRegistered = true") > particleRegistry.indexOf("registry.register(type"), "particle factory guard must commit after registrations");
    }

    private static void testParticleShaderOutputPath() throws Exception {
        String particle = Files.readString(RESOURCES.resolve("assets/minecraft/shaders/core/particle.fsh"));
        String shaderJson = Files.readString(RESOURCES.resolve("assets/minecraft/shaders/core/particle.json"));
        String shaderHook = Files.readString(JAVA.resolve("com/bloom/mixin/client/ShaderInstanceBloomUniformMixin.java"));
        String fog = Files.readString(RESOURCES.resolve("assets/minecraft/shaders/include/fog.glsl"));
        require(particle.contains("out vec4 bloomColor;") && !particle.contains("layout(location"), "particle bloom output declaration missing or GLSL 1.50-incompatible");
        require(particle.contains("ShineParticleStrength"), "particle source strength uniform missing");
        require(shaderJson.contains("ShineParticleStrength"), "particle shader uniform is not declared in JSON");
        require(shaderHook.contains("getUniform(\"ShineParticleStrength\")"), "particle shader uniform hook missing");
        require(fog.contains("linear_fog") && fog.contains("fog_distance"), "vanilla fog include missing");
    }

    private static void testEntityShaderOutputPath() throws Exception {
        Path core = RESOURCES.resolve("assets/minecraft/shaders/core");
        Set<String> service = Set.of("rendertype_armor_entity_glint", "rendertype_entity_decal", "rendertype_entity_glint", "rendertype_entity_glint_direct", "rendertype_entity_shadow");
        long entityShaders = Files.list(core).filter(path -> path.getFileName().toString().contains("entity") && path.toString().endsWith(".fsh") && !service.contains(path.getFileName().toString().replace(".fsh", ""))).count();
        require(entityShaders >= 11, "visual vanilla entity shader variants are incomplete");
        try (var stream = Files.list(core)) {
            stream.filter(path -> path.getFileName().toString().contains("entity") && path.toString().endsWith(".fsh") && !service.contains(path.getFileName().toString().replace(".fsh", ""))).forEach(path -> {
                try {
                    String text = Files.readString(path);
                    require(text.contains("out vec4 bloomColor;") && !text.contains("layout(location"), "entity bloom output missing or GLSL 1.50-incompatible: " + path.getFileName());
                    require(text.contains("ShineEntityStrength"), "entity source uniform missing: " + path.getFileName());
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
        String shaderHook = Files.readString(JAVA.resolve("com/bloom/mixin/client/ShaderInstanceBloomUniformMixin.java"));
        require(shaderHook.contains("getUniform(\"ShineEntityStrength\")"), "entity shader uniform hook missing");
        for (String serviceVariant : service) {
            Path serviceShader = core.resolve(serviceVariant + ".fsh");
            require(!Files.readString(serviceShader).contains("bloomColor"), "service shader must not write bloom output: " + serviceVariant);
        }
        require(shaderHook.contains("IrisCompat.shouldYieldToShaderPack()"), "Iris shader ownership guard missing");
        String iris = Files.readString(JAVA.resolve("com/bloom/client/compat/IrisCompat.java"));
        require(iris.contains("shouldYieldToShaderPack") && iris.contains("leaving Iris-owned shader uniforms untouched"), "Iris shader fallback wording/guard missing");
        require(iris.contains("volatile boolean reflectionInitialized") && iris.contains("synchronized boolean initReflection"), "Iris reflection cache is not race-safe");
        require(iris.contains("if (!IRIS_LOADED)") && iris.contains("Class.forName(\"net.irisshaders.iris.api.v0.IrisApi\")"), "Iris API must remain lazy and optional");
    }

    private static void testOverlayToolPolicy() throws Exception {
        String patch = Files.readString(Path.of("tools/patch_entity_shaders.py"));
        String restore = Files.readString(Path.of("tools/restore_service_entity_shaders.py"));
        String audit = Files.readString(Path.of("tools/audit_shader_overlays.py"));
        for (String variant : new String[] {"rendertype_armor_entity_glint", "rendertype_entity_decal", "rendertype_entity_glint", "rendertype_entity_glint_direct", "rendertype_entity_shadow"}) {
            require(patch.contains(variant) && restore.contains(variant) && audit.contains(variant), "service render-type policy incomplete: " + variant);
        }
        require(patch.contains("files = [n for n in files if Path(n).stem not in service]"), "entity patch tool is not service-aware");
        require(audit.contains("expected=11"), "overlay audit visual variant count guard missing");
    }

    private static void testIrisAndEventOrdering() throws Exception {
        String iris = Files.readString(JAVA.resolve("com/bloom/client/compat/IrisCompat.java"));
        String client = Files.readString(JAVA.resolve("com/bloom/client/BloomClient.java"));
        require(iris.contains("continuing with Shine bloom"), "Iris reflection failure must not disable bloom");
        require(iris.contains("return false;"), "Iris fallback must preserve bloom execution");
        require(client.contains("WorldRenderEvents.END.register(BloomPostProcessor::renderIfEnabled)"), "bloom must run at world-render END");
        require(!client.contains("WorldRenderEvents.BEFORE_ENTITIES.register"), "bloom must not run before entities");
        require(client.contains("ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)"), "client resource reload listener missing");
        require(client.contains("BloomPostProcessor.shutdown()") && client.contains("ExperimentalConfigManager.reload()"), "resource reload cleanup/reload callbacks missing");
        require(client.contains("reloadListenerRegistered") && client.contains("if (reloadListenerRegistered) return"), "reload listener registration is not idempotent");
        for (String vanillaMixin : new String[] {"EntityRendererBloomSelectionMixin.java", "ParticleEngineBloomSelectionMixin.java", "LevelRendererBloomAttachmentMixin.java", "ShaderInstanceBloomUniformMixin.java"}) {
            String source = Files.readString(JAVA.resolve("com/bloom/mixin/client/" + vanillaMixin));
            require(!source.contains("require = 0"), "confirmed Vanilla target is silently optional: " + vanillaMixin);
        }
        String sodium = Files.readString(JAVA.resolve("com/bloom/mixin/client/sodium/SodiumShaderLoaderMixin.java"));
        require(sodium.contains("require = 0"), "optional Sodium target must remain tolerant");
    }

    private static void testParticleRegistryCoverage() throws Exception {
        long definitions = Files.list(RESOURCES.resolve("assets/shine/particles"))
            .filter(path -> path.getFileName().toString().endsWith(".json"))
            .count();
        String registry = Files.readString(JAVA.resolve("com/bloom/client/particle/ShineParticleRegistry.java"));
        String types = Files.readString(JAVA.resolve("com/bloom/particle/ShineParticleTypes.java"));
        String common = Files.readString(JAVA.resolve("com/bloom/BloomMod.java"));
        String client = Files.readString(JAVA.resolve("com/bloom/client/BloomClient.java"));
        require(definitions == 84, "unexpected original particle definition count: " + definitions);
        require(types.contains("FabricParticleTypes.simple()") && types.contains("public static void register()"), "common Fabric particle registry implementation missing");
        require(types.contains("getOptional(id)") && types.contains("TYPES.containsKey(name)"), "common particle registration is not partial-safe/idempotent");
        require(types.contains("Collections.unmodifiableMap(TYPES)"), "common particle registry exposes mutable internal state");
        require(!types.contains("net.minecraft.client") && !types.contains("fabric.api.client"), "common particle registry references client-only APIs");
        require(registry.contains("factoriesRegistered") && registry.contains("if (factoriesRegistered) return"), "particle factory registration is not idempotent");
        require(registry.contains("registeredCount()") && registry.contains("ParticleFactoryRegistry"), "client particle provider registry missing");
        require(common.contains("ShineParticleTypes.register()"), "common particle registration missing");
        require(client.contains("ShineParticleRegistry.registerFactories()"), "client particle factory registration missing");
        require(client.contains("ExperimentalConfigManager.load()"), "experimental config initialization missing");
    }

    private static void testAmbienceAndLegacyIsolation() throws Exception {
        String types = Files.readString(JAVA.resolve("com/bloom/particle/ShineParticleTypes.java"));
        String mixins = Files.readString(RESOURCES.resolve("shine.mixins.json"));
        String legacy = Files.readString(JAVA.resolve("com/bloom/mixin/client/ChunkSectionsToRenderMixin.java.legacy21"));
        String experimental = Files.readString(JAVA.resolve("com/bloom/client/config/ExperimentalConfigManager.java"));
        for (String ambience : new String[] {"world_ambience_bird", "world_ambience_firefly", "world_ambience_leaf_gust", "world_ambience_light_ray", "world_ambience_wind_gust"}) {
            require(types.contains("\"" + ambience + "\""), "ambience particle registration missing: " + ambience);
            require(Files.isRegularFile(RESOURCES.resolve("assets/shine/particles/" + ambience + ".json")), "ambience particle definition missing: " + ambience);
        }
        require(!mixins.contains("ChunkSectionsToRenderMixin") && !mixins.contains("FrameGraph"), "1.21-only terrain/framegraph mixin is active");
        require(legacy.contains("GpuTextureView") && legacy.contains("RenderPass"), "legacy 1.21 marker file unexpectedly changed");
        require(experimental.contains("assets/shine/defaults/experimental.json") && experimental.contains("config/shine/experimental.json"), "experimental ambience config fallback paths missing");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
