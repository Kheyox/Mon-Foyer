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
data class Member(val id: String = "", val name: String = "", val email: String = "", val role: String = "member", val color: Long = 0xFF174C43, val avatar: String = "")
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
data class TmdbMedia(
    val id: Int = 0,
    val title: String = "",
    val mediaType: String = "movie", // "movie" or "tv"
    val posterPath: String = "",
    val backdropPath: String = "",
    val overview: String = "",
    val releaseDate: String = "",
    val voteAverage: Double = 0.0
) {
    val posterUrl get() = if (posterPath.isNotEmpty()) "https://image.tmdb.org/t/p/w342$posterPath" else ""
    val backdropUrl get() = if (backdropPath.isNotEmpty()) "https://image.tmdb.org/t/p/w780$backdropPath" else ""
    val year get() = releaseDate.take(4)
    val ratingDisplay get() = if (voteAverage > 0) "★ ${"%.1f".format(voteAverage)}" else ""
}

data class TmdbProvider(
    val id: Int = 0,
    val name: String = "",
    val logoPath: String = ""
) {
    val logoUrl get() = if (logoPath.isNotEmpty()) "https://image.tmdb.org/t/p/w45$logoPath" else ""
}

data class GoogleBook(
    val id: String = "",
    val title: String = "",
    val authors: List<String> = emptyList(),
    val description: String = "",
    val publishedDate: String = "",
    val publisher: String = "",
    val pageCount: Int = 0,
    val averageRating: Double = 0.0,
    val thumbnailUrl: String = ""
) {
    val authorsDisplay get() = authors.joinToString(", ").ifEmpty { "Auteur inconnu" }
    val year get() = publishedDate.take(4)
    val ratingDisplay get() = if (averageRating > 0) "★ ${"%.1f".format(averageRating)}" else ""
    val coverUrl get() = thumbnailUrl
}

data class MediaRequest(
    val id: String = "",
    val title: String = "",
    val kind: String = "Livre",
    val requesterId: String = "",
    val requesterName: String = "",
    val status: String = "pending",
    val adminNote: String = "",
    val posterUrl: String = "",
    val overview: String = "",
    val year: String = "",
    val tmdbId: Int = 0,
    val voteAverage: Double = 0.0
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
    val updateInfo: UpdateInfo? = null,
    val isOffline: Boolean = false,
    val sharedNote: String = "",
    val tmdbTrending: List<TmdbMedia> = emptyList(),
    val tmdbPopularMovies: List<TmdbMedia> = emptyList(),
    val tmdbPopularTv: List<TmdbMedia> = emptyList(),
    val tmdbSearchResults: List<TmdbMedia> = emptyList(),
    val tmdbSearchQuery: String = "",
    val tmdbSearching: Boolean = false,
    val tmdbDetailProviders: List<TmdbProvider> = emptyList(),
    val booksSearchResults: List<GoogleBook> = emptyList(),
    val booksSearchQuery: String = "",
    val booksSearching: Boolean = false,
    val booksSearchError: Boolean = false
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
