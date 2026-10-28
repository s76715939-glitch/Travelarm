package com.sstechzone.travelarm.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sstechzone.travelarm.MainTab
import com.sstechzone.travelarm.data.model.AppLanguage

private data class NavItemData(
    val tab: MainTab,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector,
    val labelBangla: String,
    val labelEnglish: String,
    val testTag: String
)

/**
 * Modern Bottom Navigation Bar:
 * - Map tab positioned in the center.
 * - Dynamic full-tab-width top active indicator bar that animates smoothly.
 * - Automatically recalculates tab width based on tab count for future expandability.
 */
@Composable
fun ProfessionalBottomBar(
    currentTab: MainTab,
    language: AppLanguage,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    // Defined with Map (মানচিত্র) in the middle/center
    val navItems = remember {
        listOf(
            NavItemData(
                tab = MainTab.HISTORY,
                iconFilled = Icons.Filled.History,
                iconOutlined = Icons.Outlined.History,
                labelBangla = "ইতিহাস",
                labelEnglish = "History",
                testTag = "nav_tab_history"
            ),
            NavItemData(
                tab = MainTab.MAP,
                iconFilled = Icons.Filled.Map,
                iconOutlined = Icons.Outlined.Map,
                labelBangla = "মানচিত্র",
                labelEnglish = "Map",
                testTag = "nav_tab_map"
            ),
            NavItemData(
                tab = MainTab.SETTINGS,
                iconFilled = Icons.Filled.Settings,
                iconOutlined = Icons.Outlined.Settings,
                labelBangla = "সেটিংস",
                labelEnglish = "Settings",
                testTag = "nav_tab_settings"
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("main_bottom_nav"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            HorizontalDivider(
                thickness = 0.8.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            )

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val tabCount = navItems.size
                val tabWidth = maxWidth / tabCount
                val selectedIndex = navItems.indexOfFirst { it.tab == currentTab }.coerceAtLeast(0)

                // Smooth sliding animation for the full-width indicator
                val indicatorOffset by animateDpAsState(
                    targetValue = tabWidth * selectedIndex,
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                    label = "indicator_offset"
                )

                // Top active indicator bar with dynamic full tab width
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(bottomStart = 2.dp, bottomEnd = 2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentTab == item.tab
                        val label = if (language == AppLanguage.BANGLA) item.labelBangla else item.labelEnglish

                        TopIndicatorNavItem(
                            iconFilled = item.iconFilled,
                            iconOutlined = item.iconOutlined,
                            label = label,
                            isSelected = isSelected,
                            testTag = item.testTag,
                            onClick = { onTabSelected(item.tab) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopIndicatorNavItem(
    iconFilled: ImageVector,
    iconOutlined: ImageVector,
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        animationSpec = tween(durationMillis = 200),
        label = "tab_color"
    )

    Box(
        modifier = modifier
            .height(62.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = activeColor.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSelected) iconFilled else iconOutlined,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = label,
                color = contentColor,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
