package com.tastetheory.inventory.domain;

import com.tastetheory.shared.domain.Dimension;
import com.tastetheory.shared.domain.IncompatibleUnitsException;
import com.tastetheory.shared.domain.Quantity;
import com.tastetheory.shared.domain.UnitOfMeasure;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * How an item is bought and how it is cooked with, and the bridge between them.
 *
 * <p>Onions arrive by the kilogram and are weighed out in grams. Both measure
 * mass, so the bridge is arithmetic the application already knows and nobody is
 * asked for it.
 *
 * <p>Tinned tomatoes arrive by the tin and are measured out in grams. Nothing in
 * a unit table can know how much is in a tin, so that figure — {@code
 * contentsPerPurchaseUnit} — has to be supplied. It is required in exactly that
 * situation and rejected everywhere else, because where the conversion is already
 * known, accepting a second opinion invites the two to disagree.
 *
 * <p>Mass never bridges to volume. That needs a density per ingredient, which is
 * not modelled, and guessing it would quietly corrupt every cost derived from it.
 */
public record ItemUnits(
		UnitOfMeasure purchaseUnit,
		UnitOfMeasure recipeUnit,
		Quantity contentsPerPurchaseUnit) {

	public ItemUnits {
		Objects.requireNonNull(purchaseUnit, "purchaseUnit must not be null");
		Objects.requireNonNull(recipeUnit, "recipeUnit must not be null");

		if (purchaseUnit.sharesDimensionWith(recipeUnit)) {
			if (contentsPerPurchaseUnit != null) {
				throw new IllegalArgumentException(
						"Contents must not be given for %s to %s: the conversion is already known"
								.formatted(purchaseUnit.code(), recipeUnit.code()));
			}
		}
		else if (purchaseUnit.dimension() == Dimension.COUNT) {
			if (contentsPerPurchaseUnit == null) {
				throw new IllegalArgumentException(
						"Contents are required for %s to %s: state how much one %s holds"
								.formatted(purchaseUnit.code(), recipeUnit.code(), purchaseUnit.code()));
			}
			if (contentsPerPurchaseUnit.unit() != recipeUnit) {
				throw new IllegalArgumentException("Contents must be stated in %s, the recipe unit, but were %s"
						.formatted(recipeUnit.code(), contentsPerPurchaseUnit));
			}
			if (!contentsPerPurchaseUnit.isPositive()) {
				throw new IllegalArgumentException("Contents must be more than nothing");
			}
		}
		else {
			// Mass to volume, or a weight bought by the kilo and cooked by the
			// item. Neither has an answer this application can stand behind.
			throw new IncompatibleUnitsException(purchaseUnit, recipeUnit);
		}
	}

	/** Units that measure the same thing, so the conversion is already known. */
	public static ItemUnits of(UnitOfMeasure purchaseUnit, UnitOfMeasure recipeUnit) {
		return new ItemUnits(purchaseUnit, recipeUnit, null);
	}

	/** A countable purchase measured out by weight or volume: a tin holding 400 g. */
	public static ItemUnits withContents(
			UnitOfMeasure purchaseUnit, UnitOfMeasure recipeUnit, Quantity contentsPerPurchaseUnit) {
		return new ItemUnits(purchaseUnit, recipeUnit, contentsPerPurchaseUnit);
	}

	/** Bought and cooked in the same unit. */
	public static ItemUnits same(UnitOfMeasure unit) {
		return new ItemUnits(unit, unit, null);
	}

	public Optional<Quantity> contents() {
		return Optional.ofNullable(contentsPerPurchaseUnit);
	}

	/** True when the item is counted on delivery but measured out by weight or volume. */
	public boolean bridgesFromCount() {
		return contentsPerPurchaseUnit != null;
	}

	/** How many recipe units one purchase unit yields: 1000 for a kilo cooked in grams. */
	public BigDecimal recipeUnitsPerPurchaseUnit() {
		return bridgesFromCount()
				? contentsPerPurchaseUnit.amount()
				: purchaseUnit.conversionFactorTo(recipeUnit);
	}

	/**
	 * Restates a purchased quantity in recipe units: five sacks of 5 kg become
	 * 25000 g.
	 *
	 * @throws IncompatibleUnitsException if the quantity is not in the purchase unit
	 */
	public Quantity toRecipeUnits(Quantity purchased) {
		Objects.requireNonNull(purchased, "purchased must not be null");
		Quantity inPurchaseUnit = purchased.convertedTo(purchaseUnit);
		return Quantity.of(inPurchaseUnit.amount().multiply(recipeUnitsPerPurchaseUnit()), recipeUnit);
	}
}
