package com.bibliostudio.monfoyer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReminderScheduler {
    private const val PREFS = "mon_foyer_reminders"
    private const val KEY_ENABLED = "enabled"
    private const val REQUEST_BASE = 40000
    private const val MAX_REQUESTS = 140

    fun remindersEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setRemindersEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) cancelAll(context)
    }

    fun refresh(context: Context, events: List<Event>, tasks: List<HouseholdTask>, birthdays: List<Birthday>) {
        cancelAll(context)
        if (!remindersEnabled(context)) return

        val now = LocalDateTime.now()
        val reminders = buildList {
            birthdays.forEach { birthday ->
                val next = birthday.nextBirthday()
                addReminder(next.minusDays(7).atTime(9, 0), now, "Anniversaire bientot 🎂", "${birthday.name}, c'est dans 7 jours.")
                addReminder(next.minusDays(1).atTime(9, 0), now, "Anniversaire demain 🎂", "Demain, c'est l'anniversaire de ${birthday.name}.")
                addReminder(next.atTime(9, 0), now, "Anniversaire aujourd'hui 🎉", "Aujourd'hui, c'est l'anniversaire de ${birthday.name}.")
            }
            events.forEach { event ->
                val start = event.startDateTimeOrNull() ?: return@forEach
                addReminder(start.minusDays(1).withHour(18).withMinute(0), now, "Rendez-vous demain 📅", event.title)
                if (!event.allDay) {
                    addReminder(start.minusHours(1), now, "Rendez-vous dans 1h 📅", event.title)
                }
            }
            tasks.filterNot { it.done }.forEach { task ->
                val due = runCatching { LocalDate.parse(task.dueDate) }.getOrNull() ?: return@forEach
                addReminder(due.minusDays(1).atTime(9, 0), now, "Tache pour demain ✅", task.title)
                addReminder(due.atTime(9, 0), now, "Tache a faire aujourd'hui ✅", task.title)
                addReminder(due.plusDays(1).atTime(9, 0), now, "Tache en retard ⚠️", task.title)
            }
        }.sortedBy { it.whenAt }.take(MAX_REQUESTS)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        reminders.forEachIndexed { index, reminder ->
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminder.whenAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                pendingIntent(context, REQUEST_BASE + index, reminder)
            )
        }
    }

    private fun MutableList<Reminder>.addReminder(whenAt: LocalDateTime, now: LocalDateTime, title: String, body: String) {
        if (whenAt.isAfter(now.plusMinutes(1))) add(Reminder(whenAt, title, body))
    }

    private fun Event.startDateTimeOrNull(): LocalDateTime? {
        val day = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
        val hour = runCatching {
            if (allDay) LocalTime.of(9, 0) else LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm", Locale.FRANCE))
        }.getOrDefault(LocalTime.of(9, 0))
        return day.atTime(hour)
    }

    private fun pendingIntent(context: Context, requestCode: Int, reminder: Reminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(ReminderReceiver.EXTRA_ID, requestCode)
            .putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            .putExtra(ReminderReceiver.EXTRA_BODY, reminder.body)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        repeat(MAX_REQUESTS) { index ->
            alarmManager.cancel(pendingIntent(context, REQUEST_BASE + index, Reminder(LocalDateTime.now(), "", "")))
        }
    }

    private data class Reminder(val whenAt: LocalDateTime, val title: String, val body: String)
}
