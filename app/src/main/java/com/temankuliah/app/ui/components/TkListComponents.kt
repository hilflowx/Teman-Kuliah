package com.temankuliah.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary

/** Judul daftar dengan jumlah item di kanan, mis. "Daftar Tugas" dan "4 tugas". */
@Composable
fun TkListHeading(title: String, countText: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.semantics { heading() }
        )
        Text(text = countText, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}
