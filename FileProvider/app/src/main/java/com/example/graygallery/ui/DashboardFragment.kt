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
import android.graphics.Bitmap
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

    /**
     * This [ActivityResultLauncher] calls the [AppViewModel.saveImageFromCamera] method of our field
     * [viewModel] with the [Bitmap] returned by the [TakePicturePreview] system [ActivityResultContract]
     * which launches an [Intent] with the action `MediaStore.ACTION_IMAGE_CAPTURE` (take a small
     * picture) to capture the [Bitmap] from the phone's camera. [AppViewModel.saveImageFromCamera]
     * then encodes the [Bitmap] as a JPEG and stores it in the "images/" directory of the apps private
     * storage area.
     */
    private val takePicture: ActivityResultLauncher<Void> =
        registerForActivityResult(TakePicturePreview()) { bitmap ->
            viewModel.saveImageFromCamera(bitmap)
        }

    /**
     * This [ActivityResultLauncher] registers our custom [ActivityResultContract] class
     * [GetContentWithMimeTypes] for the activity result that is returned when the [Intent] with the
     * action [Intent.ACTION_GET_CONTENT] that its [ActivityResultContract.createIntent] method creates
     * is launched. Our lambda callback calls the [AppViewModel.copyImageFromUri] method of our field
     * [viewModel] with the [Uri] that is returned from the activity that got launched to download
     * the [Uri] and store it in the "images/" directory of the apps private storage area.
     */
    private val selectPicture: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(GetContentWithMimeTypes()) { uri ->
            uri?.let {
                viewModel.copyImageFromUri(uri)
            }
        }

    /**
     * Called to have the fragment instantiate its user interface view. This will be called between
     * [onCreate] and [onViewCreated]. First we call our super's implementation of `onCreateView`,
     * then we call the [FragmentDashboardBinding.inflate] method to have it use our [LayoutInflater]
     * parameter [inflater] to inflate its associated layout file layout/fragment_dashboard.xml and
     * bind to it with our [ViewGroup] parameter [container] supplying the `LayoutParams`.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment.
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or null.
     */
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