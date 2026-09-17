package project.handson1

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import project.handson1.databinding.ActivityMainBinding
import project.handson1.data.DocumentRepository
import project.handson1.data.VectorSearchService
import project.handson1.ui.SearchFragment
import project.handson1.ui.SearchViewModel
import project.handson1.utils.EmbeddingGenerator
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: SearchViewModel
    private lateinit var vectorSearchService: VectorSearchService
    private lateinit var embeddingGenerator: EmbeddingGenerator

    private var onUploadClickListener: (() -> Unit)? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            loadDocuments()
        } else {
            Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeServices()
        setupSearchFragment()
        setupFabClickListener()
        checkPermissions()
    }

    private fun initializeServices() {
        val apiKey = getApiKeyFromLocalProperties()
        embeddingGenerator = EmbeddingGenerator(apiKey)
        val repository = DocumentRepository(this)
        vectorSearchService = VectorSearchService(repository, embeddingGenerator)
        viewModel = SearchViewModel(repository, vectorSearchService)
    }

    private fun getApiKeyFromLocalProperties(): String {
        return try {
            val properties = java.util.Properties()
            val file = java.io.File(applicationContext.filesDir.parent + "/../local.properties")
            if (file.exists()) {
                properties.load(file.inputStream())
                properties.getProperty("GROK_API_KEY", "")
            } else {
                ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun setupSearchFragment() {
        val fragment = SearchFragment()
        fragment.setViewModel(viewModel)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun setupFabClickListener() {
        binding.fabUpload.setOnClickListener {
            onUploadClickListener?.invoke()
        }
    }

    fun setOnUploadClickListener(listener: () -> Unit) {
        onUploadClickListener = listener
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            requestPermissionLauncher.launch(permissions)
        } else {
            loadDocuments()
        }
    }

    private fun loadDocuments() {
        lifecycleScope.launch {
            viewModel.getDocuments()
        }
    }

    fun getViewModel(): SearchViewModel = viewModel
}