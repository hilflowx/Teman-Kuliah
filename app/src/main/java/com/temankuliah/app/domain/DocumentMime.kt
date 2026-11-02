package com.temankuliah.app.domain

import java.util.Locale

/** MIME untuk membuka berkas KRS/KHS dengan aplikasi lain. Ekstensi tak dikenal memakai tipe umum. */
fun documentMimeType(extension: String): String = when (extension.lowercase(Locale.ROOT)) {
    "pdf" -> "application/pdf"
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "webp" -> "image/webp"
    "bmp" -> "image/bmp"
    "gif" -> "image/gif"
    "csv" -> "text/csv"
    "tsv" -> "text/tab-separated-values"
    "txt" -> "text/plain"
    "md" -> "text/markdown"
    else -> "application/octet-stream"
}
