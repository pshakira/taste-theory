package com.tastetheory.shared.domain;

import static com.tastetheory.shared.domain.UnitOfMeasure.EACH;
import static com.tastetheory.shared.domain.UnitOfMeasure.GRAM;
import static com.tastetheory.shared.domain.UnitOfMeasure.KILOGRAM;
import static com.tastetheory.shared.domain.UnitOfMeasure.LITRE;
import static com.tastetheory.shared.domain.UnitOfMeasure.MILLILITRE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QuantityTest {

	@Nested
	@DisplayName("construction")
	class Construction {

		@Test
		void refusesANegativeAmount() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> Quantity.of("-1", GRAM))
					.withMessageContaining("negative");
		}

		@Test
		void allowsZero() {
			assertThat(Quantity.zero(GRAM).isZero()).isTrue();
		}

		@Test
		void rejectsANullUnit() {
			assertThatNullPointerException().isThrownBy(() -> Quantity.of("1", null));
		}
	}

	@Nested
	@DisplayName("equality")
	class Equality {

		@Test
		void ignoresHowManyZerosWereTyped() {
			assertThat(Quantity.of("1.5", KILOGRAM)).isEqualTo(Quantity.of("1.500", KILOGRAM));
		}

		@Test
		void theSameAmountInADifferentUnitIsADifferentValue() {
			// Equal in magnitude, but not the same value — the unit is part of it.
			assertThat(Quantity.of("1", KILOGRAM)).isNotEqualTo(Quantity.of("1000", GRAM));
		}

		@Test
		void butTheyCompareAsEqualInMagnitude() {
			assertThat(Quantity.of("1", KILOGRAM)).isEqualByComparingTo(Quantity.of("1000", GRAM));
		}
	}

	@Nested
	@DisplayName("converting")
	class Converting {

		@Test
		void kilogramsBecomeGrams() {
			assertThat(Quantity.of("5", KILOGRAM).convertedTo(GRAM)).isEqualTo(Quantity.of("5000", GRAM));
		}

		@Test
		void gramsBecomeKilograms() {
			assertThat(Quantity.of("1500", GRAM).convertedTo(KILOGRAM)).isEqualTo(Quantity.of("1.5", KILOGRAM));
		}

		@Test
		void convertingToTheSameUnitChangesNothing() {
			Quantity flour = Quantity.of("500", GRAM);

			assertThat(flour.convertedTo(GRAM)).isSameAs(flour);
		}

		@Test
		void weightDoesNotBecomeVolume() {
			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> Quantity.of("1", KILOGRAM).convertedTo(LITRE));
		}
	}

	@Nested
	@DisplayName("arithmetic")
	class Arithmetic {

		@Test
		void addsWithinAUnit() {
			assertThat(Quantity.of("200", GRAM).plus(Quantity.of("300", GRAM)))
					.isEqualTo(Quantity.of("500", GRAM));
		}

		@Test
		void addsAcrossUnitsAndKeepsTheUnitOnTheLeft() {
			assertThat(Quantity.of("1", KILOGRAM).plus(Quantity.of("500", GRAM)))
					.isEqualTo(Quantity.of("1.5", KILOGRAM));
		}

		@Test
		void subtracts() {
			assertThat(Quantity.of("1", KILOGRAM).minus(Quantity.of("250", GRAM)))
					.isEqualTo(Quantity.of("0.75", KILOGRAM));
		}

		@Test
		void refusesToSubtractPastZero() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> Quantity.of("100", GRAM).minus(Quantity.of("101", GRAM)))
					.withMessageContaining("negative");
		}

		@Test
		void refusesToAddAcrossDimensions() {
			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> Quantity.of("1", KILOGRAM).plus(Quantity.of("500", MILLILITRE)));
		}

		@Test
		void multiplies() {
			assertThat(Quantity.of("250", GRAM).times(new BigDecimal("4")))
					.isEqualTo(Quantity.of("1000", GRAM));
		}

		@Test
		void divides() {
			// A recipe yielding four servings, divided out.
			assertThat(Quantity.of("1", KILOGRAM).dividedBy(new BigDecimal("4")))
					.isEqualTo(Quantity.of("0.25", KILOGRAM));
		}

		@Test
		void refusesToDivideByZero() {
			assertThatExceptionOfType(ArithmeticException.class)
					.isThrownBy(() -> Quantity.of("1", KILOGRAM).dividedBy(BigDecimal.ZERO));
		}
	}

	@Nested
	@DisplayName("counted things")
	class CountedThings {

		@Test
		void aDozenIsTwelveOfThem() {
			assertThat(Quantity.of("1", UnitOfMeasure.DOZEN).convertedTo(EACH))
					.isEqualTo(Quantity.of("12", EACH));
		}

		@Test
		void aCountIsNotAWeight() {
			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> Quantity.of("2", EACH).convertedTo(GRAM));
		}
	}

	@Test
	void readsSensiblyWhenPrinted() {
		assertThat(Quantity.of("1500", GRAM)).hasToString("1500 g");
	}
}
