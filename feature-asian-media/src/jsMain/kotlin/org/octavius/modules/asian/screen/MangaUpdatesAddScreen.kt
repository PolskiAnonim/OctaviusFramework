package org.octavius.modules.asian.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.octavius.api.contract.asian.PublicationAddRequest
import org.octavius.modules.asian.api.ApiClient
import org.octavius.modules.asian.model.AsianPublicationData
import org.octavius.navigation.Screen

/**
 * Ekran dedykowany do wyświetlania danych sparsowanych ze strony MangaUpdates
 * i umożliwiający dodanie ich do bazy danych Octavius.
 *
 * @param data Sprawdzone i zdeserializowane dane publikacji.
 */
class MangaUpdatesAddScreen(private val data: AsianPublicationData) : Screen {
    override val title = "mangaUpdatesAddScreen"

    @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        override fun Content() {
            // Stany dla UI - specyficzne dla tego ekranu
            var isLoading by remember { mutableStateOf(false) }
            var statusMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // Pair<Wiadomość, czySukces>
            val coroutineScope = rememberCoroutineScope()

            // Nie potrzebujemy już `LaunchedEffect` do pobierania danych,
            // ponieważ dane (`data`) są przekazywane w konstruktorze.

            // Układ UI - praktycznie skopiowany z Twojego App()
        Column(
            modifier = Modifier.Companion.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.Companion.CenterHorizontally
        ) {
            // Sekcja nagłówka (stała)
            Text("Octavius Helper", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Znalezione tytuły (${data.source}):", // Dodajemy informację o źródle
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.Companion.padding(top = 8.dp)
            )

            // Sekcja listy tytułów (przewijalna)
            Column(
                modifier = Modifier.Companion
                    .weight(1f) // Zajmuje dostępną przestrzeń
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                if (data.titles.isEmpty()) {
                    Text("Nie znaleziono tytułów na stronie...")
                } else {
                    data.titles.forEach { title ->
                        Text("- $title")
                    }
                }
            }

            // Sekcja kontrolek (stała na dole)

            // Typ publikacji jest już wykryty i przekazany w `data`
            OutlinedTextField(
                value = data.type.toString(), // Używamy danych z obiektu
                onValueChange = {},
                readOnly = true,
                label = { Text("Wykryty typ publikacji") },
                modifier = Modifier.Companion.fillMaxWidth().padding(vertical = 8.dp)
            )

            // Język publikacji również jest już wykryty
            OutlinedTextField(
                value = data.language.toString(), // Używamy danych z obiektu
                onValueChange = {},
                readOnly = true,
                label = { Text("Wykryty język") },
                modifier = Modifier.Companion.fillMaxWidth().padding(bottom = 8.dp)
            )

            Button(
                onClick = {
                    isLoading = true
                    statusMessage = null
                    coroutineScope.launch {
                        // Tworzymy request na podstawie danych przekazanych do ekranu
                        val request = PublicationAddRequest(
                            titles = data.titles,
                            type = data.type,
                            language = data.language
                        )
                        val response = ApiClient.addPublication(request)
                        statusMessage = Pair(response.message, response.success)
                        isLoading = false
                    }
                },
                enabled = data.titles.isNotEmpty() && !isLoading,
                modifier = Modifier.Companion.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.Companion.size(24.dp))
                } else {
                    Text("Dodaj do bazy")
                }
            }

            // Wyświetlanie statusu operacji
            statusMessage?.let { (message, isSuccess) ->
                val color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text(message, color = color, modifier = Modifier.Companion.padding(top = 8.dp))
            }
        }
        }
}