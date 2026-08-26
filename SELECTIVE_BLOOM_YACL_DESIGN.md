# Shine Fabric 1.20.1: безопасный selective bloom и перенос GUI

## 1. Что показывает лог 1y2993I

Лог не содержит нового JVM crash: Minecraft завершается с кодом `0`. Однако runtime запускается с Intel UHD Graphics 600, окном 854×480 и лимитом Java heap `-Xmx2816m`. После создания мира в логе появляются предупреждения `shine_bloom_composite could not find uniform named InSize`, `MainSize`, `DepthSize`, `MaxDistance`, `NearPlane`, `FarPlane` и `DistanceFadeRange`. Снимки показывают пересвет неба и воды, белые полосы и частичную потерю сцены. Это соответствует двум ошибкам архитектуры:

1. Composite должен читать готовую сцену из отдельного target, а не читать и записывать `minecraft:main` одновременно.
2. Полноэкранная копия main framebuffer не является selective mask: она включает небо, воду и все яркие пиксели. Поэтому низкий threshold и высокая strength визуально превращают весь кадр в bloom.

Первый пункт исправляется отдельным `scene` target. Второй требует отдельного mask target.

## 2. Целевая схема render pipeline

Нельзя добавлять `COLOR_ATTACHMENT1`, менять `glDrawBuffers` или заставлять Sodium писать второй fragment output. Sodium и Iris могут владеть текущим framebuffer, а Intel-драйвер особенно чувствителен к изменению attachment state. Каждый target Shine должен быть независимым `TextureTarget`, созданным на render thread.

Безопасная схема для Minecraft 1.20.1:

```text
vanilla + Sodium + Iris rendering
              |
              |  main framebuffer remains untouched during world rendering
              v
copy main color -> sceneTarget       (full resolution or bounded scale)
clear maskTarget                      (R8 if available, RGBA8 fallback)
render selected objects into maskTarget using a private pass
              |
              v
sceneTarget + maskTarget -> extractTarget
extractTarget -> pingTarget -> bloomTarget
sceneTarget + bloomTarget + maskTarget -> minecraft:main
```

`minecraft:main` используется только как конечный output composite. Composite input должен быть `scene`, никогда не `minecraft:main`.

## 3. Mask target без повреждения FBO

### 3.1. Жизненный цикл

`MaskTargetManager` должен жить только на client/render thread и иметь ровно один target каждого размера. При изменении окна он вызывает `resize`; при disconnect/resource reload вызывает `destroyBuffers`. Нельзя создавать target на каждый кадр.

Псевдокод:

```java
final class MaskTargetManager {
    private TextureTarget mask;
    private int width = -1;
    private int height = -1;

    void ensure(int mainWidth, int mainHeight) {
        int scale = qualityScale();
        int w = Math.max(1, Math.min(1024, mainWidth * scale / 2));
        int h = Math.max(1, Math.min(1024, mainHeight * scale / 2));
        if (mask == null) mask = new TextureTarget(w, h, false, Minecraft.ON_OSX);
        else if (w != width || h != height) mask.resize(w, h, Minecraft.ON_OSX);
        width = w;
        height = h;
    }

    void clear() {
        mask.setClearColor(0, 0, 0, 0);
        mask.clear(false);
    }

    void close() {
        if (mask != null) mask.destroyBuffers();
        mask = null;
        width = height = -1;
    }
}
```

В реальном коде перед каждым private pass нужно сохранить и восстановить `GL_FRAMEBUFFER_BINDING`, viewport, blend, depth test, scissor, active texture и read/draw framebuffer. На выходе framebuffer должен быть тем же, что был до pass. Вызовы должны быть защищены `try/finally`.

### 3.2. Как отрисовывать блоки

Для блоков есть два безопасных варианта.

**Вариант A — рекомендуемый для Sodium/Iris:** не перерисовывать terrain и не вмешиваться в Sodium. Во время обычного рендера собирать CPU-visible список `BlockPos` с bloom strength из конфигурации, а после world render рисовать только геометрию этих блоков в отдельный mask target через vanilla `BlockRenderDispatcher`. Это дороже, поэтому список ограничивается радиусом, frustum и бюджетом, например 128–512 блоков за кадр. В mask shader записывается только белый alpha/strength.

**Вариант B — быстрый fallback:** строить mask из яркости `sceneTarget`, но применять LUT/ограничения по типичным светящимся цветам. Это не является настоящим per-block selective bloom и должно называться `luminance fallback`, чтобы пользователь не принимал его за оригинальный режим.

