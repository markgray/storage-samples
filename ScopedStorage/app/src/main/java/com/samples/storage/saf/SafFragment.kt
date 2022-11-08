/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samples.storage.saf

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsProvider
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.samples.storage.R
import com.samples.storage.databinding.FragmentSafBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext

private const val DEFAULT_FILE_NAME = "SAF Demo File.txt"

/**
 * Fragment that demonstrates the most common ways to work with documents via the
 * Storage Access Framework (SAF).
 *
 * See: https://developer.android.com/guide/topics/providers/document-provider
 */
class SafFragment : Fragment() {
    /**
     * The view binding generated from our layout file layout/fragment_saf.xml (resource ID
     * [R.layout.fragment_saf]. It consists of a `ConstraintLayout` root view holding three
     * [Button] widgets labeled "Create Document" (resource ID [R.id.create_file] aka
     * [FragmentSafBinding.createFile]), "Open Document" (resource ID [R.id.open_file] aka
     * [FragmentSafBinding.openFile]) and "Open Folder" (resource ID [R.id.open_folder] aka
     * [FragmentSafBinding.openFolder]) above a [TextView] (resource ID [R.id.output] aka
     * [FragmentSafBinding.output]). This is private to prevent other classes from modifying it,
     * read-only access is provided by our [binding] field.
     */
    private var _binding: FragmentSafBinding? = null

    /**
     * Read-only access to our [FragmentSafBinding] field [_binding].
     */
    private val binding get() = _binding!!

    /**
     * The custom [AndroidViewModel] we use.
     */
    private val viewModel: SafFragmentViewModel by viewModels()

    /**
     * The [ActivityResultLauncher] that the user launches when they click the "Create Document"
     * [Button]. The [ActivityResultContract] argument [CreateDocument] to [registerForActivityResult]
     * is an [ActivityResultContract] whose [ActivityResultLauncher.launch] method takes a [String]
     * which is used as the initial default file name (which can be changed by the user), and returns
     * the [Uri] that the [DocumentsProvider] returns in the [Intent] via its [Intent.getData] (aka
     * kotlin `data` property) to the lambda argument of [registerForActivityResult].
     *
     * In the lambda argument to [registerForActivityResult] we initialize our [Uri] variable
     * `val documentUri` to the `uri` argument to the lambda, returning to [registerForActivityResult]
     * if this is `null` (the user returned to this fragment without creating a file). Otherwise we
     * initialize our [DocumentFile] variable `val documentFile` to the instance that the
     * [DocumentFile.fromSingleUri] method creates to represent the single document at the [Uri]
     * `documentUri` again returning to [registerForActivityResult] if this is `null` (probably
     * means the file has been removed or became unavailable). We get a [LifecycleOwner] that
     * represents the [Fragment]'s View lifecycle, and use the [CoroutineScope] tied to this
     * [LifecycleOwner]'s Lifecycle to launch a new coroutine block. In this block we initialize
     * our [OutputStream] variable `val documentStream` to the result of the execution of a
     * suspending block in the [CoroutineContext] of [Dispatchers.IO] with that suspending block
     * returning the [OutputStream] that the [ContentResolver.openOutputStream] method of a
     * [ContentResolver] instance for our application's package opens to write to the content
     * associated with the content [Uri] `documentUri` (exiting the launch suspending block if this
     * is `null`). If `documentStream` is not `null` the suspending block initializes its [String]
     * variable `val text` to the value returned by the [SafFragmentViewModel.createDocumentExample]
     * method of [viewModel] when it writes that same [String] to the file `documentStream`. We then
     * set the text of the [TextView] found at the [FragmentSafBinding.output] field of [binding]
     * to a [String] formed by concatenating the string "File:" followed by the name of the file
     * returned by [DocumentFile.getName] (aka kotlin `name` property) of `documentFile`, followed
     * by the [String] `text`.
     */
    @Suppress("DEPRECATION") // Should pass a mime type to `CreateDocument` constructor.
    private val actionCreateDocument: ActivityResultLauncher<String> =
        registerForActivityResult(CreateDocument()) { uri: Uri? ->
            // If the user returns to this fragment without creating a file, uri will be null
            // In this case, we return void
            val documentUri: Uri = uri ?: return@registerForActivityResult

            // If we can't instantiate a `DocumentFile`, it probably means the file has been removed
            // or became unavailable (if the SD card has been removed).
            // In this case, we return void
            val documentFile: DocumentFile = DocumentFile.fromSingleUri(requireContext(), documentUri)
                ?: return@registerForActivityResult

            // We launch a coroutine within the lifecycle of the viewmodel. The coroutine will be
            // automatically cancelled if the viewmodel is cleared
            viewLifecycleOwner.lifecycleScope.launch {
                val documentStream: OutputStream = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openOutputStream(documentUri)
                } ?: return@launch

                val text: String = viewModel.createDocumentExample(documentStream)
                binding.output.text =
                    getString(R.string.saf_create_file_output, documentFile.name, text)
            }

