package org.octavius.novels.navigator

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