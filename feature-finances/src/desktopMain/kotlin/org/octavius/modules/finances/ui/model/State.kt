package org.octavius.modules.finances.ui.model

import io.github.octaviusframework.db.api.annotation.DynamicallyMappable
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

data class FinancesHomeState(
    val totalBalance: BigDecimal = BigDecimal.ZERO,
    val expensesMonth: BigDecimal = BigDecimal.ZERO,
    val incomeMonth: BigDecimal = BigDecimal.ZERO,
    val recentTransactions: List<DashboardTransaction> = emptyList(),
    val topAccounts: List<DashboardAccount> = emptyList(),
    val isLoading: Boolean = true
)

@Serializable
@DynamicallyMappable("finances_dashboard_transaction")
data class DashboardTransaction(
    val id: Long,
    val date: String,
    val description: String,
    @Contextual
    val amount: BigDecimal
)

@Serializable
@DynamicallyMappable("finances_dashboard_account")
data class DashboardAccount(
    val id: Int,
    val name: String,
    @Contextual
    val balance: BigDecimal
)

@Serializable
internal data class DashboardData(
    @Contextual
    val totalBalance: BigDecimal,
    @Contextual
    val expensesMonth: BigDecimal,
    @Contextual
    val incomeMonth: BigDecimal,
    val recentTransactions: List<DashboardTransaction>? = null,
    val topAccounts: List<DashboardAccount>? = null
)
