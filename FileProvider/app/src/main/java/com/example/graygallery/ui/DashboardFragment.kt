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
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
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
    private val takePicture: ActivityResultLauncher<Void?> =
        registerForActivityResult(TakePicturePreview()) { bitmap ->
            viewModel.saveImageFromCamera(bitmap ?: return@registerForActivityResult)
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
     * bind to it with our [ViewGroup] parameter [container] supplying the `LayoutParams` and then
     * initialize our [FragmentDashboardBinding] field [binding] to the binding returned. We next
     * proceed to set the [OnClickListener] of our four buttons:
     *  - [FragmentDashboardBinding.takePicture] of [binding] (labeled "Take picture") is set to a
     *  lambda which calls the [ActivityResultLauncher.launch] method of our [takePicture] field
     *  with `null` as its argument, which launches an activity for the [Bitmap] result it returns
     *  then calls the [AppViewModel.saveImageFromCamera] method of our [viewModel] field to compress
     *  the [Bitmap] into a JPEG which it stores in our "images/" folder.
     *  - [FragmentDashboardBinding.selectPicture] of [binding] (labeled "Select picture") is set to
     *  a lambda which calls the [ActivityResultLauncher.launch] method of our [selectPicture] field
     *  with our array of mime-types [ACCEPTED_MIMETYPES] as its argument to have it launch an activity
     *  for the [Uri] it returns when the user selects some image using the activity launched by the
     *  [Intent] action [Intent.ACTION_GET_CONTENT] then calls the [AppViewModel.copyImageFromUri]
     *  method of our [viewModel] field to have it fetch the content pointed to by the [Uri] and
     *  store it in our "images/" folder.
     *  - [FragmentDashboardBinding.addRandomImage] of [binding] (labeled "Add Unsplash random image")
     *  is set to a lambda which calls the [AppViewModel.saveRandomImageFromInternet] method of our
     *  [viewModel] field to have it download a random image from the "UnSplash" website and store
     *  it in our "images/" folder.
     *  - [FragmentDashboardBinding.clearFiles] of [binding] (labeled "Clear files") is set to a
     *  lambda which calls the [AppViewModel.clearFiles] method of our [viewModel] field to have it
     *  delete all the files in our "images/" folder and the folder as well.
     *
     * Next we add an observer to the [AppViewModel.notification] property of our [viewModel] field
     * with a [LifecycleOwner] that represents the Fragment's View lifecycle as the [LifecycleOwner]
     * which controls the observer, and an observer lambda which makes and shows a [Snackbar]
     * displaying the string value of the [AppViewModel.notification] property whenever it changes
     * value. Finally we return the outermost [View] in the layout file associated with [binding] to
     * the caller as our UI.
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
    @Suppress("RedundantNullableReturnType") // The method we override returns nullable
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

        viewModel.notification.observe(viewLifecycleOwner) {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
        }

        return binding.root
    }
}

/**
 * The [ActivityResultContract] that is used for the [ActivityResultLauncher] field
 * [DashboardFragment.selectPicture] of [DashboardFragment] as the argument to its
 * [Fragment.registerForActivityResult] override.
 */
class GetContentWithMimeTypes : ActivityResultContract<Array<String>, Uri?>() {
    /**
     * Creates an [Intent] that can be used for [Activity.startActivityForResult]. We construct a
     * new instance of [Intent] whose action is [Intent.ACTION_GET_CONTENT] (allows the user to
     * select a particular kind of data and return it), add the category [Intent.CATEGORY_OPENABLE]
     * to it (used to indicate that an [Intent] only wants URIs that can be opened with
     * [ContentResolver.openFileDescriptor]), set its MIME data type to any type, and then add our
     * [Array] of [String] parameter [input] as an extra with the name [Intent.EXTRA_MIME_TYPES]
     * (extra used to communicate a set of acceptable MIME types). Finally we return the [Intent]
     * to the caller.
     *
     * @param context a [Context] that an be used to access resources.
     * @param input the [Array] of [String] mime types to use as the [Intent.EXTRA_MIME_TYPES] extra
     * of the [Intent] we return.
     * @return an [Intent] with the action [Intent.ACTION_GET_CONTENT].
     */
    override fun createIntent(
        context: Context,
        input: Array<String>
    ): Intent {
        return Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, input)

    }

    /**
     * An optional method you can implement that can be used to potentially provide a result in
     * lieu of starting an activity. We just return `null` indicating that the call should proceed
     * to start an activity.
     *
     * @param context a [Context] that could be used to retrieve resources.
     * @param input  the [Array] of [String] mime types to use
     * @return the result wrapped in a [ActivityResultContract.SynchronousResult]. We return `null`
     * indicating that the call should proceed to start an activity.
     */
    override fun getSynchronousResult(
        context: Context,
        input: Array<String>
    ): SynchronousResult<Uri?>? {
        return null
    }

    /**
     * Convert [Intent] result obtained from [Activity.onActivityResult] to a [Uri]. If our [Intent]
     * parameter [intent] is `null` or our [resultCode] parameter is NOT [Activity.RESULT_OK] we
     * return `null`, otherwise we return the [Intent] stored as the `data` property of our [Intent]
     * parameter [intent].
     *
     * @param resultCode The integer result code returned by the child activity through its
     * `setResult` method, one of [Activity.RESULT_OK] or [Activity.RESULT_CANCELED].
     * @param intent An [Intent], which can return result data to the caller (various data can be
     * attached as [Intent] "extras").
     * @return the [Uri] that was stored as the `data` of [intent].
     */
    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
    }
}