package com.bibliostudio.monfoyer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Household(val id: String = "", val name: String = "Mon foyer", val inviteCode: String = "", val ownerId: String = "")
data class Member(val id: String = "", val name: String = "", val email: String = "", val role: String = "member", val color: Long = 0xFF174C43)
data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val done: Boolean = false,
    val quantity: Int = 1,
    val category: String = "Epicerie",
    val favorite: Boolean = false
)
data class Bill(val id: String = "", val label: String = "", val amount: Double = 0.0, val paid: Boolean = false)
data class Event(
    val id: String = "",
    val title: String = "",
    val owner: String = "",
    val date: String = "",
    val description: String = "",
    val location: String = "",
    val typeName: String = "Repas",
    val typeIcon: String = "🍴",
    val typeColor: Long = 0xFFE86675,
    val allDay: Boolean = false,
    val time: String = "00:00",
    val recurrence: String = "Aucune"
)
data class EventType(val id: String = "", val name: String = "", val icon: String = "🍴", val color: Long = 0xFFE86675)
data class Note(val id: String = "", val title: String = "", val body: String = "")
data class HouseholdTask(
    val id: String = "",
    val title: String = "",
    val assigneeId: String = "",
    val assigneeName: String = "",
    val done: Boolean = false,
    val color: Long = 0xFF174C43,
    val description: String = "",
    val dueDate: String = "",
    val emoji: String = "🙂",
    val repeatInterval: String = "none",
    val priority: String = "normal", // "high", "normal", "low"
    val completedAt: Long = 0L
)
data class Birthday(val id: String = "", val name: String = "", val date: String = "", val birthYear: Int = 0)
data class MediaRequest(
    val id: String = "",
    val title: String = "",
    val kind: String = "Livre",
    val requesterId: String = "",
    val requesterName: String = "",
    val status: String = "pending",
    val adminNote: String = ""
)
data class ActivityItem(
    val id: String = "",
    val text: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val color: Long = 0xFF174C43,
    val createdAtMillis: Long = 0L
)
data class UpdateInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val notes: String = ""
)

data class AppUiState(
    val signedIn: Boolean = false,
    val currentUserId: String = "",
    val userName: String = "",
    val household: Household? = null,
    val members: List<Member> = emptyList(),
    val shopping: List<ShoppingItem> = emptyList(),
    val bills: List<Bill> = emptyList(),
    val events: List<Event> = emptyList(),
    val eventTypes: List<EventType> = emptyList(),
    val notes: List<Note> = emptyList(),
    val tasks: List<HouseholdTask> = emptyList(),
    val birthdays: List<Birthday> = emptyList(),
    val mediaRequests: List<MediaRequest> = emptyList(),
    val activity: List<ActivityItem> = emptyList(),
    val monthlyBudget: Double = 0.0,
    val selectedTab: Tab = Tab.Home,
    val loading: Boolean = true,
    val error: String? = null,
    val checkingUpdate: Boolean = false,
    val updateInfo: UpdateInfo? = null
)

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Accueil", Icons.Filled.Home),
    Shopping("Courses", Icons.Filled.ShoppingCart),
    Tasks("Taches", Icons.Filled.CheckCircle),
    Calendar("Agenda", Icons.Filled.CalendarMonth),
    Requests("Demandes", Icons.Filled.ViewList),
    Activity("Activite", Icons.Filled.ViewList),
    Birthdays("Anniversaires", Icons.Filled.Group),
    Notes("Notes", Icons.Filled.EditNote),
    Members("Foyer", Icons.Filled.Group)
}

data class ModuleTile(
    val tab: Tab,
    val title: String,
    val subtitle: String,
    val count: String?,
    val colors: List<Color>,
    val icon: ImageVector,
    val emoji: String,
    val accent: Color
)

data class MemberStats(
    val doneTasks: Int = 0,
    val openTasks: Int = 0,
    val pendingRequests: Int = 0,
    val birthdaysAdded: Int = 0
)
