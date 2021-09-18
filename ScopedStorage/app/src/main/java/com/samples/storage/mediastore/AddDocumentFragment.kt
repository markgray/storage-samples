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
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
    private val viewModel: AddDocumentViewModel by viewModels()

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

    private val actionRequestPermission =
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
