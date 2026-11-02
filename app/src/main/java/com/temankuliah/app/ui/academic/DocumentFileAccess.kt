package com.temankuliah.app.ui.academic

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

/** URI content:// untuk berkas milik aplikasi. Hanya folder yang terdaftar di res/xml/file_paths.xml yang bisa dibagikan. */
internal fun fileProviderUri(context: Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.files", file)

/** Menyiapkan berkas sementara (cache/camera) beserta URI-nya untuk hasil foto KRS/KHS. [type] mis. "krs" atau "khs". */
internal fun createCameraTarget(context: Context, type: String, semester: Int): Pair<File, Uri> {
    val cameraDir = File(context.cacheDir, "camera").apply { if (!exists()) mkdirs() }
    val file = File(cameraDir, "doc_${type}_s${semester}_${System.currentTimeMillis()}.jpg")
    return file to fileProviderUri(context, file)
}

/** Captures and restores the pending camera target across Activity and process recreation. */
@Composable
internal fun rememberAcademicCameraCapture(
    context: Context,
    type: String,
    semester: Int,
    onPhotoCaptured: (File, Int) -> Unit
): () -> Unit {
    var pendingFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSemester by rememberSaveable { mutableIntStateOf(semester) }
    val currentOnPhotoCaptured by rememberUpdatedState(onPhotoCaptured)

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingFilePath?.let(::File)
        val capturedSemester = pendingSemester
        pendingFilePath = null

        if (success && file != null && file.exists() && file.length() > 0) {
            currentOnPhotoCaptured(file, capturedSemester)
        } else {
            file?.delete()
        }
    }

    return remember(context, type, semester, cameraLauncher) {
        {
            val (file, uri) = createCameraTarget(context, type.lowercase(Locale.ROOT), semester)
            pendingFilePath = file.absolutePath
            pendingSemester = semester
            cameraLauncher.launch(uri)
        }
    }
}
