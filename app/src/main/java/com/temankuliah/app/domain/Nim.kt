package com.temankuliah.app.domain

import java.util.Locale

/**
 * Bentuk baku NIM untuk perbandingan/pencarian: spasi tepi dibuang, huruf kecil. NIM tersimpan tetap
 * dalam huruf aslinya (trim saja) agar tampilan profil tidak berubah; hanya perbandingan yang memakai ini.
 * Dipakai login, registrasi, ubah profil, dan validasi cadangan. DAO membandingkannya dengan
 * LOWER(TRIM(nim)) (LOWER SQLite hanya melipat huruf ASCII, cukup untuk NIM alfanumerik).
 */
fun normalizeNim(raw: String): String = raw.trim().lowercase(Locale.ROOT)
