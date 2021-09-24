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

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.samples.storage.R
import com.samples.storage.databinding.FragmentAddMediaBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Allows the user to "Take Picture", "Take Video", or "Download Picture" and store them in the
 * appropriate shared media folder. It is navigated to from the [MediaStoreFragment] when the item
 * view in its [RecyclerView] with the text "Add Media File" is clicked.
 */
class AddMediaFragment : Fragment() {
    /**
     * The view binding generated from our layout file layout/fragment_add_media.xml (resource ID
     * [R.layout.fragment_add_media]). It consists of a `ConstraintLayout` root view holding a
     * "Request Permission" `LinearLayout` section, an [ImageView] which displays the image in
     * the [AddMediaViewModel.currentMediaUri] whenever it changes, and a `LinearLayout` "Action
     * Section" holding three buttons that the user can click to "Take Picture", "Take Video", or
     * "Download Picture". When the user needs to grant us permission the "Request Permission"
     * section is visible and the "Action Section" is GONE, and if we have permission the "Request
     * Permission" section is GONE and the "Action Section" is visible. Private to prevent other
     * classes from modifying it, read-only access is provided by our [binding] field.
     */
    private var _binding: FragmentAddMediaBinding? = null

    /**
     * Read-only access to our [FragmentAddMediaBinding] field [_binding].
     */
    private val binding get() = _binding!!

    /**
     * The custom [AndroidViewModel] we use.
     */
    private val viewModel: AddMediaViewModel by viewModels()

