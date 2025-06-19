package org.octavius.report.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PaginationComponent(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    pageSize: Int = 20,
    onPageSizeChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var pageInputValue by remember(currentPage) { 
        mutableStateOf(TextFieldValue(
            text = (currentPage + 1).toString(),
            selection = TextRange((currentPage + 1).toString().length)
        ))
    }
    var isEditingPage by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.height(60.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Przycisk poprzedniej strony
            IconButton(
                onClick = {
                    if (currentPage > 0) {
                        onPageChange(currentPage - 1)
                    }
                },
                enabled = currentPage > 0
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Poprzednia strona",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Wyświetlanie aktualnej strony z możliwością edycji
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Strona ",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (isEditingPage) {
                    BasicTextField(
                        value = pageInputValue,
                        onValueChange = { newValue ->
                            // Pozwól tylko na cyfry
                            if (newValue.text.all { char -> char.isDigit() }) {
                                pageInputValue = newValue.copy(
                                    selection = TextRange(newValue.text.length)
                                )
                            }
                        },
                        modifier = Modifier
                            .width(40.dp)
                            .height(36.dp)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.small
                            )
                            .padding(4.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val newPage = pageInputValue.text.toIntOrNull()
                                if (newPage != null && newPage > 0 && newPage <= totalPages) {
                                    onPageChange(newPage - 1) // Konwersja z 1-indexed na 0-indexed
                                } else {
                                    // Przywróć poprzednią wartość jeśli wprowadzono nieprawidłową
                                    pageInputValue = TextFieldValue(
                                        text = (currentPage + 1).toString(),
                                        selection = TextRange((currentPage + 1).toString().length)
                                    )
                                }
                                isEditingPage = false
                            }
                        )
                    )

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                    
                } else {
                    TextButton(
                        onClick = { 
                            isEditingPage = true
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text("${currentPage + 1}")
                    }
                }

                Text(
                    text = " z $totalPages",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Selektor rozmiaru strony (jeśli onPageSizeChange jest dostępne)
            if (onPageSizeChange != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Rozmiar:",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    val pageSizeOptions = listOf(10, 20, 50, 100)
                    var expanded by remember { mutableStateOf(false) }
                    
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.width(60.dp).height(32.dp),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text(
                                text = pageSize.toString(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            pageSizeOptions.forEach { size ->
                                DropdownMenuItem(
                                    text = { Text(size.toString()) },
                                    onClick = {
                                        onPageSizeChange(size)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Przycisk następnej strony
            IconButton(
                onClick = {
                    if (currentPage < totalPages - 1) {
                        onPageChange(currentPage + 1)
                    }
                },
                enabled = currentPage < totalPages - 1
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Następna strona",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}