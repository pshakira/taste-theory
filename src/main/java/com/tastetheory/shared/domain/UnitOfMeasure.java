package com.tastetheory.shared.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A unit an ingredient can be bought or cooked in.
 *
 * <p>Each unit knows what it measures and how it relates to the base unit of
 * that dimension — grams for mass, millilitres for volume, single items for
 * count. Conversion within a dimension is therefore arithmetic the application
 * already knows: nobody has to tell it that a kilogram is a thousand grams, and
 * nobody can tell it otherwise. The old system stored that factor per item,
 * which meant a typo could quietly declare a kilogram to be nine hundred grams
 * and understate every cost derived from it.
 *
 * <p>Conversion across dimensions is refused. See {@link Dimension} for the one
 * bridge that is allowed, and why it does not live here.
 *
 * <p>The set is deliberately closed. A restaurant buys in a small number of
 * units, and packaging — a sack, a case, a tin — is modelled as a pack size
 * against one of these rather than as a unit of its own. Adding a unit means
 * adding a constant here, which is a change worth making consciously.
 */
public enum UnitOfMeasure {

	GRAM("g", Dimension.MASS, "1"),
	KILOGRAM("kg", Dimension.MASS, "1000"),

	MILLILITRE("ml", Dimension.VOLUME, "1"),
	LITRE("l", Dimension.VOLUME, "1000"),

	EACH("each", Dimension.COUNT, "1"),
	DOZEN("dozen", Dimension.COUNT, "12");

	private static final MathContext DIVISION_PRECISION = MathContext.DECIMAL128;

	private static final Map<String, UnitOfMeasure> BY_CODE = Arrays.stream(values())
			.collect(Collectors.toUnmodifiableMap(unit -> unit.code, unit -> unit));

	private final String code;
	private final Dimension dimension;
	private final BigDecimal factorToBaseUnit;

	UnitOfMeasure(String code, Dimension dimension, String factorToBaseUnit) {
		this.code = code;
		this.dimension = dimension;
		this.factorToBaseUnit = new BigDecimal(factorToBaseUnit);
	}

	/** The short form a person writes and the API accepts: {@code kg}, {@code ml}. */
	public String code() {
		return code;
	}

	public Dimension dimension() {
		return dimension;
	}

	/** How many of this dimension's base units make one of this unit. */
	public BigDecimal factorToBaseUnit() {
		return factorToBaseUnit;
	}

	public boolean isBaseUnit() {
		return factorToBaseUnit.compareTo(BigDecimal.ONE) == 0;
	}

	public boolean sharesDimensionWith(UnitOfMeasure other) {
		Objects.requireNonNull(other, "other must not be null");
		return dimension == other.dimension;
	}

	/**
	 * How many of {@code target} make one of this unit — 1000 from kilograms to
	 * grams, 0.001 back the other way.
	 *
	 * @throws IncompatibleUnitsException if the two units measure different things
	 */
	public BigDecimal conversionFactorTo(UnitOfMeasure target) {
		Objects.requireNonNull(target, "target must not be null");
		if (!sharesDimensionWith(target)) {
			throw new IncompatibleUnitsException(this, target);
		}
		if (this == target) {
			return BigDecimal.ONE;
		}
		return factorToBaseUnit.divide(target.factorToBaseUnit, DIVISION_PRECISION);
	}

	/**
	 * Parses the short form, case-insensitively. For the edge of the application —
	 * request bodies, imported spreadsheets — where units arrive as text.
	 */
	public static UnitOfMeasure fromCode(String code) {
		Objects.requireNonNull(code, "code must not be null");
		UnitOfMeasure unit = BY_CODE.get(code.trim().toLowerCase(Locale.ROOT));
		if (unit == null) {
			throw new IllegalArgumentException("Unknown unit '%s'. Known units: %s"
					.formatted(code, BY_CODE.keySet().stream().sorted().collect(Collectors.joining(", "))));
		}
		return unit;
	}

	@Override
	public String toString() {
		return code;
	}
}
