package com.bibliostudio.monfoyer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RECIPE_EMOJIS = listOf("🍝","🍕","🥗","🍲","🥘","🍜","🍛","🥙","🫕","🍣","🥩","🐟","🥞","🍰","🍮","🫔","🥣","🫙","🥧","🍱")

@Composable
fun RecipesScreen(vm: MonFoyerViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<Recipe?>(null) }
    var toDelete by remember { mutableStateOf<Recipe?>(null) }
    val recipes = vm.state.recipes.sortedBy { it.title }

    ModulePanel(title = "Recettes") {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("${recipes.size} recette(s)", fontSize = 15.sp, color = Muted, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(52.dp).clickable { showAdd = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        if (recipes.isEmpty()) {
            item { EmptyState("🍽️", "Aucune recette", "Ajoute les recettes du foyer et envoie les ingredients directement aux courses.") }
        } else {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    userScrollEnabled = false,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(((recipes.size + 1) / 2 * 148).dp)
                ) {
                    gridItems(recipes) { recipe ->
                        RecipeCard(recipe = recipe, onClick = { detail = recipe }, onDelete = { toDelete = recipe })
                    }
                }
            }
        }
    }

    detail?.let { recipe ->
        RecipeDetailSheet(
            recipe = recipe,
            onDismiss = { detail = null },
            onAddToShopping = { vm.addRecipeIngredientsToShopping(recipe); detail = null }
        )
    }
    toDelete?.let { recipe ->
        ConfirmDeleteDialog(
            title = "Supprimer cette recette ?",
            message = "\"${recipe.title}\" sera supprimee du carnet.",
            onConfirm = { vm.deleteRecipe(recipe.id); toDelete = null },
            onDismiss = { toDelete = null }
        )
    }
    if (showAdd) {
        AddRecipeSheet(
            onDismiss = { showAdd = false },
            onAdd = { title, emoji, description, ingredients, steps, servings, prepMinutes ->
                vm.addRecipe(title, emoji, description, ingredients, steps, servings, prepMinutes)
                showAdd = false
            }
        )
    }
}

