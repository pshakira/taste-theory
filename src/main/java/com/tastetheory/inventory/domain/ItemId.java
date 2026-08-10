package com.tastetheory.inventory.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * An item's identity.
 *
 * <p>A wrapper around a {@link UUID} rather than the bare thing, so that an item
 * id cannot be passed where a supplier id is expected. Both are UUIDs; only one
 * of them is right.
 *
 * <p>Created here rather than by the database, so an item has identity from the
 * moment it exists — before it is saved, and whether or not it ever is.
 */
public record ItemId(UUID value) {

	public ItemId {
		Objects.requireNonNull(value, "value must not be null");
	}

	public static ItemId newId() {
		return new ItemId(UUID.randomUUID());
	}

	public static ItemId of(UUID value) {
		return new ItemId(value);
	}

	public static ItemId of(String value) {
		return new ItemId(UUID.fromString(value));
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
