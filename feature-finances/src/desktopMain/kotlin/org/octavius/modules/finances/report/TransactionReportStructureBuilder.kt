package org.octavius.modules.finances.report

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import io.github.octaviusframework.db.api.QueryFragment
import org.octavius.localization.Tr
import org.octavius.modules.finances.form.transaction.ui.TransactionFormScreen
import org.octavius.navigation.AppRouter
import org.octavius.report.ReportMainAction
import org.octavius.report.ReportRowAction
import org.octavius.report.column.ReportColumn
import org.octavius.report.column.type.BigDecimalColumn
import org.octavius.report.column.type.InstantColumn
import org.octavius.report.column.type.StringColumn
import org.octavius.report.component.ReportStructureBuilder

class TransactionReportStructureBuilder : ReportStructureBuilder() {

    override fun getReportName(): String = "transactions"

    override fun buildQuery(): QueryFragment {
        return QueryFragment(
            dataAccess.select(
                "t.id",
                "t.transaction_date",
                "t.description",
                "(SELECT SUM(amount) FROM finances.splits s WHERE s.transaction_id = t.id AND s.amount > 0) AS total_amount"
            ).from("finances.transactions t")
            .toSql()
        )
    }

    override fun buildColumns(): Map<String, ReportColumn> = mapOf(
        "transaction_date" to InstantColumn(Tr.Finances.Transaction.date()),
        "description" to StringColumn(Tr.Finances.Transaction.description()),
        "total_amount" to BigDecimalColumn(Tr.Finances.Transaction.amount())
    )

    override fun buildDefaultRowAction(): ReportRowAction = ReportRowAction(
        Tr.Report.Actions.edit()
    ) {
        val id = rowData["id"] as? Long
        if (id != null) {
            AppRouter.navigateTo(TransactionFormScreen.create(entityId = id))
        }
    }

    override fun buildMainActions(): List<ReportMainAction> = listOf(
        ReportMainAction(Tr.Finances.Transaction.new(), Icons.Default.Add) {
            AppRouter.navigateTo(TransactionFormScreen.create())
        }
    )
}
