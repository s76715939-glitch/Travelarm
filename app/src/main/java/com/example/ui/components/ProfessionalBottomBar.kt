package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainTab
import com.example.data.model.AppLanguage

/**
 * Modern Docked Bottom Navigation Bar.
 * - Docked at the bottom of the screen (normal, non-floating container).
 * - Left side: History tab.
 * - Center (raised slightly above): Maps button.
 * - Right side: Settings tab.
 * - Instant rendering and responsive touch feedback.
 */
@Composable
fun ProfessionalBottomBar(
    currentTab: MainTab,
    language: AppLanguage,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("main_bottom_nav"),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(
                    thickness = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomSideTabItem(
                        iconFilled = Icons.Filled.History,
                        iconOutlined = Icons.Outlined.History,
                        label = if (language == AppLanguage.BANGLA) "ইতিহাস" else "History",
                        isSelected = currentTab == MainTab.HISTORY,
                        testTag = "nav_tab_history",
                        onClick = { onTabSelected(MainTab.HISTORY) },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.size(76.dp))

                    BottomSideTabItem(
                        iconFilled = Icons.Filled.Settings,
                        iconOutlined = Icons.Outlined.Settings,
                        label = if (language == AppLanguage.BANGLA) "সেটিংস" else "Settings",
                        isSelected = currentTab == MainTab.SETTINGS,
                        testTag = "nav_tab_settings",
                        onClick = { onTabSelected(MainTab.SETTINGS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        val isMapSelected = currentTab == MainTab.MAP
        val centerScale by animateFloatAsState(
            targetValue = if (isMapSelected) 1.05f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            label = "center_scale"
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .offset(y = (-14).dp)
                .scale(centerScale),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(
                        elevation = if (isMapSelected) 10.dp else 6.dp,
                        shape = CircleShape,
                        spotColor = if (isMapSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.3f)
                    )
                    .clip(CircleShape)
                    .background(
                        if (isMapSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White),
                        onClick = { onTabSelected(MainTab.MAP) }
                    )
                    .testTag("nav_tab_map"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMapSelected) Icons.Filled.Map else Icons.Outlined.Map,
                    contentDescription = if (language == AppLanguage.BANGLA) "মানচিত্র" else "Map",
                    tint = if (isMapSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = if (language == AppLanguage.BANGLA) "মানচিত্র" else "Map",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = if (isMapSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isMapSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BottomSideTabItem(
    iconFilled: ImageVector,
    iconOutlined: ImageVector,
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        label = "icon_color"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
        label = "text_color"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .testTag(testTag)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) iconFilled else iconOutlined,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(23.dp)
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Box(
                modifier = Modifier
                    .size(width = if (isSelected) 14.dp else 0.dp, height = 2.5.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            )
        }
    }
}
