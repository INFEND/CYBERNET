package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v2ray.ang.R

/**
 * Four real actions, not decorative tabs. Keeps connect/disconnect accessible
 * when the list is scrolled and offers quick access to core settings.
 */
@Composable
fun CybernetNavigation(
    connected: Boolean,
    status: String,
    onToggle: () -> Unit,
    onSubscriptions: () -> Unit,
    onRouting: () -> Unit,
    onSettings: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF07130E))
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 1.dp, color = accent.copy(alpha = .24f))
        Text(
            text = status,
            color = if (connected) accent else Color(0xFFA4BBAA),
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            CybernetNavItem(if (connected) "Выкл." else "VPN",
                if (connected) R.drawable.ic_stop_24dp else R.drawable.ic_play_24dp,
                accent, onToggle)
            CybernetNavItem("Серверы", R.drawable.ic_subscriptions_24dp,
                accent, onSubscriptions)
            CybernetNavItem("Маршруты", R.drawable.ic_routing_24dp,
                accent, onRouting)
            CybernetNavItem("Настройки", R.drawable.ic_settings_24dp,
                accent, onSettings)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.CybernetNavItem(
    title: String,
    iconRes: Int,
    accent: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.weight(1f)
            .padding(horizontal = 3.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = title,
            tint = accent,
            modifier = Modifier.size(21.dp)
        )
        Text(title, fontSize = 10.sp, color = Color(0xFFE3F7E9), maxLines = 1)
    }
}
