package com.temankuliah.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.temankuliah.app.ui.theme.Poppins
import com.temankuliah.app.ui.theme.TkPalette
import com.temankuliah.app.ui.theme.tkShadow

/**
 * Satu menu navigasi.
 * [selectedIcon] dipakai saat menu aktif. [keepColors] true untuk ikon dua warna (Beranda) yang
 * membawa warnanya sendiri sehingga tidak diberi tint.
 */
class TkNavItem(
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val selectedIcon: ImageVector = icon,
    val keepColors: Boolean = false
)

// Warna bilah navigasi diambil dari desain Beranda.
private val NavIconActive: Color get() = TkPalette.colors.navIconActive
private val NavLabelActive: Color get() = TkPalette.colors.navLabelActive
private val NavInactive: Color get() = TkPalette.colors.navInactive
private val NavLine: Color get() = TkPalette.colors.dashLine
private val NavSurface: Color get() = TkPalette.colors.surface
private val NavShadow = Color(0xFF2A7EE7)

/**
 * Bilah navigasi bawah gaya desain: putih (gelap: #111A2C) selebar layar, sudut atas membulat 32dp, garis tepi biru muda,
 * ikon garis. Menu aktif berikon biru langit, label biru tebal, dan titik kecil di bawahnya.
 */
@Composable
fun TkBottomNav(
    items: List<TkNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false
) {
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = tkShadow(16.dp),
                shape = shape,
                ambientColor = NavShadow.copy(alpha = 0.12f),
                spotColor = NavShadow.copy(alpha = 0.22f)
            ),
        shape = shape,
        color = NavSurface,
        border = BorderStroke(1.dp, NavLine)
    ) {
        Row(
            Modifier
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                BottomNavItemView(
                    item = item,
                    selected = index == selectedIndex,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp),
                    reducedMotion = reducedMotion
                ) { onSelect(index) }
            }
        }
    }
}

/** Satu menu bilah bawah: ikon 24dp, label 11-12sp maxLines=1 ellipsis, dan titik penanda saat aktif. */
@Composable
private fun BottomNavItemView(
    item: TkNavItem,
    selected: Boolean,
    modifier: Modifier,
    reducedMotion: Boolean,
    onClick: () -> Unit
) {
    val spec: androidx.compose.animation.core.AnimationSpec<Color> =
        if (reducedMotion) snap() else tween(durationMillis = MotionTokens.DurationShort)
    val iconTint by animateColorAsState(
        targetValue = if (selected) NavIconActive else NavInactive,
        animationSpec = spec,
        label = "navIconTint"
    )
    val labelTint by animateColorAsState(
        targetValue = if (selected) NavLabelActive else NavInactive,
        animationSpec = spec,
        label = "navLabelTint"
    )
    val dotColor by animateColorAsState(
        targetValue = if (selected) NavIconActive else Color.Transparent,
        animationSpec = spec,
        label = "navDot"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = MotionTokens.DampingBouncy,
            stiffness = MotionTokens.StiffnessMediumLow
        ),
        label = "navIconScale"
    )
    val dotScale by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.0f,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = MotionTokens.DampingBouncy,
            stiffness = MotionTokens.StiffnessMediumLow
        ),
        label = "navDotScale"
    )
    val dotWidth by animateDpAsState(
        targetValue = if (selected) 12.dp else 3.5.dp,
        animationSpec = if (reducedMotion) snap() else tween(
            durationMillis = MotionTokens.DurationShort,
            easing = MotionTokens.EasingStandard
        ),
        label = "navDotWidth"
    )
    val fontScale = LocalDensity.current.fontScale
    val labelSize = (10.5f / fontScale.coerceAtLeast(1f)).coerceAtLeast(9f).sp
    Column(
        modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(item.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.icon,
            contentDescription = null,
            tint = if (item.keepColors) Color.Unspecified else iconTint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.label,
            color = labelTint,
            fontFamily = Poppins,
            fontSize = labelSize,
            lineHeight = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .height(3.5.dp)
                .width(dotWidth)
                .graphicsLayer {
                    scaleX = dotScale
                    scaleY = dotScale
                }
                .clip(CircleShape)
                .background(dotColor)
        )
    }
}

/** Satu tab rail samping: ikon di atas, label di bawah, warna sama dengan bilah bawah. */
@Composable
private fun RailItemView(
    item: TkNavItem,
    selected: Boolean,
    modifier: Modifier,
    reducedMotion: Boolean = false,
    onClick: () -> Unit
) {
    val spec: androidx.compose.animation.core.AnimationSpec<Color> =
        if (reducedMotion) snap() else tween(durationMillis = MotionTokens.DurationShort)
    val iconTint by animateColorAsState(
        targetValue = if (selected) NavIconActive else NavInactive,
        animationSpec = spec,
        label = "railIconTint"
    )
    val labelTint by animateColorAsState(
        targetValue = if (selected) NavLabelActive else NavInactive,
        animationSpec = spec,
        label = "railLabelTint"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = if (reducedMotion) snap() else spring(
            dampingRatio = MotionTokens.DampingBouncy,
            stiffness = MotionTokens.StiffnessMediumLow
        ),
        label = "railIconScale"
    )
    val fontScale = LocalDensity.current.fontScale
    val labelSize = (10.5f / fontScale.coerceAtLeast(1f)).coerceAtLeast(9f).sp
    Column(
        modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(item.testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.icon,
            contentDescription = null,
            tint = if (item.keepColors) Color.Unspecified else iconTint,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            color = labelTint,
            fontFamily = Poppins,
            fontSize = labelSize,
            lineHeight = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Rail navigasi samping untuk layar lebar (>= 600 dp), selaras dengan bilah navigasi bawah.
 */
@Composable
fun TkNavRail(
    items: List<TkNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false
) {
    Surface(
        modifier = modifier.fillMaxHeight().width(88.dp),
        color = NavSurface,
        shadowElevation = 0.dp
    ) {
        Row(Modifier.fillMaxHeight()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items.forEachIndexed { index, item ->
                    RailItemView(
                        item = item,
                        selected = index == selectedIndex,
                        modifier = Modifier.heightIn(min = 64.dp).fillMaxWidth(),
                        reducedMotion = reducedMotion
                    ) { onSelect(index) }
                }
            }
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(NavLine)
            )
        }
    }
}
