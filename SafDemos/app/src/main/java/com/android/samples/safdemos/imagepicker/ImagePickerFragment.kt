/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.samples.safdemos.imagepicker

import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.android.samples.safdemos.databinding.FragmentImagePickerBinding
import com.android.samples.safdemos.R
import java.io.InputStream

/**
 * The MIME type of the files we would like the user to select for us to display (images of any kind)
 */
const val IMAGE_MIME_TYPE: String = "image/*"

/**
 * Demo that will launch an [Intent] for the action [Intent.ACTION_GET_CONTENT] for the mime type
 * [IMAGE_MIME_TYPE] when the user clicks the `FloatingActionButton` and then fetch and display the
 * image that the user selects in the [ImageView] in its UI.
 */
class ImagePickerFragment : Fragment() {
    /**
     * The [FragmentImagePickerBinding] that is inflated from its associated layout file
     * layout/fragment_image_picker.xml (resource ID `R.layout.fragment_image_picker`) and
     * used for the fragment's UI. The layout file consists of a `ConstraintLayout` root view
     * holding a [TextView] with the text "Select an image", an [ImageView] that the selected
     * image will be displayed in, and a `FloatingActionButton` that when clicked will launch
     * an activity that will allow the user to select an image for us to display.
     */
    private lateinit var binding: FragmentImagePickerBinding

    /**
     * The [ActivityResultLauncher] that we launch from our [selectImage] method with an [Intent]
     * for an activity that will allow the user to select an image for us to display. The method
     * [registerForActivityResult] registers a request to start an activity for result, designated
     * by the [ActivityResultContracts.StartActivityForResult] contract (this `ActivityResultContract`
     * doesn't do any type conversion, taking a raw [Intent] as its input and [ActivityResult] as
     * its output), with the [ActivityResultCallback] of the call to [registerForActivityResult] a
     * lambda which will, if the `resultCode` property of the [ActivityResult] passed it is [RESULT_OK],
     * extract the `data` [Uri] from the `data` [Intent] of the [ActivityResult] (if none of these are
     * `null`) and set the content of the [ImageView] found in the [FragmentImagePickerBinding.preview]
     * field of [binding] to the [Bitmap] that the [BitmapFactory.decodeStream] method decodes from
     * the [InputStream] that a [ContentResolver] instance for our application's package opens when
     * its [ContentResolver.openInputStream] method is called with the [Uri] returned in the
     * [ActivityResult].
     */
    private val resultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Uri? = result.data?.data
                data?.let { uri: Uri ->
                    activity?.let {
                        binding.preview.setImageBitmap(
                            BitmapFactory.decodeStream(it.contentResolver.openInputStream(uri))
                        )
                    }
                }
            }
        }

    /**
     * Called to have the fragment instantiate its user interface view. This will be called between
     * [onCreate] and [onViewCreated]. It is recommended to only inflate the layout in this method
     * and move logic that operates on the returned View to [onViewCreated]. We initialize our
     * [FragmentImagePickerBinding] field [binding] by having the [FragmentImagePickerBinding.inflate]
     * method inflate its associated layout file layout/fragment_image_picker.xml (resource ID
     * `R.layout.fragment_image_picker`) and bind to it (the use of [getLayoutInflater] (aka kotlin
     * `layoutInflater` property) in the call to inflate instead of the [inflater] parameter is a
     * bit puzzling, but the result is the same so what of it?). Next we set the [View.OnClickListener]
     * of the `FloatingActionButton` found in the [FragmentImagePickerBinding.selectImageButton] field
     * of [binding] to a lambda which calls our [selectImage] method. Finally we return the outermost
     * [View] in the layout file associated with our [FragmentImagePickerBinding] field [binding] to
     * have it used for our UI.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate any views in the
     * fragment.
     * @param container If non-`null`, this is the parent view that the fragment's UI will be
     * attached to. The fragment should not add the view itself, but this can be used to generate
     * the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed from a
     * previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or `null`.
     */
    @Suppress("RedundantNullableReturnType") // The method we override returns nullable
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentImagePickerBinding.inflate(layoutInflater)

        binding.selectImageButton.setOnClickListener {
            selectImage()
        }

        return binding.root
    }

    /**
     * Called to launch an activity which will allow the user to select an image for us to display.
     * We initialize our [Intent] variable `val intent` to a new instance whose action is
     * [Intent.ACTION_GET_CONTENT] (allows the user to select a particular kind of data and returns
     * a [Uri] to access it by). We chain a call to the [apply] extension function to the [Intent]
     * construction whose lambda sets the `type` of the [Intent] to [IMAGE_MIME_TYPE] (any kind of
     * image). Finally we call the [ActivityResultLauncher.launch] method of our [resultLauncher]
     * field to launch the activity selected by `intent` for the result which will be handled by
     * the lambda callback of [resultLauncher] (it will fetch the image using the [Uri] in the
     * [ActivityResult] returned and display it in the [ImageView] found in the
     * [FragmentImagePickerBinding.preview] field of [binding]).
     */
    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = IMAGE_MIME_TYPE
        }

        resultLauncher.launch(intent)
    }

}
