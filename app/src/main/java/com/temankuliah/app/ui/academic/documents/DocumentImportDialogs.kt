package com.temankuliah.app.ui.academic.documents

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.temankuliah.app.ui.components.Button
import com.temankuliah.app.ui.components.OutlinedTextField
import com.temankuliah.app.ui.components.TkCloseHeader
import com.temankuliah.app.ui.components.TkFullScreenDialog
import com.temankuliah.app.ui.icons.CameraAlt
import com.temankuliah.app.ui.icons.ChevronRight
import com.temankuliah.app.ui.icons.Info
import com.temankuliah.app.ui.icons.PhotoLibrary
import com.temankuliah.app.ui.icons.TextFields
import com.temankuliah.app.ui.icons.UploadFile
import com.temankuliah.app.ui.theme.DividerLight
import com.temankuliah.app.ui.theme.SurfaceLight
import com.temankuliah.app.ui.theme.tkShadow
import com.temankuliah.app.ui.theme.TemanKuliahBlue
import com.temankuliah.app.ui.theme.TemanKuliahBlueDark
import com.temankuliah.app.ui.theme.TemanKuliahBlueLight
import com.temankuliah.app.ui.theme.TextMuted
import com.temankuliah.app.ui.theme.TextPrimary
import com.temankuliah.app.ui.theme.TextSecondary
import com.temankuliah.app.ui.theme.TkShape
import com.temankuliah.app.ui.theme.TkSpace

/** Satu kartu pilihan metode impor: ubin ikon, judul, keterangan, dan panah. */
@Composable
private fun ImportMethodCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = TkShape.card,
        color = SurfaceLight,
        border = BorderStroke(1.dp, DividerLight),
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).clip(TkShape.lg).background(TemanKuliahBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = TemanKuliahBlue, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Pilihan sumber impor dokumen sebagai layar penuh: kamera, galeri, berkas, atau teks. Aksinya diberikan
 * oleh pemanggil; layar ini menutup dirinya begitu satu metode dipilih supaya tidak tertinggal di
 * belakang pratinjau hasil impor.
 */
@Composable
internal fun DocumentSourcePickerDialog(
    type: String,
    semester: Int,
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onEnterText: () -> Unit
) {
    TkFullScreenDialog(onDismissRequest = onDismiss) {
        TkCloseHeader(title = "Impor $type", supporting = "Semester $semester", onClose = onDismiss)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TkSpace.page, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Pilih cara membaca data mata kuliah $type Semester $semester.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            ImportMethodCard(
                icon = Icons.Default.CameraAlt,
                title = "Ambil foto dengan kamera",
                subtitle = "OCR offline langsung dari foto kertas dokumen",
                tag = "import_method_camera",
                onClick = { onDismiss(); onTakePhoto() }
            )
            ImportMethodCard(
                icon = Icons.Default.PhotoLibrary,
                title = "Pilih gambar dari galeri",
                subtitle = "Pindai tangkapan layar atau foto dokumen di galeri",
                tag = "import_method_gallery",
                onClick = { onDismiss(); onPickImage() }
            )
            ImportMethodCard(
                icon = Icons.Default.UploadFile,
                title = "Unggah berkas PDF / dokumen",
                subtitle = "Konversi halaman PDF dengan PdfRenderer + OCR",
                tag = "import_method_file",
                onClick = { onDismiss(); onPickFile() }
            )
            ImportMethodCard(
                icon = Icons.Default.TextFields,
                title = "Input / tempel teks biasa",
                subtitle = "Salin dan tempel daftar mata kuliah dari portal akademik",
                tag = "import_method_text",
                onClick = { onEnterText() }
            )

            Surface(
                shape = TkShape.card,
                color = TemanKuliahBlueLight,
                border = BorderStroke(1.dp, TemanKuliahBlue.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = TemanKuliahBlueDark,
                        modifier = Modifier.padding(top = 2.dp).size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Hasil bacaan ditampilkan dulu sebagai pratinjau. Tidak ada data yang tersimpan sebelum kamu menekan Simpan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/** Layar penuh untuk menempel teks daftar mata kuliah dari portal akademik. */
@Composable
internal fun PasteDocumentTextDialog(
    type: String,
    semester: Int,
    onDismiss: () -> Unit,
    onProcess: (String) -> Unit
) {
    var pastedText by remember { mutableStateOf("") }
    TkFullScreenDialog(onDismissRequest = onDismiss) {
        TkCloseHeader(title = "Tempel teks $type", supporting = "Semester $semester", onClose = onDismiss)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TkSpace.page, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Tempel teks daftar mata kuliah dari portal web universitas. Baris, kolom, dan SKS akan dipetakan otomatis menjadi kartu pratinjau.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            OutlinedTextField(
                value = pastedText,
                onValueChange = { pastedText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
                    .testTag("import_paste_field"),
                placeholder = {
                    Text(
                        if (type == "KRS") "Format per baris:\nKode  Nama Mata Kuliah  Dosen  Jam Kuliah"
                        else "Format per baris:\nKode  Nama Mata Kuliah  SKS  Nilai",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                },
                shape = TkShape.md
            )
        }

        Surface(
            color = SurfaceLight,
            shadowElevation = tkShadow(8.dp),
            border = BorderStroke(1.dp, DividerLight.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = TkSpace.page, vertical = 12.dp)
            ) {
                Button(
                    onClick = { if (pastedText.isNotBlank()) onProcess(pastedText) },
                    enabled = pastedText.isNotBlank(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = TemanKuliahBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("btn_process_pasted_text")
                ) {
                    Text("Proses teks ke pratinjau", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
