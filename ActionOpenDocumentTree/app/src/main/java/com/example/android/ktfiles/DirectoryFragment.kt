/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.ktfiles

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android.ktfiles.databinding.FragmentDirectoryBinding

/**
 * Fragment that shows a list of documents in a directory.
 */
class DirectoryFragment : Fragment() {
    /**
     * The [Uri] to the directory that the user has chosen to open.
     */
    private lateinit var directoryUri: Uri

    /**
     * This is the [FragmentDirectoryBinding] `ViewBinding` that is inflated from our layout file
     * layout/fragment_directory.xml which allows us to access views within it as kotlin properties.
     */
    private lateinit var binding: FragmentDirectoryBinding

    /**
     * The [DirectoryEntryAdapter] used to feed views to the [RecyclerView] in our UI
     */
    private lateinit var adapter: DirectoryEntryAdapter

    /**
     * The singleton `ViewModel` that we use to store and manage our dataset of directory entries.
     */
    private lateinit var viewModel: DirectoryFragmentViewModel

    /**
     * Called to have the fragment instantiate its user interface view. This will be called between
     * [onCreate] and [onViewCreated]. It is recommended to only inflate the layout in this method
     * and move logic that operates on the returned View to [onViewCreated]. First we initialize our
     * [Uri] field [directoryUri] by retrieving the [String] that was stored in the arguments
     * supplied when the fragment was instantiated under the key [ARG_DIRECTORY_URI] and converting
     * it to a [Uri] (throwing an [IllegalArgumentException] "Must pass URI of directory to open"
     * if this fails). We initialize our [DirectoryFragmentViewModel] field [viewModel] by using the
     * [ViewModelProvider.get] method to retrieve the existing [DirectoryFragmentViewModel] ViewModel
     * or create a new one in the scope of the [ViewModelProvider] if one did not exist yet.
     *
     * We use the [FragmentDirectoryBinding.inflate] method to have it use our [LayoutInflater]
     * parameter [inflater] to inflate itself in to an [FragmentDirectoryBinding] instance which we
     * use to initialize our field [binding]. ([FragmentDirectoryBinding] is created from the layout
     * file with ID [R.layout.fragment_directory] by the build system).
     *
     * We set the [RecyclerView.LayoutManager] that the [RecyclerView] in [binding] whose reference
     * is found in its [FragmentDirectoryBinding.list] property to a [LinearLayoutManager] instance.
     * Next we set our [DirectoryEntryAdapter] field [adapter] to a new instance with an anonymous
     * [ClickListeners] object whose `onDocumentClicked` override calls the `documentClicked` method
     * of [viewModel] with the [CachingDocumentFile] object associated with the item clicked (to
     * trigger an attempt to present the content of the file to the user), and whose
     * `onDocumentLongClicked` method calls our method [renameDocument] with the [CachingDocumentFile]
     * object associated with the item long clicked to enable the user to rename the file. We then
     * set the adapter of the [RecyclerView] in [binding] to [adapter].
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate any views in the
     * fragment.
     * @param container If non-null, this is the parent [ViewGroup] that the fragment's UI will be
     * attached to. The fragment should not add the view itself, but this can be used to generate
     * the `LayoutParams` of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        directoryUri = arguments?.getString(ARG_DIRECTORY_URI)?.toUri()
            ?: throw IllegalArgumentException("Must pass URI of directory to open")

        viewModel = ViewModelProvider(this)
            .get(DirectoryFragmentViewModel::class.java)

        binding = FragmentDirectoryBinding.inflate(inflater)
        binding.list.layoutManager = LinearLayoutManager(binding.list.context)

        adapter = DirectoryEntryAdapter(object : ClickListeners {
            override fun onDocumentClicked(clickedDocument: CachingDocumentFile) {
                viewModel.documentClicked(clickedDocument)
            }

            override fun onDocumentLongClicked(clickedDocument: CachingDocumentFile) {
                renameDocument(clickedDocument)
            }
        })

        binding.list.adapter = adapter

        viewModel.documents.observe(viewLifecycleOwner, { documents ->
            documents?.let { adapter.setEntries(documents) }
        })

        viewModel.openDirectory.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { directory ->
                (activity as? MainActivity)?.showDirectoryContents(directory.uri)
            }
        })

        viewModel.openDocument.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { document ->
                openDocument(document)
            }
        })

        return binding.root
    }

    /**
     * Called when all saved state has been restored into the view hierarchy
     * of the fragment.  This can be used to do initialization based on saved
     * state that you are letting the view hierarchy track itself, such as
     * whether check box widgets are currently checked.  This is called
     * after [onViewCreated] and before [onStart].
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        viewModel.loadDirectory(directoryUri)
    }

    private fun openDocument(document: CachingDocumentFile) {
        try {
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                data = document.uri
            }
            startActivity(openIntent)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                resources.getString(R.string.error_no_activity, document.name),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("InflateParams")
    private fun renameDocument(document: CachingDocumentFile) {
        // Normally we don't want to pass `null` in as the parent, but the dialog doesn't exist,
        // so there isn't a parent layout to use yet.
        val dialogView = layoutInflater.inflate(R.layout.rename_layout, null)
        val editText = dialogView.findViewById<EditText>(R.id.file_name)
        editText.setText(document.name)

        // Use a lambda so that we have access to the [EditText] with the new name.
        val buttonCallback: (DialogInterface, Int) -> Unit = { _, buttonId ->
            when (buttonId) {
                DialogInterface.BUTTON_POSITIVE -> {
                    val newName = editText.text.toString()
                    if (newName.isNotBlank()) {
                        document.rename(newName)

                        // The easiest way to refresh the UI is to load the directory again.
                        viewModel.loadDirectory(directoryUri)
                    }
                }
            }
        }

        val renameDialog = AlertDialog.Builder(requireActivity())
            .setTitle(R.string.rename_title)
            .setView(dialogView)
            .setPositiveButton(R.string.rename_okay, buttonCallback)
            .setNegativeButton(R.string.rename_cancel, buttonCallback)
            .create()

        // When the dialog is shown, select the name so it can be easily changed.
        renameDialog.setOnShowListener {
            editText.requestFocus()
            editText.selectAll()
        }

        renameDialog.show()
    }

    companion object {

        /**
         * Convenience method for constructing a [DirectoryFragment] with the directory [Uri]
         * to display.
         */
        @JvmStatic
        fun newInstance(directoryUri: Uri) =
            DirectoryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DIRECTORY_URI, directoryUri.toString())
                }
            }
    }
}

private const val ARG_DIRECTORY_URI = "com.example.android.directoryselection.ARG_DIRECTORY_URI"
