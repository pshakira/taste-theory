package com.tastetheory.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class UnitOfMeasureTest {

	@Nested
	@DisplayName("converting within a dimension")
	class WithinADimension {

		@Test
		void aKilogramIsAThousandGrams() {
			assertThat(UnitOfMeasure.KILOGRAM.conversionFactorTo(UnitOfMeasure.GRAM))
					.isEqualByComparingTo("1000");
		}

		@Test
		void andAGramIsAThousandthOfAKilogram() {
			assertThat(UnitOfMeasure.GRAM.conversionFactorTo(UnitOfMeasure.KILOGRAM))
					.isEqualByComparingTo("0.001");
		}

		@Test
		void aLitreIsAThousandMillilitres() {
			assertThat(UnitOfMeasure.LITRE.conversionFactorTo(UnitOfMeasure.MILLILITRE))
					.isEqualByComparingTo("1000");
		}

		@Test
		void aDozenIsTwelve() {
			assertThat(UnitOfMeasure.DOZEN.conversionFactorTo(UnitOfMeasure.EACH))
					.isEqualByComparingTo("12");
		}

		@Test
		void aUnitConvertsToItselfExactly() {
			assertThat(UnitOfMeasure.GRAM.conversionFactorTo(UnitOfMeasure.GRAM))
					.isEqualByComparingTo("1");
		}

		@Test
		void aFactorThatDoesNotTerminateStillProducesAnAnswer() {
			// 1/12 recurs, so an exact BigDecimal division would throw. It has to
			// come back rounded, and close enough to round-trip.
			BigDecimal eachToDozen = UnitOfMeasure.EACH.conversionFactorTo(UnitOfMeasure.DOZEN);

			assertThat(eachToDozen.multiply(new BigDecimal("12")))
					.isCloseTo(BigDecimal.ONE, within(new BigDecimal("0.0000000001")));
		}
	}

	@Nested
	@DisplayName("refusing to convert across dimensions")
	class AcrossDimensions {

		@Test
		void weightDoesNotBecomeVolume() {
			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> UnitOfMeasure.KILOGRAM.conversionFactorTo(UnitOfMeasure.MILLILITRE))
					.withMessageContaining("kg")
					.withMessageContaining("ml");
		}

		@Test
		void volumeDoesNotBecomeWeight() {
			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> UnitOfMeasure.LITRE.conversionFactorTo(UnitOfMeasure.GRAM));
		}

		@Test
		void aCountDoesNotBecomeAWeightOnItsOwn() {
			// Allowed eventually, but only with a figure the user supplies: "each
			// tin holds 400 g". That belongs on the item, not on the unit.
			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> UnitOfMeasure.EACH.conversionFactorTo(UnitOfMeasure.GRAM));
		}

		@Test
		void theExceptionCarriesBothUnits() {
			IncompatibleUnitsException thrown = null;
			try {
				UnitOfMeasure.KILOGRAM.conversionFactorTo(UnitOfMeasure.LITRE);
			}
			catch (IncompatibleUnitsException e) {
				thrown = e;
			}

			assertThat(thrown).isNotNull();
			assertThat(thrown.from()).isEqualTo(UnitOfMeasure.KILOGRAM);
			assertThat(thrown.to()).isEqualTo(UnitOfMeasure.LITRE);
		}
	}

	@Nested
	@DisplayName("parsing a code")
	class Parsing {

		@Test
		void readsTheShortForm() {
			assertThat(UnitOfMeasure.fromCode("kg")).isEqualTo(UnitOfMeasure.KILOGRAM);
		}

		@Test
		void ignoresCaseAndSurroundingSpace() {
			assertThat(UnitOfMeasure.fromCode("  ML ")).isEqualTo(UnitOfMeasure.MILLILITRE);
		}

		@Test
		void namesTheAlternativesWhenItDoesNotRecogniseOne() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> UnitOfMeasure.fromCode("cups"))
					.withMessageContaining("cups")
					.withMessageContaining("kg");
		}

		@ParameterizedTest
		@EnumSource(UnitOfMeasure.class)
		void everyUnitCanBeParsedBackFromItsOwnCode(UnitOfMeasure unit) {
			assertThat(UnitOfMeasure.fromCode(unit.code())).isEqualTo(unit);
		}
	}

	@Nested
	@DisplayName("the set of units itself")
	class TheSet {

		@Test
		void everyCodeIsUnique() {
			long distinctCodes = Arrays.stream(UnitOfMeasure.values())
					.map(UnitOfMeasure::code)
					.distinct()
					.count();

			assertThat(distinctCodes).isEqualTo(UnitOfMeasure.values().length);
		}

		@Test
		void everyDimensionHasExactlyOneBaseUnit() {
			var baseUnitsPerDimension = Arrays.stream(UnitOfMeasure.values())
					.filter(UnitOfMeasure::isBaseUnit)
					.collect(Collectors.groupingBy(UnitOfMeasure::dimension, Collectors.counting()));

			assertThat(baseUnitsPerDimension)
					.containsOnlyKeys(Dimension.values())
					.allSatisfy((dimension, count) -> assertThat(count).isEqualTo(1L));
		}

		@ParameterizedTest
		@EnumSource(UnitOfMeasure.class)
		void noUnitIsSmallerThanItsBase(UnitOfMeasure unit) {
			assertThat(unit.factorToBaseUnit()).isGreaterThanOrEqualTo(BigDecimal.ONE);
		}
	}
}
