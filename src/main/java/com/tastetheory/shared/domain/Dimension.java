package com.tastetheory.shared.domain;

/**
 * What a unit measures.
 *
 * <p>The distinction exists to make one rule structural rather than advisory:
 * mass and volume never convert into one another, because doing so needs a
 * density per ingredient that this application does not hold. Flour is bought by
 * weight and water by volume, and a cup of one is not a cup of the other.
 *
 * <p>{@link #COUNT} is the exception worth knowing about. A tin is a countable
 * thing, but its contents are a mass, and a kitchen genuinely does buy tins and
 * cook in grams. That bridge is allowed — but only with a figure the user
 * supplies ("each tin holds 400 g"), which lives on the inventory item rather
 * than here. No amount of unit metadata can know it.
 */
public enum Dimension {

	MASS,
	VOLUME,
	COUNT
}
