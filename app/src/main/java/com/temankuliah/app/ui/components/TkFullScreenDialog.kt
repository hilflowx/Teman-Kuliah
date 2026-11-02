package com.temankuliah.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.temankuliah.app.ui.icons.Close
import com.temankuliah.app.ui.theme.BackgroundLight
import com.temankuliah.app.ui.theme.TkSpace
import com.temankuliah.app.ui.theme.TkText

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer

private val FullScreenDialogMaxWidth = 720.dp

/**
 * Dialog layar penuh (sesuai desain "Redesain alur impor KRS & KHS"): menutupi seluruh layar dengan
 * latar halaman, menghormati status bar, navigation bar, dan keyboard. Isi disusun oleh pemanggil,
 * biasanya diawali [TkCloseHeader].
 */
@Composable
fun TkFullScreenDialog(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val alpha by animateFloatAsState(
        targetValue = if (appeared || reducedMotion) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = MotionTokens.EasingDecelerate),
        label = "fullScreenAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (appeared || reducedMotion) 0f else 24f,
        animationSpec = spring(
            dampingRatio = MotionTokens.DampingNatural,
            stiffness = MotionTokens.StiffnessMediumLow
        ),
        label = "fullScreenOffsetY"
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha
                    this.translationY = offsetY * density
                },
            color = BackgroundLight
        ) {
            // Di tablet isi dibatasi dan dipusatkan; latar tetap memenuhi layar.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier
                        .widthIn(max = FullScreenDialogMaxWidth)
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .displayCutoutPadding()
                        .imePadding(),
                    content = content
                )
            }
        }
    }
}

/** Header dialog layar penuh: tombol tutup "X" berbingkai di kiri, judul dan keterangan di sebelahnya. */
@Composable
fun TkCloseHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    closeDescription: String = "Tutup"
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TkSpace.page, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TkHeaderIconButton(icon = Icons.Default.Close, contentDescription = closeDescription, onClick = onClose)
        Spacer(Modifier.width(12.dp))
        TkTitleBlock(
            title = title,
            supporting = supporting,
            titleStyle = TkText.screenTitle,
            modifier = Modifier.weight(1f)
        )
    }
}
