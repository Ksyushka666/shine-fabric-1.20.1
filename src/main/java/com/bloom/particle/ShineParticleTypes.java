package com.bloom.particle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/** Common-side particle type registry; intentionally contains no client-only classes. */
public final class ShineParticleTypes {
    public static final String MOD_ID = "shine";
    private static final String[] IDS = {
        "amethyst_sparkle", "beach_pebble_litter", "block_side_rain", "boat_splash", "boat_trail_foam", "boat_trail_wake",
        "branch_litter", "cave_dust", "copper_lantern_mote", "copper_torch_spark", "custom_rain", "custom_rain_splash",
        "desert_dust", "duckweed_litter", "end_portal_eye_placement", "flower_litter", "fog_fx", "glowing_ash", "ground_mist",
        "jellyfish", "lantern_mote", "lava_droplet_splash", "lava_ember", "lava_plate", "lava_pop_big", "lava_pop_small",
        "lava_spray", "lava_spray_flash", "lava_steam", "leaf_litter", "leaf_litter_kickup", "lily_pad_litter", "mist",
        "nether_rays", "pebble_litter", "petal_litter", "rain_puddle", "shell_litter", "soul_lantern_mote", "soul_torch_spark",
        "torch_smoke", "torch_spark", "trailer_splash_band", "trailer_splash_low", "trailer_splash_middle", "trailer_splash_outer",
        "underwater_chest_bubble", "underwater_ender_chest_bubble", "water_cascade", "water_splash_droplet", "weather_particle",
        "world_ambience_bird", "world_ambience_butterfly", "world_ambience_distant_light_ray", "world_ambience_falling_acacia_leaf",
        "world_ambience_falling_azalea_leaf", "world_ambience_falling_birch_leaf", "world_ambience_falling_cherry_leaf",
        "world_ambience_falling_chorus_petal", "world_ambience_falling_jungle_leaf", "world_ambience_falling_leaf",
        "world_ambience_falling_mangrove_leaf", "world_ambience_falling_pale_oak_leaf", "world_ambience_falling_spruce_leaf",
        "world_ambience_falling_tinted_leaf", "world_ambience_firefly", "world_ambience_goose", "world_ambience_leaf",
        "world_ambience_leaf_gust", "world_ambience_leaf_water_splash", "world_ambience_lens_flare", "world_ambience_light_ray",
        "world_ambience_passive_acacia_leaf", "world_ambience_passive_azalea_leaf", "world_ambience_passive_birch_leaf",
        "world_ambience_passive_cherry_leaf", "world_ambience_passive_jungle_leaf", "world_ambience_passive_mangrove_leaf",
        "world_ambience_passive_pale_oak_leaf", "world_ambience_passive_spruce_leaf", "world_ambience_pollen", "world_ambience_tumbleweed",
        "world_ambience_water_pollen", "world_ambience_wind_gust"
    };
    private static final Map<String, SimpleParticleType> TYPES = new LinkedHashMap<>();

    private ShineParticleTypes() {}

    public static void register() {
        for (String name : IDS) {
            if (TYPES.containsKey(name)) continue;
            ResourceLocation id = new ResourceLocation(MOD_ID, name);
            SimpleParticleType type = BuiltInRegistries.PARTICLE_TYPE.getOptional(id)
                .map(existing -> (SimpleParticleType) existing)
                .orElseGet(() -> Registry.register(BuiltInRegistries.PARTICLE_TYPE, id, FabricParticleTypes.simple()));
            TYPES.put(name, type);
        }
    }

    public static Map<String, SimpleParticleType> all() {
        register();
        return Collections.unmodifiableMap(TYPES);
    }

    public static int count() {
        return TYPES.size();
    }
}
