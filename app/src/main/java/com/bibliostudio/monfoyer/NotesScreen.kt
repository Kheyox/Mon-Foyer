package com.bibliostudio.monfoyer

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotesScreen(vm: MonFoyerViewModel) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    ModulePanel(title = "Notes") {
        item {
            SoftInput(value = title, onValueChange = { title = it }, label = "Titre")
            Spacer(Modifier.height(10.dp))
            SoftInput(value = body, onValueChange = { body = it }, label = "Note", minLines = 3)
            Spacer(Modifier.height(10.dp))
            PrimaryButton(text = "Ajouter", icon = Icons.Filled.Add) {
                vm.addNote(title, body)
                title = ""
                body = ""
            }
        }
        if (vm.state.notes.isEmpty()) {
            item { EmptyState("📝", "Aucune note", "Note une idee, un code, ou une petite info a garder.") }
        }
        items(vm.state.notes) { note ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(AppRadius),
                border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder),
                modifier = Modifier.fillMaxWidth().clickable { editingNote = note }
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(note.title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text(note.body, fontSize = 16.sp, color = Muted)
                    }
                    DeleteButton { vm.delete("notes", note.id) }
                }
            }
        }
    }
    editingNote?.let { note ->
        EditNoteSheet(
            note = note,
            onDismiss = { editingNote = null },
            onSave = { noteTitle, noteBody ->
                vm.updateNote(note.id, noteTitle, noteBody)
                editingNote = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteSheet(note: Note, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.body) }
    EditSheetScaffold(title = "Modifier la note", emoji = "📝", onDismiss = onDismiss) {
        SoftInput(title, { title = it }, "Titre")
        Spacer(Modifier.height(10.dp))
        SoftInput(body, { body = it }, "Note", minLines = 4)
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Enregistrer", Icons.Filled.CheckCircle) { onSave(title, body) }
    }
}
