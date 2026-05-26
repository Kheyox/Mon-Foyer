package com.bibliostudio.monfoyer

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BirthdaysScreen(vm: MonFoyerViewModel) {
    var showAddSheet by remember { mutableStateOf(false) }
    var birthdayToDelete by remember { mutableStateOf<Birthday?>(null) }
    var editingBirthday by remember { mutableStateOf<Birthday?>(null) }
    ModulePanel(title = "Anniversaires") {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                RoundIconButton(icon = Icons.Filled.ViewList, tint = Muted, onClick = {})
                RoundIconButton(icon = Icons.Filled.Search, tint = Muted, onClick = {})
                Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(54.dp).clickable { showAddSheet = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
        if (vm.state.birthdays.isEmpty()) {
            item { EmptyState("🎂", "Aucun anniversaire", "Ajoute les dates importantes du foyer.") }
        }
        items(vm.state.birthdays.sortedBy { it.nextBirthday() }) { birthday ->
            BirthdayRow(birthday, onClick = { editingBirthday = birthday }) { birthdayToDelete = birthday }
        }
    }
    if (showAddSheet) {
        AddBirthdaySheet(
            onDismiss = { showAddSheet = false },
            onAdd = { name, date, year ->
                vm.addBirthday(name, date, year)
                showAddSheet = false
            }
        )
    }
    birthdayToDelete?.let { birthday ->
        ConfirmDeleteDialog(
            title = "Supprimer cet anniversaire ?",
            message = "L'anniversaire de ${birthday.name} sera retire du foyer.",
            onConfirm = {
                vm.delete("birthdays", birthday.id)
                birthdayToDelete = null
            },
            onDismiss = { birthdayToDelete = null }
        )
    }
    editingBirthday?.let { birthday ->
        AddBirthdaySheet(
            birthday = birthday,
            onDismiss = { editingBirthday = null },
            onAdd = { name, date, year ->
                vm.updateBirthday(birthday.id, name, date, year)
                editingBirthday = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBirthdaySheet(birthday: Birthday? = null, onDismiss: () -> Unit, onAdd: (String, LocalDate, String) -> Unit) {
    var name by remember(birthday?.id) { mutableStateOf(birthday?.name.orEmpty()) }
    var selectedDate by remember(birthday?.id) {
        mutableStateOf(birthday?.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now())
    }
    var showDateSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 14.dp).width(70.dp).height(6.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0xFF9B9B9B))
            )
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(if (birthday == null) "Ajouter une personne" else "Modifier la personne", fontSize = 32.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Ink, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(34.dp))
            Text("Nom", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            SoftInput(value = name, onValueChange = { name = it }, label = "Entre le nom de la personne", leadingIcon = Icons.Filled.Person)
            Spacer(Modifier.height(26.dp))
            Text("Date de naissance", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            DateField(selectedDate, onClick = { showDateSheet = true })
            Spacer(Modifier.height(34.dp))
            val canAdd = name.isNotBlank()
            Button(
                onClick = { onAdd(name, selectedDate, selectedDate.year.toString()) },
                enabled = canAdd,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepGreen,
                    disabledContainerColor = Color(0xFFE1E1E1),
                    disabledContentColor = Muted
                ),
                modifier = Modifier.fillMaxWidth().height(66.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(if (birthday == null) "Ajouter" else "Enregistrer", fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
    if (showDateSheet) {
        BirthdayDateSheet(
            initialDate = selectedDate,
            onDismiss = { showDateSheet = false },
            onConfirm = {
                selectedDate = it
                showDateSheet = false
            }
        )
    }
}

@Composable
fun BirthdayRow(birthday: Birthday, onClick: () -> Unit = {}, onDelete: () -> Unit) {
    val next = birthday.nextBirthday()
    val age = if (birthday.birthYear > 0) next.year - birthday.birthYear else null
    val monthsAway = ((next.year - LocalDate.now().year) * 12 + next.monthValue - LocalDate.now().monthValue).coerceAtLeast(0)
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(AppRadius),
        border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFFEAF8EE), shape = RoundedCornerShape(12.dp), modifier = Modifier.size(58.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color(0xFF54B568), modifier = Modifier.size(31.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(birthday.name, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(next.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE)), fontSize = 15.sp, color = Muted)
                Text("Dans $monthsAway mois", fontSize = 15.sp, color = Color(0xFF4CAF50))
            }
            age?.let {
                Text("$it ans", fontSize = 19.sp, fontWeight = FontWeight.Black, color = Ink)
            }
            DeleteButton(onDelete)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayDateSheet(initialDate: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var day by remember { mutableStateOf(initialDate.dayOfMonth) }
    var month by remember { mutableStateOf(initialDate.monthValue) }
    var year by remember { mutableStateOf(initialDate.year) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxDay = YearMonth.of(year, month).lengthOfMonth()
    if (day > maxDay) day = maxDay

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 14.dp).width(70.dp).height(6.dp)
                    .clip(RoundedCornerShape(50)).background(Color(0xFF9B9B9B))
            )
        }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Selectionner la date", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                Text(
                    "OK",
                    color = DeepGreen,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.clickable { onConfirm(LocalDate.of(year, month, day)) }.padding(12.dp)
                )
            }
            Spacer(Modifier.height(34.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                PickerColumn(
                    values = (1..maxDay).toList(),
                    selected = day,
                    label = { it.toString() },
                    onSelect = { day = it },
                    modifier = Modifier.weight(0.8f)
                )
                PickerColumn(
                    values = (1..12).toList(),
                    selected = month,
                    label = { LocalDate.of(2026, it, 1).month.getDisplayName(TextStyle.FULL, Locale.FRANCE).replaceFirstChar { char -> char.titlecase(Locale.FRANCE) } },
                    onSelect = { month = it },
                    modifier = Modifier.weight(1.7f)
                )
                PickerColumn(
                    values = (LocalDate.now().year downTo 1920).toList(),
                    selected = year,
                    label = { it.toString() },
                    onSelect = { year = it },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}
