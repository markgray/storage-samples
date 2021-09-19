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
package com.samples.storage.mediastore

import android.Manifest
import android.os.Bundle
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.samples.storage.R
import com.samples.storage.databinding.FragmentAddDocumentBinding
import kotlinx.coroutines.launch

/**
 * Downloads a random non-media document from the Internet to our shared "Downloads" folder. It is
 * navigated to from the [MediaStoreFragment] when the item view in its [RecyclerView] with the text
 * "Add Document to Downloads" is clicked.
 */
class AddDocumentFragment : Fragment() {
    /**
     * The view binding generated from our layout file layout/fragment_add_document.xml (resource ID
     * [R.layout.fragment_add_document]). It consists of a `ConstraintLayout` root view holding three
     * separate `LinearLayout`: a "permissions section" for requesting storage permission if needed,
     * a "file details section" for displaying information about the file that was downloaded, and a
     * "Download Random File" from the Internet section. The visibility of the permissions section
     * and the file details section is toggled depending on whether their contents is relevant at
     * the moment. This field is private to prevent other classes from modifying it, read-only access
     * is provided by our [binding] field (but it is private as well out of habit I guess).
     */
    private var _binding: FragmentAddDocumentBinding? = null
    /**
     * Read-only access to our to our [FragmentAddDocumentBinding] field [_binding].
     */
    private val binding get() = _binding!!

    /**
     * The custom [AndroidViewModel] we use.
     */
    private val viewModel: AddDocumentViewModel by viewModels()

