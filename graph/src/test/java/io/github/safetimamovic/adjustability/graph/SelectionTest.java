package io.github.safetimamovic.adjustability.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.Test;

class SelectionTest
{
        private static final BiPredicate<String, String> VANILLA = (a, b) -> switch (a)
        {
                case "fortune" -> b.equals("silk_touch");
                case "riptide" -> b.equals("loyalty") || b.equals("channeling");
                case "protection" -> b.endsWith("_protection");
                case "fire_protection" -> b.equals("blast_protection");
                default -> false;
        };

        @Test
        void preferredVertexWinsItsConflict()
        {
                ConflictGraph<String> graph = ConflictGraph.of(List.of("silk_touch", "fortune", "efficiency"), VANILLA);

                Selection<String> selection = graph.select(List.of("fortune"));

                assertEquals(Set.of("fortune", "efficiency"), selection.active());
                assertEquals(Set.of("silk_touch"), selection.dormant());
        }

        @Test
        void withoutPreferenceVertexOrderDecides()
        {
                ConflictGraph<String> graph = ConflictGraph.of(List.of("silk_touch", "fortune"), VANILLA);

                assertEquals(Set.of("silk_touch"), graph.select(List.of()).active());
        }

        @Test
        void tridentKeepsLoyaltyAndChannelingTogether()
        {
                ConflictGraph<String> graph = ConflictGraph.of(List.of("riptide", "loyalty", "channeling"), VANILLA);

                Selection<String> riptide = graph.select(List.of("riptide"));
                assertEquals(Set.of("riptide"), riptide.active());

                Selection<String> loyalty = graph.activate(riptide, "loyalty", true);
                assertEquals(Set.of("loyalty", "channeling"), loyalty.active(), "auto fill brings channeling back");
                assertEquals(Set.of("riptide"), loyalty.dormant());

                Selection<String> noFill = graph.activate(riptide, "loyalty", false);
                assertEquals(Set.of("loyalty"), noFill.active());
                assertEquals(Set.of("riptide", "channeling"), noFill.dormant());

                assertEquals(riptide, graph.activate(loyalty, "riptide", true), "toggling back restores the start");
        }

        @Test
        void activateIsIdempotent()
        {
                ConflictGraph<String> graph = ConflictGraph.of(
                        List.of("protection", "fire_protection", "blast_protection", "projectile_protection"), VANILLA);
                Selection<String> start = graph.select(List.of("protection"));

                Selection<String> once = graph.activate(start, "blast_protection", true);

                assertEquals(once, graph.activate(once, "blast_protection", true));
                assertTrue(once.active().contains("blast_protection"));
                assertTrue(once.dormant().contains("protection"));
        }

        @Test
        void unknownVertexChangesNothing()
        {
                ConflictGraph<String> graph = ConflictGraph.of(List.of("fortune", "silk_touch"), VANILLA);
                Selection<String> start = graph.select(List.of());

                assertEquals(start, graph.activate(start, "mending", true));
        }

        /** 10,000 random graphs, fixed seed: every result is a valid, maximal, deterministic split. */
        @Test
        void randomGraphsAlwaysGiveMaximalIndependentSets()
        {
                Random random = new Random(26_3);
                for (int run = 0; run < 10_000; run++)
                {
                        int size = 1 + random.nextInt(12);
                        List<Integer> vertices = new ArrayList<>();
                        for (int i = 0; i < size; i++)
                        {
                                vertices.add(i);
                        }
                        double density = random.nextDouble();
                        Set<Long> edges = new HashSet<>();
                        for (int i = 0; i < size; i++)
                        {
                                for (int j = i + 1; j < size; j++)
                                {
                                        if (random.nextDouble() < density)
                                        {
                                                edges.add((long) i * 64 + j);
                                        }
                                }
                        }
                        ConflictGraph<Integer> graph = ConflictGraph.of(vertices,
                                (a, b) -> edges.contains((long) Math.min(a, b) * 64 + Math.max(a, b)));

                        List<Integer> preferred = List.of(random.nextInt(size), random.nextInt(size));
                        Selection<Integer> selected = graph.select(preferred);
                        assertValid(graph, selected);
                        assertEquals(selected, graph.select(preferred), "deterministic");

                        int target = random.nextInt(size);
                        Selection<Integer> activated = graph.activate(selected, target, true);
                        assertValid(graph, activated);
                        assertTrue(activated.active().contains(target));
                }
        }

        private static void assertValid(ConflictGraph<Integer> graph, Selection<Integer> selection)
        {
                assertTrue(graph.isIndependent(selection.active()), "independent: " + selection);
                assertEquals(graph.vertices().size(), selection.active().size() + selection.dormant().size());
                for (Integer dormant : selection.dormant())
                {
                        assertTrue(selection.active().stream().anyMatch(active -> graph.conflicts(active, dormant)),
                                "maximal: " + dormant + " could be active in " + selection);
                }
        }
}
