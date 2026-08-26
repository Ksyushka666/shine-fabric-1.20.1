package com.bloom.client.config;

import com.bloom.BloomMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

public final class BloomConfig {
public static final int MAX_BLUR_PASSES = 4;
		public static final double MAX_STRENGTH = 10.0;
		public static final double LEGACY_MAX_RADIUS = 500.0;
		public static final double MAX_RADIUS = 700.0;
		public static final int RADIUS_SCALE_VERSION = 2;
	public static final double MIN_BLOOM_DISTANCE = 1.0;
	public static final double MAX_BLOOM_DISTANCE = 256.0;
	public static final double MIN_HIGHLIGHT_CLAMP = 0.01;
	public static final double MAX_HIGHLIGHT_CLAMP = 4.0;
	public static final double MIN_SOFT_KNEE = 0.01;
	public static final double MAX_SOFT_KNEE = 1.0;
	public static final double MIN_SOURCE_STRENGTH = 0.0;
	public static final double MAX_SOURCE_STRENGTH = 500.0;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("shine.json");
	private static final Path LEGACY_CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("bloom.json");

	private static volatile Data data = sanitize(Data.defaults());

	private BloomConfig() {
	}

	public static Data get() {
		return data;
	}

	public static synchronized Data copy() {
		return data.copy();
	}

	public static Data defaults() {
		return Data.defaults();
	}

	public static synchronized void set(Data newData) {
		data = sanitize(newData);
	}

	public static synchronized void load() {
		Path pathToLoad = Files.exists(CONFIG_PATH) ? CONFIG_PATH : (Files.exists(LEGACY_CONFIG_PATH) ? LEGACY_CONFIG_PATH : null);
		if (pathToLoad == null) {
				data = sanitize(Data.defaults());
				save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(pathToLoad)) {
			Data loaded = GSON.fromJson(reader, Data.class);
			data = sanitize(loaded);
			if (!pathToLoad.equals(CONFIG_PATH)) {
				save();
				BloomMod.LOGGER.info("Migrated legacy bloom.json config to shine.json.");
			}
		} catch (Exception e) {
			BloomMod.LOGGER.error("Failed to read Shine config, using defaults.", e);
				data = sanitize(Data.defaults());
			}
	}

	public static void save() {
		Data snapshot;
		synchronized (BloomConfig.class) {
			snapshot = data.copy();
		}
		Path temporary = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(temporary)) {
				GSON.toJson(snapshot, writer);
			}
			try {
				Files.move(temporary, CONFIG_PATH, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException atomicFailure) {
				Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
			BloomMod.LOGGER.error("Failed to write Shine config.", e);
		}
	}

