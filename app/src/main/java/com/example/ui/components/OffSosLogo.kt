package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.TacticalAmber

/**
 * OFFSOS Official Tactical Logo Component.
 * Features the tactical offline mesh beacon insignia with optional animated pulse glow.
 */
@Composable
fun OffSosLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    pulsing: Boolean = true
) {
    val pulseTransition = rememberInfiniteTransition(label = "logo_pulse")
    val scale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(Color(0xFF000000))
            .border(1.dp, Color(0xFFFFFFFF), RoundedCornerShape(size * 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_offsos_logo),
            contentDescription = "OFFSOS Tactical Mesh Logo",
            modifier = Modifier
                .size(size * 0.95f)
                .scale(if (pulsing) scale else 1f),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Full Brand Badge with Logo and Tactical Text
 */
@Composable
fun OffSosBrandBadge(
    modifier: Modifier = Modifier,
    logoSize: Dp = 32.dp
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF000000))
            .border(1.dp, Color(0xFFFFFFFF), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OffSosLogo(size = logoSize, pulsing = true)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "OFFSOS",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontSize = 14.sp
            ),
            color = Color(0xFFFFFFFF)
        )
    }
}