    /**
     * Called to have the fragment instantiate its user interface view. This will be called between
     * [onCreate] and [onViewCreated]. It is recommended to only inflate the layout in this method
     * and move logic that operates on the returned View to [onViewCreated]. If you return a [View]
     * from here, you will later be called in [onDestroyView] when the view is being released. First
     * we initialize our [FragmentAddMediaBinding] field [_binding] by having the
     * [FragmentAddMediaBinding.inflate] method use our [LayoutInflater] parameter [inflater] to
     * inflate and bind to its associated layout file layout/fragment_add_document.xml with our
     * [ViewGroup] parameter [container] supplying the `LayoutParams`.
     *
     * We add an observer to the [AddMediaViewModel.currentMediaUri] field of our [viewModel] field
     * whose lambda will begin a load with [Glide] of the [Uri] value of the observed field into the
     * [ImageView] in the [FragmentAddMediaBinding.mediaThumbnail] field of [binding] whenever the
     * [Uri] changes value.
     *
     * We set the [View.OnClickListener] of the [FragmentAddMediaBinding.requestPermissionButton]
     * button in [binding] to a lambda which launches our [ActivityResultLauncher] field
     * [actionRequestPermission] to have the system ask the user to grant us the permissons
     * [READ_EXTERNAL_STORAGE] and [WRITE_EXTERNAL_STORAGE].
     *
     * We set the [View.OnClickListener] of the [FragmentAddMediaBinding.takePictureButton] button
     * in [binding] to a lambda which gets a [LifecycleOwner] that represents the [Fragment]'s [View]
     * lifecycle and uses the [CoroutineScope] tied to this [LifecycleOwner]'s Lifecycle to launch
     * a new coroutine without blocking the current thread. In that coroutine lambda we branch on
     * whether the [AddMediaViewModel.canWriteInMediaStore] field of [viewModel] is `true`:
     *  - `true`: we call the [AddMediaViewModel.createPhotoUri] method of [viewModel] to have it
     *  create a [Uri] where the image taken by the camera will be stored using [Source.CAMERA]
     *  ("camera-") as the prefix for the image. If this is non-`null` we use the [let] extension
     *  function on the [Uri] created to call the [AddMediaViewModel.saveTemporarilyPhotoUri] method
     *  of [viewModel] to have it save the [Uri] in its private [SavedStateHandle] field under the
     *  key "temporaryPhotoUri", and then we launch our [ActivityResultLauncher] field [actionTakePicture]
     *  to have [MediaStore] take a picture and store it in the content [Uri].
     *  - `false`: (Android 10 and above always returns `true`, so this can only happen on older
     *  Android versions) we call our [showPermissionSection] to have it make the "Permissions Section"
     *  of our UI visible (has a button the user can click to have the system grant us permissions)
     *  and the "Action Section" of the UI [View.GONE] (until the user has granted us the permissions).
     *
     * We set the [View.OnClickListener] of the [FragmentAddMediaBinding.takeVideoButton] button
     * in [binding] to a lambda which gets a [LifecycleOwner] that represents the [Fragment]'s [View]
     * lifecycle and uses the [CoroutineScope] tied to this [LifecycleOwner]'s Lifecycle to launch
     * a new coroutine without blocking the current thread. In that coroutine lambda we branch on
     * whether the [AddMediaViewModel.canWriteInMediaStore] field of [viewModel] is `true`:
     *  - `true`: we call the [AddMediaViewModel.createVideoUri] method of [viewModel] to have it
     *  create a [Uri] where the video taken by the camera will be stored using [Source.CAMERA]
     *  ("camera-") as the prefix for the image. If this is non-`null` we use the [let] extension
     *  function on the [Uri] created to launch our [ActivityResultLauncher] field [actionTakeVideo]
     *  to have [MediaStore] have the camera application capture a video and store it in the content
     *  [Uri].
     *  - `false`: (Android 10 and above always returns `true`, so this can only happen on older
     *  Android versions) we call our [showPermissionSection] to have it make the "Permissions Section"
     *  of our UI visible (has a button the user can click to have the system grant us permissions)
     *  and the "Action Section" of the UI [View.GONE] (until the user has granted us the permissions).
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment.
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the `LayoutParams` of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or `null`.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddMediaBinding.inflate(inflater, container, false)

        // Every time currentMediaUri is changed, we update the ImageView
        viewModel.currentMediaUri.observe(viewLifecycleOwner) { uri: Uri? ->
            Glide.with(this).load(uri).into(binding.mediaThumbnail)
        }

        binding.requestPermissionButton.setOnClickListener {
            actionRequestPermission.launch(arrayOf(READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE))
        }

        binding.takePictureButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {

                if (viewModel.canWriteInMediaStore) {
                    viewModel.createPhotoUri(Source.CAMERA)?.let { uri: Uri ->
                        viewModel.saveTemporarilyPhotoUri(uri)
                        actionTakePicture.launch(uri)
                    }
                } else {
                    showPermissionSection()
                }
            }
        }

        binding.takeVideoButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {

                if (viewModel.canWriteInMediaStore) {
                    viewModel.createVideoUri(Source.CAMERA)?.let { uri: Uri ->
                        actionTakeVideo.launch(uri)
                    }
                } else {
                    showPermissionSection()
                }
            }
        }

        binding.downloadImageFromInternetButton.setOnClickListener {

            if (viewModel.canWriteInMediaStore) {
                binding.downloadImageFromInternetButton.isEnabled = false
                viewModel.saveRandomImageFromInternet {
                    // We re-enable the button once the download is done
                    // Keep in mind the logic is basic as it doesn't handle errors
                    binding.downloadImageFromInternetButton.isEnabled = true
                }
            } else {
                showPermissionSection()
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

    private fun handlePermissionSectionVisibility() {
        if (viewModel.canWriteInMediaStore) {
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

    private val actionRequestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(RequestMultiplePermissions()) {
            handlePermissionSectionVisibility()
        }

    private val actionTakePicture: ActivityResultLauncher<Uri> =
        registerForActivityResult(TakePicture()) { success ->
            if (!success) {
                Log.d(tag, "Image taken FAIL")
                return@registerForActivityResult
            }

            Log.d(tag, "Image taken SUCCESS")

            viewModel.temporaryPhotoUri?.let {
                viewModel.loadCameraMedia(it)
                viewModel.saveTemporarilyPhotoUri(null)
            }
        }

    private val actionTakeVideo = registerForActivityResult(CustomTakeVideo()) { uri ->
        if (uri == null) {
            Log.d(tag, "Video taken FAIL")
            return@registerForActivityResult
        }

        Log.d(tag, "Video taken SUCCESS")
        viewModel.loadCameraMedia(uri)
    }
}
