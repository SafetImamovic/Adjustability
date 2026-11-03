package io.github.safetimamovic.adjustability.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * The conflict shapes content mods and datapacks produce through {@code exclusive_set}: sets that only one side lists,
 * paths, stars, big cliques and several groups on one item. Each is built the way the enchantment JSON is: a directed
 * "A lists B", where the graph has to treat the pair as conflicting in both directions.
 */
class ExclusivityShapesTest
{
        /** A predicate from "X lists Y" pairs, one direction only: {@code listed("a", "b", "c", "b")}. */
        private static BiPredicate<String, String> listed(String... pairs)
        {
                Map<String, Set<String>> lists = new HashMap<>();
                for (int i = 0; i < pairs.length; i += 2)
                {
                        lists.computeIfAbsent(pairs[i], key -> new HashSet<>()).add(pairs[i + 1]);
                }
                return (a, b) -> lists.getOrDefault(a, Set.of()).contains(b);
        }

        /** A predicate where every two different vertices conflict, each listing all the others. */
        private static BiPredicate<String, String> everyone()
        {
                return (a, b) -> !a.equals(b);
        }

        private static List<String> names(int count)
        {
                return IntStream.range(0, count).mapToObj(i -> String.format("e%02d", i)).toList();
        }

        @Test
        void oneSidedSetConflictsBothWays()
        {
                ConflictGraph<String> graph = ConflictGraph.of(List.of("a", "b"), listed("a", "b"));

                assertTrue(graph.conflicts("a", "b"));
                assertTrue(graph.conflicts("b", "a"), "b never lists a, still conflicts");
                assertEquals(List.of(Set.of("a", "b")), graph.components());
        }

        @Test
        void vertexOrderDoesNotChangeWhoConflicts()
        {
                BiPredicate<String, String> conflicts = listed("late", "early");

                assertTrue(ConflictGraph.of(List.of("early", "late"), conflicts).conflicts("early", "late"));
                assertTrue(ConflictGraph.of(List.of("late", "early"), conflicts).conflicts("early", "late"));
        }

        @Test
        void twoOneSidedSetsMeetingInOneEnchantmentFormAStar()
        {
                // a and c each list b; b lists nothing. a and c don't conflict with each other.
                ConflictGraph<String> graph = ConflictGraph.of(List.of("a", "b", "c"), listed("a", "b", "c", "b"));

                assertFalse(graph.conflicts("a", "c"));
                assertEquals(List.of(Set.of("a", "b", "c")), graph.components(), "one group, joined through b");
                assertTrue(graph.isIndependent(Set.of("a", "c")));
                assertFalse(graph.isIndependent(Set.of("a", "b")));
        }

        @Test
        void triangle()
        {
                ConflictGraph<String> graph = ConflictGraph.of(List.of("a", "b", "c"),
                        listed("a", "b", "b", "c", "c", "a"));

                assertEquals(2, graph.neighbours("a").size());
                assertEquals(List.of(Set.of("a", "b", "c")), graph.components());
                Selection<String> selection = graph.select(List.of());
                assertEquals(Set.of("a"), selection.active(), "one of three, the first in vertex order");
                assertEquals(Set.of("b", "c"), selection.dormant());
        }

        @Test
        void pathKeepsTheEndsTogether()
        {
                // a - b - c: a and c don't conflict, both conflict with b.
                ConflictGraph<String> graph = ConflictGraph.of(List.of("a", "b", "c"), listed("a", "b", "b", "c"));

                assertFalse(graph.conflicts("a", "c"));
                assertEquals(List.of(Set.of("a", "b", "c")), graph.components());

                Selection<String> first = graph.select(List.of());
                assertEquals(Set.of("a", "c"), first.active(), "the larger side wins by vertex order, ends together");
                assertEquals(Set.of("b"), first.dormant());

                Selection<String> middle = graph.select(List.of("b"));
                assertEquals(Set.of("b"), middle.active(), "preferred b pushes both ends out");
                assertEquals(Set.of("a", "c"), middle.dormant());
        }

        @Test
        void pathCyclesBetweenTheEndsAndTheMiddle()
        {
                ConflictGraph<String> graph = ConflictGraph.of(List.of("a", "b", "c"), listed("a", "b", "b", "c"));
                Set<String> component = graph.components().getFirst();

                Selection<String> selection = graph.select(List.of());
                assertEquals(Set.of("a", "c"), selection.active());

                selection = graph.activate(selection, graph.next(selection, component).orElseThrow(), true);
                assertEquals(Set.of("b"), selection.active());

                selection = graph.activate(selection, graph.next(selection, component).orElseThrow(), true);
                assertEquals(Set.of("a", "c"), selection.active(), "activating c brings a back");
        }

        @Test
        void activatingAnEndWithoutAutoFillLeavesTheOtherEndDormant()
        {
                ConflictGraph<String> graph = ConflictGraph.of(List.of("a", "b", "c"), listed("a", "b", "b", "c"));
                Selection<String> middle = graph.select(List.of("b"));

                assertEquals(Set.of("a"), graph.activate(middle, "a", false).active());
                assertEquals(Set.of("a", "c"), graph.activate(middle, "a", true).active());
        }

