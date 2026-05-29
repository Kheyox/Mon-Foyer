package com.bibliostudio.monfoyer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun Dashboard(vm: MonFoyerViewModel) {
    val state = vm.state
    val modules = listOf(
        ModuleTile(Tab.Calendar, "Agenda", "Les rendez-vous du foyer", state.events.size.takeIf { it > 0 }?.toString(), listOf(Color(0xFFFFE5A3), Color(0xFFFFBE73)), Icons.Filled.CalendarMonth, "📅", Color(0xFFE28B21)),
        ModuleTile(Tab.Tasks, "Taches", "Ce qu'il reste a faire", state.tasks.count { !it.done }.takeIf { it > 0 }?.toString(), listOf(Color(0xFFC9EFFF), Color(0xFF92D7F6)), Icons.Filled.CheckCircle, "✅", Color(0xFF2E89C9)),
        ModuleTile(Tab.Shopping, "Courses", "La liste commune du foyer", state.shopping.count { !it.done }.takeIf { it > 0 }?.toString(), listOf(Color(0xFFD3F7DE), Color(0xFF85DFAF)), Icons.Filled.ShoppingCart, "🛒", Color(0xFF139567)),
        ModuleTile(Tab.Requests, "Demandes", "Films, series et livres", state.pendingRequestCount().takeIf { it > 0 }?.toString(), listOf(Color(0xFFE5D8FF), Color(0xFFC9D9FF)), Icons.Filled.ViewList, "🎬", Color(0xFF6B63D8)),
        ModuleTile(Tab.Birthdays, "Anniversaires", "Les dates a ne pas oublier", state.birthdays.size.takeIf { it > 0 }?.toString(), listOf(Color(0xFFFFD6E3), Color(0xFFD8CBFF)), Icons.Filled.Group, "🎂", Color(0xFFB256B4)),
        ModuleTile(Tab.Notes, "Notes", "Idees et pense-betes", state.notes.size.takeIf { it > 0 }?.toString(), listOf(Color(0xFFFFD9B8), Color(0xFFFFB8A8)), Icons.Filled.EditNote, "📝", Clay),
        ModuleTile(Tab.Members, "Foyer", "Membres, couleurs et invitation", state.members.size.toString(), listOf(Color(0xFFD6F4EF), Color(0xFFBCE8F5)), Icons.Filled.Group, "🏡", DeepGreen),
        ModuleTile(Tab.Budget, "Budget", "Charges, loyer et factures", state.bills.count { !it.paid }.takeIf { it > 0 }?.toString(), listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3)), Icons.Filled.Payments, "💶", Color(0xFFFF8F00))
    )
    val visibleModules = modules.toMutableList().apply {
        add(
            4,
            ModuleTile(
                Tab.Activity,
                "Activite",
                "Ce qui bouge dans la maison",
                state.activity.size.takeIf { it > 0 }?.toString(),
                listOf(Color(0xFFFFEDC7), Color(0xFFD9F7EF)),
                Icons.Filled.ViewList,
                "✨",
                Color(0xFFDE8C2D)
            )
        )
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp).navigationBarsPadding()
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "Bonjour 👋",
                fontSize = 15.sp,
                color = Muted,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                state.household?.name ?: "Mon Foyer",
                fontSize = 34.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Black,
                color = Ink
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Tout le quotidien familial, clair et partage.",
                fontSize = 15.sp,
                color = Muted
            )
            Spacer(Modifier.height(16.dp))
        }
        item { HomeInsightStrip(state) }
        if (state.members.isNotEmpty()) {
            item { MemberColorStrip(state.members) }
        }
        items(visibleModules) { tile ->
            ModuleCard(tile = tile, onClick = { vm.select(tile.tab) })
        }
        item { Spacer(Modifier.height(76.dp)) }
    }
}

@Composable
fun ModuleCard(tile: ModuleTile, onClick: () -> Unit) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .background(Brush.horizontalGradient(tile.colors), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        tile.accent,
                        RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
            )
            Spacer(Modifier.width(16.dp))
            Surface(
                color = tile.accent.copy(alpha = 0.13f),
                shape = CircleShape,
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(tile.emoji, fontSize = 24.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(tile.title, fontSize = 19.sp, fontWeight = FontWeight.Black, color = Ink, maxLines = 1)
                Text(tile.subtitle, fontSize = 13.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            tile.count?.let { count ->
                Surface(color = tile.accent, shape = CircleShape, modifier = Modifier.size(34.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(count, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
        }
    }
}

@Composable
fun MemberColorStrip(members: List<Member>) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp), shadowElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Couleurs du foyer", color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            members.take(6).forEach { member ->
                Surface(color = Color(member.color), shape = CircleShape, modifier = Modifier.size(34.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (member.avatar.isNotBlank()) {
                            Text(member.avatar, fontSize = 16.sp)
                        } else {
                            Text(member.name.memberInitial(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
            }
        }
    }
}

@Composable
fun HomeInsightStrip(state: AppUiState) {
    val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    val todayEvents = state.events.count { it.date == today }
    val openTasks = state.tasks.count { !it.done }
    val nextBirthday = state.birthdays.minByOrNull { it.nextBirthday() }
    val remainingShopping = state.shopping.count { !it.done }
    val birthdayLabel = nextBirthday?.let {
        val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), it.nextBirthday()).coerceAtLeast(0)
        "J-$days ${it.name}"
    } ?: "Aucun"
    val insights = listOf(
        "📅 $todayEvents aujourd'hui",
        "✅ $openTasks a faire",
        "🛒 $remainingShopping courses",
        "🎂 $birthdayLabel"
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        userScrollEnabled = false,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(116.dp)
    ) {
        gridItems(insights) { insight ->
            Surface(color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp) {
                Box(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                    Text(insight, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
