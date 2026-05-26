package com.bibliostudio.monfoyer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.random.Random

fun priorityOrder(priority: String): Int = when (priority) {
    "high" -> 0
    "normal" -> 1
    "low" -> 2
    else -> 1
}

fun priorityColor(priority: String) = when (priority) {
    "high" -> PriorityHigh
    "low" -> PriorityLow
    else -> PriorityNormal
}

fun priorityLabel(priority: String) = when (priority) {
    "high" -> "🔴 Haute"
    "low" -> "⚫ Basse"
    else -> "🟢 Normale"
}

@Composable
fun TasksScreen(vm: MonFoyerViewModel) {
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<HouseholdTask?>(null) }
    var taskToDelete by remember { mutableStateOf<HouseholdTask?>(null) }
    var statusFilter by remember { mutableStateOf("todo") }
    var memberFilter by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("date") } // "date", "priority"
    var confettiBurst by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 3600 * 1000
    val historyTasks = vm.state.tasks.filter { it.done && it.completedAt > thirtyDaysAgo }
    val filteredTasks = vm.state.tasks
        .filter { task ->
            when (statusFilter) {
                "todo" -> !task.done
                "done" -> task.done
                "history" -> false // shown separately below
                else -> true
            }
        }
        .filter { task -> memberFilter.isBlank() || task.assigneeId == memberFilter }
        .let { list ->
            if (sortBy == "priority") {
                list.sortedWith(compareBy<HouseholdTask> { it.done }.thenBy { priorityOrder(it.priority) }.thenBy { it.dueDate.ifBlank { "9999-12-31" } })
            } else {
                list.sortedWith(compareBy<HouseholdTask> { it.done }.thenBy { it.dueDate.ifBlank { "9999-12-31" } }.thenBy { it.title })
            }
        }

    ModulePanel(title = "Taches") {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                RoundIconButton(icon = Icons.Filled.Search, tint = Muted, onClick = {})
                Spacer(Modifier.width(10.dp))
                Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(54.dp).clickable { showAddSheet = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TaskFilterChip("A faire", statusFilter == "todo") { statusFilter = "todo" }
                TaskFilterChip("Terminees", statusFilter == "done") { statusFilter = "done" }
                TaskFilterChip("Toutes", statusFilter == "all") { statusFilter = "all" }
                TaskFilterChip("Historique", statusFilter == "history") { statusFilter = "history" }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TaskFilterChip("Tri: Date", sortBy == "date") { sortBy = "date" }
                TaskFilterChip("Tri: Priorite", sortBy == "priority") { sortBy = "priority" }
            }
            if (vm.state.members.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TaskFilterChip("Tous", memberFilter.isBlank()) { memberFilter = "" }
                    vm.state.members.take(3).forEach { member ->
                        TaskFilterChip(member.name.ifBlank { "Membre" }, memberFilter == member.id) { memberFilter = member.id }
                    }
                }
            }
        }
        if (statusFilter == "history") {
            if (historyTasks.isEmpty()) {
                item { EmptyState("📋", "Aucun historique", "Les taches terminees ces 30 derniers jours apparaitront ici.") }
            } else {
                // Group by week — compute groups here so items() can iterate over list
                val now = System.currentTimeMillis()
                val oneWeek = 7L * 24 * 3600 * 1000
                val weekThisLabel = "Cette semaine"
                val weekLastLabel = "Semaine derniere"
                val week2Label = "Il y a 2 semaines"
                val week3Label = "Il y a 3 semaines"
                val week4Label = "Il y a 4 semaines"
                val weekThisTasks = historyTasks.filter { now - it.completedAt < oneWeek }
                val weekLastTasks = historyTasks.filter { now - it.completedAt in oneWeek until 2 * oneWeek }
                val week2Tasks = historyTasks.filter { now - it.completedAt in 2 * oneWeek until 3 * oneWeek }
                val week3Tasks = historyTasks.filter { now - it.completedAt in 3 * oneWeek until 4 * oneWeek }
                val week4Tasks = historyTasks.filter { now - it.completedAt >= 4 * oneWeek }

                if (weekThisTasks.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)); Text(weekThisLabel, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Muted); Spacer(Modifier.height(8.dp)) }
                    items(weekThisTasks) { task -> TaskCard(task = task, onToggle = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (!task.done) confettiBurst++; vm.toggleTask(task) }, onEdit = { editingTask = task }, onDelete = { taskToDelete = task }) }
                }
                if (weekLastTasks.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)); Text(weekLastLabel, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Muted); Spacer(Modifier.height(8.dp)) }
                    items(weekLastTasks) { task -> TaskCard(task = task, onToggle = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (!task.done) confettiBurst++; vm.toggleTask(task) }, onEdit = { editingTask = task }, onDelete = { taskToDelete = task }) }
                }
                if (week2Tasks.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)); Text(week2Label, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Muted); Spacer(Modifier.height(8.dp)) }
                    items(week2Tasks) { task -> TaskCard(task = task, onToggle = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (!task.done) confettiBurst++; vm.toggleTask(task) }, onEdit = { editingTask = task }, onDelete = { taskToDelete = task }) }
                }
                if (week3Tasks.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)); Text(week3Label, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Muted); Spacer(Modifier.height(8.dp)) }
                    items(week3Tasks) { task -> TaskCard(task = task, onToggle = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (!task.done) confettiBurst++; vm.toggleTask(task) }, onEdit = { editingTask = task }, onDelete = { taskToDelete = task }) }
                }
                if (week4Tasks.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)); Text(week4Label, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Muted); Spacer(Modifier.height(8.dp)) }
                    items(week4Tasks) { task -> TaskCard(task = task, onToggle = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (!task.done) confettiBurst++; vm.toggleTask(task) }, onEdit = { editingTask = task }, onDelete = { taskToDelete = task }) }
                }
            }
        } else {
            if (filteredTasks.isEmpty()) {
                item { EmptyState("✅", "Rien a faire ici", "Les taches apparaitront selon ton filtre.") }
            }
            items(filteredTasks) { task ->
                TaskCard(
                    task = task,
                    onToggle = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); if (!task.done) confettiBurst++; vm.toggleTask(task) },
                    onEdit = { editingTask = task },
                    onDelete = { taskToDelete = task }
                )
            }
        }
    }
    if (showAddSheet) {
        AddTaskSheet(
            members = vm.state.members,
            onDismiss = { showAddSheet = false },
            onAdd = { title, description, dueDate, emoji, member, repeatInterval, priority ->
                vm.addTask(title, description, dueDate, emoji, member, repeatInterval, priority)
                showAddSheet = false
            }
        )
    }
    editingTask?.let { task ->
        AddTaskSheet(
            members = vm.state.members,
            task = task,
            onDismiss = { editingTask = null },
            onAdd = { title, description, dueDate, emoji, member, repeatInterval, priority ->
                vm.updateTask(task.id, title, description, dueDate, emoji, member, repeatInterval, priority)
                editingTask = null
            }
        )
    }
    taskToDelete?.let { task ->
        ConfirmDeleteDialog(
            title = "Supprimer cette tache ?",
            message = "La tache \"${task.title}\" sera supprimee pour tout le foyer.",
            onConfirm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                vm.delete("tasks", task.id)
                taskToDelete = null
            },
            onDismiss = { taskToDelete = null }
        )
    }
    ConfettiOverlay(trigger = confettiBurst)
}

