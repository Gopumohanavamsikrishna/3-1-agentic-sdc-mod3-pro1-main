from flask import Flask, request, jsonify
from flask_cors import CORS
import openai
import os
import json
from typing import List, Dict
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

app = Flask(__name__)
CORS(app)  # Enable CORS for all routes

# Configuration
GROK_API_KEY = os.getenv('GROK_API_KEY')
MODEL_NAME = "text-embedding-ada-002"
EMBEDDING_DIMENSION = 1536
SIMILARITY_THRESHOLD = 0.1

# Initialize Grok API
if GROK_API_KEY:
    openai.api_key = GROK_API_KEY
    openai.api_base = "https://api.x.ai/v1"
else:
    print("WARNING: GROK_API_KEY not found in environment variables")

class VectorSearch:
    def __init__(self):
        self.documents = []
        self.embeddings = []
        self.dimension = EMBEDDING_DIMENSION

    def add_document(self, doc_id: str, content: str, metadata: Dict = None):
        """Add document to vector index"""
        try:
            if not GROK_API_KEY:
                return {'success': False, 'error': 'API key not configured'}

            # Truncate content if too long
            if len(content) > 8000:
                content = content[:8000]

            response = openai.Embedding.create(
                model=MODEL_NAME,
                input=content
            )
            embedding = response['data'][0]['embedding']

            self.documents.append({
                'id': doc_id,
                'content': content,
                'metadata': metadata or {}
            })
            self.embeddings.append(embedding)

            return {'success': True, 'doc_id': doc_id, 'index': len(self.documents) - 1}
        except Exception as e:
            print(f"Error adding document: {e}")
            return {'success': False, 'error': str(e)}

    def search(self, query: str, top_k: int = 10) -> List[Dict]:
        """Search for similar documents using vector similarity"""
        try:
            if not GROK_API_KEY:
                return []

            # Generate query embedding
            response = openai.Embedding.create(
                model=MODEL_NAME,
                input=query
            )
            query_embedding = response['data'][0]['embedding']

            # Calculate similarities
            if not self.embeddings:
                return []

            similarities = cosine_similarity(
                [query_embedding],
                self.embeddings
            )[0]

            # Get top_k results
            top_indices = np.argsort(similarities)[-top_k:][::-1]

            results = []
            for idx in top_indices:
                if similarities[idx] > SIMILARITY_THRESHOLD:
                    results.append({
                        'document': self.documents[idx],
                        'similarity': float(similarities[idx])
                    })

            return results
        except Exception as e:
            print(f"Search error: {e}")
            return []

    def get_documents(self):
        """Get all indexed documents"""
        return self.documents

    def clear_all(self):
        """Clear all indexed documents"""
        self.documents = []
        self.embeddings = []

# Initialize vector search service
vector_search = VectorSearch()

@app.route('/api/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({
        'status': 'healthy',
        'documents_count': len(vector_search.documents),
        'api_configured': bool(GROK_API_KEY),
        'api_key_present': bool(GROK_API_KEY)
    })

@app.route('/api/index', methods=['POST'])
def index_document():
    """Index a new document"""
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'Invalid JSON data'}), 400

        doc_id = data.get('id')
        content = data.get('content')
        metadata = data.get('metadata', {})

        if not doc_id:
            return jsonify({'error': 'Document ID is required'}), 400
        if not content:
            return jsonify({'error': 'Document content is required'}), 400

        result = vector_search.add_document(doc_id, content, metadata)
        if result.get('success'):
            return jsonify(result), 200
        else:
            return jsonify(result), 500
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/search', methods=['POST'])
def search_documents():
    """Search documents by query"""
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'Invalid JSON data'}), 400

        query = data.get('query')
        top_k = data.get('top_k', 10)

        if not query:
            return jsonify({'error': 'Query is required'}), 400

        results = vector_search.search(query, min(top_k, 50))
        return jsonify({
            'results': results,
            'count': len(results)
        }), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/documents', methods=['GET'])
def get_documents():
    """Get all indexed documents"""
    try:
        documents = vector_search.get_documents()
        return jsonify({
            'documents': documents,
            'count': len(documents)
        }), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/clear', methods=['POST'])
def clear_documents():
    """Clear all indexed documents"""
    try:
        vector_search.clear_all()
        return jsonify({'success': True, 'message': 'All documents cleared'}), 200
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# Root route for testing
@app.route('/', methods=['GET'])
def root():
    return jsonify({
        'message': 'Vector Search Service is running',
        'endpoints': {
            'GET /': 'This message',
            'GET /api/health': 'Health check',
            'POST /api/index': 'Index a document',
            'POST /api/search': 'Search documents',
            'GET /api/documents': 'Get all documents',
            'POST /api/clear': 'Clear all documents'
        }
    }), 200

if __name__ == '__main__':
    print("\n" + "="*50)
    print("VECTOR SEARCH SERVICE")
    print("="*50)
    print(f"API Key configured: {bool(GROK_API_KEY)}")
    print(f"Model: {MODEL_NAME}")
    print("\nAvailable endpoints:")
    print("  GET  /                  - Service info")
    print("  GET  /api/health        - Health check")
    print("  POST /api/index         - Index document")
    print("  POST /api/search        - Search documents")
    print("  GET  /api/documents     - Get all documents")
    print("  POST /api/clear         - Clear all documents")
    print("\nServer running on http://localhost:5000")
    print("="*50 + "\n")

    app.run(
        debug=True,
        host='0.0.0.0',
        port=5000
    )