package org.octavius.extension.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.octavius.api.contract.asian.PublicationAddRequest
import org.octavius.domain.asian.PublicationLanguage
import org.octavius.domain.asian.PublicationType
import org.octavius.extension.api.ApiClient
import org.octavius.extension.util.chrome
import org.octavius.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    // Stany dla danych zaciągniętych ze strony
    var titles by remember { mutableStateOf(listOf<String>()) }
    var detectedType by remember { mutableStateOf(PublicationType.Manga) } // Domyślna wartość
    var language by remember { mutableStateOf(PublicationLanguage.Japanese) } // Domyślna wartość

    // Stany dla UI
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<Pair<String, Boolean>?>(null) } // Pair<Wiadomość, czySukces>
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        // 1. Znajdź aktywną kartę
        val queryInfo = js("{ active: true, currentWindow: true }")
        chrome.tabs.query(queryInfo) { tabs ->
            val activeTab = tabs.firstOrNull()
            if (activeTab?.id == null) {
                println("aaaa")
                return@query
            }

            // 2. Wyślij wiadomość do content scriptu
            val message = js("{ action: 'parsePage' }")
            chrome.tabs.sendMessage(activeTab.id!!, message) { response ->
                if (js("typeof response === 'undefined'")) { // Bezpieczne sprawdzenie undefined
                    println("Błąd: Brak odpowiedzi od content scriptu.")
                    statusMessage = Pair("Nie można znaleźć aktywnej karty.", false)
                    return@sendMessage
                }

                // 3. Odbierz i przetwórz odpowiedź
                val parsedTitles = (response.titles as? Array<String>)?.toList() ?: emptyList()
                val parsedType =
                    (response.type as? String)?.let { PublicationType.valueOf(it) } ?: PublicationType.Manga
                val parsedLang = (response.language as? String)?.let { PublicationLanguage.valueOf(it) }
                    ?: PublicationLanguage.Japanese

                titles = parsedTitles
                detectedType = parsedType
                language = parsedLang
            }
        }
    }


    AppTheme(isDarkTheme = false) {
        Surface(modifier = Modifier.fillMaxSize().width(400.dp)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sekcja nagłówka (stała)
                Text("Octavius Helper", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Tytuły do dodania:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Sekcja listy tytułów (przewijalna)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()) // Dodajemy przewijanie
                        .padding(vertical = 8.dp)
                ) {
                    if (titles.isEmpty()) {
                        Text("Nie znaleziono tytułów na stronie...")
                    } else {
                        titles.forEach {
                            Text("- $it")
                        }
                    }
                }

                // Sekcja kontrolek (stała na dole)
                var selectedType by remember(detectedType) { mutableStateOf(detectedType) }
                OutlinedTextField(
                    value = selectedType.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Typ publikacji") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                Button(
                    onClick = {
                        isLoading = true
                        statusMessage = null
                        coroutineScope.launch {
                            val request = PublicationAddRequest(
                                titles = titles,
                                type = selectedType,
                                language = language
                            )
                            val response = ApiClient.addPublication(request)
                            statusMessage = Pair(response.message, response.success)
                            isLoading = false
                        }
                    },
                    enabled = titles.isNotEmpty() && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Dodaj do bazy")
                    }
                }

                statusMessage?.let { (message, isSuccess) ->
                    val color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    Text(message, color = color, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}