package com.bloom.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

public class BloomModMenuIntegration implements ModMenuApi {
	@Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!FabricLoader.getInstance().isModLoaded("yet_another_config_lib_v3")) {
            return parent -> parent;
        }
        return BloomConfigScreen::create;
    }
}
