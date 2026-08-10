package com.tastetheory.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PercentageTest {

	@Nested
	@DisplayName("saying which convention you meant")
	class Conventions {

		@Test
		void eightyPercentIsFourFifths() {
			assertThat(Percentage.ofPercent(80)).isEqualTo(Percentage.ofFraction("0.8"));
		}

		@Test
		void theIvaCaseThatUsedToBeGuessed() {
			// The old system took either 6 or 0.06 and inferred which was meant.
			// Here the caller says so, and a genuine 0.06% stays 0.06%.
			assertThat(Percentage.ofPercent(6)).isEqualTo(Percentage.ofFraction("0.06"));
			assertThat(Percentage.ofPercent("0.06")).isNotEqualTo(Percentage.ofFraction("0.06"));
		}

		@Test
		void readsBackAsAPercentage() {
			assertThat(Percentage.ofFraction("0.06").asPercent()).isEqualByComparingTo("6");
		}

		@Test
		void readsBackAsAFraction() {
			assertThat(Percentage.ofPercent(80).asFraction()).isEqualByComparingTo("0.8");
		}

		@Test
		void survivesARoundTrip() {
			Percentage margin = Percentage.ofPercent("62.5");

			assertThat(Percentage.ofFraction(margin.asFraction())).isEqualTo(margin);
		}
	}

	@Nested
	@DisplayName("equality")
	class Equality {

		@Test
		void ignoresHowManyZerosWereTyped() {
			assertThat(Percentage.ofFraction("0.8")).isEqualTo(Percentage.ofFraction("0.800"));
		}

		@Test
		void equalValuesShareAHashCode() {
			assertThat(Percentage.ofPercent(80)).hasSameHashCodeAs(Percentage.ofFraction("0.8"));
		}
	}

	@Nested
	@DisplayName("applying it")
	class Applying {

		@Test
		void takesAShareOfAnAmount() {
			// IVA at 6% on a line of EUR 8.90.
			assertThat(Percentage.ofPercent(6).applyTo(Money.euros("8.90")))
					.isEqualTo(Money.euros("0.534"));
		}

		@Test
		void noneOfSomethingIsNothing() {
			assertThat(Percentage.ZERO.applyTo(Money.euros("8.90"))).isEqualTo(Money.euros("0"));
		}

		@Test
		void allOfSomethingIsItself() {
			assertThat(Percentage.ONE_HUNDRED_PERCENT.applyTo(Money.euros("8.90")))
					.isEqualTo(Money.euros("8.90"));
		}

		@Test
		void theComplementIsWhatIsLeft() {
			// A 10% discount leaves 90% to pay.
			assertThat(Percentage.ofPercent(10).complement()).isEqualTo(Percentage.ofPercent(90));
		}

		@Test
		void discountingAnAmount() {
			Percentage discount = Percentage.ofPercent(10);

			assertThat(discount.complement().applyTo(Money.euros("8.90"))).isEqualTo(Money.euros("8.01"));
		}
	}

	@Nested
	@DisplayName("bounds belong elsewhere")
	class Bounds {

		@Test
		void aMarginCanBeNegative() {
			// Selling below cost. Nonsense for a yield, ordinary for a margin, so
			// the rule belongs to whichever type models the thing.
			Percentage loss = Percentage.ofPercent("-12.5");

			assertThat(loss.isNegative()).isTrue();
			assertThat(loss.asPercent()).isEqualByComparingTo("-12.5");
		}

		@Test
		void aProportionCanExceedOne() {
			assertThat(Percentage.ofPercent(150).asFraction()).isEqualByComparingTo("1.5");
		}
	}

	@Nested
	@DisplayName("comparing")
	class Comparing {

		@Test
		void ordersByMagnitude() {
			assertThat(Percentage.ofPercent(23)).isGreaterThan(Percentage.ofPercent(6));
		}

		@Test
		void equalValuesCompareEqualHoweverTheyWereBuilt() {
			assertThat(Percentage.ofPercent(80)).isEqualByComparingTo(Percentage.ofFraction("0.8"));
		}
	}

	@Nested
	@DisplayName("guards")
	class Guards {

		@Test
		void rejectsANullFraction() {
			assertThatNullPointerException().isThrownBy(() -> Percentage.ofFraction((BigDecimal) null));
		}

		@Test
		void rejectsANullPercent() {
			assertThatNullPointerException().isThrownBy(() -> Percentage.ofPercent((BigDecimal) null));
		}
	}

	@Nested
	@DisplayName("printing")
	class Printing {

		@Test
		void dropsTrailingZeros() {
			assertThat(Percentage.ofPercent(6)).hasToString("6%");
		}

		@Test
		void keepsDecimalsThatMatter() {
			assertThat(Percentage.ofPercent("12.5")).hasToString("12.5%");
		}
	}
}
