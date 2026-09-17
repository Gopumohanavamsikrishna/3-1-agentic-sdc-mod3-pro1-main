package project.handson1.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class FilePickerHelper(
    private val activity: AppCompatActivity,
    private val onFileSelected: (Uri?) -> Unit
) {
    private val filePickerLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data: Intent? = result.data
        if (result.resultCode == Activity.RESULT_OK) {
            onFileSelected(data?.data)
        } else {
            onFileSelected(null)
        }
    }

    fun pickDocument() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/plain",
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/markdown"
            ))
        }
        filePickerLauncher.launch(intent)
    }
}