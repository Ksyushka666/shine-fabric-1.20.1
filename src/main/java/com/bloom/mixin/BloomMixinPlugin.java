package com.bloom.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class BloomMixinPlugin implements IMixinConfigPlugin {
	private static final boolean SODIUM_LOADED = FabricLoader.getInstance().isModLoaded("sodium");
	private static final boolean SODIUM_COMPATIBLE = isSupportedSodiumVersion();

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.contains(".sodium.")) {
				return SODIUM_LOADED && SODIUM_COMPATIBLE;
		}
		return true;
	}

	private static boolean isSupportedSodiumVersion() {
		if (!SODIUM_LOADED) return false;
		return FabricLoader.getInstance().getModContainer("sodium")
			.map(ModContainer::getMetadata)
.map(metadata -> metadata.getVersion().getFriendlyString())
				.map(BloomMixinPlugin::isSupportedSodiumVersionString)
			.orElse(false);
	}

	static boolean isSupportedSodiumVersionString(String version) {
		return version != null && version.startsWith("0.5.");
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

}
