package org.octavius.modules.sandbox.domain

import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.Tr

enum class SandboxPriority : EnumWithFormatter<SandboxPriority> {
    Low,
    Medium,
    High,
    Critical;

    override fun toDisplayString(): String {
        return when (this) {
            Low -> Tr.Sandbox.Priority.low()
            Medium -> Tr.Sandbox.Priority.medium()
            High -> Tr.Sandbox.Priority.high()
            Critical -> Tr.Sandbox.Priority.critical()
        }
    }
}
