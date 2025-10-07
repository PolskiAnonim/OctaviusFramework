package org.octavius.data

/**
 * Oznacza klasę `data class` jako cel dla dynamicznego mapowania z typu `dynamic_dto`
 * w PostgreSQL.
 *
 * Ta adnotacja jest używana przez `TypeRegistryLoader` do zbudowania bezpiecznej mapy
 * kluczy (umownych nazw) na klasy Kotlina. Pozwala to na deserializację zagnieżdżonych
 * struktur tworzonych w locie w zapytaniach SQL, bez potrzeby definiowania
 * formalnego typu kompozytowego (`CREATE TYPE`) w bazie danych.
 * UWAGA: Użycie wymaga także adnotacji `@Serializable`!
 *
 * @param typeName Umowny identyfikator (klucz), który będzie używany w funkcji SQL
 *                 `dynamic_dto('typeName', ...)` do wskazania, na którą klasę
 *                 należy zamapować wynik.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class DynamicallyMappable(val typeName: String)
