package project.handson1.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    fun getFileName(uri: Uri, contentResolver: ContentResolver): String {
        var fileName = "document_${System.currentTimeMillis()}"

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex) ?: fileName
            }
        }

        return fileName
    }

    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "txt")
    }

    fun readTextFromUri(uri: Uri, contentResolver: ContentResolver): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            Log.e("FileUtils", "Error reading text from URI: ${e.message}")
            null
        }
    }

    fun readTextFromFile(filePath: String): String? {
        return try {
            File(filePath).readText()
        } catch (e: Exception) {
            Log.e("FileUtils", "Error reading text from file: ${e.message}")
            null
        }
    }

    fun copyFileToInternalStorage(
        context: Context,
        uri: Uri,
        destinationDir: String = "documents"
    ): File? {
        return try {
            val contentResolver = context.contentResolver
            val fileName = getFileName(uri, contentResolver)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                val dir = File(context.filesDir, destinationDir)
                if (!dir.exists()) {
                    dir.mkdirs()
                }

                val destinationFile = File(dir, fileName)
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }

                destinationFile
            }
        } catch (e: Exception) {
            Log.e("FileUtils", "Error copying file: ${e.message}")
            null
        }
    }

    fun getDocumentsFromInternalStorage(context: Context, dirName: String = "documents"): List<File> {
        val dir = File(context.filesDir, dirName)
        return if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.filter { it.isFile }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    fun getReadableFileSize(size: Long): String {
        if (size <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()

        return String.format(
            "%.1f %s",
            size / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    fun deleteFile(filePath: String): Boolean {
        return try {
            File(filePath).delete()
        } catch (e: Exception) {
            Log.e("FileUtils", "Error deleting file: ${e.message}")
            false
        }
    }

    fun getMimeType(fileName: String): String {
        return when (getFileExtension(fileName).lowercase()) {
            "txt" -> "text/plain"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "rtf" -> "application/rtf"
            "md" -> "text/markdown"
            else -> "application/octet-stream"
        }
    }

    fun isDocumentFile(fileName: String): Boolean {
        val extension = getFileExtension(fileName).lowercase()
        return extension in setOf("txt", "pdf", "doc", "docx", "rtf", "md")
    }
}