	private static Data sanitize(Data input) {
		Data safe = input == null ? Data.defaults() : input.copy();
		safe.threshold = clamp(safe.threshold, 0.0, 1.0);
		safe.strength = clamp(safe.strength, 0.0, MAX_STRENGTH);
			safe.radius = clamp(safe.radius, 0.0, MAX_RADIUS);
			safe.tinyRadius = clamp(safe.tinyRadius, 0.0, MAX_RADIUS);
			safe.broadRadius = clamp(safe.broadRadius, 0.0, MAX_RADIUS);
			safe.radiusScaleVersion = RADIUS_SCALE_VERSION;
			safe.blurPassCount = (int) clamp(safe.blurPassCount, 1, MAX_BLUR_PASSES);
		if (safe.bloomDistance <= 0.0) {
			// Older config files won't contain this field yet; keep them on a sensible default.
			safe.bloomDistance = Data.defaults().bloomDistance;
		}
		safe.bloomDistance = clamp(safe.bloomDistance, MIN_BLOOM_DISTANCE, MAX_BLOOM_DISTANCE);
		if (safe.highlightClamp <= 0.0) {
			// Older config files won't contain this field yet; keep them on a sensible default.
			safe.highlightClamp = Data.defaults().highlightClamp;
		}
		safe.highlightClamp = clamp(safe.highlightClamp, MIN_HIGHLIGHT_CLAMP, MAX_HIGHLIGHT_CLAMP);
		if (safe.softKnee <= 0.0) {
			// Older config files won't contain this field yet; keep them on a sensible default.
			safe.softKnee = Data.defaults().softKnee;
		}
		safe.softKnee = clamp(safe.softKnee, MIN_SOFT_KNEE, MAX_SOFT_KNEE);
			safe.defaultLightSourceStrength = clamp(safe.defaultLightSourceStrength, MIN_SOURCE_STRENGTH, MAX_SOURCE_STRENGTH);
			safe.defaultNonLightStrength = clamp(safe.defaultNonLightStrength, MIN_SOURCE_STRENGTH, MAX_SOURCE_STRENGTH);
			safe.defaultEntityTextureStrength = clamp(safe.defaultEntityTextureStrength, MIN_SOURCE_STRENGTH, MAX_SOURCE_STRENGTH);
			safe.defaultParticleStrength = clamp(safe.defaultParticleStrength, MIN_SOURCE_STRENGTH, MAX_SOURCE_STRENGTH);
		Map<String, Double> sanitizedOverrides = new LinkedHashMap<>();
			if (safe.sourceStrengthOverrides != null) {
				for (Map.Entry<String, Double> entry : safe.sourceStrengthOverrides.entrySet()) {
				if (entry == null || entry.getKey() == null || entry.getValue() == null) {
					continue;
				}

					ResourceLocation parsed = ResourceLocation.tryParse(entry.getKey());
					if (parsed == null) continue;

					sanitizedOverrides.put(entry.getKey(), clamp(entry.getValue(), MIN_SOURCE_STRENGTH, MAX_SOURCE_STRENGTH));
			}
		}
		for (Map.Entry<String, Double> baseline : Data.defaultBlockStrengthOverrides().entrySet()) {
			sanitizedOverrides.putIfAbsent(baseline.getKey(), clamp(baseline.getValue(), MIN_SOURCE_STRENGTH, MAX_SOURCE_STRENGTH));
		}
			safe.sourceStrengthOverrides = sanitizedOverrides;
			safe.blockStrengthOverrides = sanitizedOverrides;
			safe.stateSourceStrengthOverrides = sanitizeStrengthMap(safe.stateSourceStrengthOverrides);
			safe.entityTextureStrengthOverrides = sanitizeStrengthMap(safe.entityTextureStrengthOverrides);
			safe.particleStrengthOverrides = sanitizeStrengthMap(safe.particleStrengthOverrides);
			return safe;
	}

		private static Map<String, Double> sanitizeStrengthMap(Map<String, Double> input) {
			Map<String, Double> out = new LinkedHashMap<>();
			if (input != null) {
				for (Map.Entry<String, Double> entry : input.entrySet()) {
					if (entry != null && entry.getKey() != null && entry.getValue() != null) out.put(entry.getKey(), clamp(entry.getValue(), MIN_SOURCE_STRENGTH, MAX_SOURCE_STRENGTH));
				}
			}
			return out;
		}

		private static double clamp(double value, double min, double max) {
			if (!Double.isFinite(value)) return min;
			return Math.max(min, Math.min(max, value));
		}

	private static long clamp(long value, long min, long max) {
		return Math.max(min, Math.min(max, value));
	}

	public static final class Data {
		public boolean enabled = true;
			public double strength = 8.0;
			public double threshold = 0.15;
				public double radius = 500.0;
			public double tinyRadius = 90.0;
			public double broadRadius = 700.0;
			public int radiusScaleVersion = RADIUS_SCALE_VERSION;
			public int blurPassCount = 2;
			public double bloomDistance = 256.0;
			public double highlightClamp = 0.28;
		public double softKnee = 0.2;
		public double defaultLightSourceStrength = 50.0;
			public double defaultNonLightStrength = 0.0;
			public double defaultEntityTextureStrength = 0.0;
			public double defaultParticleStrength = 0.0;
			public Map<String, Double> sourceStrengthOverrides = defaultBlockStrengthOverrides();
			public Map<String, Double> blockStrengthOverrides = sourceStrengthOverrides;
			public Map<String, Double> stateSourceStrengthOverrides = new LinkedHashMap<>();
			public Map<String, Double> entityTextureStrengthOverrides = new LinkedHashMap<>();
			public Map<String, Double> particleStrengthOverrides = new LinkedHashMap<>();
			public Map<String, Integer> sourceRadiusProfiles = new LinkedHashMap<>();

			public static Data defaults() {
				try (InputStream stream = BloomConfig.class.getResourceAsStream("/assets/shine/defaults/shine.json")) {
					if (stream != null) {
						try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
							Data bundled = GSON.fromJson(reader, Data.class);
							if (bundled != null) return bundled;
						}
					}
				} catch (Exception exception) {
					BloomMod.LOGGER.warn("Failed to load bundled original Shine visual defaults", exception);
				}
				return new Data();
			}

