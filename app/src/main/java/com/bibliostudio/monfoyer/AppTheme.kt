package com.bibliostudio.monfoyer

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

internal val Cream = Color(0xFFFFFAF1)
internal val DeepGreen = Color(0xFF103F37)
internal val Leaf = Color(0xFF42A47D)
internal val Mint = Color(0xFFD5F4E8)
internal val Lemon = Color(0xFFFFD86B)
internal val Coral = Color(0xFFFF7E6E)
internal val Sky = Color(0xFFCDEBFF)
internal val Lilac = Color(0xFFE5D8FF)
internal val Apricot = Color(0xFFFFD0A8)
internal val SoftGrey = Color(0xFFF2EEE6)
internal val CardBorder = Color(0xFFE7DDCF)
internal val Ink = Color(0xFF17201D)
internal val Muted = Color(0xFF7F776D)
internal val Paper = Color(0xFFFFFFFB)
internal val Clay = Color(0xFFC96D52)

internal val AppRadius = 22.dp
internal val PanelRadius = 34.dp
internal val FieldRadius = 18.dp

internal val PriorityHigh = Color(0xFFE53935)
internal val PriorityNormal = Color(0xFF174C43)
internal val PriorityLow = Color(0xFF9E9E9E)

internal val MemberAvatars = listOf(
    "🦁","🐼","🦊","🐨","🦋","🐸","🦄","🐬","🦉","🐺",
    "🌸","🌙","⭐","🌈","🍀","🎈","🔥","💎","🎸","🚀",
    "🍕","🎨","🌊","🏔️","🌺","🦅","🐙","🌻","🎭","🎯"
)

private val NunitoRegular = FontFamily(Font(R.font.nunito_regular))
private val NunitoBold = FontFamily(Font(R.font.nunito_bold))

internal val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = NunitoBold),
    headlineLarge = TextStyle(fontFamily = NunitoBold),
    titleLarge = TextStyle(fontFamily = NunitoBold),
    bodyLarge = TextStyle(fontFamily = NunitoRegular),
    bodyMedium = TextStyle(fontFamily = NunitoRegular),
    labelLarge = TextStyle(fontFamily = NunitoBold)
)
