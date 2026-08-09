package com.tastetheory.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * An amount of money in a particular currency.
 *
 * <p>Held to six decimal places, not two. Two would be enough for anything a
 * person actually pays, but almost every figure in this application is derived:
 * a EUR 4.50 sack of 5 kg costs EUR 0.0009 per gram, and at two decimal places
 * that is zero. Six places is roughly a ten-thousandth of a cent, comfortably
 * below anything that could matter here, and {@link #roundedToMinorUnit()}
 * produces the payable figure when one is needed.
 *
 * <p>Fixing the scale also makes equality behave. {@link BigDecimal#equals} takes
 * scale into account, so {@code 0.10} and {@code 0.1000} are unequal to it — a
 * genuinely surprising source of bugs. Because every instance is normalised on
 * construction, the record's generated {@code equals} compares what a reader
 * would expect.
 *
 * <p>Negative amounts are allowed: margins, price variations and eventually
 * credit notes all need them. Where a negative value would be nonsense, the rule
 * belongs on the type that says so, not here.
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

	public static final Currency EUR = Currency.getInstance("EUR");

	/** Decimal places every amount is normalised to. See the class comment. */
	public static final int SCALE = 6;

	private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

	public Money {
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(currency, "currency must not be null");
		amount = amount.setScale(SCALE, ROUNDING);
	}

	public static Money of(BigDecimal amount, Currency currency) {
		return new Money(amount, currency);
	}

	public static Money euros(BigDecimal amount) {
		return new Money(amount, EUR);
	}

	/**
	 * Deliberately takes a string rather than a {@code double}: {@code 0.1} has no
	 * exact binary representation, so {@code new BigDecimal(0.1)} is
	 * 0.1000000000000000055511151231257827…
	 */
	public static Money euros(String amount) {
		return euros(new BigDecimal(amount));
	}

	public static Money euros(long amount) {
		return euros(BigDecimal.valueOf(amount));
	}

	public static Money zero(Currency currency) {
		return new Money(BigDecimal.ZERO, currency);
	}

	public Money plus(Money other) {
		requireSameCurrency(other);
		return new Money(amount.add(other.amount), currency);
	}

	public Money minus(Money other) {
		requireSameCurrency(other);
		return new Money(amount.subtract(other.amount), currency);
	}

	public Money times(BigDecimal factor) {
		Objects.requireNonNull(factor, "factor must not be null");
		return new Money(amount.multiply(factor), currency);
	}

	/**
	 * Divides, rounding to this type's scale. Used for things like cost per
	 * serving, where the division genuinely does not come out exactly.
	 */
	public Money dividedBy(BigDecimal divisor) {
		Objects.requireNonNull(divisor, "divisor must not be null");
		if (divisor.signum() == 0) {
			throw new ArithmeticException("Cannot divide " + this + " by zero");
		}
		return new Money(amount.divide(divisor, SCALE, ROUNDING), currency);
	}

	public Money negated() {
		return new Money(amount.negate(), currency);
	}

	public boolean isZero() {
		return amount.signum() == 0;
	}

	public boolean isNegative() {
		return amount.signum() < 0;
	}

	public boolean isPositive() {
		return amount.signum() > 0;
	}

	/**
	 * Rounded to the currency's smallest real unit — cents, for euros.
	 *
	 * <p>Call this once, at the end of a calculation. Rounding partway through and
	 * then continuing is how totals stop adding up.
	 */
	public Money roundedToMinorUnit() {
		return new Money(amount.setScale(currency.getDefaultFractionDigits(), ROUNDING), currency);
	}

	@Override
	public int compareTo(Money other) {
		requireSameCurrency(other);
		return amount.compareTo(other.amount);
	}

	private void requireSameCurrency(Money other) {
		Objects.requireNonNull(other, "other must not be null");
		if (!currency.equals(other.currency)) {
			throw new IllegalArgumentException("Cannot combine %s with %s"
					.formatted(currency.getCurrencyCode(), other.currency.getCurrencyCode()));
		}
	}

	@Override
	public String toString() {
		return currency.getCurrencyCode() + " " + amount.toPlainString();
	}
}
