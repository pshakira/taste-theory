package com.tastetheory.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NutritionTest {

	private static final Nutrition ONIONS_PER_100G = Nutrition.of("40", "1.1", "9.3", "0.1", "1.7", "0.004");

	@Nested
	@DisplayName("arithmetic")
	class Arithmetic {

		@Test
		void addsFieldByField() {
			Nutrition total = Nutrition.of("40", "1", "9", "0.1", "1.7", "0.004")
					.plus(Nutrition.of("60", "2", "1", "0.9", "0.3", "0.006"));

			assertThat(total).isEqualTo(Nutrition.of("100", "3", "10", "1.0", "2.0", "0.010"));
		}

		@Test
		void scalesFieldByField() {
			assertThat(ONIONS_PER_100G.scaledBy(new BigDecimal("2")))
					.isEqualTo(Nutrition.of("80", "2.2", "18.6", "0.2", "3.4", "0.008"));
		}

		@Test
		void dividesForAPerServingFigure() {
			assertThat(Nutrition.of("100", "4", "20", "2", "1", "0.5").dividedBy(new BigDecimal("4")))
					.isEqualTo(Nutrition.of("25", "1", "5", "0.5", "0.25", "0.125"));
		}

		@Test
		void addingNothingChangesNothing() {
			assertThat(ONIONS_PER_100G.plus(Nutrition.zero())).isEqualTo(ONIONS_PER_100G);
		}

		@Test
		void scalingByNothingLeavesNothing() {
			assertThat(ONIONS_PER_100G.scaledBy(BigDecimal.ZERO).isZero()).isTrue();
		}
	}

	@Nested
	@DisplayName("guards")
	class Guards {

		@Test
		void refusesNegativeFigures() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> Nutrition.of("40", "-1", "9", "0.1", "1.7", "0.004"))
					.withMessageContaining("protein");
		}

		@Test
		void refusesToScaleByANegativeFactor() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> ONIONS_PER_100G.scaledBy(new BigDecimal("-1")))
					.withMessageContaining("negative");
		}

		@Test
		void refusesToDivideByZero() {
			assertThatExceptionOfType(ArithmeticException.class)
					.isThrownBy(() -> ONIONS_PER_100G.dividedBy(BigDecimal.ZERO));
		}
	}

	@Nested
	@DisplayName("equality")
	class Equality {

		@Test
		void ignoresHowManyZerosWereTyped() {
			assertThat(Nutrition.of("40", "1.1", "9.3", "0.1", "1.7", "0.004"))
					.isEqualTo(Nutrition.of("40.000", "1.10", "9.30", "0.100", "1.700", "0.0040"));
		}

		@Test
		void nothingDeclaredIsZero() {
			assertThat(Nutrition.zero().isZero()).isTrue();
			assertThat(ONIONS_PER_100G.isZero()).isFalse();
		}
	}
}
