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
import android.annotation.SuppressLint
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.AndroidViewModel
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.samples.mediastore.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.net.toUri

/**
 * The request code for requesting [Manifest.permission.READ_EXTERNAL_STORAGE] permission.
 */
private const val READ_EXTERNAL_STORAGE_REQUEST = 0x1045
// TODO: In order to target 33 One needs to request fine grained permission )-:

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
            if (result.resultCode == RESULT_OK) {
                viewModel.deletePendingImage()
            }
        }

    /**
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to edge
     * display, then we call our super's implementation of `onCreate`, and initialize our
     * [ActivityMainBinding] field [binding] to the view binding that the
     * [DataBindingUtil.setContentView] method returns when it sets the Activity's content view to
     * the layout file `R.layout.activity_main` and returns the associated binding.
     *
     * We call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for applying
     * window insets to the [ActivityMainBinding.root] root view of [binding] with the `listener`
     * argument a lambda that accepts the [View] passed the lambda in variable `v` and the
     * [WindowInsetsCompat] passed the lambda in variable `windowInsets`. It initializes its
     * [Insets] variable `insets` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument, then it updates the layout parameters
     * of `v` to be a [ViewGroup.MarginLayoutParams] with the left margin set to `insets.left`, the
     * right margin set to `insets.right`, the top margin set to `insets.top`, and the bottom margin
     * set to `insets.bottom`. Finally it returns [WindowInsetsCompat.CONSUMED] to the caller (so
     * that the window insets will not keep passing down to descendant views).
     *
     * We initialize our [GalleryAdapter] variable `val galleryAdapter` to a new instance whose
     * constructor argument is a lambda which calls our [deleteImage] method with the
     * [MediaStoreImage] parameter passed to it (the [ImageViewHolder] used for each image in our
     * [RecyclerView] sets the `OnClickListener` of the [ImageView] it holds to a lambda which
     * retrieves the [MediaStoreImage] stored as the `tag` of its root view and calls this lambda
     * with it). Our [deleteImage] method will show an [AlertDialog] which asks the user if they
     * want to delete the file, and will call the [MainActivityViewModel.deleteImage] method of
     * [viewModel] with the [MediaStoreImage] if the positive button is clicked.
     *
     * Next we use the [also] extension function on the [ActivityMainBinding.gallery] property of
     * [binding] (the [RecyclerView]) to set its `layoutManager` to a new instance of
     * [GridLayoutManager] with 3 columns, and to set its `adapter` to `galleryAdapter`.
     *
     * We add an observer to the [MainActivityViewModel.images] field of [viewModel] whose lambda
     * calls the [ListAdapter.submitList] method of `galleryAdapter` with the [List] of
     * [MediaStoreImage] objects passed it to have it diffed against the current dataset, and then
     * displayed in its associated [RecyclerView].
     *
     * We add an observer to the [MainActivityViewModel.permissionNeededForDelete] field of
     * [viewModel] whose lambda will launch our [ActivityResultLauncher] field
     * [requestPermissionToDelete] with an [IntentSenderRequest] built from the [IntentSender]
     * passed it when it transitions to a non-`null` value. [requestPermissionToDelete] will launch
     * the activity suggested by the [IntentSender] to allow the user to grant permission to delete
     * the image file in question, and its callback will interpret the [ActivityResult] returned
     * from that activity and delete the image file if the activity returns [Activity.RESULT_OK] as
     * the result code.
     *
     * We set the [View.OnClickListener] of the [ActivityMainBinding.openAlbum] button of [binding]
     * to a lambda which calls our [openMediaStore] method and the [View.OnClickListener] of the
     * [ActivityMainBinding.grantPermissionButton] button of [binding] to a lambda which calls our
     * [openMediaStore] method.
     *
     * If our [haveStoragePermission] method returns `false`
     * ([Manifest.permission.READ_EXTERNAL_STORAGE] permission has not been granted to our app) we
     * set the visibility of the `LinearView` property [ActivityMainBinding.welcomeView] of
     * [binding] to [View.VISIBLE] (it contains the button for asking for permission and an
     * explanation of why it is necessary), otherwise we call our [showImages] method to have it
     * retrieve the images using the `MediaStore` API.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v: View, windowInsets: WindowInsetsCompat ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        val galleryAdapter = GalleryAdapter { image: MediaStoreImage ->
            deleteImage(image)
        }

        binding.gallery.also { view: RecyclerView ->
            view.layoutManager = GridLayoutManager(this, 3)
            view.adapter = galleryAdapter
        }

        viewModel.images.observe(this) { images: List<MediaStoreImage> ->
            galleryAdapter.submitList(images)
        }

        viewModel.permissionNeededForDelete.observe(this) { intentSender: IntentSender? ->
            intentSender?.let {
                // On Android 10+, if the app doesn't have permission to modify
                // or delete an item, it returns an `IntentSender` that we can
                // use here to prompt the user to grant permission to delete (or modify)
                // the image.
                requestPermissionToDelete.launch(
                    IntentSenderRequest.Builder(intentSender).build()
                )
            }
        }

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

    /**
     * Queries the `MediaStore` API for all of the image files on the users device and causes them
     * to be displayed in our [RecyclerView]. First we call the [MainActivityViewModel.loadImages]
     * method of our field [viewModel] to have it query a [ContentResolver] for the
     * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] content URI and to use the [Cursor] returned
     * to build a [List] of [MediaStoreImage] objects with the information necessary to access the
     * image files. It posts this [List] to its private field `_images` and an observer added to its
     * public accessor field [MainActivityViewModel.images] of [viewModel] in our [onCreate] override
     * will submit the [List] to the [GalleryAdapter] feeding views to our [RecyclerView] to have it
     * diffed against the current dataset, and then displayed in the [RecyclerView].
     *
     * Next we set the `visibility` of the [ActivityMainBinding.welcomeView] `LinearLayout` to
     * [View.GONE] (it contains an `ImageView` and a `MaterialButton` with the label "Open Album"
     * which when clicked calls our [openMediaStore] method which either shows the images if we
     * already have permission to access them or calls our [requestPermission] method to have the
     * system ask the user to grant us permission to access them if do not have permission, and is
     * the view that the user sees when the app first starts up). Finally we set the `visibility` of
     * the [ActivityMainBinding.permissionRationaleView] `LinearLayout` to [View.GONE] (it contains a
     * `TextView` explaining why we need the permission and a `MaterialButton` with the label "Grant
     * Permission" that when clicked calls our [openMediaStore] method which either shows the images
     * if we already have permission to access them or calls our [requestPermission] method to have
     * the system ask the user to grant us permission to access them if do not have permission, and
     * is the view that the user sees if a previous call to [requestPermission] resulted in their
     * denying us permission).
     */
    private fun showImages() {
        viewModel.loadImages()
        binding.welcomeView.visibility = View.GONE
        binding.permissionRationaleView.visibility = View.GONE
    }

    /**
     * Convenience method change the visibility of the [ActivityMainBinding.welcomeView]
     * `LinearLayout` of [binding] to [View.GONE] and the visibility of the
     * [ActivityMainBinding.welcomeView] `LinearLayout` of [binding] to [View.VISIBLE].
     * This method is called from our [onRequestPermissionsResult] method when the user
     * denies us access to storage without checking the "Do not ask again" checkbox, and
     * switches the view from our "Welcome" view to a view which explains why we need to
     * have the permission in order to run.
     */
    private fun showNoAccess() {
        binding.welcomeView.visibility = View.GONE
        binding.permissionRationaleView.visibility = View.VISIBLE
    }

    /**
     * This method is called from the [View.OnClickListener] of the two buttons in our [binding]
     * field [ActivityMainBinding.openAlbum] and [ActivityMainBinding.grantPermissionButton].
     * We branch on the [Boolean] value returned from our [haveStoragePermission] method:
     *  - `true` (we already have the [Manifest.permission.READ_EXTERNAL_STORAGE] permission)
     *  we just call our [showImages] method to display all of the images that the `MediaStore` API
     *  provides us in our [RecyclerView].
     *  - `false` (we have not been granted the [Manifest.permission.READ_EXTERNAL_STORAGE] permission)
     *  we call our [requestPermission] method to have it have the system ask the user if they want to
     *  grant us the [Manifest.permission.READ_EXTERNAL_STORAGE] permission.
     */
    private fun openMediaStore() {
        if (haveStoragePermission()) {
            showImages()
        } else {
            requestPermission()
        }
    }

    /**
     * Creates an [Intent] to launch the settings app to show a screen of details about our
     * application, thereby allowing the user to either change their mind about denying us the
     * [Manifest.permission.READ_EXTERNAL_STORAGE] permission, or allowing them to uninstall our
     * app.
     *
     * We construct a new instance of [Intent] with the action [ACTION_APPLICATION_DETAILS_SETTINGS]
     * and the data [Uri] constructed from the string "package:" with the name of this application's
     * package appended to it. We use the [apply] extension to add the category [Intent.CATEGORY_DEFAULT]
     * to the [Intent] (only activities that provide this category will be considered), and the flags
     * [Intent.FLAG_ACTIVITY_NEW_TASK] (this activity will become the start of a new task on this
     * history stack). We then use the [also] extension function on the [Intent] to call the
     * [startActivity] method to launch the activity specified by the [Intent].
     *
     * This method is called from our [onRequestPermissionsResult] method when the user denies
     * us the [Manifest.permission.READ_EXTERNAL_STORAGE] permission.
     */
    private fun goToSettings() {
        Intent(
            ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:$packageName".toUri()
        ).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent: Intent ->
            startActivity(intent)
        }
    }

    /**
     * Convenience method to check if [Manifest.permission.READ_EXTERNAL_STORAGE] permission
     * has been granted to the app. We just return the [Boolean] result of comparing the value
     * returned by the [ContextCompat.checkSelfPermission] method with [PERMISSION_GRANTED] for
     * equality.
     */
    private fun haveStoragePermission() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PERMISSION_GRANTED

    /**
     * Convenience method to request [Manifest.permission.READ_EXTERNAL_STORAGE] permission. If our
     * [haveStoragePermission] method returns `false` (we do not have the permission) we initialize
     * our [Array] of [String] variable `val permissions` to a new instance containing the strings
     * [Manifest.permission.READ_EXTERNAL_STORAGE] and [Manifest.permission.WRITE_EXTERNAL_STORAGE].
     * Then we call the [ActivityCompat.requestPermissions] method with `permissions` as the
     * permissions we need, and our constant [READ_EXTERNAL_STORAGE_REQUEST] as the request code to
     * return to our [onRequestPermissionsResult] override (so it knows that the results are for
     * this request).
     */
    @SuppressLint("InlinedApi")
    private fun requestPermission() {
        if (!haveStoragePermission()) {
            val permissions: Array<String> = arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
            ActivityCompat.requestPermissions(this, permissions, READ_EXTERNAL_STORAGE_REQUEST)
        }
    }

    /**
     * Deletes the image file associated with our [MediaStoreImage] parameter image from the file
     * system. We build and show an [AlertDialog] whose title is "Delete?", whose message is the
     * string formed by appending the [MediaStoreImage.displayName] field (the file name) to the
     * string "Delete ", whose positive [Button] is labeled "Delete" and calls the
     * [MainActivityViewModel.deleteImage] method of our [viewModel] field with [image] when clicked,
     * and whose negative [Button] is labeled "Cancel" and calls the [DialogInterface.dismiss] method
     * of our `dialog` when clicked.
     *
     * @param image the [MediaStoreImage] for the image file we are to delete from the file system.
     */
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
     *
     * @param onClick a lambda that each image displayed in our [RecyclerView] will call with the
     * [MediaStoreImage] it is associated with when the image is clicked.
     */
    private class GalleryAdapter(val onClick: (MediaStoreImage) -> Unit) :
        ListAdapter<MediaStoreImage, ImageViewHolder>(MediaStoreImage.DiffCallback) {

        /**
         * Called when [RecyclerView] needs a new [ImageViewHolder] of the given type to represent
         * an item. We initialize our [LayoutInflater] variable `val layoutInflater` to the
         * [LayoutInflater] from the context of our [ViewGroup] parameter [parent]. We initialize
         * our [View] variable `val view` by having `layoutInflater` inflate the layout file with
         * resource ID `R.layout.gallery_layout` using [parent] for its layout params without
         * attaching to it (the layout file consists of a `ConstraintLayout` root view containing
         * an [ImageView]). Finally we return a new instance of [ImageViewHolder] constructed to use
         * `view` as its root view and [GalleryAdapter.onClick] as the lambda that the
         * [View.OnClickListener] of the [ImageView] in `view` will call with its associated
         * [MediaStoreImage].
         *
         * @param parent The [ViewGroup] into which the new [View] will be added after it is bound
         * to an adapter position.
         * @param viewType The view type of the new View.
         * @return A new [ImageViewHolder] that holds a [View] of the given view type.
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val view = layoutInflater.inflate(R.layout.gallery_layout, parent, false)
            return ImageViewHolder(view, onClick)
        }

        /**
         * Called by [RecyclerView] to display the data at the specified position. This method should
         * update the contents of the [ImageViewHolder.itemView] to reflect the item at the given
         * position. We initialize our [MediaStoreImage] variable `val mediaStoreImage` to the item
         * at position [position] in our dataset, then set the `tag` of the [ImageViewHolder.rootView]
         * view in our [holder] parameter to `mediaStoreImage`. Finally we begin a load with [Glide]
         * that will be tied to the lifecycle of the containing activity of the
         * [ImageViewHolder.imageView] in the parameter [holder], chain a call to `load` the [Uri]
         * in the [MediaStoreImage.contentUri] field of `mediaStoreImage` (content [Uri] for the
         * associated image) to the [RequestManager] returned by [Glide.with], chain a call to
         * `thumbnail` with a multiplier of 0.33f (multiplies the target by 0.33f when loading a
         * thumbnail version of the image), chain a call to `centerCrop` (Applies `CenterCrop` to
         * scale the image so that either the width of the image matches the given width and the
         * height of the image is greater than the given height or vice versa, and then crop the
         * larger dimension to match the given dimension), and finally chain a call to `into` to
         * set the [ImageView] the resource will be loaded into to the [ImageViewHolder.imageView]
         * field of [holder].
         *
         * @param holder The [ImageViewHolder] which should be updated to represent the contents of
         * the item at the given position in the data set.
         * @param position The position of the item within the adapter's data set.
         */
        override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
            val mediaStoreImage: MediaStoreImage = getItem(position)
            holder.rootView.tag = mediaStoreImage

            @Suppress("DEPRECATION") // TODO: replace .thumbnail with thumbnail(RequestBuilder)
            Glide.with(holder.imageView)
                .load(mediaStoreImage.contentUri)
                .thumbnail(0.33f)
                .centerCrop()
                .into(holder.imageView)
        }
    }
}

