package com.bibliostudio.monfoyer

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AgendaScreen(vm: MonFoyerViewModel) {
    var showAddSheet by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<Event?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    val selectedDateText = selectedDate.format(DateTimeFormatter.ISO_DATE)
    val selectedEvents = vm.state.events.filter { it.date == selectedDateText }
    ModulePanel(title = "Calendrier") {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                RoundIconButton(icon = Icons.Filled.Search, tint = Muted, onClick = {})
                Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(54.dp).clickable { showAddSheet = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            CalendarMonthView(
                month = visibleMonth,
                selectedDate = selectedDate,
                events = vm.state.events,
                onPrevious = {
                    visibleMonth = visibleMonth.minusMonths(1)
                    selectedDate = visibleMonth.atDay(1)
                },
                onNext = {
                    visibleMonth = visibleMonth.plusMonths(1)
                    selectedDate = visibleMonth.atDay(1)
                },
                onDateSelected = {
                    selectedDate = it
                    visibleMonth = YearMonth.from(it)
                }
            )
            Spacer(Modifier.height(18.dp))
            if (selectedEvents.isEmpty()) {
                EmptyState("🌤️", "Journee libre", "Aucun evenement prevu ce jour.")
            } else {
                selectedEvents.forEach { event ->
                    CalendarEventPill(event, onClick = { editingEvent = event })
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        items(vm.state.events) { event ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(AppRadius),
                border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder),
                modifier = Modifier.fillMaxWidth().clickable { editingEvent = event }
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(event.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text(listOf(event.date, if (event.allDay) "Toute la journee" else event.time, event.owner).filter { it.isNotBlank() }.joinToString(" - "), fontSize = 16.sp, color = Muted)
                    }
                    DeleteButton { vm.delete("events", event.id) }
                }
            }
        }
    }
    if (showAddSheet) {
        AddEventSheet(
            members = vm.state.members,
            eventTypes = vm.state.eventTypes.ifEmpty { defaultEventTypes() },
            initialDate = selectedDate,
            onDismiss = { showAddSheet = false },
            onAddType = { name, icon, color -> vm.addEventType(name, icon, color) },
            onAdd = { title, description, location, owner, date, time, allDay, recurrence, type ->
                vm.addEvent(title, description, location, owner, date.format(DateTimeFormatter.ISO_DATE), time, allDay, recurrence, type)
                selectedDate = date
                visibleMonth = YearMonth.from(date)
                showAddSheet = false
            }
        )
    }
    editingEvent?.let { event ->
        AddEventSheet(
            members = vm.state.members,
            eventTypes = vm.state.eventTypes.ifEmpty { defaultEventTypes() },
            initialDate = runCatching { LocalDate.parse(event.date) }.getOrDefault(selectedDate),
            event = event,
            onDismiss = { editingEvent = null },
            onAddType = { name, icon, color -> vm.addEventType(name, icon, color) },
            onAdd = { title, description, location, owner, date, time, allDay, recurrence, type ->
                vm.updateEvent(event.id, title, description, location, owner, date.format(DateTimeFormatter.ISO_DATE), time, allDay, recurrence, type)
                selectedDate = date
                visibleMonth = YearMonth.from(date)
                editingEvent = null
            }
        )
    }
}

