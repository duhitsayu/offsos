package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BroadcastingState
import com.example.ui.theme.TacticalAlertRed

enum class OffSosNavDestination(val label: String) {
    MESH("MESH"),
    MAP("MAP"),
    CHAT("CHAT"),
    PROFILE("PROFILE")
}

@Composable
fun OffSosBottomBar(
    currentDestination: OffSosNavDestination,
    broadcastingState: BroadcastingState,
    onNavigate: (OffSosNavDestination) -> Unit,
    onToggleBroadcasting: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBroadcasting = broadcastingState == BroadcastingState.BROADCASTING

    val pulseTransition = rememberInfiniteTransition(label = "btn_pulse")
    val pulseSize by pulseTransition.animateFloat(
        initialValue = 48f,
        targetValue = 56f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btn_pulse_size"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. MESH
            NavItem(
                label = "MESH",
                icon = if (currentDestination == OffSosNavDestination.MESH) Icons.Filled.Hub else Icons.Outlined.Hub,
                isSelected = currentDestination == OffSosNavDestination.MESH,
                onClick = { onNavigate(OffSosNavDestination.MESH) },
                testTag = "nav_mesh"
            )

            // 2. MAP
            NavItem(
                label = "MAP",
                icon = if (currentDestination == OffSosNavDestination.MAP) Icons.Filled.Map else Icons.Outlined.Map,
                isSelected = currentDestination == OffSosNavDestination.MAP,
                onClick = { onNavigate(OffSosNavDestination.MAP) },
                testTag = "nav_map"
            )

            // 3. Center ONE-TAP SOS BROADCAST Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(y = (-8).dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false, radius = 30.dp),
                        onClick = onToggleBroadcasting
                    )
                    .testTag("one_tap_sos_button")
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isBroadcasting) pulseSize.dp else 50.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            if (isBroadcasting) TacticalAlertRed else MaterialTheme.colorScheme.onBackground,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Emergency,
                        contentDescription = "One-Tap SOS Broadcast",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isBroadcasting) "BROADCASTING" else "SOS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp
                    ),
                    color = if (isBroadcasting) TacticalAlertRed else MaterialTheme.colorScheme.onBackground
                )
            }

            // 4. CHAT
            NavItem(
                label = "CHAT",
                icon = if (currentDestination == OffSosNavDestination.CHAT) Icons.Filled.Forum else Icons.Outlined.Forum,
                isSelected = currentDestination == OffSosNavDestination.CHAT,
                onClick = { onNavigate(OffSosNavDestination.CHAT) },
                testTag = "nav_chat"
            )

            // 5. PROFILE
            NavItem(
                label = "PROFILE",
                icon = if (currentDestination == OffSosNavDestination.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                isSelected = currentDestination == OffSosNavDestination.PROFILE,
                onClick = { onNavigate(OffSosNavDestination.PROFILE) },
                testTag = "nav_profile"
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 24.dp),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 9.sp,
                letterSpacing = 0.6.sp
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}
