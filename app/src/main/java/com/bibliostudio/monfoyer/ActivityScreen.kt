package com.bibliostudio.monfoyer

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActivityScreen(vm: MonFoyerViewModel) {
    val activity = vm.state.activity
    ModulePanel(title = "Activite") {
        if (activity.isEmpty()) {
            item {
                EmptyState(
                    emoji = "✨",
                    title = "Rien pour l'instant",
                    body = "Les ajouts, modifications importantes et decisions du foyer apparaitront ici."
                )
            }
        } else {
            items(activity) { item ->
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(AppRadius),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(item.color), shape = CircleShape, modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(item.actorName.memberInitial(), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.actorName.ifBlank { "Membre" }, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Ink)
                            Text(item.text, fontSize = 15.sp, lineHeight = 18.sp, color = Muted, fontWeight = FontWeight.SemiBold)
                        }
                        Text(item.createdAtMillis.activityAgeLabel(), fontSize = 12.sp, color = Muted, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
