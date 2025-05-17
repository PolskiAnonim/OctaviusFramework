package org.octavius.novels.form.control.type.dropdown

data class DropdownOption<T>(
    val value: T,
    val displayText: String
)