		public static LinkedHashMap<String, Double> defaultBlockStrengthOverrides() {
			LinkedHashMap<String, Double> defaults = new LinkedHashMap<>();
			defaults.put("minecraft:water", 0.0);
			defaults.put("minecraft:flowing_water", 0.0);
			defaults.put("minecraft:sculk", 500.0);
			defaults.put("minecraft:sculk_vein", 150.0);
			defaults.put("minecraft:amethyst_cluster", 100.0);
			defaults.put("minecraft:large_amethyst_bud", 100.0);
			defaults.put("minecraft:medium_amethyst_bud", 100.0);
			defaults.put("minecraft:small_amethyst_bud", 100.0);
			defaults.put("minecraft:glow_lichen", 400.0);
			defaults.put("minecraft:warped_stem", 100.0);
			defaults.put("minecraft:warped_fungus", 300.0);
			defaults.put("minecraft:nether_portal", 75.0);
			defaults.put("minecraft:crimson_stem", 175.0);
			defaults.put("minecraft:twisting_vines", 25.0);
			defaults.put("minecraft:twisting_vines_plant", 25.0);
			defaults.put("minecraft:weeping_vines", 75.0);
			defaults.put("minecraft:weeping_vines_plant", 75.0);
			defaults.put("minecraft:lava", 75.0);
			defaults.put("minecraft:flowing_lava", 75.0);
			defaults.put("minecraft:crimson_fungus", 150.0);
			defaults.put("minecraft:nether_wart", 50.0);
			defaults.put("minecraft:crying_obsidian", 75.0);
			defaults.put("minecraft:closed_eyeblossom", 50.0);
			defaults.put("minecraft:open_eyeblossom", 50.0);
			defaults.put("minecraft:resin_clump", 150.0);
			defaults.put("minecraft:chorus_flower", 150.0);
			defaults.put("minecraft:powder_snow", 25.0);
			defaults.put("minecraft:snow", 25.0);
			defaults.put("minecraft:snow_block", 25.0);
			defaults.put("minecraft:soul_torch", 220.0);
			defaults.put("minecraft:soul_wall_torch", 220.0);
			defaults.put("minecraft:redstone_torch", 120.0);
			defaults.put("minecraft:redstone_wall_torch", 120.0);
			defaults.put("minecraft:torch", 499.0);
			defaults.put("minecraft:wall_torch", 499.0);
			defaults.put("minecraft:lantern", 200.0);
			defaults.put("minecraft:soul_fire", 250.0);
			defaults.put("minecraft:firefly_bush", 500.0);
			defaults.put("minecraft:nether_gold_ore", 50.0);
			return defaults;
		}

		public Data copy() {
			Data copy = new Data();
			copy.enabled = this.enabled;
			copy.strength = this.strength;
			copy.threshold = this.threshold;
				copy.radius = this.radius;
				copy.tinyRadius = this.tinyRadius;
				copy.broadRadius = this.broadRadius;
				copy.radiusScaleVersion = this.radiusScaleVersion;
				copy.blurPassCount = this.blurPassCount;
			copy.bloomDistance = this.bloomDistance;
			copy.highlightClamp = this.highlightClamp;
			copy.softKnee = this.softKnee;
			copy.defaultLightSourceStrength = this.defaultLightSourceStrength;
				copy.defaultNonLightStrength = this.defaultNonLightStrength;
				copy.defaultEntityTextureStrength = this.defaultEntityTextureStrength;
				copy.defaultParticleStrength = this.defaultParticleStrength;
				copy.sourceStrengthOverrides = this.sourceStrengthOverrides == null ? new LinkedHashMap<>() : new LinkedHashMap<>(this.sourceStrengthOverrides);
				copy.blockStrengthOverrides = copy.sourceStrengthOverrides;
				copy.stateSourceStrengthOverrides = this.stateSourceStrengthOverrides == null ? new LinkedHashMap<>() : new LinkedHashMap<>(this.stateSourceStrengthOverrides);
				copy.entityTextureStrengthOverrides = this.entityTextureStrengthOverrides == null ? new LinkedHashMap<>() : new LinkedHashMap<>(this.entityTextureStrengthOverrides);
				copy.particleStrengthOverrides = this.particleStrengthOverrides == null ? new LinkedHashMap<>() : new LinkedHashMap<>(this.particleStrengthOverrides);
				copy.sourceRadiusProfiles = this.sourceRadiusProfiles == null ? new LinkedHashMap<>() : new LinkedHashMap<>(this.sourceRadiusProfiles);
			return copy;
		}
	}
}
