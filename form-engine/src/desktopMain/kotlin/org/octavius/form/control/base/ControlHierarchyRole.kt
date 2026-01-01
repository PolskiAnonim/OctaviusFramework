package org.octavius.form.control.base

enum class ControlHierarchyRole {
    /** Kontrolka niezależna, korzeń przetwarzania. */
    ROOT,

    /** Dziecko kontenera agregującego (np. Repeatable), przetwarzane przez rodzica. */
    AGGREGATED_CHILD,

    /** Dziecko kontenera organizacyjnego (np. Section), wizualnie zagnieżdżone, ale traktowane jak korzeń dla danych. */
    GROUPED_CHILD
}