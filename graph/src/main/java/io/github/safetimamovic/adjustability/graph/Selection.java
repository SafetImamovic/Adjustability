package io.github.safetimamovic.adjustability.graph;

import java.util.Set;

/**
 * Which vertices of a {@link ConflictGraph} are active and which are dormant. Every vertex is in exactly one of the two.
 */
public record Selection<T>(Set<T> active, Set<T> dormant)
{
}
