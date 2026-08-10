package com.tastetheory.inventory.domain;

import com.tastetheory.shared.domain.Money;
import com.tastetheory.shared.domain.Percentage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * How much of what you buy survives to be cooked with.
 *
 * <p>Onions lose their skins and tops, fish loses its head and bones. At 80%
 * yield you must buy 1.25 kg for every kilo that reaches the pan, so the cost of
 * what you actually cook with is higher than the cost of what you bought. This is
 * the step spreadsheets usually miss, and the reason a food cost percentage
 * calculated without it reads better than reality.
 *
 * <p>Bounded above zero and at or below 100%. Zero would mean nothing survives,
 * which is not an ingredient; above 100% would mean cooking with more than was
 * bought. {@link Percentage} imposes neither bound, because a margin may
 * legitimately be negative — the constraint belongs here, to the thing being
 * measured.
 *
 * <p>Applies to cost and to nothing else. Nutrition figures already describe the
 * edible portion, so scaling those by yield would count the loss twice.
 */
public record YieldPercentage(Percentage percentage) {

	private static final int MULTIPLIER_SCALE = 6;

	public YieldPercentage {
		Objects.requireNonNull(percentage, "percentage must not be null");
		if (!percentage.isPositive()) {
			throw new IllegalArgumentException("Yield must be more than zero, but was " + percentage);
		}
		if (percentage.compareTo(Percentage.ONE_HUNDRED_PERCENT) > 0) {
			throw new IllegalArgumentException("Yield cannot exceed 100%, but was " + percentage);
		}
	}

	/** Nothing is lost: everything bought is cooked with. */
	public static YieldPercentage full() {
		return new YieldPercentage(Percentage.ONE_HUNDRED_PERCENT);
	}

	public static YieldPercentage ofPercent(long percent) {
		return new YieldPercentage(Percentage.ofPercent(percent));
	}

	public static YieldPercentage ofPercent(String percent) {
		return new YieldPercentage(Percentage.ofPercent(percent));
	}

	public static YieldPercentage ofFraction(String fraction) {
		return new YieldPercentage(Percentage.ofFraction(fraction));
	}

	public boolean isFull() {
		return percentage.equals(Percentage.ONE_HUNDRED_PERCENT);
	}

	/** How much must be bought per unit cooked with: 1.25 at 80% yield. */
	public BigDecimal purchaseMultiplier() {
		return BigDecimal.ONE.divide(percentage.asFraction(), MULTIPLIER_SCALE, RoundingMode.HALF_UP);
	}

	/**
	 * Raises a cost to account for what is thrown away. A cost of EUR 0.0009 per
	 * gram at 80% yield is really EUR 0.001125 per gram cooked with.
	 */
	public Money applyTo(Money costBeforeLoss) {
		Objects.requireNonNull(costBeforeLoss, "costBeforeLoss must not be null");
		return costBeforeLoss.dividedBy(percentage.asFraction());
	}

	@Override
	public String toString() {
		return percentage.toString();
	}
}
