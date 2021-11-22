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
import androidx.lifecycle.MutableLiveData
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
     * We add an observer to the [MutableLiveData] wrapped list of [CachingDocumentFile] field
     * [DirectoryFragmentViewModel.documents] of [viewModel] whose lambda calls the
     * [DirectoryEntryAdapter.setEntries] method of [adapter] with the new value of the field when
     * it changes value to have it update its [DirectoryEntryAdapter.directoryEntries] dataset with
     * the new list.
     *
     * We add an observer to the [MutableLiveData] wrapped [Event] of [CachingDocumentFile] field
     * [DirectoryFragmentViewModel.openDirectory] of [viewModel] whose lambda will call the
     * [MainActivity.showDirectoryContents] method with the [CachingDocumentFile.uri] property of
     * the [CachingDocumentFile] content of the [Event] to have it open the directory clicked on in
     * a new instance of [DirectoryFragment] (only if the [Event.getContentIfNotHandled] method
     * determines that the [Event] was not handled and returns its [CachingDocumentFile] content).
     *
     * We add another observer to the [MutableLiveData] wrapped [Event] of [CachingDocumentFile]
     * field [DirectoryFragmentViewModel.openDirectory] of [viewModel] whose lambda will call our
     * [openDocument] method with the [CachingDocumentFile.uri] property of the [CachingDocumentFile]
     * content of the [Event] to have it open the document clicked on in an activity which is able
     * to handle an [Intent] with a [Intent.ACTION_VIEW] action for the document [Uri] (only if the
     * [Event.getContentIfNotHandled] method determines that the [Event] was not handled and returns
     * its [CachingDocumentFile] content).
     *
     * Finally we return the outermost [View] in the associated layout file of [binding] to the
     * caller (this is the `FrameLayout` root view of the file layout/fragment_directory.xml).
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

        @Suppress("ReplaceGetOrSet")
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
     * Called when all saved state has been restored into the view hierarchy of the fragment. This
     * can be used to do initialization based on saved state that you are letting the view hierarchy
     * track itself, such as whether check box widgets are currently checked. This is called after
     * [onViewCreated] and before [onStart]. First we call our super's implementation of
     * [onViewStateRestored], then we call the [DirectoryFragmentViewModel.loadDirectory] method of
     * [viewModel] with our [Uri] field [directoryUri] to have it access the tree of documents that
     * the [Uri] points to and create a sorted [MutableLiveData] list of [CachingDocumentFile] from
     * it to initialize its [DirectoryFragmentViewModel.documents] field.
     *
     * @param savedInstanceState If the fragment is being re-created from
     * a previous saved state, this is the state.
     */
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        viewModel.loadDirectory(directoryUri)
    }

    /**
     * Called to have an appropriate activity open the [Uri] of the [CachingDocumentFile.uri] property
     * of our [document] parameter. Wrapped in a `try` block intended to catch [ActivityNotFoundException]
     * in order to toast an error message we initialize our [Intent] variable `val openIntent` with a
     * new instance whose action if [Intent.ACTION_VIEW] and use the [apply] extension function to set
     * its `flags` to [Intent.FLAG_GRANT_READ_URI_PERMISSION] and its `data` to the [CachingDocumentFile.uri]
     * property of [document]. Finally we use the [startActivity] method to launch whatever activity
     * has an intent filter matching [Intent]s like `openIntent`. We are called by the lambda of an
     * observer of the [DirectoryFragmentViewModel.openDocument] field of [viewModel] which is added
     * in our [onCreateView] override. [DirectoryFragmentViewModel.openDocument] is a [MutableLiveData]
     * wrapped [Event] of the [CachingDocumentFile] parameter [document] we called with.
     *
     * @param document the [CachingDocumentFile] object containing the [Uri] for a file that the user
     * wants to view in its [CachingDocumentFile.uri] property.
     */
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

    /**
     * Called to launch an [AlertDialog] which allows the user to rename the file referenced in its
     * [CachingDocumentFile] parameter [document]. First we initialize our [View] variable
     * `val dialogView` to the [View] returned by the [LayoutInflater.inflate] method of the cached
     * [LayoutInflater] used to inflate Views of this [Fragment] when it inflates the layout file
     * with the resource ID [R.layout.rename_layout]. We then initialize our [EditText] variable
     * `val editText` by finding the view in `dialogView` with the ID [R.id.file_name] and set the
     * text of that [EditText] to the [CachingDocumentFile.name] property of our parameter [document].
     * We initialize our [DialogInterface.OnClickListener] variable `val buttonCallback` to a lambda
     * which will when the `buttonId` of the dialog button clicked is [DialogInterface.BUTTON_POSITIVE]:
     *  - Initialize its [String] variable `val newName` to the text of `editText`
     *  - and if `newName` is not blank, call the [CachingDocumentFile.rename] method of [document]
     *  to rename the file, then call the [DirectoryFragmentViewModel.loadDirectory] method of [viewModel]
     *  to have it reload our [Uri] field [directoryUri] into its [DirectoryFragmentViewModel.documents]
     *  dataset so that it reflects the renamed file.
     *
     * Next we initialize our [AlertDialog] variable `val renameDialog` by using an [AlertDialog.Builder]
     * for the parent context theme of the `FragmentActivity` this fragment is currently associated with
     * to create an [AlertDialog] whose title is the [String] with resource ID [R.string.rename_title]
     * ("Rename"), whose custom view contents is `dialogView`, whose listener to be invoked when the
     * positive button of the dialog is pressed is `buttonCallback` and whose text is "Rename", and
     * whose listener to be invoked when the negative button of the dialog is pressed is `buttonCallback`
     * and whose text is "Cancel".
     *
     * We then set the [DialogInterface.OnShowListener] of `renameDialog` to a lambda (which will be
     * run when the dialog is shown) which requests focus for `editText` and selects the entire text
     * that it currently contains.
     *
     * Finally we call the [AlertDialog.show] method of `renameDialog` to start the dialog and display
     * it on the screen.
     *
     * @param document a [CachingDocumentFile] referencing the file that the user has chosen to
     * rename.
     */
    @SuppressLint("InflateParams")
    private fun renameDocument(document: CachingDocumentFile) {
        // Normally we don't want to pass `null` in as the parent, but the dialog doesn't exist,
        // so there isn't a parent layout to use yet.
        val dialogView: View = layoutInflater.inflate(R.layout.rename_layout, null)
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

        val renameDialog: AlertDialog = AlertDialog.Builder(requireActivity())
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
         * to display. We construct a new instance of [DirectoryFragment] and then use the [apply]
         * extension function to set its arguments [Bundle] to a new instance which has had the
         * [apply] extension store the [String] value of our [Uri] parameter [directoryUri] under
         * the key [ARG_DIRECTORY_URI].
         *
         * @param directoryUri the [Uri] of the directory that the user has chosen to be displayed.
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

/**
 * The key of the [Uri] that is passed to [DirectoryFragment] in its arguments [Bundle].
 */
private const val ARG_DIRECTORY_URI = "com.example.android.directoryselection.ARG_DIRECTORY_URI"
