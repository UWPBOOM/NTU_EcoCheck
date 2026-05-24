package com.ntu.electricity.ui.theme

import androidx.compose.ui.graphics.Color

// Material 3 Blue tokens (default)
val NTUBlue = Color(0xFF1B6EF3)
val NTUBlueLight = Color(0xFFAAC7FF)
val NTUBlueDark = Color(0xFF00458C)
val NTUBlueContainer = Color(0xFFD6E3FF)
val NTUBlueOnContainer = Color(0xFF001B3D)

// M3 standard color schemes
data class M3Colors(
    val label: String,
    val lightPrimary: Color,
    val darkPrimary: Color,
    val lightContainer: Color,
    val darkContainer: Color,
    val lightOnContainer: Color,
    val darkOnContainer: Color
)

val m3ColorSchemes = listOf(
    M3Colors("蓝色", Color(0xFF1B6EF3), Color(0xFFAAC7FF), Color(0xFFD6E3FF), Color(0xFF00458C), Color(0xFF001B3D), Color(0xFFD6E3FF)),
    M3Colors("绿色", Color(0xFF1B8A3D), Color(0xFF7BD88D), Color(0xFFC0EBCB), Color(0xFF003918), Color(0xFF002110), Color(0xFFC0EBCB)),
    M3Colors("红色", Color(0xFFBA1A1A), Color(0xFFFFB4AB), Color(0xFFFFDAD6), Color(0xFF93000A), Color(0xFF410002), Color(0xFFFFDAD6)),
    M3Colors("紫色", Color(0xFF6750A4), Color(0xFFD0BCFF), Color(0xFFEADDFF), Color(0xFF4F378B), Color(0xFF21005D), Color(0xFFEADDFF)),
    M3Colors("橙色", Color(0xFFE65100), Color(0xFFFFB59A), Color(0xFFFFDBCE), Color(0xFF8F3300), Color(0xFF3A1300), Color(0xFFFFDBCE)),
    M3Colors("粉色", Color(0xFFC01B7D), Color(0xFFFFAFD5), Color(0xFFFFD9E6), Color(0xFF8E004E), Color(0xFF3A001E), Color(0xFFFFD9E6))
)

val Surface = Color(0xFFFCFCFF)
val SurfaceVariant = Color(0xFFE0E2EC)
val OnSurface = Color(0xFF1A1C20)
val OnSurfaceVariant = Color(0xFF44474E)
val Outline = Color(0xFF74777F)
val OutlineVariant = Color(0xFFC4C6D0)
val Error = Color(0xFFBA1A1A)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF410002)

val SurfaceDark = Color(0xFF1A1C20)
val SurfaceVariantDark = Color(0xFF44474E)
val OnSurfaceDark = Color(0xFFE2E2E9)
val OnSurfaceVariantDark = Color(0xFFC4C6D0)
val OutlineDark = Color(0xFF8E9099)
val OutlineVariantDark = Color(0xFF44474E)
val ErrorDark = Color(0xFFFFB4AB)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)
