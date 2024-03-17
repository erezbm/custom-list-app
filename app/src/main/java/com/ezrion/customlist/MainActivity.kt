package com.ezrion.customlist

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ezrion.customlist.ui.theme.CustomListTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.listItemsDataStore: DataStore<Preferences> by preferencesDataStore(name = "list_items")
val LIST_ITEMS_KEY = stringPreferencesKey("list_items")

// list items repository
class ListItemsRepository(private val dataStore: DataStore<Preferences>) {
    val data = dataStore.data.map { preferences ->
        Json.decodeFromString<List<Item>>(
            preferences[LIST_ITEMS_KEY] ?: "[]"
        )
    }

    suspend fun setData(items: List<Item>) {
        dataStore.edit { preferences ->
            preferences[LIST_ITEMS_KEY] = Json.encodeToString(items)
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CustomListTheme {
                CustomList(ListItemsRepository(listItemsDataStore))
            }
        }
    }
}

@Serializable
data class Item(val title: String, val description: String = "")

@Composable
fun CustomList(itemsRepository: ListItemsRepository) {
    val items = itemsRepository.data.collectAsState(initial = emptyList()).value
    var editItem by remember { mutableStateOf(Item("")) }
    var editIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                itemsRepository.setData(items.plus(Item("Item ${items.size + 1}")))
            }
        }) {
            Text("+")
        }
    }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            items.forEachIndexed { index, item ->
                ListItem(
                    headlineContent = { Text(item.title) },
                    modifier = Modifier.clickable {
                        editItem = item
                        editIndex = index
                    }
                )
            }

            // Dialog to edit an item
            if (editIndex != null) {
                Dialog(
                    onDismissRequest = { editIndex = null },
                ) {
                    // card to edit an item
                    Card {
                        Column {
                            TextField(
                                value = editItem.title,
                                onValueChange = { newValue ->
                                    editItem = editItem.copy(title = newValue)
                                },
                                label = { Text("Title") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            TextField(
                                value = editItem.description,
                                onValueChange = { newValue ->
                                    editItem = editItem.copy(description = newValue)
                                },
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row {
                                // cancel button
                                IconButton(onClick = { editIndex = null }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Cancel")
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                // save button
                                IconButton(onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        itemsRepository.setData(
                                            items.toMutableList().apply {
                                                this[editIndex!!] = editItem
                                            }
                                        )
                                        editIndex = null
                                    }
                                }) {
                                    Icon(Icons.Filled.Done, contentDescription = "Save")
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                // delete button
                                IconButton(onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        itemsRepository.setData(
                                            items.toMutableList().apply {
                                                this.removeAt(editIndex!!)
                                            }
                                        )
                                        editIndex = null
                                    }
                                }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomListPreview() {
    CustomListTheme {
//        CustomList()
    }
}