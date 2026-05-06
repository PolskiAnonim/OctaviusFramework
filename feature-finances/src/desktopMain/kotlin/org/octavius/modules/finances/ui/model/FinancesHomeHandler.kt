package org.octavius.modules.finances.ui.model

import io.github.octaviusframework.db.api.DataAccess
import io.github.octaviusframework.db.api.DataResult
import io.github.octaviusframework.db.api.builder.toSingleOf
import io.github.octaviusframework.db.api.exception.DatabaseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager

class FinancesHomeHandler(
    private val scope: CoroutineScope
) : KoinComponent {
    private val dataAccess: DataAccess by inject()

    private val _state = MutableStateFlow(FinancesHomeState())
    val state = _state.asStateFlow()

    private fun getSql(): String {
        val totalBalanceSubquery = dataAccess.select("COALESCE(SUM(s.amount), 0)")
            .from("finances.splits s JOIN finances.accounts a ON a.id = s.account_id")
            .where("a.type IN ('ASSET', 'LIABILITY')")
            .toSql()

        val expensesMonthSubquery = dataAccess.select("COALESCE(SUM(s.amount), 0)")
            .from("finances.splits s JOIN finances.accounts a ON a.id = s.account_id JOIN finances.transactions t ON t.id = s.transaction_id")
            .where("a.type = 'EXPENSE' AND t.transaction_date >= date_trunc('month', now())")
            .toSql()

        val incomeMonthSubquery = dataAccess.select("ABS(COALESCE(SUM(s.amount), 0))")
            .from("finances.splits s JOIN finances.accounts a ON a.id = s.account_id JOIN finances.transactions t ON t.id = s.transaction_id")
            .where("a.type = 'INCOME' AND t.transaction_date >= date_trunc('month', now())")
            .toSql()

        val recentTransactionsInner = dataAccess.select("t.id, t.transaction_date as date, t.description, COALESCE(ABS(SUM(s.amount) FILTER (WHERE s.amount > 0)), 0) as amount")
            .from("finances.transactions t JOIN finances.splits s ON s.transaction_id = t.id")
            .groupBy("t.id, t.transaction_date, t.description")
            .orderBy("t.transaction_date DESC")
            .limit(5)
            .toSql()

        val recentTransactionsSubquery = dataAccess.select("COALESCE(array_agg(dynamic_dto('finances_dashboard_transaction', jsonb_build_object('id', id, 'date', date, 'description', description, 'amount', amount))), '{}')")
            .fromSubquery(recentTransactionsInner)
            .toSql()

        val topAccountsInner = dataAccess.select("a.id, a.name, COALESCE(SUM(s.amount), 0) as balance")
            .from("finances.accounts a LEFT JOIN finances.splits s ON s.account_id = a.id")
            .where("a.type IN ('ASSET', 'LIABILITY')")
            .groupBy("a.id, a.name")
            .orderBy("ABS(COALESCE(SUM(s.amount), 0)) DESC")
            .limit(5)
            .toSql()

        val topAccountsSubquery = dataAccess.select("COALESCE(array_agg(dynamic_dto('finances_dashboard_account', jsonb_build_object('id', id, 'name', name, 'balance', balance))), '{}')")
            .fromSubquery(topAccountsInner)
            .toSql()

        return """
            ($totalBalanceSubquery) AS total_balance,
            ($expensesMonthSubquery) AS expenses_month,
            ($incomeMonthSubquery) AS income_month,
            ($recentTransactionsSubquery) AS recent_transactions,
            ($topAccountsSubquery) AS top_accounts
        """.trimIndent()
    }

    fun loadData() {
        _state.update { it.copy(isLoading = true) }
        val sql = getSql()

        dataAccess.select(sql)
            .async(scope)
            .toSingleOf<DashboardData> { result ->
                when (result) {
                    is DataResult.Success -> {
                        val data = result.value
                        _state.update {
                            it.copy(
                                totalBalance = data.totalBalance,
                                expensesMonth = data.expensesMonth,
                                incomeMonth = data.incomeMonth,
                                recentTransactions = data.recentTransactions.orEmpty(),
                                topAccounts = data.topAccounts.orEmpty(),
                                isLoading = false
                            )
                        }
                    }
                    is DataResult.Failure -> {
                        showError(result.error)
                        _state.update { it.copy(isLoading = false) }
                    }
                }
            }
    }

    private fun showError(error: DatabaseException) {
        GlobalDialogManager.show(ErrorDialogConfig(error))
    }
}