Для полного per-block режима вариант A должен использовать `BlockState`, `BlockPos`, `BlockEntity` и таблицу strength. Вода, небо и обычные листья не добавляются в mask, если для их ID нет положительного override. Разрешается отдельный entity/particle pass через уже имеющиеся render callbacks, но он также рисует только в private target.

### 3.3. Почему нельзя использовать gl_FragData/location 1

Sodium 0.5.x и Iris 1.7.x могут менять shader programs и framebuffer layout. Второй fragment output требует гарантированного второго draw buffer, которого у конкретного terrain FBO может не быть. Поэтому Shine не должен добавлять `bloomColor` output и не должен вызывать `glFramebufferTexture2D` для текущего FBO. Только отдельный `TextureTarget` и обычный full-screen copy/blit.

## 4. Legacy PostChain 1.20.1

В `bloom_poc.json` targets должны включать `scene`, `mask`, `bloom_extract` и `bloom_ping`. Пример графа:

```json
{
  "targets": ["scene", "mask", "bloom_extract", "bloom_ping"],
  "passes": [
    {"name":"shine_bloom_extract", "intarget":"scene", "outtarget":"bloom_extract"},
    {"name":"shine_bloom_blur_horizontal", "intarget":"bloom_extract", "outtarget":"bloom_ping"},
    {"name":"shine_bloom_blur_vertical", "intarget":"bloom_ping", "outtarget":"bloom_extract"},
    {"name":"shine_bloom_composite", "intarget":"scene", "outtarget":"minecraft:main",
     "auxtargets":[{"name":"MaskSampler","id":"mask"},{"name":"BloomSampler","id":"bloom_extract"}]}
  ]
}
```

Minecraft 1.20.1 legacy `PostEffectProcessor` ищет program descriptors по legacy правилам. Поэтому pass names должны состоять только из `[a-z0-9_.-]`, а runtime copies должны находиться в `assets/minecraft/shaders/program/`. Namespace `shine:` внутри legacy pass name использовать нельзя.

Composite shader должен сохранять базовую сцену и добавлять bloom только там, где mask положителен:

```glsl
vec4 scene = texture(DiffuseSampler, texCoord);
float mask = texture(MaskSampler, texCoord).r;
vec3 bloom = texture(BloomSampler, texCoord).rgb;
fragColor = vec4(scene.rgb + bloom * mask * Strength, scene.a);
```

При отсутствии mask target shader должен использовать `mask = 1.0` только в явном fallback режиме, а не молча.

## 5. Ambience

Ambience нельзя смешивать с framebuffer bloom. Это отдельная client-only система частиц/световых событий.

Рекомендуемая модель:

| Слой | Источник | Частота | Ограничение |
|---|---|---:|---|
| World ambience | biome, weather, time of day | 2–5 раз/сек | не работать на server thread |
| Light ambience | видимые torch/lantern/firefly sources | 1 раз/сек на chunk | кэш по chunk и dimension |
| Particle ambience | Fabric particle engine | каждый render tick | глобальный particle budget |
| Lens/fog accents | camera/world state | каждый кадр только при enabled | без дополнительных FBO |

Нужно использовать deterministic seed от `dimension + chunk position + world time bucket`, чтобы частицы не пересоздавались каждый кадр. На каждом tick обновляется только небольшой ring buffer активных ambience emitters. При превышении бюджета новые emitters отбрасываются, а не выделяется новая память.

Ambience настройки должны включать `enabled`, `density`, `maxParticles`, `spawnDistance`, `weatherMultiplier`, `nightMultiplier`, `biomeEffects`, `lightRayStrength` и отдельные toggles для birds, leaves, fireflies, pollen, mist и water effects. Server-side entrypoint не должен загружать client renderer или YACL.

## 6. Перенос GUI оригинала на YACL

Нельзя переносить оригинальные `Screen`/render classes 1.21.11 напрямую: они используют недоступные 1.20.1 rendering APIs. Переносится модель настроек, названия, категории, defaults и действия.

Рекомендуемые YACL категории:

