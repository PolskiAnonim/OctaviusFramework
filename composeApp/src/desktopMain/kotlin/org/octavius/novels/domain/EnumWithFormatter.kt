package org.octavius.novels.domain

interface EnumWithFormatter<T: Enum<T>> {
    fun toDisplayString(): String
}