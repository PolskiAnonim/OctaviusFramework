package org.octavius.modules.activity.report

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import org.octavius.data.QueryFragment
import org.octavius.localization.Tr
import org.octavius.modules.activity.form.category.ui.CategoryFormScreen
import org.octavius.navigation.AppRouter
import org.octavius.report.ReportMainAction
import org.octavius.report.ReportRowAction
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.LongColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportStructureBuilder

class CategoriesReportStructureBuilder : ReportStructureBuilder() {

    override fun getReportName(): String = "activity_categories"

    override fun buildQuery(): QueryFragment = QueryFragment(
        sql = """
            SELECT
                c.id,
                c.name,
                c.color,
                c.icon,
                p.name as parent_name,
                COALESCE(r.rule_count, 0) as rule_count,
                COALESCE(a.activity_count, 0) as activity_count
            FROM activity_tracker.categories c
            LEFT JOIN activity_tracker.categories p ON c.parent_id = p.id
            LEFT JOIN (
                SELECT category_id, COUNT(*) as rule_count
                FROM activity_tracker.categorization_rules
                GROUP BY category_id
            ) r ON c.id = r.category_id
            LEFT JOIN (
                SELECT category_id, COUNT(*) as activity_count
                FROM activity_tracker.activity_log
                GROUP BY category_id
            ) a ON c.id = a.category_id
        """.trimIndent()
    )

    override fun buildColumns(): Map<String, ReportColumn> = mapOf(
        "name" to StringColumn(
            header = Tr.ActivityTracker.Category.name()
        ),
        "color" to StringColumn(
            header = Tr.ActivityTracker.Category.color()
        ),
        "icon" to StringColumn(
            header = Tr.ActivityTracker.Category.icon()
        ),
        "parent_name" to StringColumn(
            header = Tr.ActivityTracker.Category.parentCategory()
        ),
        "rule_count" to LongColumn(
            header = Tr.ActivityTracker.Category.ruleCount()
        ),
        "activity_count" to LongColumn(
            header = Tr.ActivityTracker.Category.activityCount()
        )
    )

    override fun buildDefaultRowAction(): ReportRowAction = ReportRowAction(
        label = Tr.ActivityTracker.Form.editCategory(),
        icon = Icons.Default.Edit
    ) {
        val categoryId = rowData["id"] as? Int
        if (categoryId != null) {
            AppRouter.navigateTo(CategoryFormScreen.create(categoryId))
        }
    }

    override fun buildMainActions(): List<ReportMainAction> = listOf(
        ReportMainAction(
            label = Tr.ActivityTracker.Form.newCategory(),
            icon = Icons.Default.Add
        ) {
            AppRouter.navigateTo(CategoryFormScreen.create())
        }
    )
}