        @Test
        void starHubAgainstItsLeaves()
        {
                List<String> vertices = List.of("hub", "l1", "l2", "l3", "l4", "l5");
                ConflictGraph<String> graph = ConflictGraph.of(vertices,
                        listed("hub", "l1", "hub", "l2", "hub", "l3", "l4", "hub", "l5", "hub"));
                Set<String> component = graph.components().getFirst();

                assertEquals(5, graph.neighbours("hub").size());
                assertEquals(Set.of("hub"), graph.neighbours("l3"));

                Selection<String> selection = graph.select(List.of());
                assertEquals(Set.of("hub"), selection.active(), "hub comes first in vertex order");

                selection = graph.activate(selection, graph.next(selection, component).orElseThrow(), true);
                assertEquals(Set.of("l1", "l2", "l3", "l4", "l5"), selection.active(), "all five leaves together");

                selection = graph.activate(selection, graph.next(selection, component).orElseThrow(), true);
                assertEquals(Set.of("hub"), selection.active(), "and back to the hub");
        }

        @Test
        void sixMutuallyExclusiveEnchantmentsAllowOneAtATime()
        {
                List<String> vertices = names(6);
                ConflictGraph<String> graph = ConflictGraph.of(vertices, everyone());

                assertEquals(1, graph.components().size());
                assertEquals(5, graph.neighbours("e03").size());
                assertFalse(graph.isIndependent(Set.of("e00", "e05")));
                for (String vertex : vertices)
                {
                        assertTrue(graph.isIndependent(Set.of(vertex)), vertex + " alone");
                }

                Selection<String> selection = graph.select(List.of("e04"));
                assertEquals(Set.of("e04"), selection.active());
                assertEquals(5, selection.dormant().size());
        }

        @Test
        void twentyMutuallyExclusiveEnchantmentsCycleThroughAll()
        {
                List<String> vertices = names(20);
                ConflictGraph<String> graph = ConflictGraph.of(vertices, everyone());
                Set<String> component = graph.components().getFirst();
                assertEquals(20, component.size());

                Selection<String> selection = graph.select(List.of());
                List<String> visited = new ArrayList<>(selection.active());
                for (int step = 1; step < vertices.size(); step++)
                {
                        selection = graph.activate(selection, graph.next(selection, component).orElseThrow(), true);
                        assertEquals(1, selection.active().size(), "exactly one active at step " + step);
                        assertEquals(19, selection.dormant().size());
                        visited.addAll(selection.active());
                }
                assertEquals(vertices, visited, "visits every enchantment once, in order");
                assertTrue(graph.isLastChoice(selection, component));

                selection = graph.activate(selection, graph.next(selection, component).orElseThrow(), true);
                assertEquals(Set.of("e00"), selection.active(), "and wraps to the first");
        }

        @Test
        void noneDeactivatesAWholeBigGroup()
        {
                ConflictGraph<String> graph = ConflictGraph.of(names(20), everyone());
                Set<String> component = graph.components().getFirst();

                Selection<String> none = graph.deactivate(graph.select(List.of("e07")), component);

                assertTrue(none.active().isEmpty());
                assertEquals(20, none.dormant().size());
        }

        @Test
        void twoGroupsOnOneItemStayIndependent()
        {
                // {a, b} conflict, {c, d, e} are a triangle, f conflicts with nothing.
                ConflictGraph<String> graph = ConflictGraph.of(List.of("a", "b", "c", "d", "e", "f"),
                        listed("a", "b", "c", "d", "d", "e", "e", "c"));

                assertEquals(List.of(Set.of("a", "b"), Set.of("c", "d", "e")), graph.components());
                assertEquals(List.of("f"), graph.singles());

                Selection<String> selection = graph.select(List.of());
                assertEquals(new LinkedHashSet<>(List.of("a", "c", "f")), selection.active());

                selection = graph.activate(selection, "b", true);
                assertEquals(Set.of("b", "c", "f"), selection.active(), "switching one group leaves the other");
                selection = graph.activate(selection, "e", true);
                assertEquals(Set.of("b", "e", "f"), selection.active());
        }

        @Test
        void aLongChainIsOneGroup()
        {
                // e00 - e01 - e02 - ... - e09: each conflicts only with its neighbours.
                List<String> vertices = names(10);
                String[] pairs = new String[(vertices.size() - 1) * 2];
                for (int i = 0; i < vertices.size() - 1; i++)
                {
                        pairs[i * 2] = vertices.get(i);
                        pairs[i * 2 + 1] = vertices.get(i + 1);
                }
                ConflictGraph<String> graph = ConflictGraph.of(vertices, listed(pairs));

                assertEquals(1, graph.components().size());
                Selection<String> selection = graph.select(List.of());
                assertEquals(Set.of("e00", "e02", "e04", "e06", "e08"), selection.active(),
                        "every other one: a maximal independent set");
                assertTrue(graph.isIndependent(selection.active()));
                for (String dormant : selection.dormant())
                {
                        assertFalse(graph.isIndependent(addTo(selection.active(), dormant)),
                                dormant + " can't be added, so the set is maximal");
                }
        }

        @Test
        void exclusiveSetsNamingEnchantmentsNotOnTheItemAreIgnored()
        {
                // z is listed but isn't on the item, so it isn't a vertex.
                ConflictGraph<String> graph = ConflictGraph.of(List.of("a", "b"), listed("a", "z", "z", "b"));

                assertFalse(graph.conflicts("a", "b"));
                assertTrue(graph.components().isEmpty());
                assertEquals(List.of("a", "b"), graph.singles());
                assertTrue(graph.neighbours("z").isEmpty());
        }

        private static Set<String> addTo(Set<String> set, String extra)
        {
                Set<String> result = new LinkedHashSet<>(set);
                result.add(extra);
                return result;
        }
}
