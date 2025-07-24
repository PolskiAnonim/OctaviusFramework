package org.octavius.contract

/**
 * Interfejs fabryki dla ekranów, które można tworzyć dynamicznie.
 * Moduły `feature-*` implementują go, aby umożliwić nawigację do swoich ekranów z zewnątrz.
 */
interface ScreenFactory {
    /** Unikalny identyfikator ekranu, na który będzie reagować API. */
    val screenId: String

    /**
     * Tworzy instancję ekranu na podstawie przekazanych danych.
     * @param payload Opcjonalne dane z API.
     * @return Instancja implementująca interfejs Screen.
     */
    fun create(payload: Map<String, Any>?): Screen
}