package io.github.safetimamovic.adjustability.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import org.junit.jupiter.api.Test;

class ConflictGraphTest
{
        /** Vanilla's exclusive sets for the cases below, one direction only, like the enchantment JSON. */
        private static final Map<String, Set<String>> EXCLUSIVE_SETS = Map.of(
                "fortune", Set.of("silk_touch"),
                "protection", Set.of("fire_protection", "blast_protection", "projectile_protection"),
                "fire_protection", Set.of("blast_protection", "projectile_protection"),
                "blast_protection", Set.of("projectile_protection"),
                "riptide", Set.of("loyalty", "channeling"));

        private static final BiPredicate<String, String> CONFLICTS = (a, b) -> EXCLUSIVE_SETS
                .getOrDefault(a, Set.of()).contains(b);

        @Test
        void singleEdge()
        {
                ConflictGraph<String> graph = ConflictGraph.of(List.of("efficiency", "fortune", "silk_touch"),
                        CONFLICTS);

                assertTrue(graph.conflicts("silk_touch", "fortune"), "symmetric even though only fortune lists it");
                assertFalse(graph.conflicts("efficiency", "fortune"));
                assertEquals(List.of(Set.of("fortune", "silk_touch")), graph.components());
        }

        @Test
        void protectionsAreAClique()
        {
                ConflictGraph<String> graph = ConflictGraph.of(
                        List.of("protection", "fire_protection", "blast_protection", "projectile_protection"),
                        CONFLICTS);

                assertEquals(3, graph.neighbours("blast_protection").size());
                assertFalse(graph.isIndependent(Set.of("protection", "blast_protection")));
                assertTrue(graph.isIndependent(Set.of("projectile_protection")));
        }

        @Test
        void tridentIsNotAClique()
        {
                ConflictGraph<String> graph = ConflictGraph.of(List.of("loyalty", "riptide", "channeling"), CONFLICTS);

                assertFalse(graph.conflicts("loyalty", "channeling"));
                assertTrue(graph.isIndependent(Set.of("loyalty", "channeling")));
                assertFalse(graph.isIndependent(Set.of("riptide", "channeling")));
                assertEquals(List.of(Set.of("loyalty", "riptide", "channeling")), graph.components());
        }

        @Test
        void separateConflictsAreSeparateComponents()
        {
                ConflictGraph<String> graph = ConflictGraph.of(
                        List.of("fortune", "unbreaking", "protection", "silk_touch", "fire_protection"), CONFLICTS);

                assertEquals(List.of(Set.of("fortune", "silk_touch"), Set.of("protection", "fire_protection")),
                        graph.components());
        }

        @Test
        void unknownVerticesConflictWithNothing()
        {
                ConflictGraph<String> graph = ConflictGraph.of(List.of("fortune", "silk_touch"), CONFLICTS);

                assertTrue(graph.neighbours("mending").isEmpty());
                assertTrue(graph.isIndependent(Set.of("fortune", "mending")));
        }
}
