package com.tastetheory.shared.domain;

import java.util.Objects;

/**
 * What one unit of something costs — EUR 4.50 per kilogram, EUR 0.0045 per gram.
 *
 * <p>A rate, not an amount, and a separate type from {@link Money} because the
 * two behave differently. An amount is something a person pays and must
 * reconcile to the cent. A rate is nearly always derived, is not itself paid,
 * and must never be rounded to the cent: at EUR 0.0009 per gram, doing so leaves
 * nothing at all.
 *
 * <p>The separation is also what stops a whole category of mistake. Multiplying a
 * rate by a quantity gives an amount; adding a rate to an amount is meaningless,
 * and here it does not compile.
 *
 * <p>Note what is deliberately missing: there is no method to round a unit price
 * to cents, and no way to recover the total it came from. A total divided into a
 * rate does not multiply back exactly — EUR 8.90 across 12 units is EUR
 * 0.741666… — which is why invoice line totals are recorded as entered and the
 * rate is derived from them, never the other way round.
 */
public record UnitPrice(Money amount, UnitOfMeasure unit) implements Comparable<UnitPrice> {

	public UnitPrice {
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(unit, "unit must not be null");
		if (amount.isNegative()) {
			throw new IllegalArgumentException("A unit price cannot be negative: " + amount);
		}
	}

	public static UnitPrice of(Money amount, UnitOfMeasure unit) {
		return new UnitPrice(amount, unit);
	}

	/**
	 * The rate implied by paying {@code total} for {@code quantity} — a pack
	 * price divided by its pack size, or an invoice line total divided by what was
	 * delivered.
	 *
	 * @throws ArithmeticException if the quantity is zero
	 */
	public static UnitPrice from(Money total, Quantity quantity) {
		Objects.requireNonNull(total, "total must not be null");
		Objects.requireNonNull(quantity, "quantity must not be null");
		return new UnitPrice(total.dividedBy(quantity.amount()), quantity.unit());
	}

	/**
	 * What {@code quantity} of this costs, converting the quantity into this
	 * price's unit first.
	 *
	 * @throws IncompatibleUnitsException if the quantity measures something else
	 */
	public Money times(Quantity quantity) {
		Objects.requireNonNull(quantity, "quantity must not be null");
		return amount.times(quantity.convertedTo(unit).amount());
	}

	/**
	 * The same rate expressed per a different unit: EUR 4.50 per kilogram is EUR
	 * 0.0045 per gram.
	 *
	 * @throws IncompatibleUnitsException if the units measure different things
	 */
	public UnitPrice convertedTo(UnitOfMeasure target) {
		Objects.requireNonNull(target, "target must not be null");
		if (unit == target) {
			return this;
		}
		return new UnitPrice(amount.dividedBy(unit.conversionFactorTo(target)), target);
	}

	public boolean isZero() {
		return amount.isZero();
	}

	@Override
	public int compareTo(UnitPrice other) {
		Objects.requireNonNull(other, "other must not be null");
		return amount.compareTo(other.convertedTo(unit).amount);
	}

	@Override
	public String toString() {
		return amount + "/" + unit.code();
	}
}
