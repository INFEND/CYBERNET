package com.v2ray.ang.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Original CYBERNET connection screen. Uses real MainViewModel flags and actions.
 * No simulated connection state, proxy speed or fake test results.
 */
@Composable
fun CybernetHero(
    connected: Boolean,
    canConnect: Boolean,
    onToggle: () -> Unit,
    onCheck: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val indicator by animateColorAsState(
        targetValue = if (connected) primary else Color(0xFF879B90),
        animationSpec = tween(400), label = "realConnectionState"
    )
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
        color = Color(0xFF07130E),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, primary.copy(alpha = .29f), RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 19.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CYBERNET", color = primary, fontWeight = FontWeight.Black,
                        letterSpacing = 2.7.sp, fontSize = 23.sp)
                    Text("SECURE CONNECTION", color = Color(0xFF91AF9B),
                        fontSize = 10.sp, letterSpacing = 1.4.sp)
                }
                Box(Modifier.size(9.dp).background(indicator, CircleShape))
                Text(
                    if (connected) "АКТИВНО" else "ВЫКЛЮЧЕНО",
                    color = indicator, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier.size(118.dp)
                    .border(2.dp, indicator.copy(alpha = .55f), CircleShape)
                    .padding(12.dp)
                    .border(1.dp, indicator.copy(alpha = .28f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (connected) "●" else "○", color = indicator, fontSize = 52.sp,
                    textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(13.dp))
            Text(
                if (connected) "Защита включена"
                else if (canConnect) "Готов к подключению"
                else "Сервер не выбран",
                color = Color(0xFFEAFBF0),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (connected) "Соединение VPN запущено"
                else "Выберите сервер или добавьте подписку",
                color = Color(0xFF8CA898), fontSize = 12.sp,
                modifier = Modifier.padding(top = 5.dp, bottom = 16.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(
                    onClick = onToggle,
                    enabled = connected || canConnect,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary, contentColor = Color(0xFF031B12)
                    ),
                ) {
                    Text(if (connected) "Отключить" else "Подключить",
                        fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onCheck,
                    enabled = connected,
                    shape = RoundedCornerShape(13.dp)
                ) { Text("Проверить") }
            }
        }
    }
}
