package org.octavius.modules.activity.report

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import org.octavius.data.QueryFragment
import org.octavius.localization.Tr
import org.octavius.modules.activity.domain.MatchType
import org.octavius.modules.activity.form.rule.ui.RuleFormScreen
import org.octavius.navigation.AppRouter
import org.octavius.report.ReportMainAction
import org.octavius.report.ReportRowAction
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.BooleanColumn
import org.octavius.report.column.type.EnumColumn
import org.octavius.report.column.type.IntegerColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportStructureBuilder

class RulesReportStructureBuilder : ReportStructureBuilder() {

    override fun getReportName(): String = "activity_rules"

    override fun buildQuery(): QueryFragment = QueryFragment(
        sql = """
            SELECT
                r.id,
                c.name as category_name,
                r.match_type,
                r.pattern,
                r.priority,
                r.is_active
            FROM activity_tracker.categorization_rules r
            JOIN activity_tracker.categories c ON r.category_id = c.id
        """.trimIndent()
    )

    override fun buildColumns(): Map<String, ReportColumn> = mapOf(
        "category_name" to StringColumn(
            header = Tr.ActivityTracker.Rule.category()
        ),
        "match_type" to EnumColumn(
            header = Tr.ActivityTracker.Rule.matchType(),
            enumClass = MatchType::class
        ),
        "pattern" to StringColumn(
            header = Tr.ActivityTracker.Rule.pattern()
        ),
        "priority" to IntegerColumn(
            header = Tr.ActivityTracker.Rule.priority()
        ),
        "is_active" to BooleanColumn(
            header = Tr.ActivityTracker.Rule.isActive()
        )
    )

    override fun buildDefaultRowAction(): ReportRowAction = ReportRowAction(
        label = Tr.ActivityTracker.Form.editRule(),
        icon = Icons.Default.Edit
    ) {
        val ruleId = rowData["id"] as? Int
        if (ruleId != null) {
            AppRouter.navigateTo(RuleFormScreen.create(ruleId))
        }
    }

    override fun buildMainActions(): List<ReportMainAction> = listOf(
        ReportMainAction(
            label = Tr.ActivityTracker.Form.newRule(),
            icon = Icons.Default.Add
        ) {
            AppRouter.navigateTo(RuleFormScreen.create())
        }
    )
}
