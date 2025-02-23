package org.octavius.novels.state

import androidx.compose.runtime.*
import org.octavius.novels.domain.Novel

val LocalState = compositionLocalOf<State> { error("No State found!") }

class State {
    //Pagination
    var currentPage = mutableStateOf(1)
    var totalPages = mutableStateOf(1L)
    val pageSize = 10
    var searchQuery = mutableStateOf("")
}