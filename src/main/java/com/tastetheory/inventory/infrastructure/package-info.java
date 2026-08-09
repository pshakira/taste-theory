/**
 * Inventory adapters: JPA entities and repository implementations, REST
 * controllers, request and response types, and the mappers between them and the
 * domain model.
 *
 * <p>The JPA entities here are deliberately separate from the domain entities in
 * {@code inventory.domain}, so that the domain model is free to express business
 * rules without bending to what an ORM finds convenient. MapStruct maps between
 * the two.
 *
 * <p>Nothing in {@code inventory.domain} or {@code inventory.application} may
 * import from this package.
 */
package com.tastetheory.inventory.infrastructure;
