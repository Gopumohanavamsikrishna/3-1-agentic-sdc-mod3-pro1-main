package project.handson1.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import project.handson1.MainActivity
import project.handson1.databinding.FragmentSearchBinding
import project.handson1.data.Document
import project.handson1.utils.FilePickerHelper
import project.handson1.utils.FileUtils
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: DocumentListAdapter
    private var viewModel: SearchViewModel? = null
    private lateinit var filePickerHelper: FilePickerHelper

    fun setViewModel(viewModel: SearchViewModel) {
        this.viewModel = viewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFilePicker()
        setupRecyclerView()
        setupSearchListener()

        // Load initial documents
        loadDocuments()
    }

    private fun setupFilePicker() {
        val activity = requireActivity()
        if (activity is MainActivity) {
            filePickerHelper = FilePickerHelper(activity) { uri ->
                if (uri != null) {
                    processSelectedFile(uri)
                } else {
                    Toast.makeText(requireContext(), "No file selected", Toast.LENGTH_SHORT).show()
                }
            }

            activity.setOnUploadClickListener {
                filePickerHelper.pickDocument()
            }
        }
    }

    private fun processSelectedFile(uri: android.net.Uri) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE

            try {
                val content = FileUtils.readTextFromUri(uri, requireContext().contentResolver)
                if (content != null) {
                    val fileName = FileUtils.getFileName(uri, requireContext().contentResolver)

                    val file = FileUtils.copyFileToInternalStorage(requireContext(), uri)

                    if (file != null) {
                        val document = Document(
                            id = file.nameWithoutExtension,
                            title = file.nameWithoutExtension,
                            content = content,
                            filePath = file.absolutePath
                        )

                        viewModel?.indexDocument(document)

                        Toast.makeText(
                            requireContext(),
                            "Document uploaded successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadDocuments()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to read file content",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error processing file: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                e.printStackTrace()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = DocumentListAdapter { document ->
            showDocumentDetails(document)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SearchFragment.adapter
        }
    }

    private fun setupSearchListener() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                performSearch(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.length > 2) {
                    performSearch(newText)
                } else if (newText.isEmpty()) {
                    loadDocuments()
                }
                return true
            }
        })
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val results = viewModel?.searchDocuments(query) ?: emptyList()
                updateUI(results)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadDocuments() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val documents = viewModel?.getDocuments() ?: emptyList()
                updateUI(documents)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load documents", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateUI(documents: List<Document>) {
        adapter.submitList(documents)
        binding.tvResultsCount.text = "${documents.size} results"
        binding.tvEmpty.visibility = if (documents.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showDocumentDetails(document: Document) {
        Toast.makeText(requireContext(), "Selected: ${document.title}", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}