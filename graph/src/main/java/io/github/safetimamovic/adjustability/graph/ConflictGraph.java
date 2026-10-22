package io.github.safetimamovic.adjustability.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * The conflict graph of one item: a vertex per enchantment it holds, an edge between two that can't be active together.
 * Edges are symmetric but not transitive (Riptide conflicts with Loyalty and with Channeling, which are compatible), so
 * conflicts are a graph, not a partition into groups. A valid active set is an independent set of this graph.
 *
 * <p>Pure Java: {@code T} is {@code Holder<Enchantment>} in game and plain strings in tests. Vertex order is kept as
 * given, which makes every result deterministic.
 */
public final class ConflictGraph<T>
{
        private final List<T> vertices;
        private final Map<T, Integer> index;
        private final BitSet[] neighbours;

        private ConflictGraph(List<T> vertices, BiPredicate<T, T> conflicts)
        {
                this.vertices = List.copyOf(vertices);
                this.index = new HashMap<>();
                this.neighbours = new BitSet[this.vertices.size()];
                for (int i = 0; i < this.vertices.size(); i++)
                {
                        this.index.put(this.vertices.get(i), i);
                        this.neighbours[i] = new BitSet(this.vertices.size());
                }
                for (int i = 0; i < this.vertices.size(); i++)
                {
                        for (int j = i + 1; j < this.vertices.size(); j++)
                        {
                                // Either direction counts: vanilla checks both enchantments' exclusive sets.
                                if (conflicts.test(this.vertices.get(i), this.vertices.get(j))
                                        || conflicts.test(this.vertices.get(j), this.vertices.get(i)))
                                {
                                        this.neighbours[i].set(j);
                                        this.neighbours[j].set(i);
                                }
                        }
                }
        }

        /**
         * Builds the graph. Duplicate vertices are kept once.
         *
         * @param vertices  the enchantments on the item, in a stable order
         * @param conflicts whether two different vertices can't be active together; called once per unordered pair and
         *                  direction, never with the same vertex twice
         */
        public static <T> ConflictGraph<T> of(Collection<T> vertices, BiPredicate<T, T> conflicts)
        {
                return new ConflictGraph<>(new ArrayList<>(new LinkedHashSet<>(vertices)), conflicts);
        }

        public List<T> vertices()
        {
                return this.vertices;
        }

        public boolean conflicts(T a, T b)
        {
                Integer i = this.index.get(a);
                Integer j = this.index.get(b);
                return i != null && j != null && this.neighbours[i].get(j);
        }

        /** The vertices {@code vertex} conflicts with, in vertex order; empty if it isn't in the graph. */
        public Set<T> neighbours(T vertex)
        {
                Integer i = this.index.get(vertex);
                if (i == null)
                {
                        return Set.of();
                }
                Set<T> result = new LinkedHashSet<>();
                this.neighbours[i].stream().forEach(j -> result.add(this.vertices.get(j)));
                return Collections.unmodifiableSet(result);
        }

        /** Whether no two members of {@code set} conflict. Members that aren't in the graph conflict with nothing. */
        public boolean isIndependent(Set<T> set)
        {
                BitSet members = this.bits(set);
                for (int i = members.nextSetBit(0); i >= 0; i = members.nextSetBit(i + 1))
                {
                        if (this.neighbours[i].intersects(members))
                        {
                                return false;
                        }
                }
                return true;
        }

        /**
         * Connected components with at least one conflict, in order of their first vertex. Vertices that conflict with
         * nothing are left out: there is nothing to choose for them.
         */
        public List<Set<T>> components()
        {
                List<Set<T>> result = new ArrayList<>();
                BitSet seen = new BitSet(this.vertices.size());
                for (int start = 0; start < this.vertices.size(); start++)
                {
                        if (seen.get(start) || this.neighbours[start].isEmpty())
                        {
                                continue;
                        }
                        Set<T> component = new LinkedHashSet<>();
                        Deque<Integer> queue = new ArrayDeque<>();
                        queue.add(start);
                        seen.set(start);
                        while (!queue.isEmpty())
                        {
                                int i = queue.poll();
                                component.add(this.vertices.get(i));
                                for (int j = this.neighbours[i].nextSetBit(0); j >= 0;
                                        j = this.neighbours[i].nextSetBit(j + 1))
                                {
                                        if (!seen.get(j))
                                        {
                                                seen.set(j);
                                                queue.add(j);
                                        }
                                }
                        }
                        result.add(Collections.unmodifiableSet(component));
                }
                return Collections.unmodifiableList(result);
        }

        private BitSet bits(Set<T> set)
        {
                BitSet bits = new BitSet(this.vertices.size());
                for (T member : set)
                {
                        Integer i = this.index.get(member);
                        if (i != null)
                        {
                                bits.set(i);
                        }
                }
                return bits;
        }
}
