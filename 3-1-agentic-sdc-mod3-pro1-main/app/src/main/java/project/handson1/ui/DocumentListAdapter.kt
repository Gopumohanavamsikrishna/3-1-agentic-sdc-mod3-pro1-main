package project.handson1.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import project.handson1.data.Document
import project.handson1.databinding.ItemDocumentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class DocumentListAdapter(
    private val onItemClick: (Document) -> Unit
) : ListAdapter<Document, DocumentListAdapter.DocumentViewHolder>(DocumentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DocumentViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DocumentViewHolder(
        private val binding: ItemDocumentBinding,
        private val onItemClick: (Document) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(document: Document) {
            binding.apply {
                documentTitle.text = document.title
                documentDate.text = SimpleDateFormat(
                    "MMM dd, yyyy HH:mm",
                    Locale.getDefault()
                ).format(document.timestamp)

                root.setOnClickListener {
                    onItemClick(document)
                }
            }
        }
    }

    class DocumentDiffCallback : DiffUtil.ItemCallback<Document>() {
        override fun areItemsTheSame(oldItem: Document, newItem: Document): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Document, newItem: Document): Boolean {
            return oldItem == newItem
        }
    }
}