@Composable
fun CalendarMonthView(
    month: YearMonth,
    selectedDate: LocalDate,
    events: List<Event>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val leading = firstDay.dayOfWeek.value - 1
    val cells = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val monthTitle = month.month.getDisplayName(TextStyle.FULL, Locale.FRANCE)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRANCE) else it.toString() }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("$monthTitle ${month.year}", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
            RoundIconButton(icon = Icons.Filled.ChevronLeft, tint = Muted, onClick = onPrevious)
            Spacer(Modifier.width(8.dp))
            RoundIconButton(icon = Icons.Filled.ChevronRight, tint = DeepGreen, onClick = onNext)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim.").forEach {
                Text(it, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(356.dp)
        ) {
            gridItems(cells) { date ->
                if (date == null) {
                    Spacer(Modifier.height(58.dp))
                } else {
                    val dateKey = date.format(DateTimeFormatter.ISO_DATE)
                    val dayEvents = events.filter { it.date == dateKey }
                    val selected = date == selectedDate
                    Box(
                        Modifier
                            .height(58.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) DeepGreen else SoftGrey)
                            .clickable { onDateSelected(date) }
                            .padding(7.dp)
                    ) {
                        Text(date.dayOfMonth.toString(), color = if (selected) Color.White else Ink, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        if (dayEvents.isNotEmpty()) {
                            Column(Modifier.align(Alignment.BottomStart)) {
                                dayEvents.take(2).forEach { _ ->
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (selected) Color.White.copy(alpha = 0.85f) else DeepGreen.copy(alpha = 0.75f))
                                    )
                                    Spacer(Modifier.height(3.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarEventPill(event: Event, onClick: () -> Unit = {}) {
    Surface(color = Color(event.typeColor).copy(alpha = 0.12f), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(event.typeIcon, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(event.title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(listOf(event.typeName, if (event.allDay) "Toute la journee" else event.time, event.owner.ifBlank { "Tout le foyer" }).joinToString(" - "), fontSize = 14.sp, color = Color(event.typeColor), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventSheet(
    members: List<Member>,
    eventTypes: List<EventType>,
    initialDate: LocalDate,
    event: Event? = null,
    onDismiss: () -> Unit,
    onAddType: (String, String, Long) -> Unit,
    onAdd: (String, String, String, String, LocalDate, String, Boolean, String, EventType) -> Unit
) {
    var title by remember(event?.id) { mutableStateOf(event?.title.orEmpty()) }
    var description by remember(event?.id) { mutableStateOf(event?.description.orEmpty()) }
    var location by remember(event?.id) { mutableStateOf(event?.location.orEmpty()) }
    var selectedMemberIds by remember(event?.id, members) {
        mutableStateOf(
            event?.owner?.let { owner ->
                members.filter { member -> owner.contains(member.name.ifBlank { "Membre" }, ignoreCase = true) }.map { it.id }.toSet()
            }?.takeIf { it.isNotEmpty() } ?: members.map { it.id }.toSet()
        )
    }
    var selectedType by remember(event?.id, eventTypes) {
        mutableStateOf(
            event?.let { current ->
                eventTypes.firstOrNull { it.name == current.typeName } ?: EventType(name = current.typeName, icon = current.typeIcon, color = current.typeColor)
            } ?: eventTypes.firstOrNull() ?: defaultEventTypes().first()
        )
    }
    var showTypeSheet by remember { mutableStateOf(false) }
    var selectedDate by remember(event?.id) { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var time by remember(event?.id) { mutableStateOf(event?.time ?: "00:00") }
    var showTimePicker by remember { mutableStateOf(false) }
    var allDay by remember(event?.id) { mutableStateOf(event?.allDay ?: false) }
    var recurrence by remember(event?.id) { mutableStateOf(event?.recurrence ?: "Aucune") }
    var showRecurrence by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val owner = members.filter { it.id in selectedMemberIds }.joinToString(", ") { it.name.ifBlank { "Membre" } }.ifBlank { "Tout le foyer" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 22.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(if (event == null) "Nouvel evenement" else "Modifier l'evenement", fontSize = 31.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = DeepGreen, modifier = Modifier.size(34.dp)) }
                }
                Spacer(Modifier.height(18.dp))
                FieldLabel("Titre")
                SoftInput(title, { title = it }, "Nom de l'evenement")
                Spacer(Modifier.height(20.dp))
                FieldLabel("Description")
                SoftInput(description, { description = it }, "Description (optionnel)", minLines = 3)
                Spacer(Modifier.height(20.dp))
                FieldLabel("Lieu")
                SoftInput(location, { location = it }, "Adresse ou lieu (optionnel)", leadingIcon = Icons.Filled.LocationOn)
                Spacer(Modifier.height(20.dp))
                FieldLabel("Participants")
                ParticipantsField(members, selectedMemberIds) { selectedMemberIds = it }
                Spacer(Modifier.height(20.dp))
                FieldLabel("Type d'evenement")
                EventTypeField(selectedType) { showTypeSheet = true }
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Toute la journee")
                        Text("L'evenement a lieu sur la journee complete", color = Muted, fontSize = 16.sp)
                    }
                    Switch(checked = allDay, onCheckedChange = { allDay = it })
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Date")
                        CompactField(selectedDate.format(DateTimeFormatter.ofPattern("d/M/yyyy", Locale.FRANCE)), Icons.Filled.CalendarMonth) { showDatePicker = true }
                    }
                    Column(Modifier.weight(1f)) {
                        FieldLabel("Heure")
                        CompactField(time, Icons.Filled.Schedule) { showTimePicker = true }
                    }
                }
                Spacer(Modifier.height(20.dp))
                FieldLabel("Recurrence")
                RecurrenceField(recurrence) { showRecurrence = !showRecurrence }
                if (showRecurrence) {
                    Spacer(Modifier.height(10.dp))
                    RecurrencePicker(recurrence) {
                        recurrence = it
                        showRecurrence = false
                    }
                }
                Spacer(Modifier.height(34.dp))
                PrimaryButton(if (event == null) "Ajouter" else "Enregistrer", Icons.Filled.CheckCircle) {
                    onAdd(title, description, location, owner, selectedDate, time, allDay, recurrence, selectedType)
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
    if (showDatePicker) {
        TaskDateDialog(initialDate = selectedDate, onDismiss = { showDatePicker = false }) {
            selectedDate = it
            showDatePicker = false
        }
    }
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = time,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                time = it
                showTimePicker = false
            }
        )
    }
    if (showTypeSheet) {
        EventTypeSheet(
            types = eventTypes,
            selected = selectedType,
            onDismiss = { showTypeSheet = false },
            onSelect = {
                selectedType = it
                showTypeSheet = false
            },
            onAddType = { name, icon, color ->
                onAddType(name, icon, color)
                selectedType = EventType(name = name, icon = icon, color = color)
            }
        )
    }
}

@Composable
fun ParticipantsField(members: List<Member>, selectedIds: Set<String>, onChange: (Set<String>) -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(AppRadius), border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            if (members.isEmpty()) {
                Text("Tout le foyer", color = Muted, fontSize = 18.sp)
            } else {
                members.forEach { member ->
                    val selected = member.id in selectedIds
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                        onChange(if (selected) selectedIds - member.id else selectedIds + member.id)
                    }.padding(vertical = 8.dp)) {
                        Icon(if (selected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked, contentDescription = null, tint = if (selected) DeepGreen else Muted)
                        Spacer(Modifier.width(10.dp))
                        Text(member.name.ifBlank { "Membre" }, fontSize = 18.sp, color = Ink)
                    }
                }
            }
        }
    }
}

@Composable
fun EventTypeField(type: EventType, onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(FieldRadius), border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder), modifier = Modifier.fillMaxWidth().height(66.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(type.icon, fontSize = 25.sp)
            Spacer(Modifier.width(12.dp))
            Text(type.name, fontSize = 20.sp, color = Ink, modifier = Modifier.weight(1f))
            Text("×", color = Muted, fontSize = 28.sp)
        }
    }
}

@Composable
fun RecurrenceField(value: String, onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(FieldRadius), border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Repeat, contentDescription = null, tint = Muted)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Recurrence", fontSize = 19.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(value, color = Muted, fontSize = 17.sp)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Muted)
        }
    }
}

