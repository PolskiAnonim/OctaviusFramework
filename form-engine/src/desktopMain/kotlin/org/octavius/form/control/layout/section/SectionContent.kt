package org.octavius.form.control.layout.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormState
import org.octavius.form.control.base.RenderContext
import org.octavius.ui.theme.FormSpacing

@Composable
internal fun SectionContent(
    renderContext: RenderContext,
    controlNames: List<String>,
    columns: Int,
    formSchema: FormSchema,
    formState: FormState
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(FormSpacing.sectionPadding)
        ) {
            if (columns > 1) {
                // Logika dla wielu kolumn
                RenderMultiColumnContent(renderContext, controlNames, columns, formSchema, formState)
            } else {
                // Logika dla jednej kolumny
                RenderSingleColumnContent(renderContext, controlNames, formSchema, formState)
            }
        }
    }
}

@Composable
private fun RenderMultiColumnContent(
    renderContext: RenderContext,
    controlNames: List<String>,
    columns: Int,
    formSchema: FormSchema,
    formState: FormState
) {
    // Dzielimy listę kontrolek na grupy dla każdej kolumny
    val controlGroups = controlNames.chunked(
        (controlNames.size + columns - 1) / columns
    )

    Row(modifier = Modifier.fillMaxWidth()) {
        controlGroups.forEach { group ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = FormSpacing.fieldPaddingHorizontal)
            ) {
                group.forEach { controlName ->
                    RenderControlByName(renderContext, controlName, formSchema, formState)
                    Spacer(modifier = Modifier.height(FormSpacing.sectionContentSpacing))
                }
            }
        }
    }
}

@Composable
private fun RenderSingleColumnContent(
    renderContext: RenderContext,
    controlNames: List<String>,
    formSchema: FormSchema,
    formState: FormState,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        controlNames.forEach { controlName ->
            RenderControlByName(renderContext, controlName, formSchema, formState)
            Spacer(modifier = Modifier.height(FormSpacing.sectionHeaderPaddingBottom))
        }
    }
}

// Wyodrębniamy powtarzającą się logikę renderowania pojedynczej kontrolki
@Composable
private fun RenderControlByName(
    renderContext: RenderContext,
    controlName: String,
    formSchema: FormSchema,
    formState: FormState
) {
    formSchema.getControl(controlName)?.let { control ->
        formState.getControlState(controlName)?.let { state ->
            control.Render(renderContext.forSectionChild(controlName), state)
        }
    }
}
