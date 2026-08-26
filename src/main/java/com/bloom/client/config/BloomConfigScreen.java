package com.bloom.client.config;

import com.bloom.BloomMod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.bloom.client.render.BloomPostProcessor;
import dev.isxander.yacl3.api.Binding;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class BloomConfigScreen {
	private BloomConfigScreen() {
	}

	public static Screen create(Screen parent) {
		BloomConfig.Data defaults = BloomConfig.defaults();
		BloomConfig.Data editing = BloomConfig.copy();

		List<BlockEntry> blockEntries = bloom$getBlockEntries();
			Map<String, BlockEntry> blockEntryById = blockEntries.stream().collect(java.util.stream.Collectors.toMap(BlockEntry::id, entry -> entry));
			JsonObject experimentalDefaults = ExperimentalConfigManager.snapshot();
			JsonObject experimentalEditing = experimentalDefaults.deepCopy();

		ConfigCategory mainCategory = ConfigCategory.createBuilder()
			.name(Component.translatable("shine.config.category.main"))
			.group(bloom$buildOverridesInUseGroup(defaults, editing, blockEntryById))
				.group(bloom$buildGlobalSettingsGroup(defaults, editing))
				.group(bloom$buildSelectiveMaskGroup(defaults, editing))
				.build();

		ConfigCategory addOverridesCategory = ConfigCategory.createBuilder()
			.name(Component.translatable("shine.config.category.unused_blocks"))
			.group(bloom$buildUnusedBlocksGroup(defaults, editing, blockEntries))
			.build();

		return YetAnotherConfigLib.createBuilder()
			.title(Component.translatable("shine.config.title"))
			.category(mainCategory)
							.category(addOverridesCategory)
				.category(bloom$buildExperimentalCategory(experimentalDefaults, experimentalEditing))
				.save(() -> {

				bloom$pruneInactiveOverrides(defaults, editing, blockEntryById);
				BloomConfig.set(editing);
									BloomConfig.save();
					ExperimentalConfigManager.save(experimentalEditing);
					BloomPostProcessor.onConfigSaved();

				bloom$rebuildChunksForSourceStrengthChanges();
			})
			.build()
			.generateScreen(parent);
	}

	private static OptionGroup bloom$buildOverridesInUseGroup(BloomConfig.Data defaults, BloomConfig.Data editing, Map<String, BlockEntry> blockEntryById) {
			OptionGroup.Builder builder = OptionGroup.createBuilder()
				.name(Component.translatable("shine.config.group.overrides_in_use"))
				.description(OptionDescription.of(Component.translatable("shine.config.group.overrides_in_use.desc")))
				.collapsed(true)
				.option(ButtonOption.createBuilder()
					.name(Component.translatable("shine.config.reset_source"))
					.text(Component.translatable("shine.config.reset_source.value"))
					.description(OptionDescription.of(Component.translatable("shine.config.reset_source.desc")))
					.action((screen, option) -> bloom$resetSourceOverrides(defaults, editing))
					.build());

		List<String> overrideIds = new ArrayList<>();
		for (String blockId : editing.blockStrengthOverrides.keySet()) {
			BlockEntry entry = blockEntryById.getOrDefault(blockId, new BlockEntry(blockId, false));
			if (bloom$isActiveOverride(defaults, editing, entry)) {
				overrideIds.add(blockId);
			}
		}
		overrideIds.sort(String::compareTo);
		if (overrideIds.isEmpty()) {
			builder.option(
				ButtonOption.createBuilder()
					.name(Component.translatable("shine.config.overrides.empty"))
					.text(Component.translatable("shine.config.overrides.empty.value"))
					.description(OptionDescription.of(Component.translatable("shine.config.overrides.empty.desc")))
					.action((screen, option) -> {
					})
					.available(false)
					.build()
			);
			return builder.build();
		}

		for (String blockId : overrideIds) {
			BlockEntry entry = blockEntryById.getOrDefault(blockId, new BlockEntry(blockId, false));
			builder.option(bloom$buildBlockStrengthOption(defaults, editing, entry));
		}

		return builder.build();
	}

	private static OptionGroup bloom$buildGlobalSettingsGroup(BloomConfig.Data defaults, BloomConfig.Data editing) {
					return OptionGroup.createBuilder()
				.name(Component.translatable("shine.config.group.global"))
				.description(OptionDescription.of(Component.translatable("shine.config.group.global.desc")))
				.option(ButtonOption.createBuilder()
					.name(Component.translatable("shine.config.reset_globals"))
					.text(Component.translatable("shine.config.reset_globals.value"))
					.description(OptionDescription.of(Component.translatable("shine.config.reset_globals.desc")))
					.action((screen, option) -> bloom$resetGlobals(defaults, editing))
					.build())
				.option(

				Option.<Boolean>createBuilder()
					.name(Component.translatable("shine.config.enabled"))
					.description(OptionDescription.of(Component.translatable("shine.config.enabled.desc")))
					.binding(Binding.generic(defaults.enabled, () -> editing.enabled, value -> editing.enabled = value))
					.controller(option -> BooleanControllerBuilder.create(option).yesNoFormatter())
					.build()
			)
			.option(
				Option.<Double>createBuilder()
					.name(Component.translatable("shine.config.strength"))
					.description(OptionDescription.of(Component.translatable("shine.config.strength.desc")))
					.binding(Binding.generic(defaults.strength, () -> editing.strength, value -> editing.strength = value))
					.controller(option -> DoubleSliderControllerBuilder.create(option)
						.range(0.0, BloomConfig.MAX_STRENGTH)
						.step(0.01)
						.formatValue(value -> Component.literal(String.format(Locale.ROOT, "%.2f", value))))
					.build()
			)
			.option(
				Option.<Double>createBuilder()
					.name(Component.translatable("shine.config.threshold"))
					.description(OptionDescription.of(Component.translatable("shine.config.threshold.desc")))
					.binding(Binding.generic(defaults.threshold, () -> editing.threshold, value -> editing.threshold = value))
					.controller(option -> DoubleSliderControllerBuilder.create(option)
						.range(0.0, 1.0)
						.step(0.01)
						.formatValue(value -> Component.literal(String.format(Locale.ROOT, "%.2f", value))))
					.build()
			)
			.option(
				Option.<Double>createBuilder()
					.name(Component.translatable("shine.config.highlight_clamp"))
					.description(OptionDescription.of(Component.translatable("shine.config.highlight_clamp.desc")))
					.binding(Binding.generic(defaults.highlightClamp, () -> editing.highlightClamp, value -> editing.highlightClamp = value))
					.controller(option -> DoubleSliderControllerBuilder.create(option)
						.range(BloomConfig.MIN_HIGHLIGHT_CLAMP, BloomConfig.MAX_HIGHLIGHT_CLAMP)
						.step(0.01)
						.formatValue(value -> Component.literal(String.format(Locale.ROOT, "%.2f", value))))
					.build()
			)
			.option(
				Option.<Double>createBuilder()
					.name(Component.translatable("shine.config.soft_knee"))
					.description(OptionDescription.of(Component.translatable("shine.config.soft_knee.desc")))
					.binding(Binding.generic(defaults.softKnee, () -> editing.softKnee, value -> editing.softKnee = value))
					.controller(option -> DoubleSliderControllerBuilder.create(option)
						.range(BloomConfig.MIN_SOFT_KNEE, BloomConfig.MAX_SOFT_KNEE)
						.step(0.01)
						.formatValue(value -> Component.literal(String.format(Locale.ROOT, "%.2f", value))))
					.build()
			)
				.option(bloom$buildDoubleOption("shine.config.tiny_radius", "shine.config.tiny_radius.desc", defaults.tinyRadius, () -> editing.tinyRadius, value -> editing.tinyRadius = value, 0.0, BloomConfig.MAX_RADIUS, 0.25))
				.option(bloom$buildDoubleOption("shine.config.broad_radius", "shine.config.broad_radius.desc", defaults.broadRadius, () -> editing.broadRadius, value -> editing.broadRadius = value, 0.0, BloomConfig.MAX_RADIUS, 0.25))
				.option(bloom$buildSourceOption("shine.config.light_source_strength", "shine.config.light_source_strength.desc", defaults.defaultLightSourceStrength, () -> editing.defaultLightSourceStrength, value -> editing.defaultLightSourceStrength = value))
				.option(bloom$buildSourceOption("shine.config.non_light_source_strength", "shine.config.non_light_source_strength.desc", defaults.defaultNonLightStrength, () -> editing.defaultNonLightStrength, value -> editing.defaultNonLightStrength = value))
				.option(bloom$buildSourceOption("shine.config.entity_source_strength", "shine.config.entity_source_strength.desc", defaults.defaultEntityTextureStrength, () -> editing.defaultEntityTextureStrength, value -> editing.defaultEntityTextureStrength = value))
				.option(bloom$buildSourceOption("shine.config.particle_source_strength", "shine.config.particle_source_strength.desc", defaults.defaultParticleStrength, () -> editing.defaultParticleStrength, value -> editing.defaultParticleStrength = value))
				.option(
					Option.<Double>createBuilder()
						.name(Component.translatable("shine.config.radius"))
					.description(OptionDescription.of(Component.translatable("shine.config.radius.desc")))
					.binding(Binding.generic(defaults.radius, () -> editing.radius, value -> editing.radius = value))
					.controller(option -> DoubleSliderControllerBuilder.create(option)
						.range(0.0, BloomConfig.MAX_RADIUS)
						.step(0.25)
						.formatValue(value -> Component.literal(String.format(Locale.ROOT, "%.2f", value))))
					.build()
			)
			.option(
				Option.<Integer>createBuilder()
					.name(Component.translatable("shine.config.blur_pass_count"))
					.description(OptionDescription.of(Component.translatable("shine.config.blur_pass_count.desc")))
					.binding(Binding.generic(defaults.blurPassCount, () -> editing.blurPassCount, value -> editing.blurPassCount = value))
					.controller(option -> IntegerSliderControllerBuilder.create(option)
						.range(1, BloomConfig.MAX_BLUR_PASSES)
						.step(1))
					.build()
			)
			.option(
				Option.<Double>createBuilder()
					.name(Component.translatable("shine.config.distance"))
					.description(OptionDescription.of(Component.translatable("shine.config.distance.desc")))
					.binding(Binding.generic(defaults.bloomDistance, () -> editing.bloomDistance, value -> editing.bloomDistance = value))
					.controller(option -> DoubleSliderControllerBuilder.create(option)
						.range(BloomConfig.MIN_BLOOM_DISTANCE, BloomConfig.MAX_BLOOM_DISTANCE)
						.step(1.0)
						.formatValue(value -> Component.literal(String.format(Locale.ROOT, "%.0f", value))))
					.build()
			)
			.build();
	}

			private static OptionGroup bloom$buildSelectiveMaskGroup(BloomConfig.Data defaults, BloomConfig.Data editing) {
			return OptionGroup.createBuilder()
				.name(Component.translatable("shine.config.group.selective_mask"))
				.description(OptionDescription.of(Component.translatable("shine.config.group.selective_mask.desc")))
				.option(Option.<Boolean>createBuilder()
					.name(Component.translatable("shine.config.selective_mask_enabled"))
					.description(OptionDescription.of(Component.translatable("shine.config.selective_mask_enabled.desc")))
					.binding(Binding.generic(defaults.selectiveMaskEnabled, () -> editing.selectiveMaskEnabled, value -> editing.selectiveMaskEnabled = value))
					.controller(option -> BooleanControllerBuilder.create(option).yesNoFormatter())
					.build())
				.option(Option.<Integer>createBuilder()
					.name(Component.translatable("shine.config.mask_block_budget"))
					.description(OptionDescription.of(Component.translatable("shine.config.mask_block_budget.desc")))
					.binding(Binding.generic(defaults.maskBlockBudget, () -> editing.maskBlockBudget, value -> editing.maskBlockBudget = value))
					.controller(option -> IntegerSliderControllerBuilder.create(option).range(16, BloomConfig.MAX_MASK_BLOCK_BUDGET).step(16))
					.build())
				.build();
		}

		private static ConfigCategory bloom$buildExperimentalCategory(JsonObject defaults, JsonObject editing) {
			ConfigCategory.Builder category = ConfigCategory.createBuilder()
				.name(Component.translatable("shine.config.category.experimental"));
			for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
				if (!entry.getValue().isJsonObject()) continue;
				OptionGroup.Builder group = OptionGroup.createBuilder()
					.name(Component.literal(bloom$humanize(entry.getKey())))
					.description(OptionDescription.of(Component.translatable("shine.config.experimental.desc")));
				List<String> paths = new ArrayList<>();
				bloom$collectEditableLeaves(entry.getValue().getAsJsonObject(), entry.getKey(), paths);
				for (String path : paths) {
					JsonElement value = bloom$getPath(defaults, path);
					if (value == null || !value.isJsonPrimitive()) continue;
					JsonPrimitive primitive = value.getAsJsonPrimitive();
					if (primitive.isBoolean()) {
						group.option(Option.<Boolean>createBuilder()
							.name(Component.literal(bloom$humanize(path.substring(path.lastIndexOf('.') + 1))))
							.binding(Binding.generic(primitive.getAsBoolean(),
								() -> bloom$getPath(editing, path).getAsBoolean(),
								v -> bloom$setPath(editing, path, new JsonPrimitive(v))))
							.controller(option -> BooleanControllerBuilder.create(option).yesNoFormatter())
							.build());
					} else if (primitive.isNumber()) {
						group.option(Option.<Double>createBuilder()
							.name(Component.literal(bloom$humanize(path.substring(path.lastIndexOf('.') + 1))))
							.binding(Binding.generic(primitive.getAsDouble(),
								() -> bloom$getPath(editing, path).getAsDouble(),
								v -> bloom$setPath(editing, path, new JsonPrimitive(v))))
							.controller(option -> DoubleSliderControllerBuilder.create(option).range(-10000.0, 10000.0).step(0.01)
								.formatValue(v -> Component.literal(String.format(Locale.ROOT, "%.2f", v))))
							.build());
					}
				}
				category.group(group.build());
			}
			return category.build();
		}

		private static void bloom$collectEditableLeaves(JsonObject object, String prefix, List<String> output) {
			for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
				String path = prefix + "." + entry.getKey();
				if (entry.getValue().isJsonObject()) bloom$collectEditableLeaves(entry.getValue().getAsJsonObject(), path, output);
				else if (entry.getValue().isJsonPrimitive() && output.size() < 512) output.add(path);
			}
		}

		private static JsonElement bloom$getPath(JsonObject root, String path) {
			JsonElement current = root;
			for (String part : path.split("\\.")) {
				if (!current.isJsonObject() || !current.getAsJsonObject().has(part)) return new JsonPrimitive(0.0);
				current = current.getAsJsonObject().get(part);
			}
			return current;
		}

		private static void bloom$setPath(JsonObject root, String path, JsonElement value) {
			String[] parts = path.split("\\.");
			JsonObject current = root;
			for (int i = 0; i < parts.length - 1; i++) {
				if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject()) current.add(parts[i], new JsonObject());
				current = current.getAsJsonObject(parts[i]);
			}
			current.add(parts[parts.length - 1], value);
		}

		private static String bloom$humanize(String value) {
			return value.replace('_', ' ').replace('-', ' ');
		}

		private static OptionGroup bloom$buildUnusedBlocksGroup(BloomConfig.Data defaults, BloomConfig.Data editing, List<BlockEntry> blockEntries) {
		OptionGroup.Builder builder = OptionGroup.createBuilder()
			.name(Component.translatable("shine.config.group.unused_blocks"))
			.description(OptionDescription.of(Component.translatable("shine.config.group.unused_blocks.desc")))
			.collapsed(true);

		for (BlockEntry entry : blockEntries) {
			if (bloom$isActiveOverride(defaults, editing, entry)) {
				continue;
			}
			builder.option(bloom$buildBlockStrengthOption(defaults, editing, entry));
		}

		return builder.build();
	}

			private static Option<Double> bloom$buildDoubleOption(String nameKey, String descriptionKey, double defaultValue, java.util.function.Supplier<Double> getter, java.util.function.Consumer<Double> setter, double min, double max, double step) {
			return Option.<Double>createBuilder()
				.name(Component.translatable(nameKey))
				.description(OptionDescription.of(Component.translatable(descriptionKey)))
				.binding(Binding.generic(defaultValue, getter, setter))
				.controller(option -> DoubleSliderControllerBuilder.create(option).range(min, max).step(step).formatValue(value -> Component.literal(String.format(Locale.ROOT, "%.2f", value))))
				.build();
		}

		private static Option<Double> bloom$buildSourceOption(String nameKey, String descriptionKey, double defaultValue, java.util.function.Supplier<Double> getter, java.util.function.Consumer<Double> setter) {
			return bloom$buildDoubleOption(nameKey, descriptionKey, defaultValue, getter, setter, BloomConfig.MIN_SOURCE_STRENGTH, BloomConfig.MAX_SOURCE_STRENGTH, 1.0);
		}

		private static Option<Double> bloom$buildBlockStrengthOption(BloomConfig.Data defaults, BloomConfig.Data editing, BlockEntry entry) {
		return Option.<Double>createBuilder()
			.name(Component.literal(entry.id()))
			.description(
				OptionDescription.of(
					Component.translatable(
						"shine.config.block_override.desc",
						Component.literal(String.format(Locale.ROOT, "%.0f", bloom$getBaselineStrength(defaults, entry)))
					)
				)
			)
			.binding(
				Binding.generic(
					bloom$getEffectiveStrength(defaults, defaults, entry),
					() -> bloom$getEffectiveStrength(defaults, editing, entry),
					value -> bloom$setOverrideStrength(defaults, editing, entry, value)
				)
			)
			.controller(option -> DoubleSliderControllerBuilder.create(option)
				.range(BloomConfig.MIN_SOURCE_STRENGTH, BloomConfig.MAX_SOURCE_STRENGTH)
				.step(1.0)
				.formatValue(value -> Component.literal(String.format(Locale.ROOT, "%.0f", value))))
			.build();
	}

	private static double bloom$getEffectiveStrength(BloomConfig.Data defaults, BloomConfig.Data data, BlockEntry entry) {
		Double override = data.blockStrengthOverrides.get(entry.id());
		if (override != null) {
			return bloom$clampSourceStrength(override);
		}

		return bloom$getBaselineStrength(defaults, entry);
	}

	private static double bloom$getBaselineStrength(BloomConfig.Data defaults, BlockEntry entry) {
		Double baselineOverride = defaults.blockStrengthOverrides.get(entry.id());
		if (baselineOverride != null) {
			return bloom$clampSourceStrength(baselineOverride);
		}

		double fallback = entry.lightSource() ? defaults.defaultLightSourceStrength : defaults.defaultNonLightStrength;
		return bloom$clampSourceStrength(fallback);
	}

	private static boolean bloom$isActiveOverride(BloomConfig.Data defaults, BloomConfig.Data data, BlockEntry entry) {
		double baselineStrength = bloom$getBaselineStrength(defaults, entry);
		double effectiveStrength = bloom$getEffectiveStrength(defaults, data, entry);
		return Math.abs(effectiveStrength - baselineStrength) >= 1.0E-6;
	}

	private static void bloom$setOverrideStrength(BloomConfig.Data defaults, BloomConfig.Data data, BlockEntry entry, double requestedStrength) {
		double strength = bloom$clampSourceStrength(requestedStrength);
		double baselineStrength = bloom$getBaselineStrength(defaults, entry);
		if (Math.abs(strength - baselineStrength) < 1.0E-6) {
			data.blockStrengthOverrides.remove(entry.id());
			return;
		}
		data.blockStrengthOverrides.put(entry.id(), strength);
	}

			private static void bloom$resetSourceOverrides(BloomConfig.Data defaults, BloomConfig.Data editing) {
			editing.blockStrengthOverrides = new java.util.LinkedHashMap<>(defaults.blockStrengthOverrides);
			editing.sourceStrengthOverrides = editing.blockStrengthOverrides;
			editing.stateSourceStrengthOverrides.clear();
			editing.entityTextureStrengthOverrides.clear();
			editing.particleStrengthOverrides.clear();
			editing.sourceRadiusProfiles.clear();
		}

		private static void bloom$resetGlobals(BloomConfig.Data defaults, BloomConfig.Data editing) {
			editing.enabled = defaults.enabled;
			editing.strength = defaults.strength;
			editing.threshold = defaults.threshold;
			editing.radius = defaults.radius;
			editing.tinyRadius = defaults.tinyRadius;
			editing.broadRadius = defaults.broadRadius;
			editing.blurPassCount = defaults.blurPassCount;
			editing.bloomDistance = defaults.bloomDistance;
			editing.highlightClamp = defaults.highlightClamp;
			editing.softKnee = defaults.softKnee;
			editing.defaultLightSourceStrength = defaults.defaultLightSourceStrength;
			editing.defaultNonLightStrength = defaults.defaultNonLightStrength;
			editing.defaultEntityTextureStrength = defaults.defaultEntityTextureStrength;
			editing.defaultParticleStrength = defaults.defaultParticleStrength;
		}

		private static double bloom$clampSourceStrength(double value) {

		return Math.max(BloomConfig.MIN_SOURCE_STRENGTH, Math.min(BloomConfig.MAX_SOURCE_STRENGTH, value));
	}

	private static void bloom$pruneInactiveOverrides(BloomConfig.Data defaults, BloomConfig.Data data, Map<String, BlockEntry> blockEntryById) {
		data.blockStrengthOverrides.entrySet().removeIf(entry -> {
			String blockId = entry.getKey();
			Double value = entry.getValue();
			if (blockId == null || value == null) {
				return true;
			}

			BlockEntry blockEntry = blockEntryById.get(blockId);
			if (blockEntry == null) {
				return false;
			}

			double clampedOverride = bloom$clampSourceStrength(value);
			double baselineStrength = bloom$getBaselineStrength(defaults, blockEntry);
			return Math.abs(clampedOverride - baselineStrength) < 1.0E-6;
		});
	}

	private static void bloom$rebuildChunksForSourceStrengthChanges() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.levelRenderer == null) {
			return;
		}

		// Per-block source strength is baked into chunk mesh data; refresh sections after config changes.
		minecraft.levelRenderer.allChanged();
	}

	private static List<BlockEntry> bloom$getBlockEntries() {
		List<BlockEntry> entries = new ArrayList<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
			if (id == null) {
				continue;
			}

			String blockId = id.toString();
			if (
				"minecraft:air".equals(blockId)
				|| "minecraft:cave_air".equals(blockId)
				|| "minecraft:void_air".equals(blockId)
			) {
				continue;
			}

			boolean lightSource = block.defaultBlockState().getLightEmission() > 0;
			entries.add(new BlockEntry(blockId, lightSource));
		}
		entries.sort(Comparator.comparing(BlockEntry::id));
		return entries;
	}

	private record BlockEntry(String id, boolean lightSource) {
	}
}
