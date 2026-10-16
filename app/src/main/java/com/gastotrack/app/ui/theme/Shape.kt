package com.gastotrack.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Google Pixel Material 3 Expressive Shapes
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Specialized Pixel UI Shapes
val PixelPillShape = CircleShape
val PixelCardShape = RoundedCornerShape(26.dp)
val PixelDialogShape = RoundedCornerShape(32.dp)
val PixelFabShape = RoundedCornerShape(20.dp)
val PixelChipShape = RoundedCornerShape(16.dp)
val PixelInputShape = RoundedCornerShape(16.dp)
