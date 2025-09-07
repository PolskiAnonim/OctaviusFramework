package org.octavius.form.control.validator.datetime

import org.octavius.form.control.base.ControlState
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.IntervalValidation
import org.octavius.form.control.base.RenderContext
import org.octavius.localization.T
import kotlin.time.Duration

class IntervalValidator(
    private val validationOptions: IntervalValidation? = null
) : ControlValidator<Duration>() {

    private fun formatDuration(duration: Duration): String {
        return duration.toComponents { hours, minutes, seconds, _ ->
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    override fun validateSpecific(renderContext: RenderContext, state: ControlState<*>) {
        val value = state.value.value as? Duration ?: return
        val errors = mutableListOf<String>()

        validationOptions?.let { options ->
            options.min?.let { min ->
                if (value < min) {
                    errors.add(T.get("validation.minInterval", formatDuration(min)))
                }
            }
            options.max?.let { max ->
                if (value > max) {
                    errors.add(T.get("validation.maxInterval", formatDuration(max)))
                }
            }
        }
        errorManager.setFieldErrors(renderContext.fullPath, errors)
    }
}