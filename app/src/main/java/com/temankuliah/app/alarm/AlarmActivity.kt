package com.temankuliah.app.alarm

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toColorInt
import com.temankuliah.app.R
import android.content.res.Configuration
import com.temankuliah.app.data.local.AppStorage
import com.temankuliah.app.ui.profile.PreferencesController
import com.temankuliah.app.ui.theme.ThemeMode

/**
 * Layar penuh yang muncul saat alarm berdering di atas layar kunci (Lockscreen).
 * Tampilannya mengikuti gaya Beranda/Profil: latar biru muda, kartu gradasi biru bersudut besar,
 * font Poppins, dan tombol berbentuk pil.
 */
class AlarmActivity : ComponentActivity() {

    private var alarmId: Int = 0
    private var titleText: String = "Alarm"
    private var messageText: String = "Waktunya kegiatan akademik!"
    private var rippleAnimator: Animator? = null

    /** Menutup layar saat [AlarmService] berhenti (Stop dari notifikasi, pembatalan, atau penghentian otomatis). */
    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    // Warna layar alarm mengikuti pilihan tema aplikasi (Terang / Gelap / Ikuti sistem); nilai terang tidak berubah.
    private val darkMode: Boolean by lazy {
        when (ThemeMode.fromKey(AppStorage.getPreferences(this).getString(PreferencesController.KEY_THEME_MODE, null))) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }
    private val colorBackground: Int by lazy { (if (darkMode) "#0A101C" else "#FBFDFF").toColorInt() }
    private val colorNavy: Int by lazy { (if (darkMode) "#F2F6FF" else "#16335E").toColorInt() }
    private val colorSlate: Int by lazy { (if (darkMode) "#8C9CBA" else "#5B7194").toColorInt() }
    private val colorTonalBg: Int by lazy { (if (darkMode) "#1A2A48" else "#E3EEFF").toColorInt() }
    private val colorTonalText: Int by lazy { (if (darkMode) "#8CC4FF" else "#1B5BCB").toColorInt() }
    private val colorRippleTonal: Int by lazy { (if (darkMode) "#268CC4FF" else "#261B5BCB").toColorInt() }

    companion object {
        private val COLOR_CARD_START = "#2B78E4".toColorInt()
        private val COLOR_CARD_END = "#1B5BCB".toColorInt()
        private val COLOR_MINT = "#7BEBB8".toColorInt()
        private val COLOR_WHITE_FAINT = "#38FFFFFF".toColorInt()
        private val COLOR_WHITE_PILL = "#33FFFFFF".toColorInt()
        private val COLOR_RIPPLE_LIGHT = "#33FFFFFF".toColorInt()

        private const val SIDE_PADDING_DP = 20
        private const val TOP_PADDING_DP = 56
        private const val BOTTOM_PADDING_DP = 32
        private const val CARD_RADIUS_DP = 32
        private const val CARD_PADDING_TOP_DP = 40
        private const val CARD_PADDING_SIDE_DP = 24
        private const val CARD_PADDING_BOTTOM_DP = 28
        private const val CARD_ELEVATION_DP = 8
        private const val ICON_CIRCLE_DP = 96
        private const val ICON_SIZE_DP = 46
        private const val PILL_RADIUS_DP = 100
        private const val STOP_HEIGHT_DP = 56
        private const val SNOOZE_HEIGHT_DP = 52
        private const val BUTTON_GAP_DP = 12
        private const val RIPPLE_DURATION_MS = 2200L
        private const val RIPPLE_END_SCALE = 1.6f
        private const val RIPPLE_START_ALPHA = 0.45f

        private const val HEADER_TITLE_SP = 28f
        private const val HEADER_SUBTITLE_SP = 14f
        private const val CLOCK_SP = 56f
        private const val ALARM_TITLE_SP = 20f
        private const val ALARM_MESSAGE_SP = 14f
        private const val CHIP_SP = 13f
        private const val STOP_TEXT_SP = 16f
        private const val SNOOZE_TEXT_SP = 15f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockscreenFlags()
        ContextCompat.registerReceiver(
            this,
            stopReceiver,
            IntentFilter(AlarmService.ACTION_ALARM_STOPPED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onBackPressedDispatcher.addCallback(this) {
            // Jangan hentikan alarm hanya dengan menekan tombol back tanpa memilih Stop/Snooze
        }
        showAlarm(intent)
    }

    // launchMode singleInstance: alarm berikutnya memakai instance yang sama, jadi isi layar harus diperbarui.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showAlarm(intent)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(stopReceiver)
        } catch (_: IllegalArgumentException) {}
        rippleAnimator?.cancel()
        rippleAnimator = null
        super.onDestroy()
    }

