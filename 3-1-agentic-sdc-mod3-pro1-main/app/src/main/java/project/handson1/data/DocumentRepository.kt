package project.handson1.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class Document(
    val id: String,
    val title: String,
    val content: String,
    val filePath: String,
    val embedding: FloatArray? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Document

        if (id != other.id) return false
        if (title != other.title) return false
        if (content != other.content) return false
        if (filePath != other.filePath) return false
        if (embedding != null) {
            if (other.embedding == null) return false
            if (!embedding.contentEquals(other.embedding)) return false
        } else if (other.embedding != null) return false
        if (timestamp != other.timestamp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + (embedding?.contentHashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

class DocumentRepository(private val context: Context) {
    private val documents = mutableListOf<Document>()

    suspend fun loadDocuments(): List<Document> = withContext(Dispatchers.IO) {
        documents.clear()
        val docDir = context.filesDir.resolve("documents")
        if (docDir.exists()) {
            docDir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension in listOf("txt", "pdf", "docx", "md")) {
                    try {
                        val content = file.readText()
                        documents.add(
                            Document(
                                id = file.nameWithoutExtension,
                                title = file.nameWithoutExtension,
                                content = content,
                                filePath = file.absolutePath
                            )
                        )
                    } catch (e: Exception) {
                        // Skip files that can't be read
                    }
                }
            }
        }
        documents.toList()
    }

    suspend fun addDocument(uri: Uri, contentResolver: ContentResolver): Document? =
        withContext(Dispatchers.IO) {
            try {
                val fileName = getFileName(uri, contentResolver)
                val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
                val docDir = context.filesDir.resolve("documents")
                docDir.mkdirs()

                val file = File(docDir, fileName)
                inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                val content = file.readText()
                val document = Document(
                    id = file.nameWithoutExtension,
                    title = file.nameWithoutExtension,
                    content = content,
                    filePath = file.absolutePath
                )
                documents.add(document)
                document
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    private fun getFileName(uri: Uri, contentResolver: ContentResolver): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) {
                return it.getString(nameIndex) ?: "document_${System.currentTimeMillis()}.txt"
            }
        }
        return "document_${System.currentTimeMillis()}.txt"
    }
}