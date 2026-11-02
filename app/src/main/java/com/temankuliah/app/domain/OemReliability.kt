package com.temankuliah.app.domain

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri

/** Vendor produsen perangkat Android untuk deteksi mitigasi hemat daya OEM. */
enum class OemVendor {
    XIAOMI,
    SAMSUNG,
    OPPO,
    VIVO,
    HUAWEI,
    OTHER;

    companion object {
        fun fromManufacturer(manufacturer: String = Build.MANUFACTURER): OemVendor {
            val normalized = manufacturer.trim().lowercase()
            return when {
                normalized.contains("xiaomi") || normalized.contains("redmi") || normalized.contains("poco") -> XIAOMI
                normalized.contains("samsung") -> SAMSUNG
                normalized.contains("oppo") || normalized.contains("realme") || normalized.contains("oneplus") -> OPPO
                normalized.contains("vivo") || normalized.contains("iqoo") -> VIVO
                normalized.contains("huawei") || normalized.contains("honor") -> HUAWEI
                else -> OTHER
            }
        }
    }
}

/** Model panduan langkah optimasi latar belakang spesifik tiap vendor OEM. */
data class VendorGuide(
    val vendorName: String,
    val summary: String,
    val steps: List<String>
)

/** Mengembalikan panduan resmi per vendor OEM berdasarkan standar PRD Tahap 5. */
fun getVendorGuide(vendor: OemVendor): VendorGuide = when (vendor) {
    OemVendor.XIAOMI -> VendorGuide(
        vendorName = "Xiaomi / POCO (HyperOS / MIUI)",
        summary = "Sistem HyperOS/MIUI mematikan alarm latar belakang jika penghemat baterai aktif.",
        steps = listOf(
            "Aktifkan opsi 'Mulai Otomatis' (Autostart) pada info aplikasi.",
            "Ubah Penghemat Baterai ke 'Tidak ada pembatasan' (No restrictions).",
            "Izinkan 'Tampilkan di Layar Kunci' & 'Tampilkan jendela pop-up saat berjalan di latar belakang'."
        )
    )
    OemVendor.SAMSUNG -> VendorGuide(
        vendorName = "Samsung (One UI)",
        summary = "One UI menidurkan aplikasi yang jarang dibuka sehingga alarm kuliah bisa tertunda.",
        steps = listOf(
            "Buka Pengaturan > Perawatan Perangkat > Baterai.",
            "Pilih 'Batas penggunaan latar belakang' > 'Aplikasi yang tidak pernah tidur'.",
            "Tambahkan Teman Kuliah ke daftar tersebut agar alarm selalu tepat waktu."
        )
    )
    OemVendor.OPPO -> VendorGuide(
        vendorName = "Oppo / Realme (ColorOS)",
        summary = "ColorOS membatasi aktivitas latar belakang secara agresif untuk menghemat daya.",
        steps = listOf(
            "Buka Info Aplikasi > Penggunaan Baterai.",
            "Aktifkan 'Izinkan aktivitas latar belakang' dan 'Izinkan mulai otomatis'.",
            "Kunci aplikasi Teman Kuliah di daftar aplikasi terkini (Recent Apps)."
        )
    )
    OemVendor.VIVO -> VendorGuide(
        vendorName = "Vivo / iQOO (Funtouch OS)",
        summary = "Funtouch OS memerlukan izin konsumsi daya tinggi agar alarm dapat berdering tepat waktu.",
        steps = listOf(
            "Buka Pengaturan > Baterai > Manajemen Konsumsi Daya Tinggi di Latar Belakang.",
            "Izinkan konsumsi daya tinggi latar belakang untuk Teman Kuliah.",
            "Izinkan Mulai Otomatis pada aplikasi iManager."
        )
    )
    OemVendor.HUAWEI -> VendorGuide(
        vendorName = "Huawei / Honor (EMUI)",
        summary = "EMUI menghentikan layanan alarm otomatis saat layar terkunci lama.",
        steps = listOf(
            "Buka Pengaturan > Baterai > Peluncuran Aplikasi (App Launch).",
            "Cari Teman Kuliah dan ubah dari 'Kelola Otomatis' menjadi 'Kelola Manual'.",
            "Aktifkan 'Peluncuran otomatis', 'Peluncuran sekunder', dan 'Jalankan di latar belakang'."
        )
    )
    OemVendor.OTHER -> VendorGuide(
        vendorName = "Perangkat Android Standar",
        summary = "Pastikan fitur optimasi baterai sistem tidak membatasi ketepatan waktu alarm kuliah.",
        steps = listOf(
            "Nonaktifkan pengoptimalan baterai sistem untuk Teman Kuliah.",
            "Pastikan izin 'Alarm Presisi' tetap aktif di pengaturan aplikasi.",
            "Pastikan izin 'Notifikasi' diizinkan dengan prioritas tinggi."
        )
    )
}

/** Memeriksa apakah aplikasi dikecualikan dari optimasi baterai (Doze mode). */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

/** Membuka layar pengaturan sistem untuk mengecualikan aplikasi dari optimasi baterai. */
@android.annotation.SuppressLint("BatteryLife")
fun openBatteryOptimizationSettings(context: Context) {
    val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = "package:${context.packageName}".toUri()
    }
    val fallbackListIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
    }

    runCatching {
        context.startActivity(requestIntent)
    }.recoverCatching {
        context.startActivity(fallbackListIntent)
    }.recoverCatching {
        context.startActivity(detailsIntent)
    }
}
