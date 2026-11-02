package com.temankuliah.app.ui.components

import android.app.ActivityManager
import android.content.Context
import android.provider.Settings
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay

/**
 * Token Standar Durasi & Easing Material Design 3 (M3) untuk Teman Kuliah (Tahap 4).
 * Sesuai spesifikasi PRD Bab 8.5.
 */
object MotionTokens {
    /** 100 ms – 150 ms: Tekan tombol (bounce), feedback sentuh, switch toggle, ripple. */
    const val DurationMicro: Int = 120

    /** 180 ms – 240 ms: Indikator pil tab, rotasi chevron, rotasi ikon FAB, popup menu. */
    const val DurationShort: Int = 200

    /** 250 ms – 320 ms: Transisi halaman utama, ekspansi kartu, transisi modal bottom sheet. */
    const val DurationMedium: Int = 280

    /** 400 ms – 800 ms: Pengisian batang kemajuan (progress bar), ticker IPK awal. */
    const val DurationLong: Int = 600

    // Kurva Easing M3
    val EasingStandard = FastOutSlowInEasing
    val EasingDecelerate = LinearOutSlowInEasing
    val EasingAccelerate = FastOutLinearInEasing

    // Parameter Fisika Pegas (Spring Physics)
    const val DampingBouncy: Float = Spring.DampingRatioMediumBouncy
    const val DampingNatural: Float = 0.82f
    const val DampingNoBouncy: Float = Spring.DampingRatioNoBouncy
    const val StiffnessMedium: Float = Spring.StiffnessMedium
    const val StiffnessMediumLow: Float = Spring.StiffnessMediumLow
}

private const val PressScaleTarget = 0.96f
private const val IdleScaleTarget = 1.0f
private const val NavDirectionOffsetPx = 48
private const val HierarchicalSlideEnterDenominator = 3
private const val HierarchicalSlideExitDenominator = 6
private const val HierarchicalEnterForwardDuration = 300
private const val HierarchicalEnterBackwardDuration = 280
private const val HierarchicalExitDuration = 250
private const val HierarchicalFadeInForwardDuration = 220
private const val HierarchicalFadeInBackwardDuration = 200
private const val HierarchicalFadeOutForwardDuration = 160
private const val HierarchicalFadeOutBackwardDuration = 180
private const val StaggerMaxItems = 5
private const val StaggerStepDelayMillis = 40L

/**
 * Logika murni perhitungan target scale untuk press feedback, aman diuji di JVM test.
 */
fun calculatePressScale(isPressed: Boolean, reducedMotion: Boolean): Float =
    if (reducedMotion || !isPressed) IdleScaleTarget else PressScaleTarget

/**
 * Logika murni pergeseran horizontal navigasi tab atas.
 */
fun calculateNavDirectionOffset(initialIndex: Int, targetIndex: Int, isEnter: Boolean): Int {
    val isMovingForward = targetIndex > initialIndex
    return if (isEnter) {
        if (isMovingForward) NavDirectionOffsetPx else -NavDirectionOffsetPx
    } else {
        if (isMovingForward) -NavDirectionOffsetPx else NavDirectionOffsetPx
    }
}

/**
 * Logika murni pergeseran horizontal navigasi hierarki master-detail.
 */
fun calculateHierarchicalOffset(isOpeningDetail: Boolean, isEnter: Boolean, screenWidth: Int): Int {
    return if (isOpeningDetail) {
        if (isEnter) screenWidth / HierarchicalSlideEnterDenominator else -screenWidth / HierarchicalSlideExitDenominator
    } else {
        if (isEnter) -screenWidth / HierarchicalSlideExitDenominator else screenWidth / HierarchicalSlideEnterDenominator
    }
}

/** Pilihan pengguna untuk animasi aplikasi (Pengaturan > Tema & Tampilan). */
enum class AnimationMode(val key: String, val label: String) {
    AUTO("auto", "Otomatis"),
    ON("on", "Selalu hidup"),
    OFF("off", "Matikan");

    companion object {
        fun fromKey(key: String?): AnimationMode = values().firstOrNull { it.key == key } ?: AUTO
    }
}

