package com.bibliostudio.monfoyer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShoppingScreen(vm: MonFoyerViewModel) {
    var name by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    val checkedCount = vm.state.shopping.count { it.done }
    val favoriteItems = vm.state.shopping.filter { it.favorite }.distinctBy { it.name }.take(4)
    val sortedItems = vm.state.shopping.sortedWith(compareBy<ShoppingItem> { it.done }.thenBy { it.category }.thenBy { it.name })
    ModulePanel(title = "Liste de course") {
        item {
            QuickAdd(value = name, onChange = { name = it }, label = "Ajouter un article...") {
                vm.addShoppingItem(name)
                name = ""
            }
            if (favoriteItems.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    favoriteItems.forEach { item ->
                        TaskFilterChip(item.name, false) {
                            vm.addShoppingItem("${item.quantity} ${item.name}")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            if (checkedCount > 0) {
                SecondaryButton(text = "Supprimer elements coches ($checkedCount)", icon = Icons.Filled.Delete) {
                    confirmClear = true
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        if (sortedItems.isEmpty()) {
            item { EmptyState("🛒", "Liste vide", "Ajoute un article, avec une quantite si besoin : 2 lait.") }
        }
        items(sortedItems) { item ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(AppRadius),
                border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder),
                modifier = Modifier.fillMaxWidth().clickable { editingItem = item }
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.toggleShopping(item) }, modifier = Modifier.size(48.dp)) {
                        Icon(if (item.done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = "Etat", tint = if (item.done) DeepGreen else Color(0xFFDADADA), modifier = Modifier.size(34.dp))
                    }
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                        Text(item.name, fontSize = 24.sp, color = Ink, fontWeight = FontWeight.Bold)
                        Text("${item.quantity} x - ${item.category}", fontSize = 15.sp, color = Muted)
                    }
                    Text(
                        if (item.favorite) "★" else "☆",
                        color = if (item.favorite) Color(0xFFE8A64F) else Muted,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.clickable { vm.toggleShoppingFavorite(item) }.padding(horizontal = 8.dp)
                    )
                    DeleteButton { vm.delete("shoppingItems", item.id) }
                }
            }
        }
    }
    if (confirmClear) {
        ConfirmDeleteDialog(
            title = "Vider les elements coches ?",
            message = "$checkedCount article(s) coche(s) vont etre supprimes de la liste.",
            onConfirm = {
                vm.deleteCheckedShoppingItems()
                confirmClear = false
            },
            onDismiss = { confirmClear = false }
        )
    }
    editingItem?.let { item ->
        EditShoppingSheet(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { itemName, quantity, category ->
                vm.updateShoppingItem(item.id, itemName, quantity, category)
                editingItem = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditShoppingSheet(item: ShoppingItem, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember(item.id) { mutableStateOf(item.name) }
    var quantity by remember(item.id) { mutableStateOf(item.quantity.toString()) }
    var category by remember(item.id) { mutableStateOf(item.category) }
    EditSheetScaffold(title = "Modifier l'article", emoji = "🛒", onDismiss = onDismiss) {
        SoftInput(name, { name = it }, "Nom de l'article")
        Spacer(Modifier.height(10.dp))
        SoftInput(quantity, { quantity = it }, "Quantite", keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("Frais", "Epicerie", "Hygiene", "Maison").forEach { value ->
                TaskFilterChip(value, category == value) { category = value }
            }
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Enregistrer", Icons.Filled.CheckCircle) { onSave(name, quantity, category) }
    }
}
