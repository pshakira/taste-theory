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
		void a_kilogram_is_a_thousand_grams() {
			assertThat(UnitOfMeasure.KILOGRAM.conversionFactorTo(UnitOfMeasure.GRAM))
					.isEqualByComparingTo("1000");
		}

		@Test
		void and_a_gram_is_a_thousandth_of_a_kilogram() {
			assertThat(UnitOfMeasure.GRAM.conversionFactorTo(UnitOfMeasure.KILOGRAM))
					.isEqualByComparingTo("0.001");
		}

		@Test
		void a_litre_is_a_thousand_millilitres() {
			assertThat(UnitOfMeasure.LITRE.conversionFactorTo(UnitOfMeasure.MILLILITRE))
					.isEqualByComparingTo("1000");
		}

		@Test
		void a_dozen_is_twelve() {
			assertThat(UnitOfMeasure.DOZEN.conversionFactorTo(UnitOfMeasure.EACH))
					.isEqualByComparingTo("12");
		}

		@Test
		void a_unit_converts_to_itself_exactly() {
			assertThat(UnitOfMeasure.GRAM.conversionFactorTo(UnitOfMeasure.GRAM))
					.isEqualByComparingTo("1");
		}

		@Test
		void a_factor_that_does_not_terminate_still_produces_an_answer() {
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
		void weight_does_not_become_volume() {
			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> UnitOfMeasure.KILOGRAM.conversionFactorTo(UnitOfMeasure.MILLILITRE))
					.withMessageContaining("kg")
					.withMessageContaining("ml");
		}

		@Test
		void volume_does_not_become_weight() {
			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> UnitOfMeasure.LITRE.conversionFactorTo(UnitOfMeasure.GRAM));
		}

		@Test
		void a_count_does_not_become_a_weight_on_its_own() {
			// Allowed eventually, but only with a figure the user supplies: "each
			// tin holds 400 g". That belongs on the item, not on the unit.
			assertThatExceptionOfType(IncompatibleUnitsException.class)
					.isThrownBy(() -> UnitOfMeasure.EACH.conversionFactorTo(UnitOfMeasure.GRAM));
		}

		@Test
		void the_exception_carries_both_units() {
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
		void reads_the_short_form() {
			assertThat(UnitOfMeasure.fromCode("kg")).isEqualTo(UnitOfMeasure.KILOGRAM);
		}

		@Test
		void ignores_case_and_surrounding_space() {
			assertThat(UnitOfMeasure.fromCode("  ML ")).isEqualTo(UnitOfMeasure.MILLILITRE);
		}

		@Test
		void names_the_alternatives_when_it_does_not_recognise_one() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> UnitOfMeasure.fromCode("cups"))
					.withMessageContaining("cups")
					.withMessageContaining("kg");
		}

		@ParameterizedTest
		@EnumSource(UnitOfMeasure.class)
		void every_unit_can_be_parsed_back_from_its_own_code(UnitOfMeasure unit) {
			assertThat(UnitOfMeasure.fromCode(unit.code())).isEqualTo(unit);
		}
	}

	@Nested
	@DisplayName("the set of units itself")
	class TheSet {

		@Test
		void every_code_is_unique() {
			long distinctCodes = Arrays.stream(UnitOfMeasure.values())
					.map(UnitOfMeasure::code)
					.distinct()
					.count();

			assertThat(distinctCodes).isEqualTo(UnitOfMeasure.values().length);
		}

		@Test
		void every_dimension_has_exactly_one_base_unit() {
			var baseUnitsPerDimension = Arrays.stream(UnitOfMeasure.values())
					.filter(UnitOfMeasure::isBaseUnit)
					.collect(Collectors.groupingBy(UnitOfMeasure::dimension, Collectors.counting()));

			assertThat(baseUnitsPerDimension)
					.containsOnlyKeys(Dimension.values())
					.allSatisfy((dimension, count) -> assertThat(count).isEqualTo(1L));
		}

		@ParameterizedTest
		@EnumSource(UnitOfMeasure.class)
		void no_unit_is_smaller_than_its_base(UnitOfMeasure unit) {
			assertThat(unit.factorToBaseUnit()).isGreaterThanOrEqualTo(BigDecimal.ONE);
		}
	}
}