/** Pilihan animasi aktif. Diisi oleh PreferencesController saat aplikasi dibuka dan saat pengguna mengubahnya. */
object MotionPreferences {
    var mode: AnimationMode by mutableStateOf(AnimationMode.AUTO)
}

/** Perangkat dianggap spek rendah bila RAM-nya sedikit (di bawah 3 GB) atau ditandai Android sebagai low-RAM. */
private const val LowEndRamBytes: Long = 3L * 1024L * 1024L * 1024L

fun isLowEndDevice(context: Context): Boolean = runCatching {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo()
    am.getMemoryInfo(info)
    am.isLowRamDevice || info.totalMem < LowEndRamBytes
}.getOrDefault(false)

/**
 * Logika murni penentuan "tanpa animasi", aman diuji di JVM test.
 * Pilihan manual pengguna menang atas deteksi otomatis. Animasi sistem yang dimatikan selalu dihormati.
 */
fun resolveReducedMotion(mode: AnimationMode, systemAnimationsOff: Boolean, lowEndDevice: Boolean): Boolean = when {
    systemAnimationsOff -> true
    mode == AnimationMode.OFF -> true
    mode == AnimationMode.ON -> false
    else -> lowEndDevice
}

/**
 * Mendeteksi apakah animasi harus dimatikan:
 * - sistem mematikan animasi (Developer options > Animator duration scale = 0, atau "Remove animations"),
 * - pengguna memilih "Matikan" di Pengaturan, atau
 * - mode "Otomatis" dan perangkat berspek rendah.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val systemOff = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    val lowEnd = remember(context) { isLowEndDevice(context) }
    return resolveReducedMotion(MotionPreferences.mode, systemOff, lowEnd)
}

/**
 * Nilai progres untuk bar garis/penanda: naik dari 0 ke [target] setiap halaman dibuka dan setiap
 * aplikasi kembali ke latar depan (ON_RESUME). Saat daftar di-scroll, item yang kembali terlihat
 * langsung penuh (tidak mengulang). Tanpa animasi: langsung ke [target].
 */
@Composable
fun rememberEntranceProgress(
    target: Float,
    durationMillis: Int = 800,
    delayMillis: Int = 0
): State<Float> {
    val reduced = rememberReducedMotion()
    var hasPlayed by rememberSaveable { mutableStateOf(false) }
    val anim = remember { Animatable(if (reduced || hasPlayed) target else 0f) }
    var replay by remember { mutableIntStateOf(0) }
    var armed by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (armed) replay++ }
    LaunchedEffect(Unit) { armed = true }

    LaunchedEffect(target, replay, reduced) {
        if (reduced) {
            anim.snapTo(target)
        } else {
            if (replay > 0) anim.snapTo(0f)
            anim.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = MotionTokens.EasingStandard)
            )
        }
        hasPlayed = true
    }
    return anim.asState()
}

/**
 * Efek staggered entrance bergaya Framer Motion:
 * Elemen muncul perlahan (fade-in) sambil bergerak naik 16 dp menggunakan spring animation.
 * Dilengkapi jeda bertahap (index * 40 ms, dibatasi maksimal 5 item) untuk mencegah kelebihan beban recomposition.
 * Menyimpan status [hasAppeared] agar animasi TIDAK berulang ketika pengguna melakukan scroll daftar.
 */
fun Modifier.staggeredEntrance(
    index: Int,
    reducedMotion: Boolean = false
): Modifier = composed {
    val isReduced = rememberReducedMotion() || reducedMotion
    if (isReduced) {
        return@composed Modifier
    }

    // Item di luar batas stagger (mis. hasil scroll) langsung tampil: tanpa jeda & tanpa kedip kosong.
    var hasAppeared by rememberSaveable { mutableStateOf(index > StaggerMaxItems) }
    // Satu progress untuk alpha + geser Y: keduanya selesai bersamaan, jadi tidak ada lompatan
    // saat modifier dilepas (sebelumnya spring offset masih bergerak ketika layer dihapus).
    val progress = remember { Animatable(if (hasAppeared) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!hasAppeared) {
            val delayMillis = index.coerceIn(0, StaggerMaxItems) * StaggerStepDelayMillis
            if (delayMillis > 0) delay(delayMillis)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = MotionTokens.DurationMedium, easing = MotionTokens.EasingDecelerate)
            )
            hasAppeared = true
        }
    }

    this.graphicsLayer {
        if (!hasAppeared) {
            alpha = progress.value
            translationY = (1f - progress.value) * 16f * density
        } else {
            alpha = 1f
            translationY = 0f
        }
    }
}

