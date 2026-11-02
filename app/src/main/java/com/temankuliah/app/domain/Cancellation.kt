package com.temankuliah.app.domain

import kotlinx.coroutines.CancellationException

/**
 * Melempar ulang pembatalan coroutine. Dipanggil di awal `catch (Exception)` agar pembatalan (mis. layar
 * ditutup saat unggah) tidak tertelan dan dilaporkan sebagai kesalahan kepada pengguna.
 */
fun rethrowIfCancellation(error: Throwable) {
    if (error is CancellationException) throw error
}

/**
 * Seperti [runCatching], tetapi pembatalan coroutine dilempar ulang dan tidak dibungkus menjadi [Result.failure].
 * Pakai ini menggantikan `runCatching` di dalam fungsi suspend.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (error: Throwable) {
        rethrowIfCancellation(error)
        Result.failure(error)
    }
