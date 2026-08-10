package com.tastetheory.shared.domain;

import static com.tastetheory.shared.domain.UnitOfMeasure.EACH;
import static com.tastetheory.shared.domain.UnitOfMeasure.GRAM;
import static com.tastetheory.shared.domain.UnitOfMeasure.KILOGRAM;
import static com.tastetheory.shared.domain.UnitOfMeasure.LITRE;
import static com.tastetheory.shared.domain.UnitOfMeasure.MILLILITRE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UnitPriceTest {

	@Nested
	@DisplayName("deriving a rate from what was paid")
	class Deriving {

		@Test
		void aPackPriceDividedByItsPackSize() {
			// A 5 kg sack costing EUR 4.50.
			UnitPrice perKilo = UnitPrice.from(Money.euros("4.50"), Quantity.of("5", KILOGRAM));

			assertThat(perKilo).isEqualTo(UnitPrice.of(Money.euros("0.90"), KILOGRAM));
		}

		@Test
		void aRateFarBelowACentSurvives() {
			UnitPrice perGram = UnitPrice.from(Money.euros("4.50"), Quantity.of("5000", GRAM));

			assertThat(perGram).isEqualTo(UnitPrice.of(Money.euros("0.0009"), GRAM));
			assertThat(perGram.isZero()).isFalse();
		}

		@Test
		void refusesToDivideByNothing() {
			assertThatExceptionOfType(ArithmeticException.class)
					.isThrownBy(() -> UnitPrice.from(Money.euros("4.50"), Quantity.zero(KILOGRAM)));
		}
	}

	@Nested
	@DisplayName("why line totals are recorded rather than recomputed")
	class WhyTotalsAreRecorded {

		@Test
		void aRateDoesNotMultiplyBackToTheTotalItCameFrom() {
			Money entered = Money.euros("8.90");
			Quantity delivered = Quantity.of("12", EACH);

			UnitPrice rate = UnitPrice.from(entered, delivered);
			Money recovered = rate.times(delivered);

			// 8.90 / 12 is 0.741666..., and twelve of those is not 8.90 again.
			assertThat(recovered).isNotEqualTo(entered);
			assertThat(recovered.roundedToMinorUnit()).isEqualTo(entered.roundedToMinorUnit());
		}
	}

	@Nested
	@DisplayName("costing a quantity")
	class CostingAQuantity {

		@Test
		void multipliesOutWithinAUnit() {
			UnitPrice perKilo = UnitPrice.of(Money.euros("4.50"), KILOGRAM);

			assertThat(perKilo.times(Quantity.of("2", KILOGRAM))).isEqualTo(Money.euros("9.00"));
		}

		@Test
		void convertsTheQuantityFirst() {
			UnitPrice perKilo = UnitPrice.of(Money.euros("4.50"), KILOGRAM);

			// 500 g of something costing EUR 4.50 per kg.
			assertThat(perKilo.times(Quantity.of("500", GRAM))).isEqualTo(Money.euros("2.25"));
		}

		@Test
		void costsNothingForNothing() {
			UnitPrice perKilo = UnitPrice.of(Money.euros("4.50"), KILOGRAM);

			assertThat(perKilo.times(Quantity.zero(KILOGRAM))).isEqualTo(Money.euros("0"));
		}

		@Test
		void refusesAQuantityMeasuringSomethingElse() {
			UnitPrice perKilo = UnitPrice.of(Money.euros("4.50"), KILOGRAM);

			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> perKilo.times(Quantity.of("500", MILLILITRE)));
		}
	}

	@Nested
	@DisplayName("restating the rate per a different unit")
	class Restating {

		@Test
		void perKilogramBecomesPerGram() {
			UnitPrice perKilo = UnitPrice.of(Money.euros("4.50"), KILOGRAM);

			assertThat(perKilo.convertedTo(GRAM)).isEqualTo(UnitPrice.of(Money.euros("0.0045"), GRAM));
		}

		@Test
		void andBackAgain() {
			UnitPrice perGram = UnitPrice.of(Money.euros("0.0045"), GRAM);

			assertThat(perGram.convertedTo(KILOGRAM)).isEqualTo(UnitPrice.of(Money.euros("4.50"), KILOGRAM));
		}

		@Test
		void restatingInTheSameUnitChangesNothing() {
			UnitPrice perKilo = UnitPrice.of(Money.euros("4.50"), KILOGRAM);

			assertThat(perKilo.convertedTo(KILOGRAM)).isSameAs(perKilo);
		}

		@Test
		void refusesToCrossDimensions() {
			UnitPrice perKilo = UnitPrice.of(Money.euros("4.50"), KILOGRAM);

			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> perKilo.convertedTo(LITRE));
		}
	}

	@Nested
	@DisplayName("comparing")
	class Comparing {

		@Test
		void comparesAcrossUnitsOfTheSameDimension() {
			UnitPrice perKilo = UnitPrice.of(Money.euros("4.50"), KILOGRAM);
			UnitPrice sameRatePerGram = UnitPrice.of(Money.euros("0.0045"), GRAM);

			assertThat(perKilo).isEqualByComparingTo(sameRatePerGram);
		}

		@Test
		void noticesWhenAPriceHasRisen() {
			UnitPrice was = UnitPrice.of(Money.euros("4.50"), KILOGRAM);
			UnitPrice now = UnitPrice.of(Money.euros("4.95"), KILOGRAM);

			assertThat(now).isGreaterThan(was);
		}
	}

	@Nested
	@DisplayName("guards")
	class Guards {

		@Test
		void refusesANegativeRate() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> UnitPrice.of(Money.euros("-1.00"), KILOGRAM))
					.withMessageContaining("negative");
		}
	}

	@Test
	void readsSensiblyWhenPrinted() {
		assertThat(UnitPrice.of(Money.euros("4.50"), KILOGRAM)).hasToString("EUR 4.500000/kg");
	}
}
