package com.bibliostudio.monfoyer

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.absoluteValue

fun memberColorLong(seed: String): Long {
    val palette = listOf(0xFF174C43, 0xFFE86675, 0xFFE8A64F, 0xFF5C8EE6, 0xFF8A6FDF, 0xFF2F9C95)
    return palette[(seed.hashCode().absoluteValue) % palette.size]
}

fun Long.activityAgeLabel(): String {
    if (this <= 0L) return "maintenant"
    val minutes = ((System.currentTimeMillis() - this) / 60000).coerceAtLeast(0)
    return when {
        minutes < 1 -> "maintenant"
        minutes < 60 -> "${minutes} min"
        minutes < 1440 -> "${minutes / 60} h"
        else -> "${minutes / 1440} j"
    }
}

fun String.mediaStatusLabel(): String = when (this) {
    "approved" -> "Valide"
    "rejected" -> "Refuse"
    else -> "En attente"
}

fun memberColor(seed: String): Color = Color(memberColorLong(seed))

fun String.memberInitial(): String {
    val clean = trim()
    return clean.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
}

fun shoppingCategory(name: String): String {
    val value = name.lowercase(Locale.FRANCE)
    return when {
        listOf("lait", "fromage", "yaourt", "beurre", "creme", "salade", "tomate", "pomme", "banane", "viande", "poisson", "oeuf").any { it in value } -> "Frais"
        listOf("shampoing", "savon", "dentifrice", "gel douche", "coton", "rasoir", "lessive").any { it in value } -> "Hygiene"
        listOf("sopalin", "papier", "sac", "eponge", "produit", "nettoyant", "ampoule").any { it in value } -> "Maison"
        else -> "Epicerie"
    }
}

fun moduleMood(title: String): Triple<String, Color, String> = when {
    "course" in title.lowercase(Locale.FRANCE) -> Triple("🛒", Color(0xFFE0F8E7), "La liste commune, claire et rapide.")
    "agenda" in title.lowercase(Locale.FRANCE) || "calendrier" in title.lowercase(Locale.FRANCE) -> Triple("📅", Color(0xFFFFF0C8), "Les rendez-vous du foyer.")
    "tache" in title.lowercase(Locale.FRANCE) -> Triple("✅", Color(0xFFE1F4FF), "Qui fait quoi, sans flou.")
    "demande" in title.lowercase(Locale.FRANCE) -> Triple("🎬", Color(0xFFE9E1FF), "Films, series et livres a valider.")
    "anniversaire" in title.lowercase(Locale.FRANCE) -> Triple("🎂", Color(0xFFFFE0EA), "Les dates qui comptent.")
    "note" in title.lowercase(Locale.FRANCE) -> Triple("📝", Color(0xFFFFE7D4), "Les idees et pense-betes.")
    "foyer" in title.lowercase(Locale.FRANCE) -> Triple("🏡", Color(0xFFE0F5F1), "Membres, couleurs et invitation.")
    "depense" in title.lowercase(Locale.FRANCE) -> Triple("⚖️", Color(0xFFFFF0D9), "Qui a payé quoi.")
    "recette" in title.lowercase(Locale.FRANCE) -> Triple("🍽️", Color(0xFFFFF3E0), "Les plats du foyer.")
    "contact" in title.lowercase(Locale.FRANCE) -> Triple("📞", Color(0xFFE8F5E9), "Les numéros qui comptent.")
    else -> Triple("✨", SoftGrey, "Un espace simple et vivant.")
}

fun defaultEventTypes(): List<EventType> = listOf(
    EventType(id = "meal", name = "Repas", icon = "🍴", color = 0xFFE86675),
    EventType(id = "medical", name = "Medical", icon = "🏥", color = 0xFF54B568),
    EventType(id = "school", name = "Ecole", icon = "🏫", color = 0xFF5C8EE6),
    EventType(id = "leisure", name = "Loisirs", icon = "⚽", color = 0xFFE8A64F)
)

fun nextTimeValue(value: String): String {
    val parts = value.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val nextMinute = if (minute == 0) 30 else 0
    val nextHour = if (minute == 0) hour else (hour + 1) % 24
    return "%02d:%02d".format(nextHour, nextMinute)
}

fun Birthday.markerEvent(year: Int): Event? {
    val dateValue = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    val markerDate = runCatching { LocalDate.of(year, dateValue.month, dateValue.dayOfMonth) }.getOrNull() ?: return null
    return Event(id = id, title = name, owner = "Anniversaire", date = markerDate.format(DateTimeFormatter.ISO_DATE))
}

fun Birthday.nextBirthday(): LocalDate {
    val value = runCatching { LocalDate.parse(date) }.getOrNull() ?: LocalDate.now()
    val now = LocalDate.now()
    val thisYear = runCatching { LocalDate.of(now.year, value.month, value.dayOfMonth) }.getOrDefault(now)
    return if (thisYear.isBefore(now)) thisYear.plusYears(1) else thisYear
}

fun String.taskDueLabel(): String {
    val date = runCatching { LocalDate.parse(this) }.getOrNull() ?: return "Sans date"
    val today = LocalDate.now()
    return when (date) {
        today -> "Aujourd'hui"
        today.plusDays(1) -> "Demain"
        today.minusDays(1) -> "Hier"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE))
    }
}

fun LocalDate.taskDueLabel(): String = format(DateTimeFormatter.ISO_DATE).taskDueLabel()

fun moneyText(value: Double): String = String.format(Locale.FRANCE, "%.2f EUR", value)

fun String.parseMoneyOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

fun AppUiState.isCurrentUserAdmin(): Boolean =
    members.firstOrNull { it.id == currentUserId }?.role == "admin" || household?.ownerId == currentUserId

fun AppUiState.pendingRequestCount(): Int =
    if (isCurrentUserAdmin()) mediaRequests.count { it.status == "pending" }
    else mediaRequests.count { it.requesterId == currentUserId && it.status == "pending" }

fun AppUiState.memberStats(memberId: String): MemberStats = MemberStats(
    doneTasks = tasks.count { it.assigneeId == memberId && it.done },
    openTasks = tasks.count { it.assigneeId == memberId && !it.done },
    pendingRequests = mediaRequests.count { it.requesterId == memberId && it.status == "pending" },
    birthdaysAdded = activity.count { item ->
        item.actorId == memberId && item.text.lowercase(Locale.FRANCE).contains("anniversaire")
    }
)

fun HouseholdTask.isOverdue(): Boolean {
    val date = runCatching { LocalDate.parse(dueDate) }.getOrNull() ?: return false
    return date.isBefore(LocalDate.now()) && !done
}

fun requestStatusLabel(status: String): String = when (status) {
    "approved" -> "Validee"
    "rejected" -> "Refusee"
    else -> "En attente"
}

fun exportEventToCalendar(context: Context, event: Event) {
    val intent = Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, event.title)
        putExtra(CalendarContract.Events.DESCRIPTION, event.description)
        putExtra(CalendarContract.Events.EVENT_LOCATION, event.location)
        val ldt = LocalDateTime.parse("${event.date}T${event.time.ifBlank { "00:00" }}:00")
        val millis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, millis)
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, millis + 3600_000)
        putExtra(CalendarContract.Events.ALL_DAY, event.allDay)
    }
    context.startActivity(intent)
}
