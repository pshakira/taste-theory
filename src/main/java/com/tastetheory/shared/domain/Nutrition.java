package com.tastetheory.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Energy and macronutrients: kilocalories, and grams of everything else.
 *
 * <p>Used two ways. On an item it is a rate — the figures for 100 recipe units,
 * which is how a supplier's specification arrives. In a recipe it is an absolute
 * amount, summed across ingredients and divided by servings. The item is the only
 * place the per-100 reading applies, and it says so in the field name.
 *
 * <p>I considered separating the two into distinct types, as {@link Money} and
 * {@link UnitPrice} are separated. It is not worth it here: money amounts and
 * money rates are mixed constantly throughout costing, whereas the per-100
 * reading appears in exactly one place and is converted immediately.
 *
 * <p>Yield never applies to these figures. A supplier's numbers already describe
 * the edible portion, so raising them for trim loss counts the waste twice.
 */
public record Nutrition(
		BigDecimal energyKcal,
		BigDecimal protein,
		BigDecimal carbohydrate,
		BigDecimal fat,
		BigDecimal fibre,
		BigDecimal sodium) {

	public static final int SCALE = 6;

	private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
	private static final Nutrition ZERO = of("0", "0", "0", "0", "0", "0");

	public Nutrition {
		energyKcal = normalise(energyKcal, "energyKcal");
		protein = normalise(protein, "protein");
		carbohydrate = normalise(carbohydrate, "carbohydrate");
		fat = normalise(fat, "fat");
		fibre = normalise(fibre, "fibre");
		sodium = normalise(sodium, "sodium");
	}

	public static Nutrition of(
			String energyKcal, String protein, String carbohydrate, String fat, String fibre, String sodium) {
		return new Nutrition(
				new BigDecimal(energyKcal),
				new BigDecimal(protein),
				new BigDecimal(carbohydrate),
				new BigDecimal(fat),
				new BigDecimal(fibre),
				new BigDecimal(sodium));
	}

	/** All zeros — nothing declared yet, rather than a claim that it is calorie-free. */
	public static Nutrition zero() {
		return ZERO;
	}

	public Nutrition plus(Nutrition other) {
		Objects.requireNonNull(other, "other must not be null");
		return new Nutrition(
				energyKcal.add(other.energyKcal),
				protein.add(other.protein),
				carbohydrate.add(other.carbohydrate),
				fat.add(other.fat),
				fibre.add(other.fibre),
				sodium.add(other.sodium));
	}

	public Nutrition scaledBy(BigDecimal factor) {
		Objects.requireNonNull(factor, "factor must not be null");
		if (factor.signum() < 0) {
			throw new IllegalArgumentException("Cannot scale nutrition by a negative factor: " + factor);
		}
		return new Nutrition(
				energyKcal.multiply(factor),
				protein.multiply(factor),
				carbohydrate.multiply(factor),
				fat.multiply(factor),
				fibre.multiply(factor),
				sodium.multiply(factor));
	}

	public Nutrition dividedBy(BigDecimal divisor) {
		Objects.requireNonNull(divisor, "divisor must not be null");
		if (divisor.signum() <= 0) {
			throw new ArithmeticException("Cannot divide nutrition by " + divisor);
		}
		return new Nutrition(
				energyKcal.divide(divisor, SCALE, ROUNDING),
				protein.divide(divisor, SCALE, ROUNDING),
				carbohydrate.divide(divisor, SCALE, ROUNDING),
				fat.divide(divisor, SCALE, ROUNDING),
				fibre.divide(divisor, SCALE, ROUNDING),
				sodium.divide(divisor, SCALE, ROUNDING));
	}

	public boolean isZero() {
		return equals(ZERO);
	}

	private static BigDecimal normalise(BigDecimal value, String field) {
		Objects.requireNonNull(value, field + " must not be null");
		if (value.signum() < 0) {
			throw new IllegalArgumentException("%s cannot be negative: %s".formatted(field, value.toPlainString()));
		}
		return value.setScale(SCALE, ROUNDING);
	}
}
