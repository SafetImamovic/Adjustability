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
import java.util.Optional;
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

        /**
         * Splits the vertices into an active and a dormant set: walks {@code preferred} first, then the remaining
         * vertices in vertex order, and makes each one active unless it conflicts with one already active. The result
         * is a maximal independent set; vertices earlier in {@code preferred} win their conflicts.
         *
         * @param preferred vertices to keep active if possible, most important first; unknown ones are ignored
         */
        public Selection<T> select(List<T> preferred)
        {
                List<Integer> order = new ArrayList<>(this.vertices.size());
                BitSet queued = new BitSet(this.vertices.size());
                for (T vertex : preferred)
                {
                        Integer i = this.index.get(vertex);
                        if (i != null && !queued.get(i))
                        {
                                queued.set(i);
                                order.add(i);
                        }
                }
                for (int i = 0; i < this.vertices.size(); i++)
                {
                        if (!queued.get(i))
                        {
                                order.add(i);
                        }
                }
                BitSet active = new BitSet(this.vertices.size());
                for (int i : order)
                {
                        if (!this.neighbours[i].intersects(active))
                        {
                                active.set(i);
                        }
                }
                return this.selection(active);
        }

        /**
         * Makes {@code vertex} active: what it conflicts with becomes dormant, and with {@code autoFill}, dormant
         * vertices that no longer conflict with anything active come back (in vertex order). Returns {@code current}
         * unchanged if {@code vertex} isn't in the graph.
         */
        public Selection<T> activate(Selection<T> current, T vertex, boolean autoFill)
        {
                Integer target = this.index.get(vertex);
                if (target == null)
                {
                        return current;
                }
                BitSet active = this.bits(current.active());
                active.andNot(this.neighbours[target]);
                active.set(target);
                if (autoFill)
                {
                        for (int i = 0; i < this.vertices.size(); i++)
                        {
                                if (!active.get(i) && !this.neighbours[i].intersects(active))
                                {
                                        active.set(i);
                                }
                        }
                }
                return this.selection(active);
        }

        /**
         * The vertex a "cycle" step activates in {@code component}: the first dormant vertex after the last active one,
         * in vertex order, wrapping around. Activating it (with auto fill) steps through the component's choices:
         * Fortune → Silk Touch → Fortune, or Riptide → Channeling + Loyalty → Riptide on a trident.
         *
         * @return empty if the component has no dormant vertex
         */
        public Optional<T> next(Selection<T> current, Set<T> component)
        {
                List<T> members = this.vertices.stream().filter(component::contains).toList();
                int lastActive = -1;
                for (int i = 0; i < members.size(); i++)
                {
                        if (current.active().contains(members.get(i)))
                        {
                                lastActive = i;
                        }
                }
                for (int step = 1; step <= members.size(); step++)
                {
                        T candidate = members.get(Math.floorMod(lastActive + step, members.size()));
                        if (current.dormant().contains(candidate))
                        {
                                return Optional.of(candidate);
                        }
                }
                return Optional.empty();
        }

        private Selection<T> selection(BitSet active)
        {
                Set<T> on = new LinkedHashSet<>();
                Set<T> off = new LinkedHashSet<>();
                for (int i = 0; i < this.vertices.size(); i++)
                {
                        (active.get(i) ? on : off).add(this.vertices.get(i));
                }
                return new Selection<>(Collections.unmodifiableSet(on), Collections.unmodifiableSet(off));
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