    private fun showAlarm(source: Intent) {
        alarmId = source.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, 0)
        titleText = source.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE) ?: "Alarm"
        messageText = source.getStringExtra(AlarmScheduler.EXTRA_ALARM_MESSAGE) ?: "Waktunya kegiatan akademik!"

        rippleAnimator?.cancel()
        rippleAnimator = null
        setContentView(buildView())
    }

    private fun setupLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )
    }

    // ---------------------------------------------------------------- Tampilan

    private fun buildView(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBackground)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(dp(SIDE_PADDING_DP), dp(TOP_PADDING_DP), dp(SIDE_PADDING_DP), dp(BOTTOM_PADDING_DP))

            addView(buildHeader())
            addView(buildAlarmCard())
            addView(View(context), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(buildActionButtons())
        }
    }

    private fun buildHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                label("Alarm", HEADER_TITLE_SP, colorNavy, R.font.poppins_bold).apply {
                    gravity = Gravity.START
                }
            )
            addView(
                label("Pengingat jadwal dan tenggat tugas", HEADER_SUBTITLE_SP, colorSlate, R.font.poppins_regular).apply {
                    gravity = Gravity.START
                }
            )
        }
    }

    private fun buildAlarmCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            clipChildren = false
            clipToPadding = false
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(COLOR_CARD_START, COLOR_CARD_END)
            ).apply { cornerRadius = dp(CARD_RADIUS_DP).toFloat() }
            elevation = if (darkMode) 0f else dp(CARD_ELEVATION_DP).toFloat()
            setPadding(
                dp(CARD_PADDING_SIDE_DP), dp(CARD_PADDING_TOP_DP),
                dp(CARD_PADDING_SIDE_DP), dp(CARD_PADDING_BOTTOM_DP)
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20) }

            addView(buildIconBadge())
            addView(buildClock())
            addView(
                label(titleText, ALARM_TITLE_SP, Color.WHITE, R.font.poppins_bold).apply {
                    maxLines = 2
                    layoutParams = wrapParams().apply { topMargin = dp(8) }
                }
            )
            addView(
                label(messageText, ALARM_MESSAGE_SP, Color.WHITE, R.font.poppins_regular).apply {
                    maxLines = 3
                    layoutParams = wrapParams().apply { topMargin = dp(2) }
                }
            )
            addView(buildStatusChip())
        }
    }

    private fun buildIconBadge(): FrameLayout {
        val size = dp(ICON_CIRCLE_DP)
        return FrameLayout(this).apply {
            clipChildren = false
            layoutParams = LinearLayout.LayoutParams(size, size)

            val ripple = View(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(COLOR_WHITE_FAINT)
                }
                alpha = RIPPLE_START_ALPHA
            }
            addView(ripple, FrameLayout.LayoutParams(size, size))
            rippleAnimator = ObjectAnimator.ofPropertyValuesHolder(
                ripple,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, RIPPLE_END_SCALE),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, RIPPLE_END_SCALE),
                PropertyValuesHolder.ofFloat(View.ALPHA, RIPPLE_START_ALPHA, 0f)
            ).apply {
                duration = RIPPLE_DURATION_MS
                repeatCount = ObjectAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }

            val circle = FrameLayout(context).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(COLOR_WHITE_FAINT)
                }
            }
            val icon = ImageView(context).apply {
                setImageResource(R.drawable.ic_alarm_clock)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            circle.addView(
                icon,
                FrameLayout.LayoutParams(dp(ICON_SIZE_DP), dp(ICON_SIZE_DP), Gravity.CENTER)
            )
            addView(circle, FrameLayout.LayoutParams(size, size))
        }
    }

    private fun buildClock(): TextClock {
        return TextClock(this).apply {
            format12Hour = "HH.mm"
            format24Hour = "HH.mm"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, CLOCK_SP)
            setTextColor(Color.WHITE)
            typeface = font(R.font.poppins_bold)
            includeFontPadding = false
            gravity = Gravity.CENTER
            layoutParams = wrapParams().apply { topMargin = dp(24) }
        }
    }

    private fun buildStatusChip(): TextView {
        val dot = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(COLOR_MINT)
            setSize(dp(8), dp(8))
        }
        return TextView(this).apply {
            text = getString(R.string.alarm_status_ringing)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, CHIP_SP)
            setTextColor(Color.WHITE)
            typeface = font(R.font.poppins_medium)
            gravity = Gravity.CENTER_VERTICAL
            setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null)
            compoundDrawablePadding = dp(8)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = GradientDrawable().apply {
                setColor(COLOR_WHITE_PILL)
                cornerRadius = dp(PILL_RADIUS_DP).toFloat()
            }
            layoutParams = wrapParams().apply { topMargin = dp(20) }
        }
    }

    private fun buildActionButtons(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            clipChildren = false

            val stop = pillButton(
                text = "Hentikan alarm",
                textColor = Color.WHITE,
                textSizeSp = STOP_TEXT_SP,
                fontRes = R.font.poppins_bold,
                heightDp = STOP_HEIGHT_DP,
                fill = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(COLOR_CARD_START, COLOR_CARD_END)
                ),
                rippleColor = COLOR_RIPPLE_LIGHT
            ).apply {
                elevation = if (darkMode) 0f else dp(4).toFloat()
                setOnClickListener {
                    startService(Intent(context, AlarmService::class.java).apply {
                        action = AlarmService.ACTION_STOP_ALARM
                    })
                    finish()
                }
            }
            addView(stop)

            val snooze = pillButton(
                text = "Tunda 5 menit",
                textColor = colorTonalText,
                textSizeSp = SNOOZE_TEXT_SP,
                fontRes = R.font.poppins_medium,
                heightDp = SNOOZE_HEIGHT_DP,
                fill = GradientDrawable().apply { setColor(colorTonalBg) },
                rippleColor = colorRippleTonal
            ).apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dp(BUTTON_GAP_DP)
                setOnClickListener {
                    startService(Intent(context, AlarmService::class.java).apply {
                        action = AlarmService.ACTION_SNOOZE_ALARM
                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                        putExtra(AlarmScheduler.EXTRA_ALARM_TITLE, titleText)
                        putExtra(AlarmScheduler.EXTRA_ALARM_MESSAGE, messageText)
                    })
                    finish()
                }
            }
            addView(snooze)
        }
    }

    // ---------------------------------------------------------------- Pembantu

    @Suppress("LongParameterList") // Semua argumen menentukan tampilan satu tombol; tidak dipakai di tempat lain.
    private fun pillButton(
        text: String,
        textColor: Int,
        textSizeSp: Float,
        fontRes: Int,
        heightDp: Int,
        fill: GradientDrawable,
        rippleColor: Int
    ): Button {
        fill.cornerRadius = dp(PILL_RADIUS_DP).toFloat()
        val mask = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(PILL_RADIUS_DP).toFloat()
        }
        return Button(this).apply {
            this.text = text
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            setTextColor(textColor)
            typeface = font(fontRes)
            gravity = Gravity.CENTER
            stateListAnimator = null
            minHeight = 0
            minimumHeight = 0
            setPadding(0, 0, 0, 0)
            background = RippleDrawable(ColorStateList.valueOf(rippleColor), fill, mask)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(heightDp)
            )
        }
    }

    private fun label(value: String, sizeSp: Float, color: Int, fontRes: Int): TextView {
        return TextView(this).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(color)
            typeface = font(fontRes)
            gravity = Gravity.CENTER
        }
    }

    private fun wrapParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun font(resId: Int): Typeface = ResourcesCompat.getFont(this, resId) ?: Typeface.DEFAULT

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