@Composable
fun ConfettiOverlay(trigger: Int) {
    if (trigger <= 0) return
    var visible by remember(trigger) { mutableStateOf(true) }
    LaunchedEffect(trigger) {
        delay(900)
        visible = false
    }
    if (!visible) return
    val pieces = remember(trigger) { List(24) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(Modifier.fillMaxSize()) {
        pieces.forEachIndexed { index, offset ->
            val color = listOf(Lemon, Coral, Leaf, Sky, Lilac)[index % 5]
            drawCircle(color = color, radius = 8f, center = Offset(size.width * offset.x, size.height * (0.1f + offset.y * 0.6f)))
        }
    }
}

@Composable
fun TaskCard(task: HouseholdTask, onToggle: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val color = Color(task.color)
    val overdue = task.isOverdue()
    val pColor = priorityColor(task.priority)
    val cardScale by animateFloatAsState(
        targetValue = if (task.done) 0.985f else 1f,
        label = "task-card-scale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (task.done) 0.76f else 1f,
        label = "task-card-alpha"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (task.done) 1.12f else 1f,
        label = "task-check-scale"
    )
    Surface(
        color = if (overdue && !task.done) Color(0xFFFFF6F1) else Color.White,
        shape = RoundedCornerShape(AppRadius),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (overdue && !task.done) Coral else CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = cardScale, scaleY = cardScale, alpha = cardAlpha)
            .clickable(onClick = onEdit)
    ) {
        Row(Modifier.fillMaxWidth()) {
            // Priority indicator — left band
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(pColor, shape = RoundedCornerShape(topStart = AppRadius, bottomStart = AppRadius))
            )
            Column(Modifier.padding(18.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                    Text(task.title, fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, if (task.done) DeepGreen else CardBorder),
                        modifier = Modifier.size(48.dp).clickable(onClick = onToggle)
                    ) {
                        if (task.done) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer(scaleX = checkScale, scaleY = checkScale)) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = DeepGreen)
                            }
                        }
                    }
                }
                if (task.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(task.description, color = Muted, fontSize = 15.sp)
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = color.copy(alpha = 0.78f), shape = CircleShape, modifier = Modifier.size(50.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(task.assigneeName.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(task.emoji, fontSize = 24.sp)
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color(0xFFF0A000), modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(task.dueDate.taskDueLabel(), color = Color(0xFFF0A000), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("⋮⋮", color = Color(0xFFB9B9B9), fontSize = 25.sp, fontWeight = FontWeight.Black)
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = Muted)
                    }
                }
                if (overdue && !task.done) {
                    Spacer(Modifier.height(8.dp))
                    Text("En retard", color = Coral, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheet(
    members: List<Member>,
    task: HouseholdTask? = null,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, Member?, String, String) -> Unit
) {
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var description by remember(task?.id) { mutableStateOf(task?.description.orEmpty()) }
    var emoji by remember(task?.id) { mutableStateOf(task?.emoji ?: "🙂") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var selectedDate by remember(task?.id) {
        mutableStateOf(task?.dueDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now().plusDays(1))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedMemberId by remember(task?.id, members) {
        mutableStateOf(task?.assigneeId?.ifBlank { null } ?: members.firstOrNull()?.id.orEmpty())
    }
    // Point 4g: repeatInterval state
    var repeatInterval by remember(task?.id) { mutableStateOf(task?.repeatInterval ?: "none") }
    var priority by remember(task?.id) { mutableStateOf(task?.priority ?: "normal") }
    val selectedMember = members.firstOrNull { it.id == selectedMemberId }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(if (task == null) "Nouvelle tache" else "Modifier la tache", fontSize = 31.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = DeepGreen, modifier = Modifier.size(34.dp)) }
            }
            Spacer(Modifier.height(18.dp))
            Text("Nom de la tache", fontSize = 21.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder),
                    modifier = Modifier.size(66.dp).clickable { showEmojiPicker = true }
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 28.sp) }
                }
                SoftInput(value = title, onValueChange = { title = it }, label = "Saisir le nom de la tache", modifier = Modifier.weight(1f))
            }
            if (showEmojiPicker) {
                Spacer(Modifier.height(10.dp))
                EmojiPicker(selected = emoji, onSelect = {
                    emoji = it
                    showEmojiPicker = false
                })
            }
            Spacer(Modifier.height(22.dp))
            Text("Description", fontSize = 21.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            SoftInput(value = description, onValueChange = { description = it }, label = "Ajouter une description (optionnel)", minLines = 3)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Echeance", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    CompactField(text = selectedDate.taskDueLabel(), icon = Icons.Filled.CalendarMonth) { showDatePicker = true }
                }
                Column(Modifier.weight(1f)) {
                    Text("Assigner a", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    CompactField(text = selectedMember?.name ?: "Personne", icon = Icons.Filled.Person) {
                        val index = members.indexOfFirst { it.id == selectedMemberId }
                        selectedMemberId = members.getOrNull(index + 1)?.id ?: members.firstOrNull()?.id.orEmpty()
                    }
                }
            }
            // Point 4g: Recurrence section
            Spacer(Modifier.height(22.dp))
            Text("Recurrence", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    "none" to "Aucune",
                    "daily" to "Quotidienne",
                    "weekly" to "Hebdomadaire"
                )
                options.forEach { (value, label) ->
                    TaskFilterChip(label, repeatInterval == value) { repeatInterval = value }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                val options2 = listOf(
                    "biweekly" to "Bi-hebdomadaire",
                    "monthly" to "Mensuelle"
                )
                options2.forEach { (value, label) ->
                    TaskFilterChip(label, repeatInterval == value) { repeatInterval = value }
                }
            }
            Spacer(Modifier.height(22.dp))
            Text("Priorite", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("high" to "🔴 Haute", "normal" to "🟢 Normale", "low" to "⚫ Basse").forEach { (value, label) ->
                    TaskFilterChip(label, priority == value) { priority = value }
                }
            }
            Spacer(Modifier.height(96.dp))
            PrimaryButton(text = if (task == null) "Ajouter" else "Enregistrer", icon = Icons.Filled.CheckCircle) {
                onAdd(title, description, selectedDate.format(DateTimeFormatter.ISO_DATE), emoji, selectedMember, repeatInterval, priority)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showDatePicker) {
        TaskDateDialog(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                selectedDate = it
                showDatePicker = false
            }
        )
    }
}

