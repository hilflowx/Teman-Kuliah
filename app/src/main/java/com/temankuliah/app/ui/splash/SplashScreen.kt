package com.temankuliah.app.ui.splash

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toDrawable
import com.temankuliah.app.ui.components.AppLogoBadge
import com.temankuliah.app.ui.components.LightSystemBarIcons
import com.temankuliah.app.ui.components.rememberReducedMotion
import com.temankuliah.app.ui.theme.Poppins
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// ---- Ukuran dan warna sesuai mockup (1080 px = 360 dp, jadi 1 dp = 3 px) ------------------------
private val IconSize = 112.dp
private val HaloInnerSize = 132.dp
private val HaloOuterSize = 200.dp
private val BarWidth = 120.dp
private val BarHeight = 5.dp

/** Pusat ikon berada di 40,75% tinggi layar (978 px dari 2400 px pada mockup). */
private const val ICON_CENTER_FRACTION = 0.4075f

private val SplashGradient = Brush.verticalGradient(
    0f to Color(0xFF3A8DF2),
    0.5f to Color(0xFF2577E4),
    0.67f to Color(0xFF1F69D4),
    1f to Color(0xFF1653BB)
)
private val DecorCircleColor = Color.White.copy(alpha = 0.07f)
private val HaloOuterColor = Color.White.copy(alpha = 0.10f)
private val HaloInnerColor = Color.White.copy(alpha = 0.08f)

/**
 * Pembungkus splash. [content] (login atau Beranda) baru dibuat saat transisi keluar dimulai, lalu
 * terlihat karena splash di atasnya memudar. Tanpa penggandaan lapisan alpha pada layar tujuan.
 *
 * [finished] disimpan oleh pemanggil (rememberSaveable) supaya splash tidak diulang setelah rotasi.
 */
