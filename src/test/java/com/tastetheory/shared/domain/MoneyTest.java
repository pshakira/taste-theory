package com.tastetheory.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MoneyTest {

	private static final Currency USD = Currency.getInstance("USD");

	@Nested
	@DisplayName("equality")
	class Equality {

		@Test
		void ignoresHowManyZerosWereTyped() {
			assertThat(Money.euros("0.10")).isEqualTo(Money.euros("0.1000"));
		}

		@Test
		void equalAmountsShareAHashCode() {
			assertThat(Money.euros("0.10")).hasSameHashCodeAs(Money.euros("0.1000"));
		}

		@Test
		void theSameNumberInTwoCurrenciesIsNotTheSameMoney() {
			assertThat(Money.euros("5.00")).isNotEqualTo(Money.of(new BigDecimal("5.00"), USD));
		}
	}

	@Nested
	@DisplayName("precision")
	class Precision {

		@Test
		void aPriceFarBelowACentSurvives() {
			// A EUR 4.50 sack holding 5000 g. At two decimal places this is zero,
			// and every recipe cost built on it would be zero too.
			Money perGram = Money.euros("4.50").dividedBy(new BigDecimal("5000"));

			assertThat(perGram).isEqualTo(Money.euros("0.0009"));
			assertThat(perGram.isZero()).isFalse();
		}

		@Test
		void smallAmountsStillAccumulate() {
			Money perGram = Money.euros("0.0009");

			assertThat(perGram.times(new BigDecimal("5"))).isEqualTo(Money.euros("0.0045"));
		}

		@Test
		void divisionThatDoesNotComeOutEvenlyIsRoundedHalfUp() {
			// EUR 8.90 across 12 units — the case that stops line totals being
			// recomputed from unit prices later on.
			assertThat(Money.euros("8.90").dividedBy(new BigDecimal("12")))
					.isEqualTo(Money.euros("0.741667"));
		}

		@Test
		void anythingBeyondSixPlacesIsRoundedAwayOnConstruction() {
			assertThat(Money.euros("0.0000005")).isEqualTo(Money.euros("0.000001"));
		}
	}

	@Nested
	@DisplayName("arithmetic")
	class Arithmetic {

		@Test
		void adds() {
			assertThat(Money.euros("4.50").plus(Money.euros("0.75"))).isEqualTo(Money.euros("5.25"));
		}

		@Test
		void subtractsPastZeroWhenAsked() {
			assertThat(Money.euros("1.00").minus(Money.euros("1.50"))).isEqualTo(Money.euros("-0.50"));
		}

		@Test
		void multiplies() {
			assertThat(Money.euros("2.50").times(new BigDecimal("3"))).isEqualTo(Money.euros("7.50"));
		}

		@Test
		void refusesToAddDifferentCurrencies() {
			Money euros = Money.euros("5.00");
			Money dollars = Money.of(new BigDecimal("5.00"), USD);

			assertThatIllegalArgumentException()
					.isThrownBy(() -> euros.plus(dollars))
					.withMessageContaining("EUR")
					.withMessageContaining("USD");
		}

		@Test
		void refusesToCompareDifferentCurrencies() {
			Money euros = Money.euros("5.00");
			Money dollars = Money.of(new BigDecimal("5.00"), USD);

			assertThatIllegalArgumentException().isThrownBy(() -> euros.compareTo(dollars));
		}

		@Test
		void refusesToDivideByZero() {
			assertThatExceptionOfType(ArithmeticException.class)
					.isThrownBy(() -> Money.euros("5.00").dividedBy(BigDecimal.ZERO));
		}
	}

	@Nested
	@DisplayName("rounding for payment")
	class RoundingForPayment {

		@Test
		void roundsToCents() {
			assertThat(Money.euros("0.741667").roundedToMinorUnit()).isEqualTo(Money.euros("0.74"));
		}

		@Test
		void roundsHalfUp() {
			assertThat(Money.euros("8.905").roundedToMinorUnit()).isEqualTo(Money.euros("8.91"));
		}

		@Test
		void leavesAnAmountThatIsAlreadyExactAlone() {
			assertThat(Money.euros("8.90").roundedToMinorUnit()).isEqualTo(Money.euros("8.90"));
		}
	}

	@Nested
	@DisplayName("guards")
	class Guards {

		@Test
		void rejectsANullAmount() {
			assertThatNullPointerException().isThrownBy(() -> Money.euros((BigDecimal) null));
		}

		@Test
		void rejectsANullCurrency() {
			assertThatNullPointerException().isThrownBy(() -> Money.of(BigDecimal.ONE, null));
		}
	}

	@Test
	void readsSensiblyWhenPrinted() {
		assertThat(Money.euros("4.50")).hasToString("EUR 4.500000");
	}
}
