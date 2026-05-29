package com.bibliostudio.monfoyer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

@Composable
fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        BrandIcon(size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Text("Mon Foyer", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun BrandIcon(size: androidx.compose.ui.unit.Dp = 46.dp) {
    Surface(color = DeepGreen, shape = RoundedCornerShape(size * 0.3f), modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size * 0.6f)) {
                val roof = Path().apply {
                    moveTo(0f, this@Canvas.size.height * 0.54f)
                    lineTo(this@Canvas.size.width * 0.50f, 0f)
                    lineTo(this@Canvas.size.width, this@Canvas.size.height * 0.54f)
                    close()
                }
                drawPath(roof, color = Color(0xFFFFD86B))
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(this.size.width * 0.12f, this.size.height * 0.52f),
                    size = androidx.compose.ui.geometry.Size(this.size.width * 0.76f, this.size.height * 0.50f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFFB2D9CE),
                    topLeft = Offset(this.size.width * 0.36f, this.size.height * 0.68f),
                    size = androidx.compose.ui.geometry.Size(this.size.width * 0.28f, this.size.height * 0.34f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun FlowerMark() {
    Canvas(Modifier.size(38.dp)) {
        val colors = listOf(DeepGreen, Color(0xFFE86675), Color(0xFFE8A64F), Color(0xFF7BC6D4), Color(0xFF9AD7C2), Color(0xFFF2C55A))
        colors.forEachIndexed { index, color ->
            val angle = (index * 60f) * Math.PI.toFloat() / 180f
            val center = Offset(
                x = size.width / 2 + kotlin.math.cos(angle.toDouble()).toFloat() * 12.dp.toPx(),
                y = size.height / 2 + kotlin.math.sin(angle.toDouble()).toFloat() * 12.dp.toPx()
            )
            drawCircle(color = color, radius = 4.5.dp.toPx(), center = center)
        }
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(size.width / 2, size.height / 2))
    }
}

@Composable
fun ModulePanel(title: String, content: LazyListScope.() -> Unit) {
    val mood = moduleMood(title)
    Surface(
        color = Color(0xFFF8F7F5),
        shape = RoundedCornerShape(topStart = PanelRadius, topEnd = PanelRadius),
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp).navigationBarsPadding()
        ) {
            item {
                Box(Modifier.width(58.dp).height(5.dp).clip(RoundedCornerShape(50)).background(CardBorder))
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mood.first, fontSize = 36.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(title, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text(mood.third, fontSize = 14.sp, color = Muted)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
            content()
            item { Spacer(Modifier.height(92.dp)) }
        }
    }
}

@Composable
fun EmptyState(emoji: String, title: String, body: String) {
    Surface(color = Color.White, shape = RoundedCornerShape(28.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder), modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.padding(18.dp)) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Mint.copy(alpha = 0.55f), radius = 58.dp.toPx(), center = Offset(size.width - 12.dp.toPx(), 8.dp.toPx()))
                drawCircle(Lemon.copy(alpha = 0.32f), radius = 34.dp.toPx(), center = Offset(20.dp.toPx(), size.height - 8.dp.toPx()))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Cream, shape = RoundedCornerShape(22.dp)) {
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        HouseMiniMark(emoji)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                    Text(body, fontSize = 14.sp, lineHeight = 17.sp, color = Muted, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun HouseMiniMark(emoji: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(42.dp)) {
            val roof = Path().apply {
                moveTo(size.width * 0.16f, size.height * 0.52f)
                lineTo(size.width * 0.50f, size.height * 0.18f)
                lineTo(size.width * 0.84f, size.height * 0.52f)
            }
            drawPath(roof, DeepGreen)
            drawRoundRect(
                color = DeepGreen,
                topLeft = Offset(size.width * 0.25f, size.height * 0.50f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.50f, size.height * 0.34f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx(), 7.dp.toPx())
            )
            drawCircle(Leaf, radius = 4.dp.toPx(), center = Offset(size.width * 0.74f, size.height * 0.26f))
            drawCircle(Coral, radius = 4.dp.toPx(), center = Offset(size.width * 0.88f, size.height * 0.38f))
        }
        Text(emoji, fontSize = 20.sp, modifier = Modifier.offset(y = 12.dp))
    }
}

@Composable
fun QuickAdd(value: String, onChange: (String) -> Unit, label: String, onAdd: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        SoftInput(value = value, onValueChange = onChange, label = label, modifier = Modifier.weight(1f))
        Surface(color = DeepGreen, shape = RoundedCornerShape(16.dp), modifier = Modifier.size(64.dp).clickable(onClick = onAdd)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = Color.White, modifier = Modifier.size(38.dp))
            }
        }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
fun SoftInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1,
    leadingIcon: ImageVector? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = Muted, fontSize = 18.sp) },
        leadingIcon = leadingIcon?.let { icon ->
            { Icon(icon, contentDescription = null, tint = Muted) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        minLines = minLines,
        shape = RoundedCornerShape(FieldRadius),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DeepGreen,
            unfocusedBorderColor = CardBorder,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        modifier = modifier
    )
}

