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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
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
     * and move logic that operates on the returned [View] to [onViewCreated]. First we call our
     * super's implementation of `onCreateView`, then we call the [FragmentGalleryBinding.inflate]
     * method to have it use our [LayoutInflater] parameter [inflater] to inflate its associated
     * layout file layout/fragment_gallery.xml and bind to it with our [ViewGroup] parameter
     * [container] supplying the `LayoutParams` and we then initialize our [FragmentGalleryBinding]
     * field [binding] to the binding returned. Next we call the [AppViewModel.loadImages] method of
     * our field [viewModel] to have it read the directory contents of our "images/" folder into its
     * [List] of [File] dataset [AppViewModel._images]. Then we initialize our [GalleryAdapter]
     * variable val galleryAdapter` to a new instance whose `onClick` function field is a lambda
     * that calls our [viewImageUsingExternalApp] method to have it launch an activity that allows
     * the user to view the [File] associated with the image that was clicked in the [GalleryAdapter].
     *
     * We use the [also] extension function on the [FragmentGalleryBinding.gallery] `RecyclerView` of
     * our [binding] field to:
     *  - Call the [RecyclerView.setLayoutManager] method of the [RecyclerView] (kotlin `layoutManager`
     *  property) to set its [RecyclerView.LayoutManager] to a new instance of [GridLayoutManager] with
     *  [GALLERY_COLUMNS] (3) columns in the grid.
     *  - Call the [RecyclerView.setAdapter] method of the [RecyclerView] (kotlin `adapter` property)
     *  to set its [RecyclerView.Adapter] to our `galleryAdapter` variable.
     *
     * Next we add an observer to the [AppViewModel.images] field of our [viewModel] field whose
     * lambda calls the [ListAdapter.submitList] method of `galleryAdapter` to submit the new [List]
     * of [File] value of the field to be diffed, and displayed whenever it changes value. Then we
     * add an observer to the [AppViewModel.notification] field of our [viewModel] field whose lambda
     * shows a [Snackbar] with the [String] value of that field whenever it changes value.
     *
     * Finally we return the outermost [View] in the layout file associated with [binding] to the
     * caller as our UI.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment.
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
            GalleryAdapter { image: File ->
                viewImageUsingExternalApp(image)
            }

        binding.gallery.also { view: RecyclerView ->
            view.layoutManager = GridLayoutManager(
                activity,
                GALLERY_COLUMNS
            )
            view.adapter = galleryAdapter
        }

        viewModel.images.observe(viewLifecycleOwner) { images: List<File> ->
            galleryAdapter.submitList(images)
        }

        viewModel.notification.observe(viewLifecycleOwner) {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
        }

        // TODO: Add popup menu https://material.io/develop/android/components/menu/
        //  https://www.techotopia.com/index.php/Working_with_the_Android_GridLayout_in_XML_Layout_Resources
        return binding.root
    }

    /**
     * Launches an [Intent] with the action [Intent.ACTION_VIEW] to start an activity which will
     * allow the user to view our [File] parameter [imageFile]. First we initialize our [Context]
     * variable `val context` to the [Context] this fragment is currently associated with. We then
     * initialize our [String] variable `val authority` to a string formed by concatenating the name
     * of this application's package followed by the string ".fileprovider" (a `provider` element in
     * our AndroidManifest.xml file has a "android:authorities" attribute that uses this string, and
     * its "android:name" attribute assigns the `provider` responsibility to the default functionality
     * that is provided by [androidx.core.content.FileProvider]). Next we initialize our [Uri] variable
     * `val contentUri` to the content URI returned by the [FileProvider.getUriForFile] method when
     * passed `context` for its [Context], `authority` for the authority of the [FileProvider] defined
     * by the `<provider>` element in our app's manifest, and `imageFile` as the [File] pointing to
     * the filename for which we want a content [Uri]. Then we initialize our [Intent] variable
     * `val viewIntent` to a new instance whose action is [Intent.ACTION_VIEW] and use the [apply]
     * extension function on it to set its `data` property to `contentUri`, and then add the flag
     * [Intent.FLAG_GRANT_READ_URI_PERMISSION] (the recipient of the [Intent] will be granted
     * permission to perform read operations on the [Uri] in the [Intent]'s data).
     *
     * Finally wrapped in a `try` block intendened to catch [ActivityNotFoundException] we call
     * [startActivity] to launch launch the activity specified by `viewIntent` (the `catch` block
     * will make and display a [Snackbar] with the message "Couldn't find suitable app to display
     * the image" if an Activity can not be found to execute the `viewIntent` [Intent]).
     *
     * @param imageFile the [File] we want the activity we launch to display. In our case it is the
     * [File] associated with the image in our [RecyclerView] that the user has clicked.
     */
    private fun viewImageUsingExternalApp(imageFile: File) {
        val context: Context = requireContext()
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, imageFile)

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            data = contentUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(viewIntent)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(
                binding.root,
                "Couldn't find suitable app to display the image",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }
}
