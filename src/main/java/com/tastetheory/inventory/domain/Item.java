package com.tastetheory.inventory.domain;

import com.tastetheory.shared.domain.AllergenProfile;
import com.tastetheory.shared.domain.Money;
import com.tastetheory.shared.domain.Nutrition;
import com.tastetheory.shared.domain.Percentage;
import com.tastetheory.shared.domain.Quantity;
import com.tastetheory.shared.domain.UnitPrice;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * An ingredient: something bought from a supplier and cooked with.
 *
 * <p>The figure everything else depends on is {@link #costPerRecipeUnit()}, and
 * it is calculated rather than stored. A 5 kg sack of onions at EUR 4.50, cooked
 * in grams, at 80% yield:
 *
 * <pre>
 *   EUR 4.50 per sack / 5 kg      = EUR 0.90     per kg
 *   EUR 0.90 per kg   / 1000      = EUR 0.0009   per gram
 *   EUR 0.0009        / 0.80      = EUR 0.001125 per gram cooked with
 * </pre>
 *
 * <p>Two prices are held, and they answer different questions. The
 * {@link #agreedPrice()} is what you negotiated and maintain by hand; it is what
 * recipe costing uses, so a recipe's margin moves when you decide it does. The
 * {@link #latestPurchasePrice()} is whatever the most recent invoice charged, and
 * exists to be compared against the agreed price. Keeping them apart is what
 * makes {@link #priceVariancePerPurchaseUnit()} meaningful — the system this
 * replaces held one field and overwrote it from invoices, so comparing an invoice
 * against it always reported no change.
 *
 * <p>An item's VAT rate is recorded and never applied. Recipe costs are net of
 * tax, matching how the cost sheets this was built from work.
 *
 * <p>This is an entity, not a value object: two items with identical names and
 * prices are still two different items, and an item that gets renamed is still
 * the same item. Equality is therefore by {@link ItemId} alone.
 */
public class Item {

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	private final ItemId id;
	private String name;
	private String category;
	private ItemUnits units;
	private AgreedPrice agreedPrice;
	private YieldPercentage yieldPercentage;
	private UnitPrice latestPurchasePrice;
	private Percentage vatRate;
	private Nutrition nutritionPer100RecipeUnits;
	private AllergenProfile allergens;

	private Item(
			ItemId id,
			String name,
			String category,
			ItemUnits units,
			AgreedPrice agreedPrice,
			YieldPercentage yieldPercentage,
			UnitPrice latestPurchasePrice,
			Percentage vatRate,
			Nutrition nutritionPer100RecipeUnits,
			AllergenProfile allergens) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.units = Objects.requireNonNull(units, "units must not be null");
		this.yieldPercentage = Objects.requireNonNull(yieldPercentage, "yieldPercentage must not be null");
		this.name = requireName(name);
		this.category = blankToNull(category);
		this.agreedPrice = requirePriceInPurchaseUnit(agreedPrice, units);
		this.latestPurchasePrice = latestPurchasePrice;
		this.vatRate = vatRate;
		this.nutritionPer100RecipeUnits =
				nutritionPer100RecipeUnits == null ? Nutrition.zero() : nutritionPer100RecipeUnits;
		this.allergens = allergens == null ? AllergenProfile.none() : allergens;
	}

	/** A new item, with an identity of its own. */
	public static Item create(String name, ItemUnits units, AgreedPrice agreedPrice, YieldPercentage yieldPercentage) {
		return new Item(ItemId.newId(), name, null, units, agreedPrice, yieldPercentage, null, null, null, null);
	}

	/**
	 * Rebuilds an item that already exists, for use when loading one back out of
	 * storage. Everything else goes through {@link #create}.
	 *
	 * <p>The parameter list is already uncomfortable and persistence will make it
	 * worse; a builder is the obvious next move when the JPA mapper lands.
	 */
	public static Item restore(
			ItemId id,
			String name,
			String category,
			ItemUnits units,
			AgreedPrice agreedPrice,
			YieldPercentage yieldPercentage,
			UnitPrice latestPurchasePrice,
			Percentage vatRate,
			Nutrition nutritionPer100RecipeUnits,
			AllergenProfile allergens) {
		return new Item(id, name, category, units, agreedPrice, yieldPercentage, latestPurchasePrice, vatRate,
				nutritionPer100RecipeUnits, allergens);
	}

	// --------------------------------------------------------------- costing ---

	/**
	 * What one recipe unit of this costs to cook with, converted out of the
	 * purchase unit and raised to account for trim loss.
	 */
	public UnitPrice costPerRecipeUnit() {
		Money perPurchaseUnit = agreedPrice.perPurchaseUnit().amount();
		Money perRecipeUnit = perPurchaseUnit.dividedBy(units.recipeUnitsPerPurchaseUnit());
		return UnitPrice.of(yieldPercentage.applyTo(perRecipeUnit), units.recipeUnit());
	}

	/** What a given amount of this costs a recipe. */
	public Money costOf(Quantity recipeQuantity) {
		Objects.requireNonNull(recipeQuantity, "recipeQuantity must not be null");
		return costPerRecipeUnit().times(recipeQuantity);
	}

	/**
	 * How far the last invoice sat from the agreed price, per purchase unit.
	 * Positive means paying more than agreed. Empty until something has been
	 * bought.
	 */
	public Optional<Money> priceVariancePerPurchaseUnit() {
		return Optional.ofNullable(latestPurchasePrice)
				.map(latest -> latest.convertedTo(units.purchaseUnit()).amount()
						.minus(agreedPrice.perPurchaseUnit().amount()));
	}

	/** True when the last invoice charged more than the agreed price. */
	public boolean isOverAgreedPrice() {
		return priceVariancePerPurchaseUnit().filter(Money::isPositive).isPresent();
	}

	// ------------------------------------------------------------- nutrition ---

	/**
	 * The nutrition in a given amount of this ingredient.
	 *
	 * <p>Yield is not applied. The declared figures already describe the edible
	 * portion, so raising them for trim loss would count the waste twice — which
	 * is precisely the opposite of what happens to cost.
	 */
	public Nutrition nutritionFor(Quantity recipeQuantity) {
		Objects.requireNonNull(recipeQuantity, "recipeQuantity must not be null");
		Quantity inRecipeUnits = recipeQuantity.convertedTo(units.recipeUnit());
		return nutritionPer100RecipeUnits.scaledBy(inRecipeUnits.amount().divide(ONE_HUNDRED));
	}

	// -------------------------------------------------------------- behaviour ---

	public void rename(String newName) {
		this.name = requireName(newName);
	}

	public void recategorise(String newCategory) {
		this.category = blankToNull(newCategory);
	}

	public void changeAgreedPrice(AgreedPrice newPrice) {
		this.agreedPrice = requirePriceInPurchaseUnit(newPrice, units);
	}

	public void changeYield(YieldPercentage newYield) {
		this.yieldPercentage = Objects.requireNonNull(newYield, "newYield must not be null");
	}

	/**
	 * Changes how the item is bought and measured out. The agreed price has to be
	 * restated at the same time, because a price per kilogram means nothing once
	 * the item is bought by the tin.
	 */
	public void changeUnits(ItemUnits newUnits, AgreedPrice restatedPrice) {
		Objects.requireNonNull(newUnits, "newUnits must not be null");
		this.agreedPrice = requirePriceInPurchaseUnit(restatedPrice, newUnits);
		this.units = newUnits;
	}

	/** Records what an invoice actually charged. Does not touch the agreed price. */
	public void recordPurchasePrice(UnitPrice invoicedPrice) {
		Objects.requireNonNull(invoicedPrice, "invoicedPrice must not be null");
		this.latestPurchasePrice = invoicedPrice.convertedTo(units.purchaseUnit());
	}

	/** Recorded for the accountant. Never applied to costing. */
	public void recordVatRate(Percentage rate) {
		this.vatRate = rate;
	}

	/** The supplier's figures, per 100 recipe units. */
	public void recordNutritionPer100(Nutrition nutrition) {
		this.nutritionPer100RecipeUnits = Objects.requireNonNull(nutrition, "nutrition must not be null");
	}

	/** What this contains, and what it might contain. */
	public void declareAllergens(AllergenProfile profile) {
		this.allergens = Objects.requireNonNull(profile, "profile must not be null");
	}

	// --------------------------------------------------------------- accessors ---

	public ItemId id() {
		return id;
	}

	public String name() {
		return name;
	}

	public Optional<String> category() {
		return Optional.ofNullable(category);
	}

	public ItemUnits units() {
		return units;
	}

	public AgreedPrice agreedPrice() {
		return agreedPrice;
	}

	public YieldPercentage yieldPercentage() {
		return yieldPercentage;
	}

	public Optional<UnitPrice> latestPurchasePrice() {
		return Optional.ofNullable(latestPurchasePrice);
	}

	public Optional<Percentage> vatRate() {
		return Optional.ofNullable(vatRate);
	}

	public Nutrition nutritionPer100RecipeUnits() {
		return nutritionPer100RecipeUnits;
	}

	public AllergenProfile allergens() {
		return allergens;
	}

	// ------------------------------------------------------------------ guards ---

	private static String requireName(String name) {
		Objects.requireNonNull(name, "name must not be null");
		String trimmed = name.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("An item must have a name");
		}
		return trimmed;
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static AgreedPrice requirePriceInPurchaseUnit(AgreedPrice price, ItemUnits units) {
		Objects.requireNonNull(price, "agreedPrice must not be null");
		if (price.purchaseUnit() != units.purchaseUnit()) {
			throw new IllegalArgumentException("Agreed price is per %s but the item is bought in %s"
					.formatted(price.purchaseUnit().code(), units.purchaseUnit().code()));
		}
		return price;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof Item item && id.equals(item.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return "%s (%s)".formatted(name, costPerRecipeUnit());
	}
}