@Composable
fun PrimaryButton(text: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(FieldRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = DeepGreen,
            disabledContainerColor = Color(0xFFE1E1E1),
            disabledContentColor = Muted
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp, pressedElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().height(64.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun SecondaryButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(FieldRadius),
        colors = ButtonDefaults.buttonColors(containerColor = SoftGrey, contentColor = DeepGreen),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().height(58.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ListRow(content: @Composable RowScope.() -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(AppRadius),
        border = androidx.compose.foundation.BorderStroke(1.4.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
fun StatBubble(label: String, value: String) {
    Surface(color = SoftGrey, shape = RoundedCornerShape(AppRadius), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(label, color = Muted, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text(value, color = DeepGreen, fontSize = 30.sp, lineHeight = 32.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun MonthPreview() {
    val days = (1..30).toList()
    Column {
        Text("Avril 2026", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            userScrollEnabled = false,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(300.dp)
        ) {
            gridItems(days) { day ->
                Box(
                    Modifier.height(58.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (day == 29) DeepGreen else SoftGrey)
                        .padding(8.dp)
                ) {
                    Text(day.toString(), color = if (day == 29) Color.White else Ink, fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
fun DeleteButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Filled.Delete, contentDescription = "Supprimer", tint = Muted) }
}

@Composable
fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Spacer(Modifier.height(12.dp))
    Text(message, color = MaterialTheme.colorScheme.error, modifier = modifier)
}

@Composable
fun CenterMessage(message: String) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        BrandLogo()
        Spacer(Modifier.height(18.dp))
        Text(message, color = DeepGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BoxScope.FloatingHomeButton(visible: Boolean, onClick: () -> Unit) {
    if (!visible) return
    Surface(
        color = Color(0xEE1C1C1E),
        shape = RoundedCornerShape(50),
        shadowElevation = 12.dp,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = 20.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text("Accueil", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RoundIconButton(icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Surface(
        color = Paper,
        shape = CircleShape,
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder.copy(alpha = 0.65f)),
        modifier = Modifier.size(52.dp).clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    confirmLabel: String = "Supprimer",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Black, color = Ink) },
        text = { Text(message, color = Muted) },
        confirmButton = {
            Text(
                confirmLabel,
                color = Color(0xFFE86675),
                fontWeight = FontWeight.Black,
                modifier = Modifier.clickable(onClick = onConfirm).padding(12.dp)
            )
        },
        dismissButton = {
            Text(
                "Annuler",
                color = DeepGreen,
                fontWeight = FontWeight.Black,
                modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp)
            )
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun TaskFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) DeepGreen else SoftGrey,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            color = if (selected) Color.White else Muted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp)
        )
    }
}

@Composable
fun MemberChip(label: String, selected: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) color else SoftGrey,
        shape = RoundedCornerShape(50),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            color = if (selected) Color.White else Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            maxLines = 2,
            lineHeight = 16.sp,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
fun MemberPicker(members: List<Member>, selectedMemberId: String, onSelect: (String) -> Unit) {
    Text("Pour qui ?", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Ink)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (members.isEmpty()) {
            MemberChip(label = "Tout le foyer", selected = selectedMemberId.isBlank(), color = DeepGreen) { onSelect("") }
        } else {
            members.take(4).forEach { member ->
                MemberChip(
                    label = member.name.ifBlank { "Membre" },
                    selected = selectedMemberId == member.id,
                    color = memberColor(member.id)
                ) { onSelect(member.id) }
            }
        }
    }
}

@Composable
fun DateChip(date: LocalDate) {
    Surface(color = Color.White, shape = RoundedCornerShape(AppRadius), border = androidx.compose.foundation.BorderStroke(2.dp, DeepGreen), modifier = Modifier.fillMaxWidth()) {
        Text(
            date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE)),
            color = DeepGreen,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@Composable
fun CompactField(text: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().height(66.dp).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = Muted, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(icon, contentDescription = null, tint = Muted, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun CompactTextField(text: String, tint: Color = Muted, onClick: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(FieldRadius), border = androidx.compose.foundation.BorderStroke(1.5.dp, CardBorder), modifier = Modifier.fillMaxWidth().height(64.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = tint, fontSize = 19.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Muted)
        }
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
    Spacer(Modifier.height(8.dp))
}

@Composable
fun DateField(date: LocalDate, onClick: () -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(FieldRadius),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD9D9D9)),
        modifier = Modifier.fillMaxWidth().height(72.dp).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRANCE)),
                color = Muted,
                fontSize = 21.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Muted, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
fun EmojiPicker(selected: String, onSelect: (String) -> Unit) {
    val emojis = listOf("🙂", "🏠", "🛒", "📞", "🧹", "🍽", "💸", "📦", "🎂", "📝")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        emojis.forEach { item ->
            Surface(
                color = if (item == selected) DeepGreen.copy(alpha = 0.14f) else SoftGrey,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp).clickable { onSelect(item) }
            ) {
                Box(contentAlignment = Alignment.Center) { Text(item, fontSize = 22.sp) }
            }
        }
    }
}

@Composable
fun <T> PickerColumn(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    LaunchedEffect(values, selected) {
        val index = values.indexOf(selected).coerceAtLeast(0)
        listState.scrollToItem(index)
    }
    Box(modifier.height(238.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(values) { value ->
                val isSelected = value == selected
                Surface(
                    color = if (isSelected) SoftGrey else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).clickable { onSelect(value) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            label(value),
                            fontSize = if (isSelected) 26.sp else 22.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) Ink else Muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Snackbar maison : pilule sombre en bas, auto-disparition, action optionnelle (ex. Annuler).
 * Pilotee par un SnackbarEvent porteur d'un id unique (re-declenche l'affichage).
 */
@Composable
fun FoyerSnackbar(event: SnackbarEvent?, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<SnackbarEvent?>(null) }

    LaunchedEffect(event?.id) {
        if (event != null) {
            current = event
            visible = true
            kotlinx.coroutines.delay(if (event.action != null) 4200 else 2600)
            visible = false
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = visible && current != null,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 2 },
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it / 2 },
        modifier = modifier
    ) {
        val evt = current
        Surface(
            color = Color(0xF21C1C1E),
            shape = RoundedCornerShape(16.dp),
            shadowElevation = 10.dp,
            modifier = Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 86.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 18.dp, end = 8.dp).height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    evt?.message ?: "",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (evt?.actionLabel != null) {
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            visible = false
                            evt.action?.invoke()
                        }
                    ) {
                        Text(
                            evt.actionLabel,
                            color = Lemon,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSheetScaffold(title: String, emoji: String, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = PanelRadius, topEnd = PanelRadius)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 24.dp).navigationBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(emoji, fontSize = 34.sp)
                Spacer(Modifier.width(12.dp))
                Text(title, fontSize = 28.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer", tint = Ink, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            content()
            Spacer(Modifier.height(18.dp))
        }
    }
}