@Composable
internal fun SplashHost(
    finished: Boolean,
    dataReady: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var revealing by remember { mutableStateOf(false) }

    // Jendela memakai latar gradasi biru selama peluncuran (lihat themes.xml) agar tidak ada kilat putih.
    // Setelah splash selesai, kembalikan ke latar terang bawaan supaya layar lain tidak berubah.
    val view = LocalView.current
    LaunchedEffect(finished) {
        if (finished) {
            runCatching {
                (view.context as? android.app.Activity)?.window
                    ?.setBackgroundDrawable(0xFFFAFAFA.toInt().toDrawable())
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        if (finished || revealing) content()
        if (!finished) {
            SplashScreen(
                dataReady = dataReady,
                onExitStart = { revealing = true },
                onFinished = onFinished
            )
        }
    }
}

/** Semua nilai animasi splash. Dibaca hanya di fase layer/draw (graphicsLayer), tanpa recomposition. */
@Stable
internal class SplashAnimState(val reduced: Boolean) {
    val bg = Animatable(1f)
    val iconAlpha = Animatable(0f)
    val iconScale = Animatable(if (reduced) 1f else SplashMotion.ICON_SCALE_FROM)
    val haloInner = Animatable(0f)
    val haloOuter = Animatable(0f)
    val pulse = Animatable(1f)

    /** Sisa jarak naik judul/subjudul sebagai pecahan [SplashMotion.TITLE_RISE]: 1 -> 0. */
    val titleAlpha = Animatable(0f)
    val titleRise = Animatable(if (reduced) 0f else 1f)
    val subtitleAlpha = Animatable(0f)
    val subtitleRise = Animatable(if (reduced) 0f else 1f)
    val footerAlpha = Animatable(0f)
    val progress = Animatable(0f)

    /** Pengali opasitas judul, bar, dan teks status saat keluar: 1 -> 0. */
    val exitContent = Animatable(1f)

    /** Kemajuan keluar ikon dan halo: 0 -> 1. */
    val exitIcon = Animatable(0f)

    /** Kemajuan memudarnya latar splash: 0 -> 1. */
    val exitBg = Animatable(0f)

    suspend fun playEntrance(): Unit = coroutineScope {
        val m = SplashMotion
        if (reduced) {
            // Reduce motion: tanpa scale, geser, overshoot, denyut, dan drift. Semua hanya fade.
            val spec = tween<Float>(m.REDUCED_FADE_MS, easing = m.EaseEnter)
            launch { iconAlpha.animateTo(1f, spec) }
            launch { haloInner.animateTo(1f, spec) }
            launch { haloOuter.animateTo(1f, spec) }
            launch { titleAlpha.animateTo(1f, spec) }
            launch { subtitleAlpha.animateTo(1f, spec) }
            launch { footerAlpha.animateTo(1f, spec) }
            return@coroutineScope
        }

        // 1) Latar langsung aktif (1f) agar menyatu dengan latar jendela cold start
        bg.snapTo(1f)
        // 2) Ikon: fade + spring scale dengan overshoot kecil
        launch {
            delay(m.ICON_DELAY_MS)
            iconAlpha.animateTo(1f, tween(m.ICON_FADE_MS, easing = m.EaseEnter))
        }
        launch {
            delay(m.ICON_DELAY_MS)
            iconScale.animateTo(
                1f,
                spring(dampingRatio = m.ICON_SPRING_DAMPING, stiffness = m.ICON_SPRING_STIFFNESS)
            )
        }
        // 3) Halo: mengembang bergantian, lalu berdenyut
        launch {
            delay(m.HALO_INNER_DELAY_MS)
            haloInner.animateTo(1f, tween(m.HALO_EXPAND_MS, easing = m.EaseEnter))
        }
        launch {
            delay(m.HALO_OUTER_DELAY_MS)
            haloOuter.animateTo(1f, tween(m.HALO_EXPAND_MS, easing = m.EaseEnter))
        }
        launch {
            delay(m.HALO_OUTER_DELAY_MS + m.HALO_EXPAND_MS)
            pulse.animateTo(
                m.HALO_PULSE_SCALE,
                infiniteRepeatable(
                    animation = tween(m.HALO_PULSE_CYCLE_MS / 2, easing = m.EaseBreathing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
        // 4) Judul, lalu subjudul
        launch {
            delay(m.TITLE_DELAY_MS)
            titleAlpha.animateTo(1f, tween(m.TITLE_MS, easing = m.EaseEnter))
        }
        launch {
            delay(m.TITLE_DELAY_MS)
            titleRise.animateTo(0f, tween(m.TITLE_MS, easing = m.EaseEnter))
        }
        launch {
            delay(m.TITLE_DELAY_MS + m.SUBTITLE_STAGGER_MS)
            subtitleAlpha.animateTo(1f, tween(m.TITLE_MS, easing = m.EaseEnter))
        }
        launch {
            delay(m.TITLE_DELAY_MS + m.SUBTITLE_STAGGER_MS)
            subtitleRise.animateTo(0f, tween(m.TITLE_MS, easing = m.EaseEnter))
        }
        // 5) Bar loading dan teks status
        launch {
            delay(m.FOOTER_DELAY_MS)
            footerAlpha.animateTo(1f, tween(m.FOOTER_FADE_MS, easing = m.EaseEnter))
        }
    }

    /**
     * 6) Bar terisi 0 -> 100% (ease-in-out). Bila data belum siap saat mencapai 90%, bar berhenti di sana
     * sampai data siap, lalu menyelesaikan sisanya.
     */
    suspend fun runProgress(dataReady: State<Boolean>): Unit = coroutineScope {
        val m = SplashMotion
        delay(m.BAR_FILL_DELAY_MS)
        val fill = launch { progress.animateTo(1f, tween(m.BAR_FILL_MS, easing = m.EaseInOutStandard)) }
        snapshotFlow { progress.value >= m.BAR_HOLD_FRACTION }.first { it }
        if (SplashGate.shouldHoldBar(progress.value, dataReady.value)) {
            fill.cancelAndJoin()
            snapshotFlow { dataReady.value }.first { it }
            progress.animateTo(1f, tween(m.BAR_FINISH_MS, easing = m.EaseEnter))
        } else {
            fill.join()
        }
    }

    /** Transisi keluar: judul/bar/status memudar, ikon mengecil ke 0,9 sambil memudar, lalu latar memudar. */
    suspend fun playExit(): Unit = coroutineScope {
        val m = SplashMotion
        val textMs = if (reduced) m.REDUCED_FADE_MS else m.EXIT_TEXT_MS
        val iconMs = if (reduced) m.REDUCED_FADE_MS else m.EXIT_ICON_MS
        val revealDelay = if (reduced) 0L else m.EXIT_REVEAL_DELAY_MS
        val revealMs = if (reduced) m.REDUCED_FADE_MS else m.EXIT_REVEAL_MS
        launch { exitContent.animateTo(0f, tween(textMs, easing = m.EaseExit)) }
        launch { exitIcon.animateTo(1f, tween(iconMs, easing = m.EaseExit)) }
        launch {
            delay(revealDelay)
            exitBg.animateTo(1f, tween(revealMs, easing = m.EaseEnter))
        }
    }
}

@Composable
internal fun SplashScreen(
    dataReady: Boolean,
    onExitStart: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = rememberReducedMotion()
) {
    LightSystemBarIcons()

    val m = remember(reducedMotion) { SplashAnimState(reducedMotion) }
    val dataReadyState = rememberUpdatedState(dataReady)
    val onExitStartState = rememberUpdatedState(onExitStart)
    val onFinishedState = rememberUpdatedState(onFinished)

    LaunchedEffect(m) {
        val startMs = SystemClock.elapsedRealtime()
        launch { m.playEntrance() }
        launch { m.runProgress(dataReadyState) }

        // Keluar bila data siap DAN bar penuh DAN waktu minimal terlewati; paling lambat di batas atas.
        withTimeoutOrNull(SplashMotion.MAX_VISIBLE_MS) {
            snapshotFlow { dataReadyState.value && m.progress.value >= 1f }.first { it }
            delay(SplashGate.remainingMinimumMs(SystemClock.elapsedRealtime() - startMs))
        }
        onExitStartState.value()
        m.playExit()
        onFinishedState.value()
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // Menelan sentuhan selama splash menutupi layar tujuan.
            .pointerInput(Unit) {}
    ) {
        val screenHeight = maxHeight
        val iconCenterY = screenHeight * ICON_CENTER_FRACTION

        // Latar + lingkaran dekoratif (satu lapisan agar fade-in/out cukup satu alpha)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = m.bg.value * (1f - m.exitBg.value) }
        ) {
            Box(Modifier.fillMaxSize().background(SplashGradient))
            DriftingCircle(
                diameter = 420.dp,
                reduced = reducedMotion,
                periodMs = SplashMotion.DRIFT_PERIOD_TOP_RIGHT_MS,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 170.dp, y = (-130).dp)
            )
            DriftingCircle(
                diameter = 200.dp,
                reduced = reducedMotion,
                periodMs = SplashMotion.DRIFT_PERIOD_LEFT_MS,
                modifier = Modifier.align(Alignment.TopStart).offset(x = (-60).dp, y = screenHeight * 0.3125f - 100.dp)
            )
            DriftingCircle(
                diameter = 360.dp,
                reduced = reducedMotion,
                periodMs = SplashMotion.DRIFT_PERIOD_BOTTOM_MS,
                modifier = Modifier.align(Alignment.TopStart).offset(x = (-180).dp, y = screenHeight - 60.dp - 180.dp)
            )
        }

        // Halo + ikon, berpusat di iconCenterY
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = iconCenterY - HaloOuterSize / 2)
                .size(HaloOuterSize),
            contentAlignment = Alignment.Center
        ) {
            Halo(HaloOuterSize, IconSize / HaloOuterSize, HaloOuterColor, m.haloOuter, m)
            Halo(HaloInnerSize, IconSize / HaloInnerSize, HaloInnerColor, m.haloInner, m)
            AppLogoBadge(
                logoSize = IconSize,
                modifier = Modifier
                    .graphicsLayer {
                        val shrink = if (m.reduced) 1f else 1f - (1f - SplashMotion.EXIT_ICON_SCALE_TO) * m.exitIcon.value
                        val s = m.iconScale.value * shrink
                        scaleX = s
                        scaleY = s
                        alpha = m.iconAlpha.value * (1f - m.exitIcon.value)
                    }
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(35.dp),
                        clip = false,
                        ambientColor = Color(0xFF0B3C94).copy(alpha = 0.35f),
                        spotColor = Color(0xFF0B3C94).copy(alpha = 0.45f)
                    )
            )
        }

        // Judul + subjudul (baris pertama berada 125 dp di bawah pusat ikon pada mockup)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = iconCenterY + 125.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Jadwal, tugas, & IPK dalam satu aplikasi",
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                // Lebar dibatasi agar baris pecah setelah "IPK" seperti mockup.
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .graphicsLayer {
                        alpha = m.titleAlpha.value * m.exitContent.value
                        translationY = m.titleRise.value * SplashMotion.TITLE_RISE.toPx()
                    }
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Kuliah jadi lebih teratur",
                color = Color.White.copy(alpha = 0.92f),
                fontFamily = Poppins,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = m.subtitleAlpha.value * m.exitContent.value
                    translationY = m.subtitleRise.value * SplashMotion.TITLE_RISE.toPx()
                }
            )
        }

        // Bar loading + teks status di bawah
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp)
                .graphicsLayer { alpha = m.footerAlpha.value * m.exitContent.value },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(BarWidth, BarHeight)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                // Pil penuh yang digeser ke kiri sejauh (1 - progres) x lebar: hanya transform, ujung kanan tetap bulat.
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { translationX = -(1f - m.progress.value) * size.width }
                        .background(Color.White, CircleShape)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Menyiapkan semester kamu…",
                color = Color.White.copy(alpha = 0.78f),
                fontFamily = Poppins,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Halo transparan: mengembang dari ukuran ikon ke ukuran penuh sambil fade in, lalu ikut berdenyut. */
