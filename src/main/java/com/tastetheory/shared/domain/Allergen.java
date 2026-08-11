package com.tastetheory.shared.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * The fourteen allergens that EU food information rules require to be declared.
 *
 * <p>A closed list, because the regulation is a closed list. An ingredient that
 * someone is intolerant to but which is not on it — coriander, say — is not an
 * allergen for these purposes and does not belong here.
 *
 * <p>Nothing in this application is verified against the regulation, and the
 * emphasis and format a printed label requires come from the regulation rather
 * than from this list. What is here propagates what was entered; it is not
 * label-ready output.
 */
public enum Allergen {

	CEREALS_CONTAINING_GLUTEN("Cereals containing gluten"),
	CRUSTACEANS("Crustaceans"),
	EGGS("Eggs"),
	FISH("Fish"),
	PEANUTS("Peanuts"),
	SOYBEANS("Soybeans"),
	MILK("Milk"),
	TREE_NUTS("Tree nuts"),
	CELERY("Celery"),
	MUSTARD("Mustard"),
	SESAME("Sesame"),
	SULPHITES("Sulphur dioxide and sulphites"),
	LUPIN("Lupin"),
	MOLLUSCS("Molluscs");

	private static final Map<String, Allergen> BY_NAME = Arrays.stream(values())
			.collect(Collectors.toUnmodifiableMap(allergen -> allergen.name().toLowerCase(Locale.ROOT), a -> a));

	private final String label;

	Allergen(String label) {
		this.label = label;
	}

	/** How it reads to a person. */
	public String label() {
		return label;
	}

	/** Parses the constant name, case-insensitively, for the edge of the application. */
	public static Allergen fromName(String name) {
		Objects.requireNonNull(name, "name must not be null");
		Allergen allergen = BY_NAME.get(name.trim().toLowerCase(Locale.ROOT));
		if (allergen == null) {
			throw new IllegalArgumentException("Unknown allergen '%s'. Known allergens: %s"
					.formatted(name, BY_NAME.keySet().stream().sorted().collect(Collectors.joining(", "))));
		}
		return allergen;
	}

	@Override
	public String toString() {
		return label;
	}
}
