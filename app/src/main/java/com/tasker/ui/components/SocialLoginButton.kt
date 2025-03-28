package com.tasker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun SocialLoginButton(
    icon: Int,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val elevation = if (LocalConfiguration.current.isScreenRound) 4.dp else 8.dp
    
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .shadow(elevation, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp))
        }
    }
}