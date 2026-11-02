package com.temankuliah.app.data.backup

import android.graphics.BitmapFactory
import com.temankuliah.app.data.backup.LocalDataArchive.MAX_ENTRY_BYTES
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/** Pemeriksaan lampiran cadangan (foto profil dan dokumen akademik) serta penentuan ekstensi yang aman. */
internal object BackupAssetFiles {
    internal fun safeImageExtension(path: String): String = when (path.substringAfterLast('.', "").lowercase()) { "png" -> ".png"; "webp" -> ".webp"; "heic" -> ".heic"; else -> ".jpg" }

    internal fun safeDocumentExtension(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
        "pdf" -> ".pdf"
        "jpg", "jpeg" -> ".jpg"
        "png" -> ".png"
        "webp" -> ".webp"
        "bmp" -> ".bmp"
        "gif" -> ".gif"
        "csv" -> ".csv"
        "tsv" -> ".tsv"
        "md" -> ".md"
        else -> ".txt"
    }

    internal fun validateAcademicDocumentFile(path: String, file: File) {
        val extension = path.substringAfterLast('.', "").lowercase()
        if (file.length() == 0L) throw IOException("Dokumen kosong dalam cadangan: $path")
        when (extension) {
            "pdf" -> {
                if (file.length() < 8) throw IOException("PDF dalam cadangan tidak valid: $path")
                val header = ByteArray(5)
                FileInputStream(file).use { DataInputStream(it).readFully(header) }
                if (String(header, Charsets.US_ASCII) != "%PDF-") throw IOException("PDF dalam cadangan tidak valid: $path")
            }
            "jpg", "jpeg", "png", "webp", "bmp", "gif" -> {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, options)
                if (options.outWidth <= 0 || options.outHeight <= 0) throw IOException("Gambar dokumen dalam cadangan tidak valid: $path")
            }
            "txt", "csv", "tsv", "md" -> if (file.length() > MAX_ENTRY_BYTES) throw IOException("File teks dalam cadangan terlalu besar: $path")
            else -> throw IOException("Format dokumen dalam cadangan tidak didukung: $path")
        }
    }

    internal fun validateProfileImageFile(file: File) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) throw IOException("Foto profil dalam cadangan rusak.")
    }
}
