package com.tastetheory.shared.domain;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * What something contains, and what it might contain.
 *
 * <p>The two are kept apart deliberately and must never be merged. "Contains
 * milk" and "may contain milk" mean different things to whoever is eating it,
 * and collapsing them either alarms people needlessly or, far worse, understates
 * a real ingredient.
 *
 * <p>Where an allergen appears in both, the definite statement wins and the trace
 * is dropped: something known to contain milk gains nothing from also warning
 * that it might.
 *
 * <p>{@link #combinedWith} is what allergen propagation will be built from in
 * recipes — a dish contains the union of everything below it, however deeply
 * nested.
 */
public record AllergenProfile(Set<Allergen> contains, Set<Allergen> mayContain) {

	private static final AllergenProfile NONE =
			new AllergenProfile(EnumSet.noneOf(Allergen.class), EnumSet.noneOf(Allergen.class));

	public AllergenProfile {
		Objects.requireNonNull(contains, "contains must not be null");
		Objects.requireNonNull(mayContain, "mayContain must not be null");

		Set<Allergen> definite = copyOf(contains);
		Set<Allergen> traces = copyOf(mayContain);
		traces.removeAll(definite);

		contains = Set.copyOf(definite);
		mayContain = Set.copyOf(traces);
	}

	/** Declares nothing. Not the same as "checked and found to be free of allergens". */
	public static AllergenProfile none() {
		return NONE;
	}

	public static AllergenProfile containing(Allergen... allergens) {
		return new AllergenProfile(Set.of(allergens), Set.of());
	}

	public static AllergenProfile of(Collection<Allergen> contains, Collection<Allergen> mayContain) {
		return new AllergenProfile(Set.copyOf(contains), Set.copyOf(mayContain));
	}

	/** Everything both profiles declare, with definite statements taking precedence. */
	public AllergenProfile combinedWith(AllergenProfile other) {
		Objects.requireNonNull(other, "other must not be null");
		Set<Allergen> definite = copyOf(contains);
		definite.addAll(other.contains);
		Set<Allergen> traces = copyOf(mayContain);
		traces.addAll(other.mayContain);
		return new AllergenProfile(definite, traces);
	}

	public boolean declaresNothing() {
		return contains.isEmpty() && mayContain.isEmpty();
	}

	public boolean contains(Allergen allergen) {
		return contains.contains(allergen);
	}

	public boolean mayContain(Allergen allergen) {
		return mayContain.contains(allergen);
	}

	private static EnumSet<Allergen> copyOf(Set<Allergen> source) {
		return source.isEmpty() ? EnumSet.noneOf(Allergen.class) : EnumSet.copyOf(source);
	}
}
