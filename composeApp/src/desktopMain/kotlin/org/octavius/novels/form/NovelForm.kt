package org.octavius.novels.form

import org.octavius.novels.form.control.type.HiddenControl
import org.octavius.novels.form.control.type.SectionControl

class NovelForm : Form() {
    override fun createSchema(): FormControls {
        return FormControls(
            mapOf(
                Pair("id", HiddenControl<Int>("id", "novels")),
                Pair(
                    "novelInfo", SectionControl(
                        ctrls = listOf(),
                        collapsible = false,
                        initiallyExpanded = false,
                        columns = 1,
                        label = "Nowelki"
                    )
                )

            ),
            listOf("novelInfo")
        )
    }

    override fun initialize() {
        TODO("Not yet implemented")
    }
}