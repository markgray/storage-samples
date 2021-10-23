/*
* Copyright (C) 2012 The Android Open Source Project
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
package com.example.android.storageclient

import android.app.Activity
import android.app.Dialog
import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.view.MenuItem
import android.view.Window
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.android.common.logger.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.IOException
import kotlin.coroutines.CoroutineContext

/**
 * [Fragment] which has the user choose an image file using the system's file browser and displays
 * that image in a [Dialog]. This action is triggered when the user clicks the options menu [MenuItem]
 * in the UI of [MainActivity] by our [onOptionsItemSelected] override, and this [Fragment] has no
 * layout file of its own (unless you count the layout file of the [Dialog]).
 */
class StorageClientFragment : Fragment() {
    /**
     * Called to do initial creation of a fragment. This is called after [onAttach] and before
     * [onCreateView]. We call our super's implementation of `onCreate` then call the method
     * [setHasOptionsMenu] with `true` to report that this fragment would like to participate in
     * populating the options menu by receiving a call to [onCreateOptionsMenu] and related methods.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    /**
     * This hook is called whenever an item in your options menu is selected. If the `itemId` property
     * (aka [MenuItem.getItemId] return value) of [item] is [R.id.sample_action] (the item with the
     * title "Show Me The Image") then we call our [performFileSearch] method to have it fire an
     * intent to spin up the "file chooser" activity and have the user select an image. In any case
     * we return `true` to the caller to consume the event here.
     *
     * @param item The [MenuItem] that was selected.
     * @return boolean Return `false` to allow normal menu processing to proceed, `true` to consume
     * it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sample_action) {
            performFileSearch()
        }
        return true
    }

    /**
     * The [ActivityResultLauncher] launcher used to start the activity specified by the [Intent]
     * passed to its [ActivityResultLauncher.launch] method for the result returned by that activity,
     * with the [ActivityResult] result returned by that activity handed to the lambda argument of
     * [registerForActivityResult] for use by our activity. Our lamda callback argument checks to
     * make sure that the `resultCode` of the [ActivityResult] `result` is [Activity.RESULT_OK] and
     * if so initializes its [Intent] variable `val data` to the [Intent] in the the `data` property
     * of `result`. It then accesses the [Uri] stored in the `data` property of that `data` [Intent]
     * and uses the [also] extension function of that [Uri] to log the [String] value of the [Uri]
     * and call our [showImage] method
     */
    private val resultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                data?.data.also { uri: Uri? ->
                    Log.i(TAG, "Uri: " + uri.toString())
                    showImage(uri)
                }
            }
        }

    /**
     * Creates an [Intent] to spin up the "file chooser" activity to have the user select an image
     * for us to display and launches our [ActivityResultLauncher] field [resultLauncher] with it.
     * We initialize our [Intent] to a new instance whose action is [Intent.ACTION_OPEN_DOCUMENT]
     * (allows the user to select and return one or more existing documents), add the category
     * [Intent.CATEGORY_OPENABLE] (used to indicate that an intent only wants URIs that can be opened
     * with [ContentResolver.openFileDescriptor]), and set the MIME type of `intent` to any type of
     * "image". Then we call the [ActivityResultLauncher.launch] method of [resultLauncher] with
     * `intent` as its [Intent] argument to have it fire the [Intent] and deal with the [ActivityResult]
     * that the file chooser activity returns.
     */
    private fun performFileSearch() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file browser.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)

        // Filter to only show results that can be "opened", such as a file (as opposed to a list
        // of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers, it would be
        // "*/*".
        intent.type = "image/*"
        resultLauncher.launch(intent)
    }

    /**
     * Given the URI of an image, shows it on the screen using an [ImageDialogFragment]. If our [Uri]
     * parameter [uri] is not `null` we initialize our [FragmentManager] variable `val fm` to the
     * [FragmentManager] for interacting with fragments associated with this activity, initialize
     * our [ImageDialogFragment] variable `val imageDialog` to a new instance, and initialize our
     * [Bundle] variable `val fragmentArguments` to a new instance. We insert [uri] in the
     * `fragmentArguments` [Bundle] under the key "URI", set the `arguments` property of `imageDialog`
     * to `fragmentArguments` then call the [ImageDialogFragment.show] method to display the dialog
     * using `fm` as the [FragmentManager] this fragment will be added to and the [String] "image_dialog"
     * as the tag for this fragment.
     *
     * @param uri the [Uri] of the image to display.
     */
    private fun showImage(uri: Uri?) {
        if (uri != null) {
            // Since the URI is to an image, create and show a ImageDialogFragment to display the
            // image to the user.
            val fm: FragmentManager = requireActivity().supportFragmentManager
            val imageDialog = ImageDialogFragment()
            val fragmentArguments = Bundle()
            fragmentArguments.putParcelable("URI", uri)
            imageDialog.arguments = fragmentArguments
            imageDialog.show(fm, "image_dialog")
        }
    }

    /**
     * [DialogFragment] which displays an image, given a content URI.
     */
    class ImageDialogFragment : DialogFragment() {
        /**
         * The [Dialog] that holds the [ImageView] that we use to display our image.
         */
        private var mDialog: Dialog? = null

        /**
         * The content [Uri] for the image we are to fetch and display in our [Dialog] field [mDialog]
         */
        private var mUri: Uri? = null

        /**
         * Called to do initial creation of a [DialogFragment]. This is called after [onAttach] and
         * before [onCreateView]. First we call our super's implementation of `onCreate`, then we
         * initialize our [Uri] field [mUri] by retrieving the arguments [Bundle] supplied when our
         * fragment was instantiated using the [requireArguments] method (which throws the exception
         * [IllegalStateException] if no arguments were supplied) and using the [Bundle.getParcelable]
         * method on that [Bundle] to retrieve the [Uri] stored under the key "URI" in it.
         *
         * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
         */
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            mUri = requireArguments().getParcelable("URI")
        }

        /**
         * Create a [Bitmap] from the content [Uri] parameter [uri] for that image and return it. We
         * initialize our [ParcelFileDescriptor] variable `var parcelFileDescriptor` to `null` then
         * we return the [Bitmap] returned by a `try` block intended to catch and log any exception
         * (the `catch` block returns `null` as the result). In the `try` block we set our variable
         * `parcelFileDescriptor` to the [ParcelFileDescriptor] returned by a [ContentResolver]
         * instance for our application's package when we call its [ContentResolver.openFileDescriptor]
         * method to open a raw file descriptor to read data from a our [Uri] parameter [uri]. We
         * initialize our [Bitmap] variable `val image` to the [Bitmap] decoded by the method
         * [BitmapFactory.decodeFileDescriptor] from the file descriptor `fileDescriptor`. We then
         * close `parcelFileDescriptor` and return `image` as the result of the `try` block. In the
         * `finally` block of the `try` we `try` to close `parcelFileDescriptor` and if that throws
         * an [IOException] we print the [IOException] and its backtrace to the standard error stream
         * and log the error.
         *
         * @param uri the [Uri] for the image to return.
         */
        private fun getBitmapFromUri(uri: Uri): Bitmap? {
            var parcelFileDescriptor: ParcelFileDescriptor? = null
            return try {
                parcelFileDescriptor = requireActivity().contentResolver.openFileDescriptor(uri, "r")
                val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
                val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                parcelFileDescriptor.close()
                image
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image.", e)
                null
            } finally {
                try {
                    parcelFileDescriptor?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e(TAG, "Error closing ParcelFile Descriptor")
                }
            }
        }

        /**
         * Override to build your own custom [Dialog] container. This method will be called after
         * [onCreate] and immediately before [onCreateView].
         *
         * First we set our [Dialog] field [mDialog] to the instance create by our super's
         * implementation of [onCreateDialog]. Then we retrieve the current [Window] of [mDialog]
         * and request the feature [Window.FEATURE_NO_TITLE] (the "no title" feature, turning off
         * the title at the top of the screen). WE initialize our [ImageView] variable `val imageView`
         * to a new instance and set it to be the content view of [mDialog].
         *
         * Next we use the [CoroutineScope.getBitmapAndDisplay] extension function on the
         * [CoroutineScope] tied to this [LifecycleOwner]'s Lifecycle supplying a lambda for
         * its `doInBackground` argument that takes a [Uri], calls our [dumpImageMetaData] method
         * on it and returns the [Bitmap] that our [getBitmapFromUri] method creates when it
         * fetches and decodes the image from the [Uri]. The `doInBackground` lambda is exectuted
         * on the [CoroutineContext] of [Dispatchers.IO] and the [Bitmap] it returns is then used
         * as the argument to the `onPostExecute` lambda which is run on the UI thread and the
         * `onPostExecute` lambda we use will call the [ImageView.setImageBitmap] method of
         * `imageView` with that [Bitmap] to set it as the content of the [ImageView]. For the
         * [Uri] argument of [CoroutineScope.getBitmapAndDisplay] we pass it our [Uri] field [mUri].
         *
         * Finally we return our [Dialog] field [mDialog] to the caller.
         *
         * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
         * @return Return a new [Dialog] instance to be displayed by the [Fragment].
         */
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            mDialog = super.onCreateDialog(savedInstanceState)
            // To optimize for the "lightbox" style layout.  Since we're not actually displaying a
            // title, remove the bar along the top of the fragment where a dialog title would
            // normally go.
            mDialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)
            val imageView = ImageView(activity)
            mDialog!!.setContentView(imageView)

            // Loading the image is going to require some sort of I/O, which must occur off the UI
            // thread. Changing the ImageView to display the image must occur ON the UI thread.
            // The easiest way to divide up this labor is with an AsyncTask but that is deprecated.
            // Instead we use the CoroutineScope extension function getBitmapAndDisplay which takes
            // two lambdas and the Uri to fetch, decode and display. The doInBackground lambda will
            // run on the Dispatchers.IO CoroutineContext in a separate thread, but the onPostExecute
            // lambda will be run in the main UI thread.
            lifecycleScope.getBitmapAndDisplay(
                doInBackground = { uri: Uri ->
                    dumpImageMetaData(uri)
                    getBitmapFromUri(uri)!!
                },
                onPostExecute = { bitmap: Bitmap ->
                    imageView.setImageBitmap(bitmap)
                },
                mUri!!
            )
            return mDialog!!
        }

        /**
         * Called when the [DialogFragment] is no longer started. First we call our super's
         * implementation of `onStop`, then if the [DialogFragment.getDialog] method (aka kotlin
         * `dialog` property) is not `null` we call its [Dialog.dismiss] method to dismiss the
         * dialog, removing it from the screen.
         */
        override fun onStop() {
            super.onStop()
            if (dialog != null) {
                dialog!!.dismiss()
            }
        }

        /**
         * Grabs metadata for a document specified by [Uri], logs it to the screen. We initialize
         * our [Cursor] variable `val cursor` to the [Cursor] returned by a [ContentResolver]
         * instance for our application's package when we call its [ContentResolver.query] method
         * on our [Uri] parameter [uri]. If `cursor` is not `null` we use the [use] extension method
         * on it to execute a block wherein:
         *  - we call the [Cursor.moveToFirst] method on it to move it to the first row, and it that
         *  method returns `false` the [Cursor] is empty so we do nothing.
         *  - otherwise we initialize our [String] variable `val displayName` to the value stored in
         *  the [OpenableColumns.DISPLAY_NAME] column and we log `displayName`.
         *  - we initialize our [Int] variable `val sizeIndex` to the column index for the
         *  [OpenableColumns.SIZE] column.
         *  - we initialize our [String] variable `val size` to the [String] value of the column
         *  at index `sizeIndex` if it is not `null`, or to the [String] "Unknown" if it is `null`.
         *  - Finally we log `size`.
         *
         * @param uri The [Uri] for the document whose metadata should be printed.
         */
        fun dumpImageMetaData(uri: Uri?) {
            // The query, since it only applies to a single document, will only return one row.
            // no need to filter, sort, or select fields, since we want all fields for one
            // document.
            val cursor: Cursor? = requireActivity().contentResolver
                .query(uri!!, null, null, null, null, null)
            cursor?.use { cursorIt: Cursor ->
                // moveToFirst() returns false if the cursorIt has 0 rows.  Very handy for
                // "if there's anything to look at, look at it" conditionals.
                if (cursorIt.moveToFirst()) {

                    // Note it's called "Display Name".  This is provider-specific, and
                    // might not necessarily be the file name.
                    val displayName: String = cursorIt.getString(
                        cursorIt.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                    )
                    Log.i(TAG, "Display Name: $displayName")
                    val sizeIndex: Int = cursorIt.getColumnIndex(OpenableColumns.SIZE)
                    // If the size is unknown, the value stored is null.  But since an int can't be
                    // null in java, the behavior is implementation-specific, which is just a fancy
                    // term for "unpredictable".  So as a rule, check if it's null before assigning
                    // to an int.  This will happen often:  The storage API allows for remote
                    // files, whose size might not be locally known.
                    val size: String? = if (!cursorIt.isNull(sizeIndex)) {
                        // Technically the column stores an int, but cursorIt.getString will do the
                        // conversion automatically.
                        cursorIt.getString(sizeIndex)
                    } else {
                        "Unknown"
                    }
                    Log.i(TAG, "Size: $size")
                }
            }
        }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        const val TAG = "StorageClientFragment"
    }
}