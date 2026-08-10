package com.tastetheory.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount together with the unit it is measured in.
 *
 * <p>The point of the type is that a bare number can never be passed where a
 * quantity is meant. "500" is ambiguous in a kitchen; "500 g" is not, and once
 * the unit travels with the number the application can refuse to add grams to
 * litres instead of silently producing 1500 of nothing in particular.
 *
 * <p>Quantities are never negative. Direction belongs to the operation — a
 * delivery adds, waste removes — not to the amount itself, and a negative
 * quantity of flour is not a thing that exists. Subtracting past zero therefore
 * fails rather than quietly producing one.
 *
 * <p>Normalised to a fixed scale for the same reason as {@link Money}: so that
 * equality compares values rather than how many zeros somebody typed.
 */
public record Quantity(BigDecimal amount, UnitOfMeasure unit) implements Comparable<Quantity> {

	public static final int SCALE = 6;

	private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

	public Quantity {
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(unit, "unit must not be null");
		if (amount.signum() < 0) {
			throw new IllegalArgumentException(
					"Quantity cannot be negative: %s %s".formatted(amount.toPlainString(), unit.code()));
		}
		amount = amount.setScale(SCALE, ROUNDING);
	}

	public static Quantity of(BigDecimal amount, UnitOfMeasure unit) {
		return new Quantity(amount, unit);
	}

	/** Takes a string rather than a double, for the reason given on {@link Money}. */
	public static Quantity of(String amount, UnitOfMeasure unit) {
		return new Quantity(new BigDecimal(amount), unit);
	}

	public static Quantity of(long amount, UnitOfMeasure unit) {
		return new Quantity(BigDecimal.valueOf(amount), unit);
	}

	public static Quantity zero(UnitOfMeasure unit) {
		return new Quantity(BigDecimal.ZERO, unit);
	}

	/**
	 * The same quantity expressed in another unit of the same dimension.
	 *
	 * @throws IncompatibleUnitsException if the units measure different things
	 */
	public Quantity convertedTo(UnitOfMeasure target) {
		Objects.requireNonNull(target, "target must not be null");
		if (unit == target) {
			return this;
		}
		return new Quantity(amount.multiply(unit.conversionFactorTo(target)), target);
	}

	/**
	 * Adds, converting the other quantity into this one's unit first. 1 kg plus
	 * 500 g is 1.5 kg; 1 kg plus 500 ml is an error.
	 */
	public Quantity plus(Quantity other) {
		Objects.requireNonNull(other, "other must not be null");
		return new Quantity(amount.add(other.convertedTo(unit).amount), unit);
	}

	/**
	 * Subtracts, converting first as {@link #plus} does.
	 *
	 * @throws IllegalArgumentException if the result would be below zero
	 */
	public Quantity minus(Quantity other) {
		Objects.requireNonNull(other, "other must not be null");
		BigDecimal result = amount.subtract(other.convertedTo(unit).amount);
		if (result.signum() < 0) {
			throw new IllegalArgumentException("Cannot subtract %s from %s: the result would be negative"
					.formatted(other, this));
		}
		return new Quantity(result, unit);
	}

	public Quantity times(BigDecimal factor) {
		Objects.requireNonNull(factor, "factor must not be null");
		return new Quantity(amount.multiply(factor), unit);
	}

	public Quantity dividedBy(BigDecimal divisor) {
		Objects.requireNonNull(divisor, "divisor must not be null");
		if (divisor.signum() == 0) {
			throw new ArithmeticException("Cannot divide " + this + " by zero");
		}
		return new Quantity(amount.divide(divisor, SCALE, ROUNDING), unit);
	}

	public boolean isZero() {
		return amount.signum() == 0;
	}

	public boolean isPositive() {
		return amount.signum() > 0;
	}

	public Dimension dimension() {
		return unit.dimension();
	}

	@Override
	public int compareTo(Quantity other) {
		Objects.requireNonNull(other, "other must not be null");
		return amount.compareTo(other.convertedTo(unit).amount);
	}

	/** Trailing zeros dropped, so a failing test reads "1500 g" and not "1500.000000 g". */
	@Override
	public String toString() {
		return amount.stripTrailingZeros().toPlainString() + " " + unit.code();
	}
}
