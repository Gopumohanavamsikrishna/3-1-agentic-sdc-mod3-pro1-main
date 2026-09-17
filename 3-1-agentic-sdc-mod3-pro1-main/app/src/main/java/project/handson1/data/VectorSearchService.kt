package project.handson1.data

import project.handson1.utils.EmbeddingGenerator
import kotlin.math.sqrt

class VectorSearchService(
    private val repository: DocumentRepository,
    private val embeddingGenerator: EmbeddingGenerator
) {
    private val indexedDocuments = mutableListOf<Document>()

    suspend fun indexDocument(document: Document): Document {
        val embedding = embeddingGenerator.generateEmbedding(document.content)
        val indexedDoc = document.copy(embedding = embedding)
        // Remove existing document with same ID if exists
        indexedDocuments.removeAll { it.id == document.id }
        indexedDocuments.add(indexedDoc)
        return indexedDoc
    }

    suspend fun searchDocuments(query: String, topK: Int = 10): List<Document> {
        if (indexedDocuments.isEmpty()) {
            // Load existing documents
            val docs = repository.loadDocuments()
            docs.forEach { doc ->
                val embedding = embeddingGenerator.generateEmbedding(doc.content)
                val indexedDoc = doc.copy(embedding = embedding)
                indexedDocuments.add(indexedDoc)
            }
        }

        val queryEmbedding = embeddingGenerator.generateEmbedding(query)
        return indexedDocuments
            .map { doc ->
                val similarity = cosineSimilarity(queryEmbedding, doc.embedding ?: FloatArray(0))
                doc to similarity
            }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Float {
        if (vec1.isEmpty() || vec2.isEmpty()) return 0f

        var dotProduct = 0f
        var norm1 = 0f
        var norm2 = 0f

        val size = minOf(vec1.size, vec2.size)
        for (i in 0 until size) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }

        return if (norm1 > 0 && norm2 > 0) {
            dotProduct / (sqrt(norm1) * sqrt(norm2))
        } else {
            0f
        }
    }
}