            Log.d("SafFragment", "Created: ${documentFile.name}, type ${documentFile.type}")
        }

    /**
     * The [ActivityResultLauncher] that the user launches when they click the "Open Document"
     * [Button]. The [ActivityResultContract] argument [OpenDocument] to [registerForActivityResult]
     * is an [ActivityResultContract] whose [ActivityResultLauncher.launch] method takes a [String]
     * array of the mime types to filter by and returns the [Uri] that the [DocumentsProvider] returns
     * in the [Intent] via its [Intent.getData] (aka kotlin `data` property) to the lambda argument
     * of [registerForActivityResult].
     *
     * In the lambda argument to [registerForActivityResult] we initialize our [Uri] variable
     * `val documentUri` to the `uri` argument to the lambda, returning to [registerForActivityResult]
     * if this is `null` (the user returned to this fragment without selecting a file). Otherwise we
     * initialize our [DocumentFile] variable `val documentFile` to the instance that the
     * [DocumentFile.fromSingleUri] method creates to represent the single document at the [Uri]
     * `documentUri` again returning to [registerForActivityResult] if this is `null` (probably
     * means the file has been removed or became unavailable). We get a [LifecycleOwner] that
     * represents the [Fragment]'s View lifecycle, and use the [CoroutineScope] tied to this
     * [LifecycleOwner]'s Lifecycle to launch a new coroutine block. In this block we initialize
     * our [InputStream] variable `val documentStream` to the result of the execution of a
     * suspending block in the [CoroutineContext] of [Dispatchers.IO] with that suspending block
     * returning the [InputStream] that the [ContentResolver.openInputStream] method of a
     * [ContentResolver] instance for our application's package opens to read the content associated
     * with the content [Uri] `documentUri` (exiting the launch suspending block if this is `null`).
     * If `documentStream` is not `null` the suspending block initializes its [String] variable
     * `val text` to the value returned by the [SafFragmentViewModel.openDocumentExample] method of
     * [viewModel] when it reads the contents of the [InputStream] `documentStream`, creates a
     * [String] hashcode of that contents and returns that to us. We then set the text of the
     * [TextView] found at the [FragmentSafBinding.output] field of [binding] to a [String] formed
     * by concatenating the string "File:" followed by the name of the file returned by
     * [DocumentFile.getName] (aka kotlin `name` property) of `documentFile`, followed by the
     * string "Contents hash:" followed by the [String] `text`.
     */
    private val actionOpenDocument = registerForActivityResult(OpenDocument()) { uri: Uri? ->
        // If the user returns to this fragment without selecting a file, uri will be null
        // In this case, we return void
        val documentUri: Uri = uri ?: return@registerForActivityResult

        // If we can't instantiate a `DocumentFile`, it probably means the file has been removed
        // or became unavailable (if the SD card has been removed).
        // In this case, we return void
        val documentFile: DocumentFile = DocumentFile.fromSingleUri(requireContext(), documentUri)
            ?: return@registerForActivityResult

        viewLifecycleOwner.lifecycleScope.launch {
            val documentStream: InputStream = withContext(Dispatchers.IO) {
                requireContext().contentResolver.openInputStream(documentUri)
            } ?: return@launch

            val text: String = viewModel.openDocumentExample(documentStream)
            binding.output.text = getString(R.string.saf_open_file_output, documentFile.name, text)
        }
    }

    /**
     * The [ActivityResultLauncher] that the user launches when they click the "Open Folder"
     * [Button]. The [ActivityResultContract] argument [OpenDocumentTree] to [registerForActivityResult]
     * is an [ActivityResultContract] whose [ActivityResultLauncher.launch] method takes an optional
     * [Uri] of the initial starting location, and returns the [Uri] that the [DocumentsProvider]
     * returns in the [Intent] via its [Intent.getData] (aka kotlin `data` property) to the lambda
     * argument of [registerForActivityResult].
     *
     * In the lambda argument to [registerForActivityResult] we initialize our [Uri] variable
     * `val documentUri` to the `uri` argument to the lambda, returning to [registerForActivityResult]
     * if this is `null` (the user returned to this fragment without selecting a folder). We initialize
     * our [Context] variable `val context` to the [Context] of the single, global Application object
     * of the current process. We initialize our [DocumentFile] variable `val parentFolder` to the
     * instance that the [DocumentFile.fromTreeUri] method creates to represent the document tree
     * rooted at the [Uri] `documentUri` again returning to [registerForActivityResult] if this is
     * `null`. We get a [LifecycleOwner] that represents the [Fragment]'s View lifecycle, and use
     * the [CoroutineScope] tied to this [LifecycleOwner]'s Lifecycle to launch a new coroutine block.
     * We initialize our [String] variable `val text` to a string that we build by calling the
     * [SafFragmentViewModel.listFiles] method of [viewModel] to retrieve the list of [Pair] that it
     * forms from the [String] name of the files in `parentFolder` and the [Uri] to that file, then
     * sorting that list on the [Pair.first] entries, and then joining all of the [Pair.first] names
     * in a [String] separated by the default ", " separator. Finally we set the text of the [TextView]
     * found at the [FragmentSafBinding.output] field of [binding] to a [String] formed by
     * concatenating the string "Folder contents:" to `text`.
     */
    private val actionOpenDocumentTree = registerForActivityResult(OpenDocumentTree()) { uri ->
        val documentUri: Uri = uri ?: return@registerForActivityResult
        val context: Context = requireContext().applicationContext
        val parentFolder: DocumentFile = DocumentFile.fromTreeUri(context, documentUri)
            ?: return@registerForActivityResult

        viewLifecycleOwner.lifecycleScope.launch {
            val text: String = viewModel.listFiles(parentFolder)
                .sortedBy { it.first }
                .joinToString { it.first }
            binding.output.text = getString(R.string.saf_folder_output, text)
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view. This will be called between
     * [onCreate] and [onViewCreated]. It is recommended to only inflate the layout in this method
     * and move logic that operates on the returned View to [onViewCreated]. If you return a [View]
     * from here, you will later be called in [onDestroyView] when the view is being released.
     *
     * We initialize our [FragmentSafBinding] field [_binding] by having the [FragmentSafBinding.inflate]
     * method use our [LayoutInflater] parameter [inflater] to inflate and bind to its associated
     * layout file layout/fragment_saf.xml with our [ViewGroup] parameter [container] supplying the
     * `LayoutParams`.
     *
     * Next we add a [View.OnClickListener] to the [FragmentSafBinding.createFile] button of [binding]
     * whose lambda launches our [ActivityResultLauncher] field [actionCreateDocument] with
     * [DEFAULT_FILE_NAME] as the preferred default filename (which can be overwritten by the
     * user).
     *
     * Then we add a [View.OnClickListener] to the [FragmentSafBinding.openFile] button of [binding]
     * whose lambda launches our [ActivityResultLauncher] field [actionOpenDocument] with an array
     * of [String] holding a single MIME type [String] specifying any type of file.
     *
     * Then we add a [View.OnClickListener] to the [FragmentSafBinding.openFolder] button of [binding]
     * whose lambda launches our [ActivityResultLauncher] field [actionOpenDocumentTree] with `null`
     * as the optional initial starting location.
     *
     * Finally we return the outermost [View] in the associated layout file of [binding] to serve as
     * our UI.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment.
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself, but this
     * can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSafBinding.inflate(inflater, container, false)

        binding.createFile.setOnClickListener {
            // We ask the user to create a file with a preferred default filename, which can be
            // overwritten by the user
            actionCreateDocument.launch(DEFAULT_FILE_NAME)
        }

        binding.openFile.setOnClickListener {
            // We ask the user to select any file. If we want to select a specific one, we would do
            // this: `actionOpenDocument.launch(arrayOf("image/png", "image/gif"))`
            actionOpenDocument.launch(arrayOf("*/*"))
        }

        binding.openFolder.setOnClickListener {
            // We ask the user to select a folder. We can specify a preferred folder to be opened
            // if we have its URI and the device is running on API 26+
            actionOpenDocumentTree.launch(null)
        }

        return binding.root
    }

    /**
     * Called when the view previously created by [onCreateView] has been detached from the fragment.
     * The next time the fragment needs to be displayed, a new view will be created. This is called
     * after [onStop] and before [onDestroy]. Internally it is called after the view's state has been
     * saved but before it has been removed from its parent. First we call our super's implementation
     * of `onDestroy`, then we set our [FragmentSafBinding] field [_binding] to `null`.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
