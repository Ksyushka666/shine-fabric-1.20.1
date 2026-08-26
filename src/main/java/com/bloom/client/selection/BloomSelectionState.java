package com.bloom.client.selection;

public final class BloomSelectionState {
	private static final ThreadLocal<Double> BLOCK_STRENGTH = ThreadLocal.withInitial(() -> 0.0);
	private static final ThreadLocal<Double> FLUID_STRENGTH = ThreadLocal.withInitial(() -> 0.0);
	private static final ThreadLocal<Double> ENTITY_STRENGTH = ThreadLocal.withInitial(() -> 0.0);
	private static final ThreadLocal<Double> PARTICLE_STRENGTH = ThreadLocal.withInitial(() -> 0.0);

	private BloomSelectionState() {
	}

	public static double pushBlockStrength(double strength) {
		double previous = BLOCK_STRENGTH.get();
		BLOCK_STRENGTH.set(strength);
		return previous;
	}

	public static void popBlockStrength(double previous) {
		BLOCK_STRENGTH.set(previous);
	}

	public static double getBlockStrength() {
		return BLOCK_STRENGTH.get();
	}

	public static double pushFluidStrength(double strength) {
		double previous = FLUID_STRENGTH.get();
		FLUID_STRENGTH.set(strength);
		return previous;
	}

	public static void popFluidStrength(double previous) {
		FLUID_STRENGTH.set(previous);
	}

	public static double getFluidStrength() {
		return FLUID_STRENGTH.get();
	}

	public static double pushEntityStrength(double strength) {
		double previous = ENTITY_STRENGTH.get();
		ENTITY_STRENGTH.set(strength);
		return previous;
	}

	public static void popEntityStrength(double previous) {
		ENTITY_STRENGTH.set(previous);
	}

	public static double getEntityStrength() {
		return ENTITY_STRENGTH.get();
	}

	public static double pushParticleStrength(double strength) {
		double previous = PARTICLE_STRENGTH.get();
		PARTICLE_STRENGTH.set(strength);
		return previous;
	}

	public static void popParticleStrength(double previous) {
		PARTICLE_STRENGTH.set(previous);
	}

	public static double getParticleStrength() {
			return PARTICLE_STRENGTH.get();
		}

	/** Clears all thread-local render state at a frame/world lifecycle boundary. */
	public static void reset() {
		BLOCK_STRENGTH.set(0.0);
		FLUID_STRENGTH.set(0.0);
		ENTITY_STRENGTH.set(0.0);
		PARTICLE_STRENGTH.set(0.0);
	}
	}