/**
 * Efek mikro interaksi sentuhan tombol & kartu (Button Press Spring Scale - PRD 8.3 A):
 * Elemen mengecil ke 0.96f saat ditekan dan membal dengan spring saat dilepas.
 * Mendukung pembacaan [interactionSource] eksplisit maupun deteksi sentuhan pasif melalui pointerInput
 * tanpa mengonsumsi gesture sentuh pengguna, sehingga tombol dan kartu tetap berfungsi normal.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource? = null,
    reducedMotion: Boolean = false
): Modifier = composed {
    val isReduced = rememberReducedMotion() || reducedMotion
    if (isReduced) {
        return@composed Modifier
    }

    var isPointerPressed by remember { mutableStateOf(false) }
    val isSourcePressed = interactionSource?.collectIsPressedAsState()?.value ?: false
    val isPressed = isSourcePressed || isPointerPressed

    val scale by animateFloatAsState(
        targetValue = calculatePressScale(isPressed, isReduced),
        animationSpec = spring(
            dampingRatio = MotionTokens.DampingBouncy,
            stiffness = MotionTokens.StiffnessMediumLow
        ),
        label = "pressScale"
    )

    val pointerMod = if (interactionSource == null) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Main)
                isPointerPressed = true
                waitForUpOrCancellation(pass = PointerEventPass.Main)
                isPointerPressed = false
            }
        }
    } else {
        Modifier
    }

    this.then(pointerMod).graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Navigasi Tingkat Atas (Bottom Navigation Bar - PRD 8.2 A):
 * Directional Fade Through / Crossfade dengan Offset Mikro 48px.
 * - Jika tab baru > tab asal: meluncur dari kanan ke kiri
 * - Jika tab baru < tab asal: meluncur dari kiri ke kanan
 * - Reduced motion: crossfade cepat 100 ms tanpa translasi
 */
fun topLevelNavTransition(
    reducedMotion: Boolean
): AnimatedContentTransitionScope<Int>.() -> ContentTransform = {
    if (reducedMotion) {
        (fadeIn(animationSpec = tween(durationMillis = 100)) togetherWith
            fadeOut(animationSpec = tween(durationMillis = 100)))
            .using(SizeTransform(clip = false))
    } else {
        val enterSlide = slideInHorizontally(
            initialOffsetX = { calculateNavDirectionOffset(initialState, targetState, isEnter = true) },
            animationSpec = tween(durationMillis = 220, easing = MotionTokens.EasingStandard)
        ) + fadeIn(animationSpec = tween(durationMillis = 220, easing = MotionTokens.EasingDecelerate))

        val exitSlide = slideOutHorizontally(
            targetOffsetX = { calculateNavDirectionOffset(initialState, targetState, isEnter = false) },
            animationSpec = tween(durationMillis = 180, easing = MotionTokens.EasingAccelerate)
        ) + fadeOut(animationSpec = tween(durationMillis = 180, easing = MotionTokens.EasingAccelerate))

        (enterSlide togetherWith exitSlide).using(SizeTransform(clip = false))
    }
}

/**
 * Navigasi Hirarki Master-Detail Layar Turunan (PRD 8.2 B):
 * Shared-Axis Horizontal Slide.
 * - Maju ke sub-layar: layar baru masuk dari kanan ({ it / 3 }, 300 ms), layar lama keluar ke kiri ({ -it / 6 }, 250 ms).
 * - Mundur ke root/layar sebelumnya: layar lama masuk dari kiri ({ -it / 6 }, 280 ms), layar aktif keluar ke kanan ({ it / 3 }, 250 ms).
 */
