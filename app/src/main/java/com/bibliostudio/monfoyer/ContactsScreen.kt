package com.bibliostudio.monfoyer

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CONTACT_CATEGORIES = listOf("Artisan", "Medical", "Livraison", "Voisinage", "Administratif", "Autre")
private val CONTACT_EMOJIS = listOf("👤","🔧","🩺","🚚","🏘️","🏢","💼","📦","🔑","🌿","📋","🎓","🍕","🚑","⚡","🪟","🚰","🏗️","🐾","📫")

@Composable
fun ContactsScreen(vm: MonFoyerViewModel) {
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<FoyerContact?>(null) }
    var toDelete by remember { mutableStateOf<FoyerContact?>(null) }
    val contacts = vm.state.contacts.sortedWith(compareBy({ it.category }, { it.name }))
    val grouped = contacts.groupBy { it.category }

    ModulePanel(title = "Contacts") {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("${contacts.size} contact(s)", fontSize = 15.sp, color = Muted, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(color = DeepGreen, shape = CircleShape, modifier = Modifier.size(52.dp).clickable { showAdd = true }) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        if (contacts.isEmpty()) {
            item { EmptyState("📞", "Aucun contact", "Ajoute le plombier, le medecin, le livreur... tous les numeros utiles du foyer.") }
        } else {
            grouped.forEach { (category, list) ->
                item {
                    Spacer(Modifier.height(6.dp))
                    Text(category, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Muted)
                    Spacer(Modifier.height(8.dp))
                }
                items(list) { contact ->
                    ContactCard(contact = contact, onEdit = { editing = contact }, onDelete = { toDelete = contact })
                }
            }
        }
    }
    toDelete?.let { contact ->
        ConfirmDeleteDialog(
            title = "Supprimer ce contact ?",
            message = "${contact.name} sera supprime du carnet.",
            onConfirm = { vm.deleteContact(contact.id); toDelete = null },
            onDismiss = { toDelete = null }
        )
    }
    editing?.let { contact ->
        AddContactSheet(
            existing = contact,
            onDismiss = { editing = null },
            onSave = { name, role, phone, email, note, emoji, category ->
                vm.updateContact(contact.id, name, role, phone, email, note, emoji, category)
                editing = null
            }
        )
    }
    if (showAdd) {
        AddContactSheet(
            existing = null,
            onDismiss = { showAdd = false },
            onSave = { name, role, phone, email, note, emoji, category ->
                vm.addContact(name, role, phone, email, note, emoji, category)
                showAdd = false
            }
        )
    }
}

@Composable
fun ContactCard(contact: FoyerContact, onEdit: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    Surface(color = Color.White, shape = RoundedCornerShape(AppRadius), border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = DeepGreen.copy(alpha = 0.10f), shape = CircleShape, modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(contact.emoji.ifBlank { "👤" }, fontSize = 26.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(contact.name, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (contact.role.isNotBlank()) Text(contact.role, fontSize = 14.sp, color = Muted, fontWeight = FontWeight.Bold, maxLines = 1)
                if (contact.phone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                            context.startActivity(intent)
                        }
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = "Appeler", tint = DeepGreen, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(contact.phone, fontSize = 14.sp, color = DeepGreen, fontWeight = FontWeight.Black)
                    }
                }
                if (contact.note.isNotBlank()) Text(contact.note, fontSize = 13.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(38.dp)) {
                    Icon(Icons.Filled.EditNote, contentDescription = "Modifier", tint = Muted, modifier = Modifier.size(22.dp))
                }
                if (contact.email.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = "Email", tint = Muted, modifier = Modifier.size(20.dp))
                    }
                } else {
                    IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = Muted, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactSheet(
    existing: FoyerContact?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String) -> Unit
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var role by remember(existing?.id) { mutableStateOf(existing?.role ?: "") }
    var phone by remember(existing?.id) { mutableStateOf(existing?.phone ?: "") }
    var email by remember(existing?.id) { mutableStateOf(existing?.email ?: "") }
    var note by remember(existing?.id) { mutableStateOf(existing?.note ?: "") }
    var emoji by remember(existing?.id) { mutableStateOf(existing?.emoji ?: "👤") }
    var category by remember(existing?.id) { mutableStateOf(existing?.category ?: "Autre") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val canSave = name.isNotBlank()

    EditSheetScaffold(title = if (existing == null) "Nouveau contact" else "Modifier", emoji = emoji, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = SoftGrey, shape = CircleShape, modifier = Modifier.size(52.dp).clickable { showEmojiPicker = !showEmojiPicker }) {
                Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = 26.sp) }
            }
            Spacer(Modifier.width(12.dp))
            SoftInput(name, { name = it }, "Nom (ex: Plombier Dupont)", modifier = Modifier.weight(1f))
        }
        if (showEmojiPicker) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                CONTACT_EMOJIS.take(10).forEach { e ->
                    Surface(color = if (emoji == e) DeepGreen else SoftGrey, shape = CircleShape, modifier = Modifier.size(38.dp).clickable { emoji = e; showEmojiPicker = false }) {
                        Box(contentAlignment = Alignment.Center) { Text(e, fontSize = 18.sp) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                CONTACT_EMOJIS.drop(10).take(10).forEach { e ->
                    Surface(color = if (emoji == e) DeepGreen else SoftGrey, shape = CircleShape, modifier = Modifier.size(38.dp).clickable { emoji = e; showEmojiPicker = false }) {
                        Box(contentAlignment = Alignment.Center) { Text(e, fontSize = 18.sp) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SoftInput(role, { role = it }, "Role (ex: Plombier, Medecin...)", leadingIcon = Icons.Filled.EditNote)
        Spacer(Modifier.height(10.dp))
        SoftInput(phone, { phone = it }, "Telephone", leadingIcon = Icons.Filled.Phone)
        Spacer(Modifier.height(10.dp))
        SoftInput(email, { email = it }, "Email (optionnel)", leadingIcon = Icons.Filled.Email)
        Spacer(Modifier.height(10.dp))
        SoftInput(note, { note = it }, "Note (ex: disponible le matin)", minLines = 2)
        Spacer(Modifier.height(14.dp))
        FieldLabel("Categorie")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CONTACT_CATEGORIES.take(3).forEach { cat ->
                TaskFilterChip(cat, category == cat) { category = cat }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            CONTACT_CATEGORIES.drop(3).forEach { cat ->
                TaskFilterChip(cat, category == cat) { category = cat }
            }
        }
        Spacer(Modifier.height(22.dp))
        PrimaryButton(if (existing == null) "Ajouter" else "Enregistrer", Icons.Filled.Check) {
            if (canSave) onSave(name.trim(), role.trim(), phone.trim(), email.trim(), note.trim(), emoji, category)
        }
    }
}
