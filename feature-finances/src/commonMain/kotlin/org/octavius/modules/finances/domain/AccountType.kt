package org.octavius.modules.finances.domain

import io.github.octaviusframework.db.api.annotation.PgEnum
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.Tr

@PgEnum
enum class AccountType : EnumWithFormatter<AccountType> {
    ASSET,
    LIABILITY,
    INCOME,
    EXPENSE,
    EQUITY;

    override fun toDisplayString(): String {
        return when (this) {
            ASSET -> Tr.Finances.AccountType.asset()
            LIABILITY -> Tr.Finances.AccountType.liability()
            INCOME -> Tr.Finances.AccountType.income()
            EXPENSE -> Tr.Finances.AccountType.expense()
            EQUITY -> Tr.Finances.AccountType.equity()
        }
    }
}
