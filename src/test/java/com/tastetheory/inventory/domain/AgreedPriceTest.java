package com.tastetheory.inventory.domain;

import static com.tastetheory.shared.domain.UnitOfMeasure.EACH;
import static com.tastetheory.shared.domain.UnitOfMeasure.KILOGRAM;
import static com.tastetheory.shared.domain.UnitOfMeasure.MILLILITRE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.tastetheory.shared.domain.Money;
import com.tastetheory.shared.domain.Quantity;
import com.tastetheory.shared.domain.UnitPrice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AgreedPriceTest {

	@Nested
	@DisplayName("entered as a pack")
	class AsAPack {

		@Test
		void aSackCostingFourFiftyAndHoldingFiveKilos() {
			AgreedPrice onions = AgreedPrice.perPack(Money.euros("4.50"), Quantity.of("5", KILOGRAM));

			assertThat(onions.perPurchaseUnit()).isEqualTo(UnitPrice.of(Money.euros("0.90"), KILOGRAM));
			assertThat(onions.purchaseUnit()).isEqualTo(KILOGRAM);
		}

		@Test
		void aCaseOfTwelveTins() {
			AgreedPrice tomatoes = AgreedPrice.perPack(Money.euros("8.90"), Quantity.of("12", EACH));

			assertThat(tomatoes.perPurchaseUnit())
					.isEqualTo(UnitPrice.of(Money.euros("0.741667"), EACH));
		}

		@Test
		void theRateIsDerivedEachTimeRatherThanStored() {
			AgreedPrice oil = AgreedPrice.perPack(Money.euros("12.00"), Quantity.of("750", MILLILITRE));

			// Nothing to drift: the pack figures are the only stored truth.
			assertThat(oil.perPurchaseUnit()).isEqualTo(oil.perPurchaseUnit());
			assertThat(oil).isEqualTo(AgreedPrice.perPack(Money.euros("12.00"), Quantity.of("750", MILLILITRE)));
		}

		@Test
		void refusesAPackHoldingNothing() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> AgreedPrice.perPack(Money.euros("4.50"), Quantity.zero(KILOGRAM)))
					.withMessageContaining("more than nothing");
		}

		@Test
		void refusesANegativePackPrice() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> AgreedPrice.perPack(Money.euros("-4.50"), Quantity.of("5", KILOGRAM)))
					.withMessageContaining("negative");
		}

		@Test
		void allowsAFreePack() {
			// A sample or a promotional line. Zero is a price; negative is not.
			AgreedPrice sample = AgreedPrice.perPack(Money.euros("0"), Quantity.of("5", KILOGRAM));

			assertThat(sample.perPurchaseUnit().isZero()).isTrue();
		}
	}

	@Nested
	@DisplayName("entered as a rate")
	class AsARate {

		@Test
		void isUsedAsGiven() {
			AgreedPrice flour = AgreedPrice.perUnit(Money.euros("0.90"), KILOGRAM);

			assertThat(flour.perPurchaseUnit()).isEqualTo(UnitPrice.of(Money.euros("0.90"), KILOGRAM));
			assertThat(flour.purchaseUnit()).isEqualTo(KILOGRAM);
		}

		@Test
		void refusesANegativeRate() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> AgreedPrice.perUnit(Money.euros("-0.90"), KILOGRAM))
					.withMessageContaining("negative");
		}
	}

	@Nested
	@DisplayName("the two paths are distinguishable without inspecting numbers")
	class TellingThemApart {

		@Test
		void aPackAndARateThatWorkOutTheSameAreStillDifferentThings() {
			AgreedPrice asPack = AgreedPrice.perPack(Money.euros("4.50"), Quantity.of("5", KILOGRAM));
			AgreedPrice asRate = AgreedPrice.perUnit(Money.euros("0.90"), KILOGRAM);

			// Same resulting rate...
			assertThat(asPack.perPurchaseUnit()).isEqualTo(asRate.perPurchaseUnit());
			// ...but not the same information, and never confused for one another.
			assertThat(asPack).isNotEqualTo(asRate);
			assertThat(describe(asPack)).isEqualTo("pack");
			assertThat(describe(asRate)).isEqualTo("rate");
		}

		/**
		 * Compiles with no default branch only because the interface is sealed. Add
		 * a third way of entering a price and this stops compiling until it is
		 * handled -- which is the point.
		 */
		private String describe(AgreedPrice price) {
			return switch (price) {
				case AgreedPrice.PerPack ignored -> "pack";
				case AgreedPrice.PerUnit ignored -> "rate";
			};
		}
	}
}
