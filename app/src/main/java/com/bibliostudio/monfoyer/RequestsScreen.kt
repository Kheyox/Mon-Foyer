package com.bibliostudio.monfoyer

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun RequestsScreen(vm: MonFoyerViewModel) {
    var selectedMemberId by remember(vm.state.members) {
        mutableStateOf(vm.state.members.firstOrNull { it.id == vm.state.currentUserId }?.id ?: vm.state.members.firstOrNull()?.id.orEmpty())
    }
    var kind by remember { mutableStateOf("Livre") }
    var showAdd by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<MediaRequest?>(null) }
    val members = vm.state.members.ifEmpty { listOf(Member(vm.state.currentUserId, vm.state.userName.ifBlank { "Moi" })) }
    val selectedMember = members.firstOrNull { it.id == selectedMemberId } ?: members.first()
    val requests = vm.state.mediaRequests
        .filter { it.requesterId == selectedMember.id && it.kind == kind }
        .sortedWith(compareBy<MediaRequest> { it.status != "pending" }.thenBy { it.title.lowercase(Locale.FRANCE) })
    val isAdmin = vm.state.isCurrentUserAdmin()

    ModulePanel(title = "Demandes") {
        item {
            Text("Par personne", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height((((members.size + 1) / 2) * 54).dp)
            ) {
                gridItems(members) { member ->
                    MemberRequestTab(
                        member = member,
                        selected = selectedMember.id == member.id,
                        pendingCount = vm.state.mediaRequests.count { it.requesterId == member.id && it.status == "pending" },
                        onClick = { selectedMemberId = member.id }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                TaskFilterChip("Livres", kind == "Livre") { kind = "Livre" }
                TaskFilterChip("Films / series", kind == "FilmSerie") { kind = "FilmSerie" }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(selectedMember.name.ifBlank { "Membre" }, fontSize = 28.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, color = Ink)
                    Text(if (kind == "Livre") "Demandes de livres" else "Demandes de films et series", color = Muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                if (selectedMember.id == vm.state.currentUserId) {
                    Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(58.dp).clickable { showAdd = true }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        if (requests.isEmpty()) {
            item {
                EmptyState(
                    emoji = if (kind == "Livre") "📚" else "🎬",
                    title = "Aucune demande",
                    body = if (selectedMember.id == vm.state.currentUserId) "Ajoute une idee, elle apparaitra ici pour l'admin." else "Cette personne n'a rien demande dans cette categorie."
                )
            }
        }
        items(requests) { request ->
            MediaRequestCard(
                request = request,
                canModerate = isAdmin && request.status == "pending",
                canDelete = isAdmin || request.requesterId == vm.state.currentUserId,
                onApprove = { vm.updateMediaRequestStatus(request, "approved") },
                onReject = { vm.updateMediaRequestStatus(request, "rejected") },
                onDelete = { confirmDelete = request }
            )
        }
    }
    if (showAdd) {
        AddMediaRequestSheet(
            kind = kind,
            onDismiss = { showAdd = false },
            onAdd = { title ->
                vm.addMediaRequest(title, kind)
                showAdd = false
            }
        )
    }
    confirmDelete?.let { request ->
        ConfirmDeleteDialog(
            title = "Supprimer la demande ?",
            message = "${request.title} sera retire de la liste.",
            onConfirm = {
                vm.delete("requests", request.id)
                confirmDelete = null
            },
            onDismiss = { confirmDelete = null }
        )
    }
}

@Composable
fun MemberRequestTab(member: Member, selected: Boolean, pendingCount: Int, onClick: () -> Unit) {
    Surface(
        color = if (selected) DeepGreen else Color.White,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) DeepGreen else CardBorder),
        modifier = Modifier.fillMaxWidth().height(46.dp).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(member.color), shape = CircleShape, modifier = Modifier.size(26.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(member.name.take(1).uppercase().ifBlank { "?" }, color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(member.name.ifBlank { "Membre" }, color = if (selected) Color.White else Ink, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (pendingCount > 0) {
                Text(pendingCount.toString(), color = if (selected) Lemon else DeepGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun MediaRequestCard(
    request: MediaRequest,
    canModerate: Boolean,
    canDelete: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (request.status) {
        "approved" -> DeepGreen
        "rejected" -> Coral
        else -> Color(0xFFE39318)
    }
    val statusText = when (request.status) {
        "approved" -> "Valide"
        "rejected" -> "Refuse"
        else -> "En attente"
    }
    val cardScale by animateFloatAsState(
        targetValue = if (request.status == "pending") 1f else 0.985f,
        label = "request-card-scale"
    )
    val statusScale by animateFloatAsState(
        targetValue = if (request.status == "pending") 1f else 1.08f,
        label = "request-status-scale"
    )
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(AppRadius),
        border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = cardScale, scaleY = cardScale)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                Text(if (request.kind == "Livre") "📚" else "🎬", fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(request.title, fontSize = 22.sp, lineHeight = 25.sp, fontWeight = FontWeight.Black, color = Ink)
                    Text("Demande de ${request.requesterName.ifBlank { "Membre" }}", fontSize = 14.sp, color = Muted, fontWeight = FontWeight.Bold)
                }
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.graphicsLayer(scaleX = statusScale, scaleY = statusScale)
                ) {
                    Text(statusText, color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
            }
            if (canModerate || canDelete) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    if (canModerate) {
                        RequestActionButton("Valider", DeepGreen, Modifier.weight(1f), onApprove)
                        RequestActionButton("Refuser", Coral, Modifier.weight(1f), onReject)
                    }
                    if (canDelete) {
                        Surface(color = SoftGrey, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(48.dp).clickable(onClick = onDelete)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestActionButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(color = color, shape = RoundedCornerShape(14.dp), modifier = modifier.height(48.dp).clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMediaRequestSheet(kind: String, onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    val label = if (kind == "Livre") "Nom du livre" else "Nom du film ou de la serie"
    EditSheetScaffold(title = if (kind == "Livre") "Demander un livre" else "Demander un film", emoji = if (kind == "Livre") "📚" else "🎬", onDismiss = onDismiss) {
        Text("Titre", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
        Spacer(Modifier.height(8.dp))
        SoftInput(title, { title = it }, label, leadingIcon = Icons.Filled.EditNote)
        Spacer(Modifier.height(22.dp))
        androidx.compose.material3.Button(
            onClick = { onAdd(title) },
            enabled = title.isNotBlank(),
            shape = RoundedCornerShape(FieldRadius),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = DeepGreen,
                disabledContainerColor = Color(0xFFE1E1E1),
                disabledContentColor = Muted
            ),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(10.dp))
            Text("Envoyer la demande", fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun MediaRequestNotificationEffect(state: AppUiState) {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    var previous by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(state.mediaRequests, state.currentUserId, state.members) {
        val current = state.mediaRequests.associate { it.id to it.status }
        if (ready) {
            val isAdmin = state.isCurrentUserAdmin()
            state.mediaRequests.forEach { request ->
                val oldStatus = previous[request.id]
                if (oldStatus == null && isAdmin && request.requesterId != state.currentUserId && request.status == "pending") {
                    ReminderReceiver.showNow(
                        context,
                        request.id.hashCode().absoluteValue,
                        "Nouvelle demande ${if (request.kind == "Livre") "livre" else "film/serie"}",
                        "${request.requesterName.ifBlank { "Un membre" }} demande : ${request.title}"
                    )
                } else if (oldStatus != null && oldStatus != request.status && request.requesterId == state.currentUserId && request.status != "pending") {
                    ReminderReceiver.showNow(
                        context,
                        request.id.hashCode().absoluteValue,
                        "Demande ${requestStatusLabel(request.status).lowercase(Locale.FRANCE)}",
                        request.title
                    )
                }
            }
        } else {
            ready = true
        }
        previous = current
    }
}