/**
 * Basic [RecyclerView.ViewHolder] for our gallery. Our `init` block sets the [View.OnClickListener]
 * of the [ImageView] in our `view` parameter to a lambda which calls our `onClick` lambda parameter
 * with the [MediaStoreImage] that the `onBindViewHolder` override of `GalleryAdapter` stores in the
 * tag of our [rootView] field when this [ImageViewHolder] is bound to its [MediaStoreImage]. Our
 * constructor saves our `view` parameter in our [rootView] field and initializes our [ImageView]
 * field [imageView] by finding the view in `view` with ID `R.id.image`.
 *
 * @param view the view to use as our root [View].
 * @param onClick the lambda that the [View.OnClickListener] of the [ImageView] in our root view
 * should call with its associated [MediaStoreImage] when the [ImageView] is clicked.
 */
private class ImageViewHolder(
    view: View,
    onClick: (MediaStoreImage) -> Unit
) : RecyclerView.ViewHolder(view) {
    /**
     * The [View] we use as our [ImageViewHolder.itemView].
     */
    val rootView = view

    /**
     * The [ImageView] in our [rootView] field that we use to display our image.
     */
    val imageView: ImageView = view.findViewById(R.id.image)

    init {
        imageView.setOnClickListener {
            val image = rootView.tag as? MediaStoreImage ?: return@setOnClickListener
            onClick(image)
        }
    }
}
