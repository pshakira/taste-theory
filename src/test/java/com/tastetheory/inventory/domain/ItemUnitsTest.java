package com.tastetheory.inventory.domain;

import static com.tastetheory.shared.domain.UnitOfMeasure.DOZEN;
import static com.tastetheory.shared.domain.UnitOfMeasure.EACH;
import static com.tastetheory.shared.domain.UnitOfMeasure.GRAM;
import static com.tastetheory.shared.domain.UnitOfMeasure.KILOGRAM;
import static com.tastetheory.shared.domain.UnitOfMeasure.LITRE;
import static com.tastetheory.shared.domain.UnitOfMeasure.MILLILITRE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.tastetheory.shared.domain.IncompatibleUnitsException;
import com.tastetheory.shared.domain.Quantity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ItemUnitsTest {

	@Nested
	@DisplayName("units that measure the same thing")
	class SameDimension {

		@Test
		void aKiloBoughtAndGramsCookedNeedsNothingElse() {
			ItemUnits onions = ItemUnits.of(KILOGRAM, GRAM);

			assertThat(onions.recipeUnitsPerPurchaseUnit()).isEqualByComparingTo("1000");
			assertThat(onions.bridgesFromCount()).isFalse();
			assertThat(onions.contents()).isEmpty();
		}

		@Test
		void litresBoughtAndMillilitresCooked() {
			assertThat(ItemUnits.of(LITRE, MILLILITRE).recipeUnitsPerPurchaseUnit())
					.isEqualByComparingTo("1000");
		}

		@Test
		void boughtAndCookedInTheSameUnit() {
			assertThat(ItemUnits.same(EACH).recipeUnitsPerPurchaseUnit()).isEqualByComparingTo("1");
		}

		@Test
		void countableUnitsStillCountAsTheSameThing() {
			// Eggs bought by the dozen, used singly.
			assertThat(ItemUnits.of(DOZEN, EACH).recipeUnitsPerPurchaseUnit()).isEqualByComparingTo("12");
		}

		@Test
		void refusesASecondOpinionOnAConversionItAlreadyKnows() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> ItemUnits.withContents(KILOGRAM, GRAM, Quantity.of("900", GRAM)))
					.withMessageContaining("already known");
		}
	}

	@Nested
	@DisplayName("counted purchases measured out by weight")
	class TheTinCase {

		@Test
		void aTinHoldingFourHundredGrams() {
			ItemUnits tomatoes = ItemUnits.withContents(EACH, GRAM, Quantity.of("400", GRAM));

			assertThat(tomatoes.recipeUnitsPerPurchaseUnit()).isEqualByComparingTo("400");
			assertThat(tomatoes.bridgesFromCount()).isTrue();
			assertThat(tomatoes.contents()).contains(Quantity.of("400", GRAM));
		}

		@Test
		void aBottleHoldingSevenHundredAndFiftyMillilitres() {
			ItemUnits oil = ItemUnits.withContents(EACH, MILLILITRE, Quantity.of("750", MILLILITRE));

			assertThat(oil.recipeUnitsPerPurchaseUnit()).isEqualByComparingTo("750");
		}

		@Test
		void insistsOnBeingToldHowMuchIsInOne() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> ItemUnits.of(EACH, GRAM))
					.withMessageContaining("required");
		}

		@Test
		void insistsTheContentsAreStatedInTheRecipeUnit() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> ItemUnits.withContents(EACH, GRAM, Quantity.of("0.4", KILOGRAM)))
					.withMessageContaining("stated in g");
		}

		@Test
		void refusesAnEmptyTin() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> ItemUnits.withContents(EACH, GRAM, Quantity.zero(GRAM)))
					.withMessageContaining("more than nothing");
		}
	}

	@Nested
	@DisplayName("pairings with no honest answer")
	class Refused {

		@Test
		void weightDoesNotBridgeToVolume() {
			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> ItemUnits.of(KILOGRAM, LITRE));
		}

		@Test
		void volumeDoesNotBridgeToWeight() {
			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> ItemUnits.of(LITRE, GRAM));
		}

		@Test
		void weightBoughtAndCountedOutIsNotSupported() {
			// The reverse of the tin case, and far rarer. Refused rather than
			// guessed at.
			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> ItemUnits.of(KILOGRAM, EACH));
		}
	}

	@Nested
	@DisplayName("restating a purchase in recipe units")
	class Restating {

		@Test
		void fiveKilosBecomeFiveThousandGrams() {
			ItemUnits onions = ItemUnits.of(KILOGRAM, GRAM);

			assertThat(onions.toRecipeUnits(Quantity.of("5", KILOGRAM))).isEqualTo(Quantity.of("5000", GRAM));
		}

		@Test
		void threeTinsBecomeTwelveHundredGrams() {
			ItemUnits tomatoes = ItemUnits.withContents(EACH, GRAM, Quantity.of("400", GRAM));

			assertThat(tomatoes.toRecipeUnits(Quantity.of("3", EACH))).isEqualTo(Quantity.of("1200", GRAM));
		}

		@Test
		void convertsTheDeliveredQuantityIntoThePurchaseUnitFirst() {
			ItemUnits onions = ItemUnits.of(KILOGRAM, GRAM);

			// Delivered as 500 g of something bought by the kilo.
			assertThat(onions.toRecipeUnits(Quantity.of("500", GRAM))).isEqualTo(Quantity.of("500", GRAM));
		}

		@Test
		void aDozenTinsIsTwelveTinsWorth() {
			ItemUnits tomatoes = ItemUnits.withContents(EACH, GRAM, Quantity.of("400", GRAM));

			assertThat(tomatoes.toRecipeUnits(Quantity.of("1", DOZEN))).isEqualTo(Quantity.of("4800", GRAM));
		}

		@Test
		void refusesAQuantityMeasuringSomethingElse() {
			ItemUnits onions = ItemUnits.of(KILOGRAM, GRAM);

			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> onions.toRecipeUnits(Quantity.of("500", MILLILITRE)));
		}
	}
}
