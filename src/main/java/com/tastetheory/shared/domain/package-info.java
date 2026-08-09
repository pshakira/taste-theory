/**
 * Value objects shared by every bounded context: money, quantities, units of
 * measure and the conversions between them.
 *
 * <p>Pure Java. No Spring, no JPA, no imports from anywhere else in this
 * project. This is the one package everything else depends on, so its
 * invariants are the ones that most need to be right.
 */
package com.tastetheory.shared.domain;
