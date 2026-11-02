package com.temankuliah.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.temankuliah.app.R

/**
 * Logo aplikasi (topi wisuda + buku) berukuran [logoSize] dengan cincin putih transparan di luarnya,
 * seperti pada mockup splash dan layar masuk. Ukuran total = [logoSize] + 2 x [ringWidth].
 */
@Composable
fun AppLogoBadge(
    logoSize: Dp,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 2.5.dp,
    ringColor: Color = Color.White.copy(alpha = 0.6f),
    contentDescription: String? = null
) {
    val corner = logoSize * 0.29f
    Box(
        modifier = modifier
            .size(logoSize + ringWidth * 2)
            .border(ringWidth, ringColor, RoundedCornerShape(corner + ringWidth))
            .padding(ringWidth),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(corner))
        )
    }
}