fun <T : Enum<T>> hierarchicalSubPageTransition(
    reducedMotion: Boolean,
    root: T
): AnimatedContentTransitionScope<T>.() -> ContentTransform = {
    if (reducedMotion) {
        (fadeIn(animationSpec = tween(durationMillis = 100)) togetherWith
            fadeOut(animationSpec = tween(durationMillis = 100)))
            .using(SizeTransform(clip = false))
    } else {
        val isOpeningDetail = initialState == root && targetState != root
        val enterDuration = if (isOpeningDetail) HierarchicalEnterForwardDuration else HierarchicalEnterBackwardDuration
        val enterFadeDuration = if (isOpeningDetail) HierarchicalFadeInForwardDuration else HierarchicalFadeInBackwardDuration
        val exitDuration = HierarchicalExitDuration
        val exitFadeDuration = if (isOpeningDetail) HierarchicalFadeOutForwardDuration else HierarchicalFadeOutBackwardDuration

        val enterTransition = slideInHorizontally(
            initialOffsetX = { calculateHierarchicalOffset(isOpeningDetail, isEnter = true, it) },
            animationSpec = tween(durationMillis = enterDuration, easing = MotionTokens.EasingStandard)
        ) + fadeIn(animationSpec = tween(durationMillis = enterFadeDuration, easing = MotionTokens.EasingDecelerate))

        val exitTransition = slideOutHorizontally(
            targetOffsetX = { calculateHierarchicalOffset(isOpeningDetail, isEnter = false, it) },
            animationSpec = tween(durationMillis = exitDuration, easing = MotionTokens.EasingStandard)
        ) + fadeOut(animationSpec = tween(durationMillis = exitFadeDuration, easing = MotionTokens.EasingStandard))

        (enterTransition togetherWith exitTransition).using(SizeTransform(clip = false))
    }
}

/**
 * Transisi Horizontal Slide Standar NavHost / Tab.
 */
fun <S : Comparable<S>> pageSlideTransitionSpec(
    reducedMotion: Boolean
): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    if (reducedMotion) {
        (EnterTransition.None togetherWith ExitTransition.None).using(SizeTransform(clip = false))
    } else {
        val dir = if (targetState > initialState) 1 else -1
        ((slideInHorizontally(tween(MotionTokens.DurationMedium, easing = MotionTokens.EasingStandard)) { dir * it / 8 } +
            fadeIn(tween(MotionTokens.DurationShort, delayMillis = 30))) togetherWith
            (slideOutHorizontally(tween(MotionTokens.DurationShort, easing = MotionTokens.EasingStandard)) { -dir * it / 8 } +
                fadeOut(tween(140)))).using(SizeTransform(clip = false))
    }
}

/**
 * Transisi Dialog Peringatan & Input (PRD 8.2 C):
 * - Masuk: Skala 92% ke 100% dengan spring bouncy + fade-in 180 ms.
 * - Keluar: Menyusut ke skala 95% dengan fade-out 150 ms.
 */
fun dialogEnterTransition(): EnterTransition =
    scaleIn(
        initialScale = 0.92f,
        animationSpec = spring(
            dampingRatio = MotionTokens.DampingBouncy,
            stiffness = MotionTokens.StiffnessMediumLow
        )
    ) + fadeIn(animationSpec = tween(durationMillis = 180))

fun dialogExitTransition(): ExitTransition =
    scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(durationMillis = 150)
    ) + fadeOut(animationSpec = tween(durationMillis = 150))

/**
 * Transisi Modal Bottom Sheet (PRD 8.2 C):
 * - Masuk: Meluncur naik dari dasar layar via spring + fade in.
 * - Keluar: Meluncur turun kembali ke dasar layar via tween 200 ms + fade out.
 */
fun modalBottomSheetEnterTransition(): EnterTransition =
    slideInVertically(
        initialOffsetY = { it },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = MotionTokens.StiffnessMediumLow)
    ) + fadeIn(animationSpec = tween(durationMillis = 180))

fun modalBottomSheetExitTransition(): ExitTransition =
    slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(durationMillis = 200, easing = MotionTokens.EasingAccelerate)
    ) + fadeOut(animationSpec = tween(durationMillis = 150))