| Категория | Основные опции |
|---|---|
| Bloom | enabled, strength, threshold, highlight clamp, soft knee |
| Selective Sources | per-block overrides, entities, particles, source radius, mask quality |
| Blur | tiny radius, broad radius, blur passes, quality scale |
| Ambience | enabled, density, max particles, weather/time/biome multipliers |
| Compatibility | Iris mode, disable on shaderpack, fallback mode, debug logging |
| Performance | target scale, frame budget, memory cap, auto quality |
| Diagnostics | show mask, show bloom, show targets, reset config, export config |

Каждая YACL option должна иметь `Binding.generic(defaults.field, getter, setter)`. Setter изменяет только editing copy. Сохранение выполняется один раз в `save()` экрана; после него вызываются render-thread-safe `BloomPostProcessor.onConfigSaved()` и chunk rebuild только если реально изменились source overrides.

Для оригинальных пресетов следует добавить enum option `DEFAULT`, `CINEMATIC`, `LOW_END`, `SOFT`, `STRONG`, `CUSTOM`. Preset применяет значения к editing copy, но не вызывает `BloomConfig.save()` до нажатия Save. Кнопки `Reset Bloom`, `Reset Selective`, `Reset Ambience`, `Reset Performance` должны сбрасывать только свою категорию.

## 7. Native-memory hardening

При 854×480 один RGBA8 target занимает примерно 1.6 MiB без учёта depth/staging; несколько full-size targets быстро становятся дорогими на Intel-драйвере. Нужны следующие правила:

1. Не создавать GL target внутри render loop.
2. Кэшировать targets и пересоздавать только при resize/quality change.
3. Использовать half-resolution mask и bloom targets на слабом GPU.
4. Ограничить максимальную площадь всех Shine targets, например 4–8 megapixels суммарно.
5. Ограничить blur passes до 1–2 на low-end профиле.
6. Не читать пиксели через `glReadPixels` каждый кадр.
7. Не хранить `ByteBuffer`/texture upload buffers в списках без bounded pool.
8. Освобождать PostChain и targets на render thread при disconnect/resource reload.
9. Уменьшать particle textures до 256 px, но не пересжимать их каждый кадр.
10. При ошибке allocation автоматически переходить в `SAFE` профиль и отключать ambience/mask, сохраняя основной кадр.

Профили:

| Профиль | Mask | Bloom scale | Blur | Ambience budget |
|---|---|---:|---:|---:|
| Safe | 1/2 | 1/2 | 1 | 32 |
| Balanced | 1/2 | 1/2 | 2 | 96 |
| Quality | 1/1 | 1/2 | 3 | 256 |

## 8. Порядок реализации

1. Оставить main framebuffer нетронутым и завершить scene-target fix.
2. Добавить `MaskTargetManager` с resize/close/GL-state guard.
3. Добавить CPU-visible block list с frustum/radius/budget filtering.
4. Реализовать private block mask pass; сначала только torch, lantern, sculk, amethyst и lava.
5. Подключить `MaskSampler` в extract/composite и добавить debug preview.
6. Добавить ambience manager с bounded emitter pool.
7. Перенести категории и presets Shine 3.0 в YACL.
8. Добавить safe/balanced/quality memory profiles.
9. Проверить без Iris, затем с Iris без shaderpack, затем с shaderpack.
10. Выпускать новый JAR только после проверки на Intel UHD 600 по трём обязательным тестам: обычная сцена, вода/небо, выход из мира.

## 9. Критерии готовности

Функция считается рабочей только если: обычная сцена не темнеет; вода и небо не получают bloom без положительного mask; torch/lantern дают локальный halo; выключение bloom возвращает исходный кадр пиксель-в-пиксель по цвету; изменение GUI не вызывает resource reload crash; disconnect не вызывает GL calls из Netty; и native memory не растёт при 5–10 минутах вращения камеры.

Текущий релиз 1.0.16 должен рассматриваться как безопасная база для этой работы, а не как готовая полная копия Shine 3.0. В частности, full per-block mask, ambience и все оригинальные experimental GUI screens ещё требуют реализации по плану выше.

## References

[1]: https://mclo.gs/1y2993I — лог runtime 1y2993I, Minecraft 1.20.1, Sodium 0.5.13, Iris 1.7.6.
[2]: https://github.com/Ksyushka666/shine-fabric-1.20.1 — репозиторий порта Shine Fabric 1.20.1.
[3]: https://github.com/isxander/yacl — YetAnotherConfigLib, используемый для конфигурационного GUI.
[4]: https://fabricmc.net/wiki/tutorial:rendering — общая документация Fabric rendering API.
