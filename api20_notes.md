# Minecraft 1.20.1 terrain API notes

В Minecraft 1.20.1 отсутствуют классы `ChunkSectionsToRender`, `ChunkSectionLayer`, `ChunkSectionLayerGroup`, `GpuTextureView`, `GpuBufferSlice` и `RenderPass`, используемые Shine 3.0.

Реальный vanilla terrain renderer 1.20.1 — `net.minecraft.client.renderer.chunk.ChunkRenderDispatcher`. Его важные вложенные классы: `ChunkRenderDispatcher$RenderChunk`, `CompiledChunk`, `RenderChunk$ChunkCompileTask`, `RebuildTask` и `ResortTransparencyTask`. Отрисовка использует `RenderType` и `VertexBuffer`, а компиляция чанков — `ChunkBufferBuilderPack` и старые `BufferBuilder`/`VertexBuffer` API.

Sodium 0.5.11 содержит `me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer` и `FluidRenderer`; у `FluidRenderer` метод `render(WorldSlice, FluidState, BlockPos, BlockPos, ChunkBuildBuffers)`. Новая сигнатура Shine с `TranslucentGeometryCollector`, `Material`, массивами sprite и GPU slices относится к более новой Sodium и напрямую несовместима.

Следующий технический шаг — переписать selective bloom source capture через отдельный 1.20.1 `RenderTarget` и старый `PostChain`, а код записи bloom strength перенести на Sodium 0.5.11 `ChunkModelBuilder`/vertex encoder. Простая замена имён классов недостаточна.
