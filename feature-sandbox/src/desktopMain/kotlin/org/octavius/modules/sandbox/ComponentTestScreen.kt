package org.octavius.modules.sandbox

import androidx.compose.runtime.Composable
import org.octavius.localization.Tr
import org.octavius.navigation.Screen

class ComponentTestScreen : Screen {


    override val title: String = Tr.Sandbox.componentTestScreen()

    @Composable
    override fun Content() {

    }
}