@Composable
fun RecipeCard(recipe: Recipe, onClick: () -> Unit, onDelete: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = Muted.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
            }
            Text(recipe.emoji.ifBlank { "🍽️" }, fontSize = 44.sp)
            Spacer(Modifier.height(6.dp))
            Text(recipe.title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.Center) {
                if (recipe.servings > 0) Text("👥 ${recipe.servings}", fontSize = 12.sp, color = Muted, fontWeight = FontWeight.Bold)
                if (recipe.prepMinutes > 0) { Text("  ⏱ ${recipe.prepMinutes}min", fontSize = 12.sp, color = Muted, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailSheet(recipe: Recipe, onDismiss: () -> Unit, onAddToShopping: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(recipe.emoji.ifBlank { "🍽️" }, fontSize = 40.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(recipe.title, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Ink, lineHeight = 28.sp)
                    if (recipe.addedByName.isNotBlank()) Text("Par ${recipe.addedByName}", fontSize = 13.sp, color = Muted, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (recipe.servings > 0) Surface(color = SoftGrey, shape = RoundedCornerShape(50)) { Text("👥 ${recipe.servings} pers.", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }
                if (recipe.prepMinutes > 0) Surface(color = SoftGrey, shape = RoundedCornerShape(50)) { Text("⏱ ${recipe.prepMinutes} min", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) }
            }
            if (recipe.description.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(recipe.description, fontSize = 15.sp, color = Muted, lineHeight = 21.sp)
            }
            if (recipe.ingredients.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Ingredients", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                Spacer(Modifier.height(8.dp))
                recipe.ingredients.filter { it.isNotBlank() }.forEach { ingredient ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(8.dp)) {}
                        Spacer(Modifier.width(10.dp))
                        Text(ingredient, fontSize = 16.sp, color = Ink)
                    }
                }
            }
            if (recipe.steps.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Preparation", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                Spacer(Modifier.height(8.dp))
                recipe.steps.filter { it.isNotBlank() }.forEachIndexed { index, step ->
                    Row(Modifier.padding(vertical = 6.dp)) {
                        Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(26.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("${index + 1}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(step, fontSize = 15.sp, color = Ink, lineHeight = 21.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
            if (recipe.ingredients.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SecondaryButton("Ajouter aux courses", Icons.Filled.ShoppingCart, onAddToShopping)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeSheet(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, List<String>, List<String>, Int, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🍽️") }
    var description by remember { mutableStateOf("") }
    var ingredients by remember { mutableStateOf(listOf("", "")) }
    var steps by remember { mutableStateOf(listOf("")) }
    var servings by remember { mutableStateOf("2") }
    var prepMinutes by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val canAdd = title.isNotBlank()

    EditSheetScaffold(title = "Nouvelle recette", emoji = emoji, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = SoftGrey, shape = CircleShape, modifier = Modifier.size(52.dp).clickable { showEmojiPicker = !showEmojiPicker }) {
                Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 26.sp) }
            }
            Spacer(Modifier.width(12.dp))
            SoftInput(title, { title = it }, "Nom de la recette...", modifier = Modifier.weight(1f))
        }
        if (showEmojiPicker) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                RECIPE_EMOJIS.take(10).forEach { e ->
                    Surface(color = if (emoji == e) DeepGreen else SoftGrey, shape = CircleShape, modifier = Modifier.size(38.dp).clickable { emoji = e; showEmojiPicker = false }) {
                        Box(contentAlignment = Alignment.Center) { Text(e, fontSize = 18.sp) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                RECIPE_EMOJIS.drop(10).take(10).forEach { e ->
                    Surface(color = if (emoji == e) DeepGreen else SoftGrey, shape = CircleShape, modifier = Modifier.size(38.dp).clickable { emoji = e; showEmojiPicker = false }) {
                        Box(contentAlignment = Alignment.Center) { Text(e, fontSize = 18.sp) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftInput(description, { description = it }, "Description (optionnel)...", minLines = 2)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                FieldLabel("Personnes")
                SoftInput(servings, { servings = it }, "2", keyboardType = KeyboardType.Number)
            }
            Column(Modifier.weight(1f)) {
                FieldLabel("Prep (min)")
                SoftInput(prepMinutes, { prepMinutes = it }, "30", keyboardType = KeyboardType.Number)
            }
        }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Ingredients")
        ingredients.forEachIndexed { index, ingredient ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                SoftInput(ingredient, { newVal -> ingredients = ingredients.toMutableList().also { it[index] = newVal } }, "Ex: 200g farine", modifier = Modifier.weight(1f))
                IconButton(onClick = { if (ingredients.size > 1) ingredients = ingredients.toMutableList().also { it.removeAt(index) } }) {
                    Icon(Icons.Filled.Close, contentDescription = "Retirer", tint = Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
        Surface(color = SoftGrey, shape = RoundedCornerShape(12.dp), modifier = Modifier.clickable { ingredients = ingredients + "" }) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = DeepGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ajouter un ingredient", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepGreen)
            }
        }
        Spacer(Modifier.height(14.dp))
        FieldLabel("Etapes")
        steps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 8.dp)) {
                Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(28.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("${index + 1}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black) }
                }
                Spacer(Modifier.width(10.dp))
                SoftInput(step, { newVal -> steps = steps.toMutableList().also { it[index] = newVal } }, "Etape ${index + 1}...", minLines = 2, modifier = Modifier.weight(1f))
                IconButton(onClick = { if (steps.size > 1) steps = steps.toMutableList().also { it.removeAt(index) } }) {
                    Icon(Icons.Filled.Close, contentDescription = "Retirer", tint = Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
        Surface(color = SoftGrey, shape = RoundedCornerShape(12.dp), modifier = Modifier.clickable { steps = steps + "" }) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = DeepGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ajouter une etape", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DeepGreen)
            }
        }
        Spacer(Modifier.height(22.dp))
        PrimaryButton("Enregistrer la recette", Icons.Filled.Check, enabled = canAdd) {
            onAdd(title.trim(), emoji, description.trim(), ingredients.filter { it.isNotBlank() }, steps.filter { it.isNotBlank() }, servings.toIntOrNull() ?: 2, prepMinutes.toIntOrNull() ?: 0)
        }
    }
}
