/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.samples.mediastore

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.AndroidViewModel
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.samples.mediastore.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * The request code for requesting [Manifest.permission.READ_EXTERNAL_STORAGE] permission.
 */
private const val READ_EXTERNAL_STORAGE_REQUEST = 0x1045

/**
 * MainActivity for the sample that displays a gallery of images retrieved using the `MediaStore`
 * API in a [RecyclerView] whose layout manager is a [GridLayoutManager].
 */
class MainActivity : AppCompatActivity() {

    /**
     * The [AndroidViewModel] subclass we use as our view model.
     */
    private val viewModel: MainActivityViewModel by viewModels()
    /**
     * The [ActivityMainBinding] view binding that is inflated, bound, and set as our content view
     * and which is generated from the layout file layout/activity_main.xml which consists of a
     * `layout` root element (apparently data binding was considered at one time but not used) which
     * holds a `ConstraintLayout` with either a [RecyclerView] to display our images in or when we
     * do not yet have permission to access the images a `LinearLayout` holding an `ImageView`
     * above a `TextView` explaining that we require access to storage, with a `MaterialButton`
     * at the bottom which the user can click to go through the process of granting us permission.
     */
    private lateinit var binding: ActivityMainBinding

    /**
     * This [ActivityResultLauncher] is launched by an observer when our [viewModel]'s field
     * [MainActivityViewModel.permissionNeededForDelete] changes to a non-`null` value. It's
     * parameter is the [IntentSenderRequest] that is derived from the [RecoverableSecurityException]
     * caught by the `performDeleteImage` method of [viewModel] when we do not have permission and
     * launching this [ActivityResultLauncher] will launch an activity for its result that will allow
     * the user to grant us permission. On return from that activity the lambda we register for the
     * result will execute with the [ActivityResult] object returned, and if the `resultCode` of
     * that [ActivityResult] is [Activity.RESULT_OK] it calls the [MainActivityViewModel.deletePendingImage]
     * method of [viewModel] do try to delete that image again.
     */
    private val requestPermissionToDelete: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.deletePendingImage()
            }
        }

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we initialize our [ActivityMainBinding] field [binding] to the view binding that the
     * [DataBindingUtil.setContentView] method returns when it sets the Activity's content view to
     * the layout file [R.layout.activity_main] and returns the associated binding. We initialize our
     * [GalleryAdapter] variable `val galleryAdapter` to a new instance whose constructor argument is
     * a lambda which calls our [deleteImage] method with the [MediaStoreImage] parameter passed to
     * it (the [ImageViewHolder] used for each image in our [RecyclerView] sets the `OnClickListener`
     * of the [ImageView] it holds to a lambda which retrieves the [MediaStoreImage] stored as the
     * `tag` of its root view and calls this lambda with it). Our [deleteImage] method will show an
     * [AlertDialog] which asks the user if they want to delete the file, and will call the
     * [MainActivityViewModel.deleteImage] method of [viewModel] with the [MediaStoreImage] if the
     * positive button is clicked.
     *
     * Next we use the [also] extension function on the [ActivityMainBinding.gallery] property of
     * [binding] (the [RecyclerView]) to set its `layoutManager` to a new instance of [GridLayoutManager]
     * with 3 columns, and to set its `adapter` to `galleryAdapter`.
     *
     * We add an observer to the [MainActivityViewModel.images] field of [viewModel] whose lambda
     * calls the [ListAdapter.submitList] method of `galleryAdapter` with the [List] of [MediaStoreImage]
     * objects passed it to have it diffed against the current dataset, and then displayed in its
     * associated [RecyclerView].
     *
     * We add an observer to the [MainActivityViewModel.permissionNeededForDelete] field of [viewModel]
     * whose lambda will launch our [ActivityResultLauncher] field [requestPermissionToDelete] with
     * an [IntentSenderRequest] built from the [IntentSender] passed it when it transitions to a
     * non-`null` value. [requestPermissionToDelete] will launch the activity suggested by the
     * [IntentSender] to allow the user to grant permission to delete the image file in question,
     * and its callback will interpret the [ActivityResult] returned from that activity and delete
     * the image file if the activity returns [Activity.RESULT_OK] as the result code.
     *
     * We set the [View.OnClickListener] of the [ActivityMainBinding.openAlbum] button of [binding]
     * to a lambda which calls our [openMediaStore] method and the [View.OnClickListener] of the
     * [ActivityMainBinding.grantPermissionButton] button of [binding] to a lambda which calls our
     * [openMediaStore] method.
     *
     * If our [haveStoragePermission] method returns `false` ([Manifest.permission.READ_EXTERNAL_STORAGE]
     * permission has not been granted to our app) we set the visibility of the `LinearView` property
     * [ActivityMainBinding.welcomeView] of [binding] to [View.VISIBLE] (it contains the button for
     * asking for permission and an explanation of why it is necessary), otherwise we call our
     * [showImages] method to have it retrieve the images using the `MediaStore` API.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val galleryAdapter = GalleryAdapter { image: MediaStoreImage ->
            deleteImage(image)
        }

        binding.gallery.also { view: RecyclerView ->
            view.layoutManager = GridLayoutManager(this, 3)
            view.adapter = galleryAdapter
        }

        viewModel.images.observe(this, { images: List<MediaStoreImage> ->
            galleryAdapter.submitList(images)
        })

        viewModel.permissionNeededForDelete.observe(this, { intentSender: IntentSender? ->
            intentSender?.let {
                // On Android 10+, if the app doesn't have permission to modify
                // or delete an item, it returns an `IntentSender` that we can
                // use here to prompt the user to grant permission to delete (or modify)
                // the image.
                requestPermissionToDelete.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
        })

        binding.openAlbum.setOnClickListener { openMediaStore() }
        binding.grantPermissionButton.setOnClickListener { openMediaStore() }

        if (!haveStoragePermission()) {
            binding.welcomeView.visibility = View.VISIBLE
        } else {
            showImages()
        }
    }

    /**
     * Callback for the result from requesting permissions. This method is invoked for every call
     * on [ActivityCompat.requestPermissions]. [ActivityCompat.requestPermissions] is called by our
     * [requestPermission] method to request the permissions [Manifest.permission.READ_EXTERNAL_STORAGE],
     * and [Manifest.permission.WRITE_EXTERNAL_STORAGE]. [requestPermission] is called from our
     * [openMediaStore] method if [haveStoragePermission] determines we do not have these permissions.
     *
     * First we call our super's implementation of `onRequestPermissionsResult`. Then if our
     * [requestCode] parameter is the constant [READ_EXTERNAL_STORAGE_REQUEST] that is passed to
     * [ActivityCompat.requestPermissions] by our [requestPermission] method we check that our
     * [grantResults] parameter is not empty and the 0 index entry of [grantResults] is equal to
     * [PERMISSION_GRANTED] and if so we call our [showImages] method to have it retrieve the images
     * using the `MediaStore` API and display them in our [RecyclerView].
     *
     * If our [grantResults] parameter is empty or the 0 index entry of [grantResults] is not equal
     * to [PERMISSION_GRANTED] the user has denied us access so we:
     *  - Initialize our [Boolean] variable `val showRationale` to the value returned by the method
     *  [ActivityCompat.shouldShowRequestPermissionRationale] for the permission
     *  [Manifest.permission.READ_EXTERNAL_STORAGE].
     *  - If `showRationale` is `true` the user has denied us the permission but has not checked the
     *  "Do not ask again" box so we call our [showNoAccess] method to display a rationale for why
     *  we need the permission along with a button that allows us to ask again.
     *  - If `showRationale` is `false` the user has checked the "Do not ask again" box so we have
     *  to call our [goToSettings] method to launch the settings activity which will allow him
     *  either to change his mind about allowing that permission or to remove our app.
     *
     * @param requestCode The request code passed to [ActivityCompat.requestPermissions]
     * @param permissions The requested permissions. Never `null`.
     * @param grantResults The grant results for each of the corresponding permissions which are
     * either [PERMISSION_GRANTED] or [PERMISSION_DENIED]. Never `null`.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            READ_EXTERNAL_STORAGE_REQUEST -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                    showImages()
                } else {
                    // If we weren't granted the permission, check to see if we should show
                    // rationale for the permission.
                    val showRationale: Boolean =
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )

                    /**
                     * If we should show the rationale for requesting storage permission, then
                     * we'll show [ActivityMainBinding.permissionRationaleView] which does this.
                     *
                     * If `showRationale` is false, this means the user has not only denied
                     * the permission, but they've clicked "Don't ask again". In this case
                     * we send the user to the settings page for the app so they can grant
                     * the permission (Yay!) or uninstall the app.
                     */
                    if (showRationale) {
                        showNoAccess()
                    } else {
                        goToSettings()
                    }
                }
                return
            }
        }
    }

    private fun showImages() {
        viewModel.loadImages()
        binding.welcomeView.visibility = View.GONE
        binding.permissionRationaleView.visibility = View.GONE
    }

    private fun showNoAccess() {
        binding.welcomeView.visibility = View.GONE
        binding.permissionRationaleView.visibility = View.VISIBLE
    }

    private fun openMediaStore() {
        if (haveStoragePermission()) {
            showImages()
        } else {
            requestPermission()
        }
    }

    private fun goToSettings() {
        Intent(
            ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            startActivity(intent)
        }
    }

    /**
     * Convenience method to check if [Manifest.permission.READ_EXTERNAL_STORAGE] permission
     * has been granted to the app.
     */
    private fun haveStoragePermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PERMISSION_GRANTED

    /**
     * Convenience method to request [Manifest.permission.READ_EXTERNAL_STORAGE] permission.
     */
    private fun requestPermission() {
        if (!haveStoragePermission()) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(this, permissions, READ_EXTERNAL_STORAGE_REQUEST)
        }
    }

    private fun deleteImage(image: MediaStoreImage) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_dialog_title)
            .setMessage(getString(R.string.delete_dialog_message, image.displayName))
            .setPositiveButton(R.string.delete_dialog_positive) { _: DialogInterface, _: Int ->
                viewModel.deleteImage(image)
            }
            .setNegativeButton(R.string.delete_dialog_negative) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * A [ListAdapter] for [MediaStoreImage]s.
     */
    private inner class GalleryAdapter(val onClick: (MediaStoreImage) -> Unit) :
        ListAdapter<MediaStoreImage, ImageViewHolder>(MediaStoreImage.DiffCallback) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.gallery_layout, parent, false)
            return ImageViewHolder(view, onClick)
        }

        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val mediaStoreImage = getItem(position)
            holder.rootView.tag = mediaStoreImage

            Glide.with(holder.imageView)
                .load(mediaStoreImage.contentUri)
                .thumbnail(0.33f)
                .centerCrop()
                .into(holder.imageView)
        }
    }
}

/**
 * Basic [RecyclerView.ViewHolder] for our gallery.
 */
private class ImageViewHolder(view: View, onClick: (MediaStoreImage) -> Unit) :
    RecyclerView.ViewHolder(view) {
    val rootView = view
    val imageView: ImageView = view.findViewById(R.id.image)

    init {
        imageView.setOnClickListener {
            val image = rootView.tag as? MediaStoreImage ?: return@setOnClickListener
            onClick(image)
        }
    }
}