@Composable
fun TaskDateDialog(initialDate: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Mois precedent", tint = Ink)
                    }
                    val title = visibleMonth.month.getDisplayName(TextStyle.FULL, Locale.FRANCE)
                        .replaceFirstChar { it.titlecase(Locale.FRANCE) }
                    Text(
                        "$title ${visibleMonth.year}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Mois suivant", tint = Ink)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim.").forEach {
                        Text(it, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
                TaskDateGrid(month = visibleMonth, selectedDate = selectedDate) {
                    selectedDate = it
                    visibleMonth = YearMonth.from(it)
                }
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DeepGreen),
                        modifier = Modifier.weight(1f).height(58.dp)
                    ) {
                        Text("Annuler", fontSize = 19.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = { onConfirm(selectedDate) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(58.dp)
                    ) {
                        Text("Ok", fontSize = 19.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(AppRadius)
    )
}

@Composable
fun TaskDateGrid(month: YearMonth, selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val firstDay = month.atDay(1)
    val leading = firstDay.dayOfWeek.value - 1
    val previousMonth = month.minusMonths(1)
    val previousStart = previousMonth.lengthOfMonth() - leading + 1
    val previousDays = if (leading == 0) emptyList() else (previousStart..previousMonth.lengthOfMonth()).map { previousMonth.atDay(it) }
    val currentDays = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val trailingCount = (42 - previousDays.size - currentDays.size).coerceAtLeast(0)
    val nextDays = (1..trailingCount).map { month.plusMonths(1).atDay(it) }
    val days = previousDays + currentDays + nextDays
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(300.dp)
    ) {
        gridItems(days) { date ->
            val isCurrent = YearMonth.from(date) == month
            val selected = date == selectedDate
            Box(
                Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) DeepGreen else Color.Transparent)
                    .clickable { onDateSelected(date) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    date.dayOfMonth.toString(),
                    fontSize = 20.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.Normal,
                    color = when {
                        selected -> Color.White
                        isCurrent -> Ink
                        else -> Muted.copy(alpha = 0.65f)
                    }
                )
            }
        }
    }
}

@Composable
fun TimePickerDialog(initialTime: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val parts = initialTime.split(":")
    var hour by remember { mutableStateOf(parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 0) }
    var minute by remember { mutableStateOf(parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Column {
                Text("Selectionner l'heure", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
                Spacer(Modifier.height(26.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                    PickerColumn(
                        values = (0..23).toList(),
                        selected = hour,
                        label = { "%02d".format(it) },
                        onSelect = { hour = it },
                        modifier = Modifier.weight(1f)
                    )
                    PickerColumn(
                        values = listOf(0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55),
                        selected = minute - (minute % 5),
                        label = { "%02d".format(it) },
                        onSelect = { minute = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DeepGreen),
                        modifier = Modifier.weight(1f).height(58.dp)
                    ) {
                        Text("Annuler", fontSize = 19.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = { onConfirm("%02d:%02d".format(hour, minute)) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
                        modifier = Modifier.weight(1f).height(58.dp)
                    ) {
                        Text("Ok", fontSize = 19.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(AppRadius)
    )
}
