package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.column.ReportColumn

class EnumColumn(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    private val formatter: (Enum<*>?) -> String = {
        it?.let {
            try {
                val method = it::class.members.find { member -> member.name == "toDisplayString" }
                if (method != null) {
                    method.call(it) as String
                } else {
                    it.toString()
                }
            } catch (e: Exception) {
                it.toString()
            }
        } ?: ""
    }
) : ReportColumn(name, header, width, sortable) {

    @Composable
    override fun RenderCell(item: Map<String, Any?>, modifier: Modifier) {
        val value = item[name] as? Enum<*>

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = formatter(value),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
