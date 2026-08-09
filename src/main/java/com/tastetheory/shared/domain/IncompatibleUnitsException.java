package com.tastetheory.shared.domain;

/**
 * Thrown when two units cannot be reconciled — converting kilograms to
 * millilitres, or adding grams to litres.
 *
 * <p>Named rather than a bare {@link IllegalArgumentException} because it is a
 * domain rule being enforced, not a programming slip, and callers higher up may
 * reasonably want to catch it and explain it to a user.
 */
public class IncompatibleUnitsException extends RuntimeException {

	private final UnitOfMeasure from;
	private final UnitOfMeasure to;

	public IncompatibleUnitsException(UnitOfMeasure from, UnitOfMeasure to) {
		super("Cannot convert %s (%s) to %s (%s): an item is measured by one or the other, never both"
				.formatted(from.code(), from.dimension(), to.code(), to.dimension()));
		this.from = from;
		this.to = to;
	}

	public UnitOfMeasure from() {
		return from;
	}

	public UnitOfMeasure to() {
		return to;
	}
}
