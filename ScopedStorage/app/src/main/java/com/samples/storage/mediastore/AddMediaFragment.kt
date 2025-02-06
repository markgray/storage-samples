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
     * `R.layout.fragment_add_media`). It consists of a `ConstraintLayout` root view holding a
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
     * whether the [AddMediaViewModel.canWriteInMediaStore] property of [viewModel] is `true`:
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
     * whether the [AddMediaViewModel.canWriteInMediaStore] property of [viewModel] is `true`:
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
     * We set the [View.OnClickListener] of the [FragmentAddMediaBinding.downloadImageFromInternetButton]
     * button in [binding] to a lambda which branches on whether the [AddMediaViewModel.canWriteInMediaStore]
     * property of [viewModel] is `true`:
     *  - `true`: we disable the [FragmentAddMediaBinding.downloadImageFromInternetButton] button of
     *  [binding] then call the [AddMediaViewModel.saveRandomImageFromInternet] method to have it
     *  download a random image and save the image in shared storage. The lambda passed to that
     *  method will re-enable the [FragmentAddMediaBinding.downloadImageFromInternetButton] button of
     *  [binding] when the download completes (even if an error occurs).
     *  - `false`: (Android 10 and above always returns `true`, so this can only happen on older
     *  Android versions) we call our [showPermissionSection] to have it make the "Permissions Section"
     *  of our UI visible (has a button the user can click to have the system grant us permissions)
     *  and the "Action Section" of the UI [View.GONE] (until the user has granted us the permissions).
     *
     * Finally we return the outermost [View] in the associated layout file of [binding] to serve as
     * our UI.
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

    /**
     * Called when the view previously created by [onCreateView] has been detached from the fragment.
     * The next time the fragment needs to be displayed, a new view will be created. This is called
     * after [onStop] and before [onDestroy]. Internally it is called after the view's state has been
     * saved but before it has been removed from its parent. First we call our super's implementation
     * of `onDestroy`, then we set our [FragmentAddMediaBinding] field [_binding] to `null`.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Called when the fragment is visible to the user and actively running. First we call our super's
     * implementation of `onResume`, then we call our [handlePermissionSectionVisibility] to have it
     * set the visibility of the "Permissions Section" and "Actions Section" of our UI appropriately.
     */
    override fun onResume() {
        super.onResume()

        handlePermissionSectionVisibility()
    }

    /**
     * If the [AddMediaViewModel.canWriteInMediaStore] property of [viewModel] is `true` (indicating
     * that we have permission to write to shared storage we call our [hidePermissionSection] method
     * to have it set the visibility of the "Permissions Section" of our UI to [View.GONE] and the
     * visibility of the "Actions Section" of our UI to [View.VISIBLE]. If the field is `false` we
     * call our [showPermissionSection] method to have it set the visibility of the "Permissions
     * Section" of our UI to [View.VISIBLE] and the visibility of the "Actions Section" of our UI
     * to [View.GONE].
     */
    private fun handlePermissionSectionVisibility() {
        if (viewModel.canWriteInMediaStore) {
            hidePermissionSection()
        } else {
            showPermissionSection()
        }
    }

    /**
     * Sets the visibility of the "Permissions Section" of our UI to [View.GONE] and the
     * visibility of the "Actions Section" of our UI to [View.VISIBLE].
     */
    private fun hidePermissionSection() {
        binding.permissionSection.visibility = View.GONE
        binding.actions.visibility = View.VISIBLE
    }

    /**
     * Sets the visibility of the "Permissions Section" of our UI to [View.VISIBLE] and the visibility
     * of the "Actions Section" of our UI to [View.GONE].
     */
    private fun showPermissionSection() {
        binding.permissionSection.visibility = View.VISIBLE
        binding.actions.visibility = View.GONE
    }

    /**
     * This is the [ActivityResultLauncher] that we launch to have the system ask the user to grant
     * us the permissions we need to write to shared storage. The lambda which is executed with the
     * result returned from that activity calls our [handlePermissionSectionVisibility] method to
     * have it set the visibility of the "Permissions Section" of our UI and the "Actions Section"
     * of our UI to [View.GONE] and [View.VISIBLE] (or vice versa depending on the possibly updated
     * value of the [AddMediaViewModel.canWriteInMediaStore] property of [viewModel]).
     */
    private val actionRequestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(RequestMultiplePermissions()) {
            handlePermissionSectionVisibility()
        }

    /**
     * This is the [ActivityResultLauncher] that we launch to take a picture and save it into the
     * provided content [Uri]. The lambda that is executed when the `true` or `false` result is
     * returned will log the message "Image taken FAIL" if the result is `false` and return. If the
     * result is `true` (the image was saved into the given [Uri]) it logs the message "Image taken
     * SUCCESS", then retrieves the [AddMediaViewModel.temporaryPhotoUri] property from the
     * [SavedStateHandle] of [viewModel] and if it is not `null` uses the [let] extension function
     * on its [Uri] value to have the [AddMediaViewModel.loadCameraMedia] method of [viewModel]
     * save the [Uri] in its [SavedStateHandle] under the key "currentMediaUri" (an observer added
     * to that entry will display the [Uri] in the [ImageView] of the UI), and calls the
     * [AddMediaViewModel.saveTemporarilyPhotoUri] method of [viewModel] to set the value stored
     * under the key "temporaryPhotoUri" in its [SavedStateHandle] to `null`.
     */
    private val actionTakePicture: ActivityResultLauncher<Uri> =
        registerForActivityResult(TakePicture()) { success: Boolean ->
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

    /**
     * This is the [ActivityResultLauncher] that we launch to have the camera application capture a
     * video and save it into the provided content [Uri]. The lambda that is executed when we return
     * from the camera application receives back the same content [Uri] if the activity succeeded or
     * `null` if it failed. If the [Uri] returned is `null` we log the message "Video taken FAIL"
     * and return, otherwise we log the message "Video taken SUCCESS" and call the method
     * [AddMediaViewModel.loadCameraMedia] of [viewModel] with the [Uri] to have it save it in its
     * [SavedStateHandle] under the key "currentMediaUri" (an observer added to that entry will
     * display the [Uri] in the [ImageView] of the UI).
     */
    private val actionTakeVideo: ActivityResultLauncher<Uri> =
        registerForActivityResult(CustomTakeVideo()) { uri: Uri? ->
            if (uri == null) {
                Log.d(tag, "Video taken FAIL")
                return@registerForActivityResult
            }

            Log.d(tag, "Video taken SUCCESS")
            viewModel.loadCameraMedia(uri)
        }
}
