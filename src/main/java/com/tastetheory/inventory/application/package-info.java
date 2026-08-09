/**
 * Inventory use cases, and the ports they need from the outside world.
 *
 * <p>Classes here orchestrate — load, hand off to the domain, save. They do not
 * calculate; arithmetic in this package is a sign a rule has leaked out of
 * {@code inventory.domain}.
 *
 * <p>Port interfaces such as repositories are declared here and implemented in
 * {@code inventory.infrastructure}. That is what keeps the dependency pointing
 * inward: the outside depends on this package's interfaces, never the reverse.
 */
package com.tastetheory.inventory.application;
