/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.graygallery.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewbinding.ViewBinding
import com.example.graygallery.databinding.FragmentDashboardBinding
import com.google.android.material.snackbar.Snackbar

/**
 * This is the start destination of our demo, and consists of four buttons:
 *  - "Take picture" takes a picture using the phones camera and stores it in our private storage area
 *  - "Select picture" allows the user to select a picture from a variety of sources ("Google Drive".
 *  "Google Photos", from the phone itself etc.) and stores it in our private storage area.
 *  - "Add Unsplash random image" downloads a random image from the "Unsplash" website and stores it
 *  in our private storage area.
 *  - "Clear files" removes all the files in the "images/" directory in our private storage area
 *  and deletes the "images/" directory as well.
 */
class DashboardFragment : Fragment() {
    /**
     * The [AppViewModel] used by both [DashboardFragment] and [GalleryFragment].
     */
    private val viewModel by viewModels<AppViewModel>()

    /**
     * The [ViewBinding] which is inflated from our layout file layout/fragment_dashboard.xml, it
     * consists of a `ConstraintLayout` root view holding a vertical `LinearLayout` which holds
     * four buttons labeled: "Take picture", "Select picture", "Add Unsplash random image", and
     * "Clear files".
     */
    private lateinit var binding: FragmentDashboardBinding

    private val takePicture: ActivityResultLauncher<Void> =
        registerForActivityResult(TakePicturePreview()) { bitmap ->
            viewModel.saveImageFromCamera(bitmap)
        }

    private val selectPicture: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(GetContentWithMimeTypes()) { uri ->
            uri?.let {
                viewModel.copyImageFromUri(uri)
            }
        }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreate(savedInstanceState)

        binding = FragmentDashboardBinding.inflate(inflater, container, false)

        binding.takePicture.setOnClickListener {
            takePicture.launch(null)
        }

        binding.selectPicture.setOnClickListener {
            selectPicture.launch(ACCEPTED_MIMETYPES)
        }

        binding.addRandomImage.setOnClickListener {
            viewModel.saveRandomImageFromInternet()
        }

        binding.clearFiles.setOnClickListener {
            viewModel.clearFiles()
        }

        viewModel.notification.observe(viewLifecycleOwner, {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
        })

        return binding.root
    }
}

class GetContentWithMimeTypes : ActivityResultContract<Array<String>, Uri?>() {
    override fun createIntent(
        context: Context,
        input: Array<String>
    ): Intent {
        return Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, input)

    }

    override fun getSynchronousResult(
        context: Context,
        input: Array<String>
    ): SynchronousResult<Uri?>? {
        return null
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
    }
}