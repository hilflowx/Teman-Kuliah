package com.temankuliah.app.ui.dashboard

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import com.temankuliah.app.ui.components.rememberEntranceProgress
import com.temankuliah.app.ui.theme.TkShape
import com.temankuliah.app.ui.theme.tkShadow
import com.temankuliah.app.ui.components.tkHeroCircles
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.temankuliah.app.ui.components.IndonesianLocale

/** Batas cumlaude pada kartu IPK: IPK harus di atas angka ini (selaras dengan predikat >= 3,51). */
internal const val CUMLAUDE_LIMIT = 3.50
private const val GPA_SCALE = 4.0

// -------------------------------------------------------------------------------------------------
// Kartu Pencapaian Akademik
// -------------------------------------------------------------------------------------------------

private fun formatGpa(value: Double): String = String.format(IndonesianLocale, "%.2f", value)

/** Kartu gradien biru: IPK, jalur cumlaude, progres terhadap batas, dan ringkasan IPS/SKS/Matkul. */
@Composable
internal fun GpaHeroCard(
    cumulativeGpa: Double,
    totalCredits: Int,
    targetCredits: Int?,
    semesterGpa: Double,
    semesterGradedCredits: Int,
    semester: Int,
    courseCount: Int,
    onOpenAcademic: () -> Unit,
    onAddCourse: () -> Unit
) {
    val target = targetCredits ?: 144
    val hasGrades = cumulativeGpa > 0.0 || totalCredits > 0
    val shape = TkShape.xl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_gpa_card")
            .shadow(
                elevation = tkShadow(20.dp),
                shape = shape,
                ambientColor = DashBlue.copy(alpha = 0.25f),
                spotColor = DashBlue.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(Brush.linearGradient(listOf(DashHeroStart, DashHeroEnd)))
            .tkHeroCircles()
            .clickable(onClickLabel = "Buka akademik", onClick = onOpenAcademic)
            .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 16.dp)
    ) {
        if (!hasGrades) {
            EmptyHeroContent(semester = semester, target = target, onAddCourse = onAddCourse)
        } else {
            val progress = (cumulativeGpa / GPA_SCALE).toFloat().coerceIn(0f, 1f)
            val markerFraction = (CUMLAUDE_LIMIT / GPA_SCALE).toFloat()
            val onCumlaudeTrack = cumulativeGpa > CUMLAUDE_LIMIT
            val ipsText = if (semesterGradedCredits > 0) formatGpa(semesterGpa) else "-"

            Column(modifier = Modifier.fillMaxWidth()) {
                HeroTitleRow(semester = semester)
                Spacer(Modifier.height(14.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isNarrow = maxWidth < 340.dp
                    val gpaSize = when {
                        maxWidth < 320.dp -> 32.sp
                        maxWidth < 360.dp -> 38.sp
                        else -> 45.sp
                    }
                    val gpaLineHeight = when {
                        maxWidth < 320.dp -> 40.sp
                        maxWidth < 360.dp -> 48.sp
                        else -> 56.sp
                    }

                    if (isNarrow) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                DashText(
                                    text = formatGpa(cumulativeGpa),
                                    color = Color.White,
                                    size = gpaSize,
                                    weight = FontWeight.Bold,
                                    lineHeight = gpaLineHeight,
                                    letterSpacing = (-1).sp,
                                    maxLines = 1
                                )
                                Spacer(Modifier.width(8.dp))
                                DashText(
                                    text = "/ ${formatGpa(GPA_SCALE)}",
                                    color = Color.White.copy(alpha = 0.72f),
                                    size = 14.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            CumlaudeBadge(
                                onTrack = onCumlaudeTrack,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                DashText(
                                    text = formatGpa(cumulativeGpa),
                                    color = Color.White,
                                    size = gpaSize,
                                    weight = FontWeight.Bold,
                                    lineHeight = gpaLineHeight,
                                    letterSpacing = (-1).sp,
                                    maxLines = 1
                                )
                                Spacer(Modifier.width(8.dp))
                                DashText(
                                    text = "/ ${formatGpa(GPA_SCALE)}",
                                    color = Color.White.copy(alpha = 0.72f),
                                    size = 14.sp,
                                    maxLines = 1,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }
                            CumlaudeBadge(
                                onTrack = onCumlaudeTrack,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                CumlaudeProgress(progress = progress, markerFraction = markerFraction)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    DashText(
                        text = "Batas cumlaude ${formatGpa(CUMLAUDE_LIMIT)}",
                        color = Color.White.copy(alpha = 0.85f),
                        size = 11.sp,
                        lineHeight = 18.sp,
                        maxLines = 1,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
                Spacer(Modifier.height(4.dp))
                StatsPanel(
                    semester = semester,
                    ipsText = ipsText,
                    totalCredits = totalCredits,
                    target = target,
                    courseCount = courseCount
                )
            }
        }
    }
}

@Composable
private fun HeroTitleRow(semester: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 26.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DashText(
            text = "Pencapaian Akademik",
            color = Color.White,
            size = 15.sp,
            weight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .heightIn(min = 26.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
                .padding(horizontal = 14.dp)
                .testTag("dashboard_semester_chip"),
            contentAlignment = Alignment.Center
        ) {
            DashText(
                text = "Semester $semester",
                color = Color.White,
                size = 11.5.sp,
                weight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EmptyHeroContent(semester: Int, target: Int, onAddCourse: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HeroTitleRow(semester = semester)
        DashText(
            text = "Belum ada nilai",
            color = Color.White,
            size = 24.sp,
            weight = FontWeight.Bold,
            lineHeight = 32.sp,
            modifier = Modifier.padding(top = 14.dp)
        )
        DashText(
            text = "Tambahkan mata kuliah dan nilai untuk memantau IPK kumulatif serta progres $target SKS kelulusan.",
            color = Color.White.copy(alpha = 0.9f),
            size = 12.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
        Button(
            onClick = onAddCourse,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DashBlueOnWhite),
            shape = CircleShape,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            DashText("+ Isi nilai mata kuliah", DashBlue, 13.sp, weight = FontWeight.Bold)
        }
    }
}

/** Pil "Jalur cumlaude": titik hijau bila IPK di atas batas, kuning bila masih di bawahnya. */
@Composable
private fun CumlaudeBadge(onTrack: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .heightIn(min = 32.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (onTrack) DashMint else DashAmber)
        )
        Spacer(Modifier.width(9.dp))
        DashText(
            text = if (onTrack) "Jalur cumlaude" else "Di bawah batas",
            color = Color.White,
            size = 12.sp,
            weight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/** Batang IPK/4,00 dengan penanda hijau pada batas cumlaude. */
@Composable
private fun CumlaudeProgress(progress: Float, markerFraction: Float) {
    // Garis terisi dulu, lalu penanda batas cumlaude muncul menyusul (fade + membesar).
    val fill by rememberEntranceProgress(progress, durationMillis = 900)
    val markerIn by rememberEntranceProgress(1f, durationMillis = 350, delayMillis = 450)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val markerWidth = 3.dp
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(fill.coerceIn(0f, 1f))
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = maxWidth * markerFraction - markerWidth / 2)
                .width(markerWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    alpha = markerIn
                    scaleY = 0.4f + 0.6f * markerIn
                }
                .clip(CircleShape)
                .background(DashMint)
        )
    }
}

/** Panel kaca berisi tiga angka ringkas: IPS semester, SKS, dan jumlah mata kuliah. */
@Composable
private fun StatsPanel(
    semester: Int,
    ipsText: String,
    totalCredits: Int,
    target: Int,
    courseCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 59.dp)
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatCell(label = "IPS Smt $semester", value = ipsText, suffix = null, modifier = Modifier.weight(1f))
        StatDivider()
        StatCell(label = "SKS", value = "$totalCredits", suffix = "/ $target", modifier = Modifier.weight(1f))
        StatDivider()
        StatCell(label = "Matkul", value = "$courseCount", suffix = null, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight(0.65f)
            .background(Color.White.copy(alpha = 0.28f))
    )
}

@Composable
private fun StatCell(label: String, value: String, suffix: String?, modifier: Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DashText(
            text = label,
            color = Color.White.copy(alpha = 0.78f),
            size = 11.sp,
            lineHeight = 15.sp,
            maxLines = 2,
            textAlign = TextAlign.Center
        )
        Row(verticalAlignment = Alignment.Bottom) {
            DashText(
                text = value,
                color = Color.White,
                size = 15.sp,
                weight = FontWeight.Bold,
                lineHeight = 22.sp,
                maxLines = 1
            )
            if (suffix != null) {
                Spacer(Modifier.width(4.dp))
                DashText(
                    text = suffix,
                    color = Color.White.copy(alpha = 0.72f),
                    size = 11.sp,
                    lineHeight = 22.sp,
                    maxLines = 1
                )
            }
        }
    }
}
