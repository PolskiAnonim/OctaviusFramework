package org.octavius.form.control.layout.section

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormState
import org.octavius.form.control.base.ControlContext
import org.octavius.theme.FormSpacing

@Composable
internal fun SectionContent(
    controlContext: ControlContext,
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
                RenderMultiColumnContent(controlContext, controlNames, columns, formSchema, formState)
            } else {
                // Logika dla jednej kolumny
                RenderSingleColumnContent(controlContext, controlNames, formSchema, formState)
            }
        }
    }
}

@Composable
private fun RenderMultiColumnContent(
    controlContext: ControlContext,
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
                    RenderControlByName(controlContext, controlName, formSchema, formState)
                    Spacer(modifier = Modifier.height(FormSpacing.sectionContentSpacing))
                }
            }
        }
    }
}

@Composable
private fun RenderSingleColumnContent(
    controlContext: ControlContext,
    controlNames: List<String>,
    formSchema: FormSchema,
    formState: FormState,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        controlNames.forEach { controlName ->
            RenderControlByName(controlContext, controlName, formSchema, formState)
            Spacer(modifier = Modifier.height(FormSpacing.sectionHeaderPaddingBottom))
        }
    }
}

// Wyodrębniamy powtarzającą się logikę renderowania pojedynczej kontrolki
@Composable
private fun RenderControlByName(
    controlContext: ControlContext,
    controlName: String,
    formSchema: FormSchema,
    formState: FormState
) {
    formSchema.getControl(controlName)?.let { control ->
        formState.getControlState(controlName)?.let { state ->
            control.Render(controlContext.forSectionChild(controlName), state)
        }
    }
}