@Composable
fun RecurrencePicker(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "Aucune",
        "Tous les jours",
        "Tous les jours ouvrés",
        "Toutes les semaines",
        "Toutes les 2 semaines",
        "Tous les mois",
        "Tous les ans"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { value ->
                    MemberChip(label = value, selected = selected == value, color = DeepGreen, modifier = Modifier.weight(1f)) { onSelect(value) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTypeSheet(types: List<EventType>, selected: EventType, onDismiss: () -> Unit, onSelect: (EventType) -> Unit, onAddType: (String, String, Long) -> Unit) {
    var query by remember { mutableStateOf("") }
    var showNewType by remember { mutableStateOf(false) }
    val visibleTypes = types.filter { it.name.contains(query, ignoreCase = true) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Type d'evenement", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = null, tint = DeepGreen) }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SoftInput(query, { query = it }, "Rechercher un type ...", leadingIcon = Icons.Filled.Search, modifier = Modifier.weight(1f))
                Surface(color = DeepGreen, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(62.dp).clickable { showNewType = true }) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp)) }
                }
            }
            Spacer(Modifier.height(18.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(260.dp)) {
                gridItems(visibleTypes) { type ->
                    Surface(color = if (type.name == selected.name) Color(type.color) else SoftGrey, shape = RoundedCornerShape(16.dp), modifier = Modifier.height(88.dp).clickable { onSelect(type) }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(type.icon, fontSize = 28.sp)
                            Text(type.name, color = if (type.name == selected.name) Color.White else Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
    if (showNewType) {
        NewEventTypeSheet(onDismiss = { showNewType = false }, onAdd = { name, icon, color ->
            onAddType(name, icon, color)
            showNewType = false
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEventTypeSheet(onDismiss: () -> Unit, onAdd: (String, String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🍴") }
    var color by remember { mutableStateOf(0xFF174C43) }
    var showIcons by remember { mutableStateOf(false) }
    var showColors by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp)) {
            Text("Nouveau type", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(26.dp))
            FieldLabel("Nom du type")
            SoftInput(name, { name = it }, "Ex: Medical, Ecole, Loisirs...")
            Spacer(Modifier.height(22.dp))
            FieldLabel("Icone du type")
            CompactTextField("$icon  Choisir une icone") { showIcons = true }
            Spacer(Modifier.height(22.dp))
            FieldLabel("Couleur du type")
            CompactTextField("●  Choisir une couleur", Color(color)) { showColors = true }
            Spacer(Modifier.height(34.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Annuler", color = Muted, fontSize = 19.sp, modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp))
                Spacer(Modifier.weight(1f))
                Button(onClick = { onAdd(name, icon, color) }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = DeepGreen), modifier = Modifier.width(180.dp).height(58.dp)) {
                    Text("Ajouter", fontSize = 19.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (showIcons) {
        IconBankSheet(onDismiss = { showIcons = false }, onSelect = {
            icon = it
            showIcons = false
        })
    }
    if (showColors) {
        ColorBankSheet(onDismiss = { showColors = false }, onSelect = {
            color = it
            showColors = false
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconBankSheet(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val icons = listOf("🍎", "🥩", "🌾", "🍬", "🥕", "🍒", "🥐", "🥚", "🐟", "🍇", "🍪", "🍉", "🥛", "🍕", "🥤", "🥗", "🍗", "🥪", "🍜", "🍷", "🛒", "📦", "🎁", "🍴", "☕", "🎂", "🍌", "💧", "🏥", "🏫", "⚽", "🎬")
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Choisir une icone", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = null, tint = Muted) }
            }
            Spacer(Modifier.height(20.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(22.dp), horizontalArrangement = Arrangement.spacedBy(22.dp), modifier = Modifier.height(430.dp)) {
                gridItems(icons) { icon ->
                    Surface(color = Color.White, shape = CircleShape, modifier = Modifier.size(58.dp).clickable { onSelect(icon) }) {
                        Box(contentAlignment = Alignment.Center) { Text(icon, fontSize = 34.sp) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorBankSheet(onDismiss: () -> Unit, onSelect: (Long) -> Unit) {
    val colors = listOf(0xFF174C43, 0xFFE86675, 0xFFE8A64F, 0xFF54B568, 0xFF5C8EE6, 0xFF8A6FDF, 0xFF2F9C95, 0xFF111111)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        Column(Modifier.fillMaxWidth().padding(28.dp)) {
            Text("Choisir une couleur", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                colors.forEach { value ->
                    Surface(color = Color(value), shape = CircleShape, modifier = Modifier.size(54.dp).clickable { onSelect(value) }) {}
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
