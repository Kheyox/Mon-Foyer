package com.bibliostudio.monfoyer

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import org.json.JSONArray

class MonFoyerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("widget_data", Context.MODE_PRIVATE)
        val tasksJson = prefs.getString("tasks_today", "[]") ?: "[]"
        val eventsJson = prefs.getString("events_today", "[]") ?: "[]"

        val tasks = parseJsonArray(tasksJson).take(3)
        val events = parseJsonArray(eventsJson).take(3)

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(horizontal = 16, vertical = 12)
                ) {
                    // Header
                    Text(
                        text = "Mon Foyer",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF103F37)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    Spacer(GlanceModifier.height(8))

                    if (tasks.isEmpty() && events.isEmpty()) {
                        Text(
                            text = "Tout est calme aujourd'hui",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF7F776D)),
                                fontSize = 14.sp
                            )
                        )
                    } else {
                        // Tasks section
                        if (tasks.isNotEmpty()) {
                            Text(
                                text = "Taches",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF103F37)),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                            tasks.forEach { task ->
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Vertical.CenterVertically
                                ) {
                                    Text(
                                        text = "• $task",
                                        style = TextStyle(
                                            color = ColorProvider(Color(0xFF17201D)),
                                            fontSize = 12.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Events section
                        if (events.isNotEmpty()) {
                            if (tasks.isNotEmpty()) Spacer(GlanceModifier.height(6))
                            Text(
                                text = "Aujourd'hui",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF103F37)),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                            events.forEach { event ->
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Vertical.CenterVertically
                                ) {
                                    Text(
                                        text = "• $event",
                                        style = TextStyle(
                                            color = ColorProvider(Color(0xFF17201D)),
                                            fontSize = 12.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseJsonArray(json: String): List<String> {
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }
}
