import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

class Config:
    GROK_API_KEY = os.getenv('GROK_API_KEY')
    MODEL_NAME = "text-embedding-ada-002"
    EMBEDDING_DIMENSION = 1536
    SIMILARITY_THRESHOLD = 0.1
    MAX_DOCUMENTS = 100

    # Flask configuration
    DEBUG = True
    HOST = '0.0.0.0'
    PORT = 5000
    CORS_ORIGINS = ['*']