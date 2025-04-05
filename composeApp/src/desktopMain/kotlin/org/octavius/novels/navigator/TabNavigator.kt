package org.octavius.novels.navigator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier

//class TabNavigator(
//    private val tabs: List<Tab>,
//    initialIndex: UShort,
//    private val swipeEnabled: Boolean =false
//) {
//    private var directionOfTabAnimation=0
//    private val currentIndexState: MutableState<UShort> = mutableStateOf(initialIndex)
//
//    var currentIndex: UShort
//        get() = currentIndexState.value
//        set(value) {
//            currentIndexState.value = value
//        }
//
//    val current: Tab
//        @Composable
//        get() = tabs.first { it.index == currentIndex }
//
//    @Composable
//    fun CurrentTab() {
//        var offset =0f
//
//
//        Box(
//            modifier = Modifier.fillMaxSize()
//        ) {
//            tabs.forEach { tab ->
//                AnimatedVisibility(
//                    visible = tab.index == currentIndex,
//                    enter = slideInHorizontally(animationSpec = tween(300)) { fullWidth ->
//                        if (directionOfTabAnimation == 1) fullWidth else -fullWidth
//                    } + fadeIn(),
//                    exit = slideOutHorizontally(animationSpec = tween(300)) { fullWidth ->
//                        if (directionOfTabAnimation == 1) -fullWidth else fullWidth
//                    } + fadeOut()
//                ) {
//                    tab.Content()
//                }
//            }
//        }
//    }
//
//    fun ChangeTab(newIndex: UShort) {
//        directionOfTabAnimation = if (currentIndex>newIndex)
//            -1 else 1
//        currentIndex = newIndex
//    }
//}