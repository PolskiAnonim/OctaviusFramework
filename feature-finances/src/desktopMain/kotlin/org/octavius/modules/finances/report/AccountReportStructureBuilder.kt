package org.octavius.modules.finances.report

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import io.github.octaviusframework.db.api.QueryFragment
import org.octavius.localization.Tr
import org.octavius.modules.finances.domain.AccountType
import org.octavius.modules.finances.form.account.ui.AccountFormScreen
import org.octavius.navigation.AppRouter
import org.octavius.report.ReportMainAction
import org.octavius.report.ReportRowAction
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.EnumColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportStructureBuilder

class AccountReportStructureBuilder : ReportStructureBuilder() {

    override fun getReportName(): String = "accounts"

    override fun buildQuery(): QueryFragment {
        return QueryFragment(
            dataAccess.select(
                "a.id",
                "a.name",
                "a.type",
                "a.currency",
                "p.name AS parent_name"
            ).from("finances.accounts a LEFT JOIN finances.accounts p ON p.id = a.parent_id")
            .toSql()
        )
    }

    override fun buildColumns(): Map<String, ReportColumn> = mapOf(
        "name" to StringColumn(Tr.Finances.Account.name()),
        "type" to EnumColumn(header = Tr.Finances.Account.type(), enumClass = AccountType::class),
        "currency" to StringColumn(Tr.Finances.Account.currency()),
        "parent_name" to StringColumn(Tr.Finances.Account.parent())
    )

    override fun buildDefaultRowAction(): ReportRowAction = ReportRowAction(
        Tr.Report.Actions.edit()
    ) {
        val id = rowData["id"] as? Int
        if (id != null) {
            AppRouter.navigateTo(AccountFormScreen.create(entityId = id))
        }
    }

    override fun buildMainActions(): List<ReportMainAction> = listOf(
        ReportMainAction(Tr.Finances.Account.new(), Icons.Default.Add) {
            AppRouter.navigateTo(AccountFormScreen.create())
        }
    )
}
