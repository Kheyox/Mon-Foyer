package com.bibliostudio.monfoyer

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MembersScreen(vm: MonFoyerViewModel) {
    val state = vm.state
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<Member?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    var remindersEnabled by remember { mutableStateOf(ReminderScheduler.remindersEnabled(context)) }
    val currentMember = state.members.firstOrNull { it.id == state.currentUserId }
    val isAdmin = currentMember?.role == "admin"
    var householdName by remember(state.household?.id) { mutableStateOf(state.household?.name ?: "Mon foyer") }

    ModulePanel(title = "Mon foyer") {
        item {
            Surface(color = Color.White, shape = RoundedCornerShape(AppRadius), border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Parametres du foyer", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
                    Spacer(Modifier.height(8.dp))
                    if (isAdmin) {
                        SoftInput(value = householdName, onValueChange = { householdName = it }, label = "Nom du foyer", leadingIcon = Icons.Filled.Home)
                        Spacer(Modifier.height(10.dp))
                        SecondaryButton(text = "Enregistrer le nom", icon = Icons.Filled.CheckCircle) {
                            vm.updateHouseholdName(householdName)
                        }
                    } else {
                        Text(state.household?.name ?: "Mon foyer", fontSize = 18.sp, fontWeight = FontWeight.Black, color = DeepGreen)
                        Text("Seuls les admins peuvent modifier les parametres du foyer.", fontSize = 14.sp, color = Muted, lineHeight = 17.sp)
                    }
                }
            }
        }
        item {
            Surface(color = SoftGrey, shape = RoundedCornerShape(AppRadius), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Code d'invitation", fontSize = 16.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            state.household?.inviteCode.orEmpty(),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = DeepGreen,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(54.dp).clickable {
                                clipboard.setText(AnnotatedString(state.household?.inviteCode.orEmpty()))
                                copied = true
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copier", tint = DeepGreen)
                            }
                        }
                    }
                    if (copied) {
                        Spacer(Modifier.height(6.dp))
                        Text("Code copie", fontSize = 14.sp, color = DeepGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        items(state.members.sortedWith(compareByDescending<Member> { it.role == "admin" }.thenBy { it.name.ifBlank { it.email } })) { member ->
            val canEdit = isAdmin || member.id == state.currentUserId
            MemberProfileCard(
                member = member,
                stats = state.memberStats(member.id),
                canEdit = canEdit,
                onClick = { editingMember = member }
            )
        }
        item {
            Surface(color = Color(0xFFFFF3C9), shape = RoundedCornerShape(AppRadius), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔔", fontSize = 30.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Notifications", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text("Anniversaires, rendez-vous et taches proches.", fontSize = 14.sp, color = Muted)
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = {
                            remindersEnabled = it
                            ReminderScheduler.setRemindersEnabled(context, it)
                            ReminderScheduler.refresh(context, state.events, state.tasks, state.birthdays)
                        }
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = "Quitter le foyer", icon = Icons.Filled.Logout) {
                confirmLeave = true
            }
        }
    }

    editingMember?.let { member ->
        MemberEditorSheet(
            member = member,
            canManageRole = isAdmin && member.id != state.currentUserId,
            onDismiss = { editingMember = null },
            onSave = { name, color, role ->
                vm.updateMember(member.id, name, color, role)
                editingMember = null
            }
        )
    }
    if (confirmLeave) {
        ConfirmDeleteDialog(
            title = "Quitter le foyer",
            message = "Ton compte ne verra plus ce foyer. Tu pourras le rejoindre plus tard avec le code d'invitation.",
            confirmLabel = "Quitter",
            onConfirm = {
                confirmLeave = false
                vm.leaveHousehold()
            },
            onDismiss = { confirmLeave = false }
        )
    }
}

@Composable
fun MemberProfileCard(member: Member, stats: MemberStats, canEdit: Boolean, onClick: () -> Unit) {
    val accent = Color(member.color)
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        shadowElevation = if (member.role == "admin") 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth().clickable(enabled = canEdit, onClick = onClick)
    ) {
        Box(Modifier.padding(16.dp)) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(accent.copy(alpha = 0.10f), radius = 74.dp.toPx(), center = Offset(size.width - 20.dp.toPx(), 8.dp.toPx()))
                drawCircle(Lemon.copy(alpha = 0.16f), radius = 34.dp.toPx(), center = Offset(18.dp.toPx(), size.height - 6.dp.toPx()))
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Surface(color = accent, shape = CircleShape, modifier = Modifier.size(62.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(member.name.memberInitial(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(member.name.ifBlank { "Membre" }, fontSize = 23.sp, fontWeight = FontWeight.Black, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(member.email.ifBlank { "Compte Google" }, fontSize = 13.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    RoleBadge(member.role)
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MemberStatChip("✅", stats.doneTasks.toString(), "faites", Modifier.weight(1f))
                    MemberStatChip("🕒", stats.openTasks.toString(), "a faire", Modifier.weight(1f))
                    MemberStatChip("🎬", stats.pendingRequests.toString(), "demandes", Modifier.weight(1f))
                    MemberStatChip("🎂", stats.birthdaysAdded.toString(), "dates", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun RoleBadge(role: String) {
    val admin = role == "admin"
    Surface(color = if (admin) DeepGreen else SoftGrey, shape = RoundedCornerShape(50)) {
        Text(
            if (admin) "Admin" else "Membre",
            color = if (admin) Color.White else Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun MemberStatChip(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(color = Cream, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder), modifier = modifier.height(58.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
            Text("$emoji $value", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Ink, maxLines = 1)
            Text(label, fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberEditorSheet(member: Member, canManageRole: Boolean, onDismiss: () -> Unit, onSave: (String, Long, String) -> Unit) {
    var name by remember(member.id) { mutableStateOf(member.name.ifBlank { "Membre" }) }
    var color by remember(member.id) { mutableStateOf(member.color) }
    var role by remember(member.id) { mutableStateOf(member.role) }
    val colors = listOf(0xFF174C43, 0xFFE86675, 0xFFE8A64F, 0xFF5C8EE6, 0xFF8A6FDF, 0xFF2F9C95, 0xFF54B568, 0xFFB56AE8)
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
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Modifier le membre", fontSize = 31.sp, lineHeight = 33.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Ink, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(color), shape = CircleShape, modifier = Modifier.size(72.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(name.memberInitial(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(member.email.ifBlank { "Compte Google" }, color = Muted, fontSize = 15.sp)
                    Text(if (member.role == "admin") "Administrateur" else "Membre", color = DeepGreen, fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text("Nom affiche", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            SoftInput(value = name, onValueChange = { name = it }, label = "Nom du membre", leadingIcon = Icons.Filled.Person)
            Spacer(Modifier.height(24.dp))
            Text("Couleur", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                colors.forEach { value ->
                    Surface(
                        color = Color(value),
                        shape = CircleShape,
                        modifier = Modifier.size(if (value == color) 58.dp else 48.dp).clickable { color = value }
                    ) {
                        if (value == color) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
            if (canManageRole) {
                Spacer(Modifier.height(24.dp))
                Text("Role", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    RoleChip("Membre", role == "member", Modifier.weight(1f)) { role = "member" }
                    RoleChip("Admin", role == "admin", Modifier.weight(1f)) { role = "admin" }
                }
            }
            Spacer(Modifier.height(30.dp))
            PrimaryButton(text = "Enregistrer", icon = Icons.Filled.CheckCircle) {
                onSave(name, color, role)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun RoleChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) DeepGreen else SoftGrey,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.height(52.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.White else Muted, fontSize = 17.sp, fontWeight = FontWeight.Black)
        }
    }
}
