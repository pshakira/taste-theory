package com.tastetheory.inventory.domain;

import com.tastetheory.shared.domain.Money;
import com.tastetheory.shared.domain.Quantity;
import com.tastetheory.shared.domain.UnitOfMeasure;
import com.tastetheory.shared.domain.UnitPrice;
import java.util.Objects;

/**
 * The price agreed with a supplier for one purchase unit, and how it was
 * arrived at.
 *
 * <p>There are two ways to enter it, and they are genuinely different pieces of
 * information rather than two spellings of the same one. Most invoices quote a
 * pack: a sack costs EUR 4.50 and holds 5 kg, so a kilo is EUR 0.90. Some quote
 * a rate directly, and there is nothing to divide.
 *
 * <p>Modelling that as two types rather than four nullable fields fixes a real
 * defect in the system this replaces. That one stored a single cost and worked
 * out where it had come from by dividing the pack fields and checking whether
 * the answer matched — so an invoice price that happened to equal the pack price
 * was reported as a pack price, and any difference in decimal places flipped the
 * answer. Here the question is not asked, because the type is the answer.
 *
 * <p>This is what the item costs to buy. What it costs to cook with is a
 * different figure, arrived at by converting into recipe units and applying
 * yield.
 */
public sealed interface AgreedPrice permits AgreedPrice.PerPack, AgreedPrice.PerUnit {

	/** What one purchase unit costs, however the price was entered. */
	UnitPrice perPurchaseUnit();

	/** The unit this price is per — the unit the item is bought in. */
	default UnitOfMeasure purchaseUnit() {
		return perPurchaseUnit().unit();
	}

	static AgreedPrice perPack(Money packPrice, Quantity packSize) {
		return new PerPack(packPrice, packSize);
	}

	static AgreedPrice perUnit(UnitPrice price) {
		return new PerUnit(price);
	}

	static AgreedPrice perUnit(Money amount, UnitOfMeasure unit) {
		return new PerUnit(UnitPrice.of(amount, unit));
	}

	/**
	 * A pack price and the size of the pack: EUR 4.50 for a 5 kg sack. The rate is
	 * derived and never stored, so it cannot drift from the figures it came from.
	 */
	record PerPack(Money packPrice, Quantity packSize) implements AgreedPrice {

		public PerPack {
			Objects.requireNonNull(packPrice, "packPrice must not be null");
			Objects.requireNonNull(packSize, "packSize must not be null");
			if (packPrice.isNegative()) {
				throw new IllegalArgumentException("A pack price cannot be negative: " + packPrice);
			}
			if (!packSize.isPositive()) {
				throw new IllegalArgumentException("A pack must hold more than nothing");
			}
		}

		@Override
		public UnitPrice perPurchaseUnit() {
			return UnitPrice.from(packPrice, packSize);
		}

		@Override
		public UnitOfMeasure purchaseUnit() {
			return packSize.unit();
		}

		@Override
		public String toString() {
			return "%s per %s".formatted(packPrice, packSize);
		}
	}

	/** A rate entered directly, with no pack to divide by. */
	record PerUnit(UnitPrice price) implements AgreedPrice {

		public PerUnit {
			Objects.requireNonNull(price, "price must not be null");
		}

		@Override
		public UnitPrice perPurchaseUnit() {
			return price;
		}

		@Override
		public String toString() {
			return price.toString();
		}
	}
}
