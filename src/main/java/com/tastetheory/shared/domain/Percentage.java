package com.tastetheory.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * A proportion — a yield of 80%, IVA at 6%, a gross margin of 62.5%.
 *
 * <p>Held internally as a fraction, always. The application it replaces accepted
 * IVA as either {@code 6} or {@code 0.06} and guessed which was meant, which
 * works until someone enters a genuine 0.06% and quietly gets six percent. Here
 * there is no guessing: {@link #ofPercent} and {@link #ofFraction} say which
 * convention the caller had in mind, and there is no constructor that infers it.
 * Whatever coercion an untidy spreadsheet import needs belongs at the edge of the
 * application, not in here.
 *
 * <p>No bounds are imposed. A yield must be above zero and at most 100%, IVA
 * cannot be negative, and a margin can be either — those are facts about yields,
 * tax rates and margins rather than about proportions, so each belongs to the
 * type that models it.
 */
public record Percentage(BigDecimal fraction) implements Comparable<Percentage> {

	public static final int SCALE = 6;

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
	private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

	public static final Percentage ZERO = ofFraction(BigDecimal.ZERO);
	public static final Percentage ONE_HUNDRED_PERCENT = ofFraction(BigDecimal.ONE);

	public Percentage {
		Objects.requireNonNull(fraction, "fraction must not be null");
		fraction = fraction.setScale(SCALE, ROUNDING);
	}

	/** {@code ofPercent(80)} is four fifths. */
	public static Percentage ofPercent(BigDecimal percent) {
		Objects.requireNonNull(percent, "percent must not be null");
		return new Percentage(percent.movePointLeft(2));
	}

	public static Percentage ofPercent(String percent) {
		return ofPercent(new BigDecimal(percent));
	}

	public static Percentage ofPercent(long percent) {
		return ofPercent(BigDecimal.valueOf(percent));
	}

	/** {@code ofFraction(0.8)} is the same four fifths. */
	public static Percentage ofFraction(BigDecimal fraction) {
		return new Percentage(fraction);
	}

	public static Percentage ofFraction(String fraction) {
		return new Percentage(new BigDecimal(fraction));
	}

	/** 0.8 for eighty percent. Use this to scale something. */
	public BigDecimal asFraction() {
		return fraction;
	}

	/** 80 for eighty percent. Use this to show someone. */
	public BigDecimal asPercent() {
		return fraction.multiply(ONE_HUNDRED);
	}

	/** This much of an amount — 6% of EUR 8.90 being the IVA on it. */
	public Money applyTo(Money amount) {
		Objects.requireNonNull(amount, "amount must not be null");
		return amount.times(fraction);
	}

	/** What is left over: the complement of a 10% discount is 90%. */
	public Percentage complement() {
		return new Percentage(BigDecimal.ONE.subtract(fraction));
	}

	public boolean isZero() {
		return fraction.signum() == 0;
	}

	public boolean isNegative() {
		return fraction.signum() < 0;
	}

	public boolean isPositive() {
		return fraction.signum() > 0;
	}

	@Override
	public int compareTo(Percentage other) {
		Objects.requireNonNull(other, "other must not be null");
		return fraction.compareTo(other.fraction);
	}

	@Override
	public String toString() {
		return asPercent().stripTrailingZeros().toPlainString() + "%";
	}
}