    /**
     * Called to have the fragment instantiate its user interface view. This will be called between
     * [onCreate] and [onViewCreated]. It is recommended to only inflate the layout in this method
     * and move logic that operates on the returned [View] to [onViewCreated]. If you return a [View]
     * from here, you will later be called in [onDestroyView] when the view is being released. First
     * we initialize our [FragmentAddDocumentBinding] field [_binding] by having the
     * [FragmentAddDocumentBinding.inflate] method use our [LayoutInflater] parameter [inflater] to
     * inflate and bind to its associated layout file layout/fragment_add_document.xml with our
     * [ViewGroup] parameter [container] supplying the `LayoutParams`.
     *
     * Next we add an observer whose lifespan is tied to this fragments [View] lifecycle  to the
     * [AddDocumentViewModel.currentFileEntry] field of our [viewModel] field whose lambda will:
     *  - If the [FileEntry] value observed changes to `null` set the visibility of the `LinearLayout`
     *  property [FragmentAddDocumentBinding.fileDetails] field of [binding] to [View.GONE] and return.
     *  - Otherwise sets the text of the [TextView] at [FragmentAddDocumentBinding.filename] of [binding]
     *  to the [String] in the [FileEntry.filename] field of the value observed.
     *  - Sets the text of the [TextView] at [FragmentAddDocumentBinding.filePath] of [binding] to
     *  the [String] in the [FileEntry.path] field of the value observed.
     *  - Sets the text of the [TextView] at [FragmentAddDocumentBinding.fileSizeAndMimeType] of
     *  [binding] to a [String] describing the file size and mime type of the observed [FileEntry].
     *  - Sets the text of the [TextView] at [FragmentAddDocumentBinding.fileAddedAt] of [binding]
     *  to a [String] describing the [FileEntry.addedAt] date added field of the observed value.
     *  - Finally the lambda sets the visibility of the [FragmentAddDocumentBinding.fileDetails]
     *  field of [binding] to [View.VISIBLE].
     *
     * Next we add an observer whose lifespan is tied to this fragments [View] lifecycle  to the
     * [AddDocumentViewModel.isDownloading] field of our [viewModel] field whose lambda will set
     * the enabled property of the [FragmentAddDocumentBinding.downloadRandomFileFromInternet] to
     * the inverse of the [Boolean] value it is observing (disabling it when we are downloading, and
     * enabling it again when the download finishes.
     *
     * Next we add a [View.OnClickListener] to the [FragmentAddDocumentBinding.requestPermissionButton]
     * button of [binding] which launches our [ActivityResultLauncher] field [actionRequestPermission]
     * with an [Array] of [String] containing the permissions [Manifest.permission.READ_EXTERNAL_STORAGE]
     * and [Manifest.permission.WRITE_EXTERNAL_STORAGE] which launches an activity that will ask the
     * user to grant us these permissions.
     *
     * Next we add a [View.OnClickListener] to the [FragmentAddDocumentBinding.downloadRandomFileFromInternet]
     * button of [binding] which launches a new coroutine using the [LifecycleCoroutineScope] of the
     * a [LifecycleOwner] that represents the [Fragment]'s [View] lifecycle. In the coroutine block
     * we branch on the [Boolean] value returned from the [AddDocumentViewModel.canAddDocument] method
     * of our [viewModel] field:
     *  - `true`: we call the [AddDocumentViewModel.addRandomFile] method of [viewModel] to have it
     *  download a random file from the Internet and store it in the devices "Downloads" folder.
     *  - `false`: we call our [showPermissionSection] method to have it change the visibility of the
     *  [FragmentAddDocumentBinding.permissionSection] view of [binding] to [View.VISIBLE] (displays
     *  the `LinearLayout` which informs the user that: "To add files, we need to request the storage
     *  permission" and contains a button that they can click to have the system ask them to grant us
     *  permission), and [showPermissionSection] also changes the visibility of the
     *  [FragmentAddDocumentBinding.permissionSection] view of [binding] to [View.GONE] (`LinearLayout`
     *  that holds the "Download Random File" button).
     *
     * Finally our [onCreateView] override returns the outermost [View] in the layout file associated
     * with [binding] to have it used for our UI.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate any views in
     * the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's UI will be
     * attached to. The fragment should not add the view itself, but this can be used to generate
     * the `LayoutParams` of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or `null`.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDocumentBinding.inflate(inflater, container, false)

        // Every time currentFileEntry is changed, we update the file details
        viewModel.currentFileEntry.observe(viewLifecycleOwner) { fileDetails: FileEntry? ->
            if (fileDetails == null) {
                binding.fileDetails.visibility = View.GONE
                return@observe
            }

            binding.filename.text = fileDetails.filename
            binding.filePath.text = fileDetails.path
            binding.fileSizeAndMimeType.text = getString(
                R.string.mediastore_file_size_and_mimetype,
                Formatter.formatShortFileSize(context, fileDetails.size),
                fileDetails.mimeType
            )
            binding.fileAddedAt.text = getString(
                R.string.mediastore_file_added_at,
                DateUtils.formatDateTime(
                    context,
                    fileDetails.addedAt,
                    DateUtils.FORMAT_SHOW_TIME or
                        DateUtils.FORMAT_SHOW_DATE or
                        DateUtils.FORMAT_SHOW_YEAR or
                        DateUtils.FORMAT_SHOW_WEEKDAY or
                        DateUtils.FORMAT_ABBREV_ALL
                )
            )
            binding.fileDetails.visibility = View.VISIBLE
        }

        // Every time isDownloading is changed, we toggle the download button
        viewModel.isDownloading.observe(viewLifecycleOwner) { isDownloading: Boolean ->
            binding.downloadRandomFileFromInternet.isEnabled = !isDownloading
        }

        binding.requestPermissionButton.setOnClickListener {
            actionRequestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }

        binding.downloadRandomFileFromInternet.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {

                if (viewModel.canAddDocument) {
                    viewModel.addRandomFile()
                } else {
                    showPermissionSection()
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        handlePermissionSectionVisibility()
    }

    private val actionRequestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            handlePermissionSectionVisibility()
        }

    private fun handlePermissionSectionVisibility() {
        if (viewModel.canAddDocument) {
            hidePermissionSection()
        } else {
            showPermissionSection()
        }
    }

    private fun hidePermissionSection() {
        binding.permissionSection.visibility = View.GONE
        binding.actions.visibility = View.VISIBLE
    }

    private fun showPermissionSection() {
        binding.permissionSection.visibility = View.VISIBLE
        binding.actions.visibility = View.GONE
    }
}
