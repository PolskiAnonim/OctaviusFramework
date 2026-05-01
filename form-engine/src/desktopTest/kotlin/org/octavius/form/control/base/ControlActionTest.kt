package org.octavius.form.control.base

import androidx.compose.runtime.mutableStateOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.octavius.form.component.ErrorManager
import org.octavius.form.component.FormActionTrigger
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormState

class ControlActionTest {

    @Test
    fun `updateControls should update all matching controls`() {
        // Arrange
        val formState = mockk<FormState>()
        val formSchema = mockk<FormSchema>()
        val errorManager = mockk<ErrorManager>()
        val trigger = mockk<FormActionTrigger>()
        val scope = CoroutineScope(Dispatchers.Unconfined)
        
        val sourceContext = ControlContext(localName = "triggerSource", statePath = "triggerSource")
        
        val state1 = ControlState<String>(value = mutableStateOf("old1"))
        val state2 = ControlState<String>(value = mutableStateOf("old2"))
        val stateOther = ControlState<String>(value = mutableStateOf("other"))

        val allStates = mapOf(
            "items[1]/value" to state1,
            "items[2]/value" to state2,
            "other/value" to stateOther
        )

        every { formState.getAllStates() } returns allStates
        every { formState.getControlState("items[1]/value") } returns state1
        every { formState.getControlState("items[2]/value") } returns state2

        val context = ActionContext<String>(
            sourceValue = "trigger",
            sourceControlContext = sourceContext,
            formState = formState,
            formSchema = formSchema,
            errorManager = errorManager,
            trigger = trigger,
            coroutineScope = scope
        )

        // Act
        context.updateControls<String>("items[*]/value", "new")

        // Assert
        assertThat(state1.value.value).isEqualTo("new")
        assertThat(state2.value.value).isEqualTo("new")
        assertThat(stateOther.value.value).isEqualTo("other")
        
        assertThat(state1.revision.value).isEqualTo(1)
        assertThat(state2.revision.value).isEqualTo(1)
        assertThat(stateOther.revision.value).isEqualTo(0)
    }
}
