package com.temankuliah.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import kotlinx.coroutines.launch

/**
 * Wadah form panjang sebagai modal bottom sheet:
 * - Sudut atas membulat 28dp (TkShape.xl)
 * - Handle drag bersih dan elegan
 * - [sheetGesturesEnabled] = false: menonaktifkan nested scroll connection bawaan ModalBottomSheet
 *   yang menjadi akar masalah getar/guncang (jitter/shake) saat pengguna mengusap ke atas (swipe up)
 *   pada formulir scrollable.
 * - Usap ke bawah pada drag handle tetap didukung untuk menutup sheet secara alami, selain tombol X,
 *   tombol Batal, ketukan pada scrim luar, dan tombol kembali sistem.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TkFormSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val screenHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val maxSheetHeight = (screenHeight * 0.92f).coerceAtLeast(280.dp)
    var dragDownOffset by remember { mutableFloatStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = SurfaceLight,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 8.dp)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            if (delta > 0f) dragDownOffset += delta
                        },
                        onDragStopped = { velocity ->
                            if (velocity > 300f || dragDownOffset > 80f) {
                                coroutineScope.launch {
                                    sheetState.hide()
                                    onDismissRequest()
                                }
                            }
                            dragDownOffset = 0f
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(DividerLight)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .heightIn(max = maxSheetHeight),
            content = content
        )
    }
}