@Composable
private fun Halo(
    diameter: Dp,
    fromScale: Float,
    color: Color,
    expansion: Animatable<Float, AnimationVector1D>,
    m: SplashAnimState
) {
    val from = if (m.reduced) 1f else fromScale
    Box(
        Modifier
            .size(diameter)
            .graphicsLayer {
                val p = expansion.value
                val s = (from + (1f - from) * p) * m.pulse.value
                scaleX = s
                scaleY = s
                alpha = p * (1f - m.exitIcon.value)
            }
            .background(color, CircleShape)
    )
}

/** Lingkaran dekoratif yang bergerak sangat pelan (drift) selama splash tampil. */
@Composable
private fun DriftingCircle(
    diameter: Dp,
    reduced: Boolean,
    periodMs: Int,
    modifier: Modifier = Modifier
) {
    val drift: State<Float>
    val driftY: State<Float>
    if (reduced) {
        drift = remember { mutableFloatStateOf(0f) }
        driftY = drift
    } else {
        val transition = rememberInfiniteTransition(label = "SplashDrift")
        drift = transition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(periodMs, easing = SplashMotion.EaseBreathing), RepeatMode.Reverse),
            label = "SplashDriftX"
        )
        driftY = transition.animateFloat(
            initialValue = 1f,
            targetValue = -1f,
            animationSpec = infiniteRepeatable(tween((periodMs * 0.8f).toInt(), easing = SplashMotion.EaseBreathing), RepeatMode.Reverse),
            label = "SplashDriftY"
        )
    }
    Box(
        modifier
            .size(diameter)
            .graphicsLayer {
                val amp = SplashMotion.DRIFT_AMPLITUDE.toPx()
                translationX = drift.value * amp
                translationY = driftY.value * amp
            }
            .background(DecorCircleColor, CircleShape)
    )
}
