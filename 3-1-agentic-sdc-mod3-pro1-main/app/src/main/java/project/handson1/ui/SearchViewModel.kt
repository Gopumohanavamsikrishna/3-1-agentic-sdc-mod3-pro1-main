package project.handson1.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import project.handson1.data.Document
import project.handson1.data.DocumentRepository
import project.handson1.data.VectorSearchService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SearchViewModel(
    private val repository: DocumentRepository,
    private val vectorSearchService: VectorSearchService
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<Document>>(emptyList())
    val searchResults: StateFlow<List<Document>> = _searchResults

    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents

    suspend fun searchDocuments(query: String): List<Document> {
        return try {
            val results = vectorSearchService.searchDocuments(query)
            _searchResults.value = results
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDocuments(): List<Document> {
        return try {
            val docs = repository.loadDocuments()
            _documents.value = docs
            docs
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun indexDocument(document: Document) {
        try {
            vectorSearchService.indexDocument(document)
        } catch (e: Exception) {
            // Handle error
        }
    }
}