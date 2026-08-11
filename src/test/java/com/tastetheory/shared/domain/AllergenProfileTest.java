package com.tastetheory.shared.domain;

import static com.tastetheory.shared.domain.Allergen.CEREALS_CONTAINING_GLUTEN;
import static com.tastetheory.shared.domain.Allergen.EGGS;
import static com.tastetheory.shared.domain.Allergen.MILK;
import static com.tastetheory.shared.domain.Allergen.SESAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AllergenProfileTest {

	@Nested
	@DisplayName("contains and may contain stay apart")
	class KeptApart {

		@Test
		void declaringNothing() {
			assertThat(AllergenProfile.none().declaresNothing()).isTrue();
		}

		@Test
		void aDefiniteDeclarationIsNotATrace() {
			AllergenProfile profile = AllergenProfile.containing(MILK);

			assertThat(profile.contains(MILK)).isTrue();
			assertThat(profile.mayContain(MILK)).isFalse();
		}

		@Test
		void aTraceIsNotADefiniteDeclaration() {
			AllergenProfile profile = AllergenProfile.of(List.of(), List.of(SESAME));

			assertThat(profile.contains(SESAME)).isFalse();
			assertThat(profile.mayContain(SESAME)).isTrue();
		}

		@Test
		void theDefiniteStatementWinsWhenBothAreClaimed() {
			// "Contains milk, and may contain milk" says nothing extra.
			AllergenProfile profile = AllergenProfile.of(List.of(MILK), List.of(MILK, SESAME));

			assertThat(profile.contains()).containsExactly(MILK);
			assertThat(profile.mayContain()).containsExactly(SESAME);
		}
	}

	@Nested
	@DisplayName("combining, which is what propagation will be built from")
	class Combining {

		@Test
		void takesEverythingFromBothSides() {
			AllergenProfile pastry = AllergenProfile.containing(CEREALS_CONTAINING_GLUTEN, MILK);
			AllergenProfile filling = AllergenProfile.of(List.of(EGGS), List.of(SESAME));

			AllergenProfile tart = pastry.combinedWith(filling);

			assertThat(tart.contains()).containsExactlyInAnyOrder(CEREALS_CONTAINING_GLUTEN, MILK, EGGS);
			assertThat(tart.mayContain()).containsExactly(SESAME);
		}

		@Test
		void aTraceOnOneSideIsOutrankedByADeclarationOnTheOther() {
			AllergenProfile mightHave = AllergenProfile.of(List.of(), List.of(MILK));
			AllergenProfile definitelyHas = AllergenProfile.containing(MILK);

			assertThat(mightHave.combinedWith(definitelyHas).contains()).containsExactly(MILK);
			assertThat(mightHave.combinedWith(definitelyHas).mayContain()).isEmpty();
		}

		@Test
		void combiningWithNothingChangesNothing() {
			AllergenProfile profile = AllergenProfile.containing(EGGS);

			assertThat(profile.combinedWith(AllergenProfile.none())).isEqualTo(profile);
		}

		@Test
		void isTheSameWhicheverWayRound() {
			AllergenProfile a = AllergenProfile.of(List.of(MILK), List.of(SESAME));
			AllergenProfile b = AllergenProfile.of(List.of(EGGS), List.of(MILK));

			assertThat(a.combinedWith(b)).isEqualTo(b.combinedWith(a));
		}
	}

	@Nested
	@DisplayName("the allergen list itself")
	class TheList {

		@Test
		void hasTheFourteenTheRegulationNames() {
			assertThat(Allergen.values()).hasSize(14);
		}

		@Test
		void parsesByName() {
			assertThat(Allergen.fromName("milk")).isEqualTo(MILK);
			assertThat(Allergen.fromName("  TREE_NUTS ")).isEqualTo(Allergen.TREE_NUTS);
		}

		@Test
		void namesTheAlternativesWhenItDoesNotRecogniseOne() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> Allergen.fromName("coriander"))
					.withMessageContaining("coriander")
					.withMessageContaining("milk");
		}

		@Test
		void readsSensiblyWhenPrinted() {
			assertThat(CEREALS_CONTAINING_GLUTEN).hasToString("Cereals containing gluten");
		}
	}
}
