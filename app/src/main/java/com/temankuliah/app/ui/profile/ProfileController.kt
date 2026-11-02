package com.temankuliah.app.ui.profile

import com.temankuliah.app.domain.rethrowIfCancellation
import android.net.Uri
import com.temankuliah.app.data.files.StudentFileStore
import com.temankuliah.app.ui.FeatureContext
import com.temankuliah.app.ui.FeatureController
import kotlinx.coroutines.launch

internal class ProfileController(
    ctx: FeatureContext
) : FeatureController(ctx) {
    fun setProfileImage(uri: Uri) {
        val user = currentUser.value ?: return
        scope.launch {
            var newPath: String? = null
            try {
                newPath = StudentFileStore.saveProfileImage(application, user.id, uri)
                val result = repository.updateProfileImage(user.id, newPath)
                if (result.isFailure) throw result.exceptionOrNull() ?: IllegalStateException("Gagal memperbarui foto profil.")
                user.profileImagePath?.let { StudentFileStore.deleteRelative(application, it) }
                snackbar.value = "Foto profil berhasil diperbarui."
            } catch (error: Exception) {
                rethrowIfCancellation(error)
                newPath?.let { StudentFileStore.deleteRelative(application, it) }
                snackbar.value = error.message ?: "Foto profil gagal disimpan."
            }
        }
    }

    fun removeProfileImage() {
        val user = currentUser.value ?: return
        scope.launch {
            val oldPath = user.profileImagePath
            val result = repository.updateProfileImage(user.id, null)
            if (result.isSuccess) {
                oldPath?.let { StudentFileStore.deleteRelative(application, it) }
                snackbar.value = "Foto profil dihapus."
            } else snackbar.value = result.exceptionOrNull()?.message ?: "Foto profil gagal dihapus."
        }
    }

    fun updateProfile(
        fullName: String,
        nim: String,
        studyProgram: String,
        faculty: String,
        university: String
    ) {
        val user = currentUser.value ?: return
        profileEditError.value = null
        scope.launch {
            val result = repository.updateProfile(
                userId = user.id,
                fullName = fullName,
                nim = nim,
                studyProgram = studyProgram,
                faculty = faculty,
                university = university
            )
            if (result.isSuccess) {
                closeDialog()
                snackbar.value = "Profil berhasil diperbarui."
            } else {
                profileEditError.value = result.exceptionOrNull()?.message ?: "Gagal memperbarui profil."
            }
        }
    }
}
