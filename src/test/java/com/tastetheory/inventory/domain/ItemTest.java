package com.tastetheory.inventory.domain;

import static com.tastetheory.shared.domain.UnitOfMeasure.EACH;
import static com.tastetheory.shared.domain.UnitOfMeasure.GRAM;
import static com.tastetheory.shared.domain.UnitOfMeasure.KILOGRAM;
import static com.tastetheory.shared.domain.UnitOfMeasure.LITRE;
import static com.tastetheory.shared.domain.UnitOfMeasure.MILLILITRE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.tastetheory.shared.domain.Money;
import com.tastetheory.shared.domain.Percentage;
import com.tastetheory.shared.domain.Quantity;
import com.tastetheory.shared.domain.UnitPrice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ItemTest {

	/** A 5 kg sack at EUR 4.50, cooked in grams, losing a fifth to skins and tops. */
	private static Item onions() {
		return Item.create(
				"Onions",
				ItemUnits.of(KILOGRAM, GRAM),
				AgreedPrice.perPack(Money.euros("4.50"), Quantity.of("5", KILOGRAM)),
				YieldPercentage.ofPercent(80));
	}

	/** Tins bought singly at EUR 0.80, each holding 400 g, all of it usable. */
	private static Item tinnedTomatoes() {
		return Item.create(
				"Tinned tomatoes",
				ItemUnits.withContents(EACH, GRAM, Quantity.of("400", GRAM)),
				AgreedPrice.perUnit(Money.euros("0.80"), EACH),
				YieldPercentage.full());
	}

	@Nested
	@DisplayName("cost per recipe unit")
	class CostPerRecipeUnit {

		@Test
		void convertsOutOfThePurchaseUnitAndPaysForTheWaste() {
			// 4.50 / 5 kg = 0.90 per kg; / 1000 = 0.0009 per g; / 0.80 = 0.001125
			assertThat(onions().costPerRecipeUnit())
					.isEqualTo(UnitPrice.of(Money.euros("0.001125"), GRAM));
		}

		@Test
		void withoutTrimLossItIsJustTheConversion() {
			Item carrots = Item.create(
					"Carrots",
					ItemUnits.of(KILOGRAM, GRAM),
					AgreedPrice.perPack(Money.euros("4.50"), Quantity.of("5", KILOGRAM)),
					YieldPercentage.full());

			assertThat(carrots.costPerRecipeUnit()).isEqualTo(UnitPrice.of(Money.euros("0.0009"), GRAM));
		}

		@Test
		void worksThroughTheCountBridge() {
			// EUR 0.80 a tin, 400 g in a tin, so EUR 0.002 a gram.
			assertThat(tinnedTomatoes().costPerRecipeUnit())
					.isEqualTo(UnitPrice.of(Money.euros("0.002"), GRAM));
		}

		@Test
		void costsOutAQuantityForARecipe() {
			assertThat(onions().costOf(Quantity.of("200", GRAM))).isEqualTo(Money.euros("0.225"));
		}

		@Test
		void aQuantityInAnotherUnitOfTheSameKindIsConvertedFirst() {
			assertThat(onions().costOf(Quantity.of("0.2", KILOGRAM))).isEqualTo(Money.euros("0.225"));
		}

		@Test
		void yieldMovesTheCost() {
			Item item = onions();
			UnitPrice before = item.costPerRecipeUnit();

			item.changeYield(YieldPercentage.full());

			assertThat(item.costPerRecipeUnit()).isLessThan(before);
		}
	}

	@Nested
	@DisplayName("tax is recorded, not applied")
	class Vat {

		@Test
		void recordingARateLeavesTheCostAlone() {
			Item item = onions();
			UnitPrice before = item.costPerRecipeUnit();

			item.recordVatRate(Percentage.ofPercent(6));

			assertThat(item.vatRate()).contains(Percentage.ofPercent(6));
			assertThat(item.costPerRecipeUnit()).isEqualTo(before);
		}

		@Test
		void isAbsentUntilGiven() {
			assertThat(onions().vatRate()).isEmpty();
		}
	}

	@Nested
	@DisplayName("the agreed price and what was actually charged")
	class TwoPrices {

		@Test
		void nothingHasBeenBoughtYet() {
			Item item = onions();

			assertThat(item.latestPurchasePrice()).isEmpty();
			assertThat(item.priceVariancePerPurchaseUnit()).isEmpty();
			assertThat(item.isOverAgreedPrice()).isFalse();
		}

		@Test
		void anInvoiceAboveTheAgreedPriceShowsUp() {
			Item item = onions();

			item.recordPurchasePrice(UnitPrice.of(Money.euros("0.99"), KILOGRAM));

			assertThat(item.priceVariancePerPurchaseUnit()).contains(Money.euros("0.09"));
			assertThat(item.isOverAgreedPrice()).isTrue();
		}

		@Test
		void anInvoiceBelowItShowsUpAsNegative() {
			Item item = onions();

			item.recordPurchasePrice(UnitPrice.of(Money.euros("0.85"), KILOGRAM));

			assertThat(item.priceVariancePerPurchaseUnit()).contains(Money.euros("-0.05"));
			assertThat(item.isOverAgreedPrice()).isFalse();
		}

		@Test
		void anInvoicePricedPerGramIsRestatedPerKilo() {
			Item item = onions();

			item.recordPurchasePrice(UnitPrice.of(Money.euros("0.00099"), GRAM));

			assertThat(item.latestPurchasePrice()).contains(UnitPrice.of(Money.euros("0.99"), KILOGRAM));
			assertThat(item.priceVariancePerPurchaseUnit()).contains(Money.euros("0.09"));
		}

		@Test
		void recordingAPurchaseDoesNotChangeWhatARecipeCosts() {
			Item item = onions();
			UnitPrice before = item.costPerRecipeUnit();

			item.recordPurchasePrice(UnitPrice.of(Money.euros("9.99"), KILOGRAM));

			assertThat(item.costPerRecipeUnit()).isEqualTo(before);
		}
	}

	@Nested
	@DisplayName("invariants")
	class Invariants {

		@Test
		void thePriceMustBePerTheUnitTheItemIsBoughtIn() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> Item.create(
							"Confused onions",
							ItemUnits.of(KILOGRAM, GRAM),
							AgreedPrice.perUnit(Money.euros("0.90"), LITRE),
							YieldPercentage.full()))
					.withMessageContaining("bought in kg");
		}

		@Test
		void anItemMustHaveAName() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> Item.create(
							"   ",
							ItemUnits.of(KILOGRAM, GRAM),
							AgreedPrice.perUnit(Money.euros("0.90"), KILOGRAM),
							YieldPercentage.full()))
					.withMessageContaining("must have a name");
		}

		@Test
		void namesAreTrimmed() {
			Item item = Item.create(
					"  Onions  ",
					ItemUnits.of(KILOGRAM, GRAM),
					AgreedPrice.perUnit(Money.euros("0.90"), KILOGRAM),
					YieldPercentage.full());

			assertThat(item.name()).isEqualTo("Onions");
		}

		@Test
		void changingTheUnitsRequiresRestatingThePrice() {
			Item item = onions();

			assertThatIllegalArgumentException()
					.isThrownBy(() -> item.changeUnits(
							ItemUnits.of(LITRE, MILLILITRE),
							AgreedPrice.perUnit(Money.euros("0.90"), KILOGRAM)))
					.withMessageContaining("bought in l");
		}

		@Test
		void changingBothTogetherIsFine() {
			Item item = onions();

			item.changeUnits(
					ItemUnits.of(LITRE, MILLILITRE),
					AgreedPrice.perUnit(Money.euros("2.00"), LITRE));

			// 2.00 per litre / 1000 = 0.002 per ml, and the yield is untouched at
			// 80%, so 0.002 / 0.8 = 0.0025.
			assertThat(item.costPerRecipeUnit()).isEqualTo(UnitPrice.of(Money.euros("0.0025"), MILLILITRE));
		}

		@Test
		void repricingMustStayInThePurchaseUnit() {
			Item item = onions();

			assertThatIllegalArgumentException()
					.isThrownBy(() -> item.changeAgreedPrice(AgreedPrice.perUnit(Money.euros("1.00"), GRAM)));
		}
	}

	@Nested
	@DisplayName("identity")
	class Identity {

		@Test
		void twoItemsThatLookAlikeAreStillTwoItems() {
			assertThat(onions()).isNotEqualTo(onions());
		}

		@Test
		void anItemStaysItselfThroughAnyChange() {
			Item item = onions();
			ItemId id = item.id();

			item.rename("Brown onions");
			item.changeAgreedPrice(AgreedPrice.perUnit(Money.euros("1.20"), KILOGRAM));

			assertThat(item.id()).isEqualTo(id);
			assertThat(item.name()).isEqualTo("Brown onions");
		}

		@Test
		void restoringTheSameIdGivesTheSameItem() {
			Item item = onions();

			Item reloaded = Item.restore(
					item.id(),
					"Onions",
					null,
					item.units(),
					item.agreedPrice(),
					item.yieldPercentage(),
					null,
					null);

			assertThat(reloaded).isEqualTo(item).hasSameHashCodeAs(item);
		}
	}
}
