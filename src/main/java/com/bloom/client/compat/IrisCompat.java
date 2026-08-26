package com.bloom.client.compat;

import com.bloom.BloomMod;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;

public final class IrisCompat {
	private static final boolean IRIS_LOADED = FabricLoader.getInstance().isModLoaded("iris");
	private static final String ACTIVE_MESSAGE = "Shine bloom disabled: Iris shader pack is active.";
	private static final String INSTALLED_MESSAGE = "Shine could not verify Iris shader-pack state; native shader uniforms are left untouched.";

	private static volatile boolean reflectionInitialized;
	private static volatile boolean reflectionAvailable;
	private static volatile boolean loggedReflectionFailure;
	private static Method irisGetInstanceMethod;
	private static Method irisIsShaderPackInUseMethod;
	private static String disableMessage;

	private IrisCompat() {
	}

	public static boolean shouldDisableBloom() {
		disableMessage = null;
		if (!IRIS_LOADED) {
			return false;
		}

		if (!initReflection()) {
				// Iris may be present without exposing the optional API (or may be an older 1.20.1 build).
				// Do not disable Shine in that case: the user explicitly enabled bloom and the legacy
				// pipeline remains safe to run unless Iris confirms that a shader pack owns the frame.
				disableMessage = null;
				return false;
			}

		try {
			Object irisApi = irisGetInstanceMethod.invoke(null);
			Object result = irisIsShaderPackInUseMethod.invoke(irisApi);
			boolean shaderPackActive = result instanceof Boolean bool && bool;
			disableMessage = shaderPackActive ? ACTIVE_MESSAGE : null;
			return shaderPackActive;
		} catch (ReflectiveOperationException | RuntimeException e) {
			if (!loggedReflectionFailure) {
					BloomMod.LOGGER.warn("Shine could not verify Iris shader-pack state; continuing with Shine bloom.", e);
				loggedReflectionFailure = true;
			}
				disableMessage = null;
				return false;
		}
	}

	public static boolean shouldYieldToShaderPack() {
		return IRIS_LOADED && shouldDisableBloom();
	}

	public static String disableMessage() {
		return disableMessage;
	}

	private static synchronized boolean initReflection() {
		if (reflectionInitialized) {
			return reflectionAvailable;
		}

		reflectionInitialized = true;
		try {
			Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			irisGetInstanceMethod = irisApiClass.getMethod("getInstance");
			irisIsShaderPackInUseMethod = irisApiClass.getMethod("isShaderPackInUse");
			reflectionAvailable = true;
			return true;
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			if (!loggedReflectionFailure) {
				BloomMod.LOGGER.warn("Shine could not access Iris API state; leaving Iris-owned shader uniforms untouched.");
				loggedReflectionFailure = true;
			}
			reflectionAvailable = false;
			return false;
		}
	}
}
