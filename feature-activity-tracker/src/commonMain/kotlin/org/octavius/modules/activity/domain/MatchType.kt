package org.octavius.modules.activity.domain

import org.octavius.data.annotation.PgEnum
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.Tr

@PgEnum
enum class MatchType : EnumWithFormatter<MatchType> {
    ProcessName,
    WindowTitle,
    Regex,
    PathContains;

    override fun toDisplayString(): String {
        return when (this) {
            ProcessName -> Tr.ActivityTracker.MatchType.processName()
            WindowTitle -> Tr.ActivityTracker.MatchType.windowTitle()
            Regex -> Tr.ActivityTracker.MatchType.regex()
            PathContains -> Tr.ActivityTracker.MatchType.pathContains()
        }
    }
}
