package org.octavius.modules.finances.form.transaction

import io.github.octaviusframework.db.api.DataResult
import io.github.octaviusframework.db.api.builder.execute
import io.github.octaviusframework.db.api.builder.toField
import io.github.octaviusframework.db.api.transaction.TransactionPlan
import io.github.octaviusframework.db.api.transaction.TransactionValue
import io.github.octaviusframework.db.api.transaction.toTransactionValue
import io.github.octaviusframework.db.api.type.PgStandardType
import io.github.octaviusframework.db.api.type.withPgType
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.*
import org.octavius.form.control.type.repeatable.RepeatableResultValue
import java.math.BigDecimal

class TransactionFormDataManager : FormDataManager() {

    private fun loadTransactionData(loadedId: Any?) = loadData(loadedId) {
        from("finances.transactions", "t")
        map("id")
        map("date", "transaction_date")
        map("description")

        mapRelatedList("splits") {
            from("finances.splits", "s")
            linkedBy("s.transaction_id")
            map("id")
            map("accountId", "account_id")
            map("amount")
        }
    }

    override fun initData(payload: Map<String, Any?>): Map<String, Any?> {
        val id = payload["id"]
        val loadedData = loadTransactionData(id)
        
        val defaultData = if (id == null) {
            mapOf(
                "splits" to emptyList<Map<String, Any?>>()
            )
        } else {
            emptyMap()
        }
        
        return defaultData + loadedData + payload
    }

    override fun definedFormActions(): Map<String, (FormResultData) -> FormActionResult> {
        return mapOf(
            "save" to { formData -> processSave(formData) },
            "cancel" to { _ -> FormActionResult.CloseScreen }
        )
    }

    private fun processSave(formResultData: FormResultData): FormActionResult {
        val plan = TransactionPlan()
        val loadedId = formResultData.getInitial("id")
        
        val transactionIdRef: TransactionValue<Long>
        val transactionData = mapOf(
            "transaction_date" to formResultData.getCurrent("date"),
            "description" to formResultData.getCurrent("description")
        )

        if (loadedId != null) {
            transactionIdRef = (loadedId as Long).toTransactionValue()
            plan.add(
                dataAccess.update("finances.transactions")
                    .setValues(transactionData)
                    .where("id = @id")
                    .asStep()
                    .execute(transactionData + mapOf("id" to transactionIdRef))
            )
        } else {
            transactionIdRef = plan.add(
                dataAccess.insertInto("finances.transactions")
                    .values(transactionData)
                    .returning("id")
                    .asStep()
                    .toField<Long>(transactionData)
            ).field()
        }

        val splitsResult = formResultData.getCurrentAs<RepeatableResultValue>("splits")

        // In a simple accounting system, we can just delete all old splits and insert new ones
        if (loadedId != null) {
            plan.add(
                dataAccess.deleteFrom("finances.splits")
                    .where("transaction_id = @tid")
                    .asStep()
                    .execute(mapOf("tid" to transactionIdRef))
            )
        }

        // Insert all current splits
        splitsResult.allCurrentRows.forEach { rowData ->
            val splitData = mapOf(
                "transaction_id" to transactionIdRef,
                "account_id" to rowData.getCurrent("accountId"),
                "amount" to rowData.getCurrent("amount")
            )
            plan.add(
                dataAccess.insertInto("finances.splits")
                    .values(splitData)
                    .asStep()
                    .execute(splitData)
            )
        }

        return when (val result = dataAccess.executeTransactionPlan(plan)) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                FormActionResult.Failure
            }
            is DataResult.Success -> FormActionResult.CloseScreen
        }
    }
}
