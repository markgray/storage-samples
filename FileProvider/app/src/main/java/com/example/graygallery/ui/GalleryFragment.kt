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

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.graygallery.databinding.FragmentGalleryBinding
import com.google.android.material.snackbar.Snackbar
import java.io.File

/**
 * Number of columns in the grid of our [RecyclerView].
 */
const val GALLERY_COLUMNS = 3

/**
 * This [Fragment] displays all of the images we have downloaded to our "images/" folder.
 */
class GalleryFragment : Fragment() {
    /**
     * The [AppViewModel] used by both [DashboardFragment] and [GalleryFragment].
     */
    private val viewModel by viewModels<AppViewModel>()
    /**
     * The [ViewBinding] which is inflated from our layout file layout/fragment_gallery.xml, it
     * consists of a `ConstraintLayout` root view holding a [RecyclerView].
     */
    private lateinit var binding: FragmentGalleryBinding

    /**
     * Called to have the fragment instantiate its user interface view. This will be called between
     * [onCreate] and [onViewCreated]. It is recommended to only inflate the layout in this method
     * and move logic that operates on the returned [View] to [onViewCreated].
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the `LayoutParams` of the view.
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
        binding = FragmentGalleryBinding.inflate(inflater, container, false)

        viewModel.loadImages()

        val galleryAdapter =
            GalleryAdapter { image ->
                viewImageUsingExternalApp(image)
            }

        binding.gallery.also { view ->
            view.layoutManager = GridLayoutManager(
                activity,
                GALLERY_COLUMNS
            )
            view.adapter = galleryAdapter
        }

        viewModel.images.observe(viewLifecycleOwner, { images ->
            galleryAdapter.submitList(images)
        })

        viewModel.notification.observe(viewLifecycleOwner, {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
        })

        // TODO: Add popup menu https://material.io/develop/android/components/menu/
        //  https://www.techotopia.com/index.php/Working_with_the_Android_GridLayout_in_XML_Layout_Resources
        return binding.root
    }

    private fun viewImageUsingExternalApp(imageFile: File) {
        val context = requireContext()
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, imageFile)

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            data = contentUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(viewIntent)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.root, "Couldn't find suitable app to display the image", Snackbar.LENGTH_SHORT).show()
        }
    }
}
