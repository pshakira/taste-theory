package com.tastetheory.inventory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.tastetheory.shared.domain.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class YieldPercentageTest {

	@Nested
	@DisplayName("what it costs once the waste is paid for")
	class RaisingACost {

		@Test
		void eightyPercentYieldMeansBuyingAQuarterMore() {
			assertThat(YieldPercentage.ofPercent(80).purchaseMultiplier()).isEqualByComparingTo("1.25");
		}

		@Test
		void theOnionCase() {
			// EUR 0.0009 a gram bought, but a fifth of it is skins and tops.
			assertThat(YieldPercentage.ofPercent(80).applyTo(Money.euros("0.0009")))
					.isEqualTo(Money.euros("0.001125"));
		}

		@Test
		void losingHalfDoublesTheCost() {
			assertThat(YieldPercentage.ofPercent(50).applyTo(Money.euros("1.00")))
					.isEqualTo(Money.euros("2.00"));
		}

		@Test
		void losingNothingChangesNothing() {
			assertThat(YieldPercentage.full().applyTo(Money.euros("1.00"))).isEqualTo(Money.euros("1.00"));
			assertThat(YieldPercentage.full().purchaseMultiplier()).isEqualByComparingTo("1");
			assertThat(YieldPercentage.full().isFull()).isTrue();
		}
	}

	@Nested
	@DisplayName("bounds")
	class Bounds {

		@Test
		void acceptsAnythingUpToAndIncludingAllOfIt() {
			assertThat(YieldPercentage.ofPercent(100).isFull()).isTrue();
			assertThat(YieldPercentage.ofPercent("0.5").isFull()).isFalse();
		}

		@Test
		void refusesNothingSurviving() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> YieldPercentage.ofPercent(0))
					.withMessageContaining("more than zero");
		}

		@Test
		void refusesANegativeYield() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> YieldPercentage.ofPercent(-10))
					.withMessageContaining("more than zero");
		}

		@Test
		void refusesCookingWithMoreThanWasBought() {
			assertThatIllegalArgumentException()
					.isThrownBy(() -> YieldPercentage.ofPercent(120))
					.withMessageContaining("cannot exceed 100%");
		}
	}

	@Test
	void readsSensiblyWhenPrinted() {
		assertThat(YieldPercentage.ofPercent(80)).hasToString("80%");
	}
}
