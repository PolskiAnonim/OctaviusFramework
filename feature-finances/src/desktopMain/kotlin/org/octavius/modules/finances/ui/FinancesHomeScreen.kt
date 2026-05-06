package org.octavius.modules.finances.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.octavius.localization.Tr
import org.octavius.modules.finances.form.transaction.ui.TransactionFormScreen
import org.octavius.modules.finances.report.AccountReportStructureBuilder
import org.octavius.modules.finances.report.TransactionReportStructureBuilder
import org.octavius.modules.finances.ui.model.DashboardAccount
import org.octavius.modules.finances.ui.model.DashboardTransaction
import org.octavius.modules.finances.ui.model.FinancesHomeHandler
import org.octavius.modules.finances.ui.model.FinancesHomeState
import org.octavius.navigation.AppRouter
import org.octavius.navigation.Screen
import org.octavius.report.component.ReportHandler
import org.octavius.report.component.ReportScreen
import java.math.BigDecimal

class FinancesHomeScreen : Screen {
    override val title: String get() = Tr.Finances.Home.title()

    @Composable
    override fun Content() {
        val scope = rememberCoroutineScope()
        val handler = remember(scope) { FinancesHomeHandler(scope) }
        val state by handler.state.collectAsState()

        LaunchedEffect(Unit) {
            handler.loadData()
        }

        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text(Tr.Finances.Transaction.new()) },
                    icon = { Icon(Icons.Default.Add, null) },
                    onClick = { AppRouter.navigateTo(TransactionFormScreen.create()) }
                )
            }
        ) { padding ->
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item { StatsHeader(state) }

                    item {
                        QuickAccessButtons()
                    }

                    item {
                        RecentTransactionsList(state.recentTransactions)
                    }

                    item {
                        TopAccountsList(state.topAccounts)
                    }
                }
            }
        }
    }

    @Composable
    private fun StatsHeader(state: FinancesHomeState) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                Tr.Finances.Home.totalBalance(),
                formatAmount(state.totalBalance),
                Modifier.weight(1f),
                MaterialTheme.colorScheme.primary
            )
            StatCard(
                Tr.Finances.Home.expensesThisMonth(),
                formatAmount(state.expensesMonth),
                Modifier.weight(1f),
                MaterialTheme.colorScheme.error
            )
            StatCard(
                Tr.Finances.Home.incomeThisMonth(),
                formatAmount(state.incomeMonth),
                Modifier.weight(1f),
                MaterialTheme.colorScheme.tertiary
            )
        }
    }

    @Composable
    private fun StatCard(title: String, value: String, modifier: Modifier, color: androidx.compose.ui.graphics.Color) {
        Card(modifier = modifier) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(text = value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
                Text(text = title, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    @Composable
    private fun QuickAccessButtons() {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val reportHandler = ReportHandler(TransactionReportStructureBuilder())
                    AppRouter.navigateTo(ReportScreen(Tr.Finances.Tabs.transactions(), reportHandler))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.Article, null)
                Spacer(Modifier.width(8.dp))
                Text(Tr.Finances.Home.viewAllTransactions())
            }

            OutlinedButton(
                onClick = {
                    val reportHandler = ReportHandler(AccountReportStructureBuilder())
                    AppRouter.navigateTo(ReportScreen(Tr.Finances.Tabs.accounts(), reportHandler))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Filled.Article, null)
                Spacer(Modifier.width(8.dp))
                Text(Tr.Finances.Home.viewAllAccounts())
            }
        }
    }

    @Composable
    private fun RecentTransactionsList(transactions: List<DashboardTransaction>) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Tr.Finances.Home.recentTransactions(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (transactions.isEmpty()) {
                Text(Tr.Finances.Home.noTransactions(), style = MaterialTheme.typography.bodyMedium)
            } else {
                Card {
                    Column {
                        transactions.forEachIndexed { index, transaction ->
                            ListItem(
                                headlineContent = { Text(transaction.description) },
                                supportingContent = { Text(transaction.date) },
                                trailingContent = {
                                    Text(
                                        formatAmount(transaction.amount),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                modifier = Modifier.clickable {
                                    AppRouter.navigateTo(TransactionFormScreen.create(transaction.id))
                                }
                            )
                            if (index < transactions.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TopAccountsList(accounts: List<DashboardAccount>) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Tr.Finances.Home.accounts(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (accounts.isEmpty()) {
                Text(Tr.Finances.Home.noAccounts(), style = MaterialTheme.typography.bodyMedium)
            } else {
                Card {
                    Column {
                        accounts.forEachIndexed { index, account ->
                            ListItem(
                                headlineContent = { Text(account.name) },
                                trailingContent = {
                                    Text(
                                        formatAmount(account.balance),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (account.balance >= BigDecimal.ZERO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                            if (index < accounts.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun formatAmount(amount: BigDecimal): String {
        return "${amount.setScale(2, java.math.RoundingMode.HALF_UP)} PLN"
    }
}
