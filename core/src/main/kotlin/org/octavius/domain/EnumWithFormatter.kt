package org.octavius.domain

interface EnumWithFormatter<T : Enum<T>> {
    fun toDisplayString(): String
}