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
import androidx.lifecycle.lifecycleScope
import com.example.android.common.logger.Log.e
import com.example.android.common.logger.Log.i
import java.io.IOException

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
                    i(TAG, "Uri: " + uri.toString())
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
        private var mDialog: Dialog? = null
        private var mUri: Uri? = null
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            mUri = requireArguments().getParcelable("URI")
        }

        /**
         * Create a Bitmap from the URI for that image and return it.
         *
         * @param uri the Uri for the image to return.
         */
        private fun getBitmapFromUri(uri: Uri): Bitmap? {
            var parcelFileDescriptor: ParcelFileDescriptor? = null
            return try {
                parcelFileDescriptor = requireActivity().contentResolver.openFileDescriptor(uri, "r")
                val fileDescriptor = parcelFileDescriptor!!.fileDescriptor
                val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                parcelFileDescriptor.close()
                image
            } catch (e: Exception) {
                e(TAG, "Failed to load image.", e)
                null
            } finally {
                try {
                    parcelFileDescriptor?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                    e(TAG, "Error closing ParcelFile Descriptor")
                }
            }
        }

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            mDialog = super.onCreateDialog(savedInstanceState)
            // To optimize for the "lightbox" style layout.  Since we're not actually displaying a
            // title, remove the bar along the top of the fragment where a dialog title would
            // normally go.
            mDialog!!.window!!.requestFeature(Window.FEATURE_NO_TITLE)
            val imageView = ImageView(activity)
            mDialog!!.setContentView(imageView)

            // Loading the image is going to require some sort of I/O, which must occur off the UI
            // thread.  Changing the ImageView to display the image must occur ON the UI thread.
            // The easiest way to divide up this labor is with an AsyncTask.  The doInBackground
            // method will run in a separate thread, but onPostExecute will run in the main
            // UI thread.
            lifecycleScope.getBitmapAndDisplay(
                doInBackground = { uri: Uri ->
                    dumpImageMetaData(uri)
                    getBitmapFromUri(uri)!!
                }, onPostExecute = { bitmap: Bitmap ->
                imageView.setImageBitmap(bitmap)
            },
                mUri!!
            )
            return mDialog!!
        }

        override fun onStop() {
            super.onStop()
            if (dialog != null) {
                dialog!!.dismiss()
            }
        }

        /**
         * Grabs metadata for a document specified by URI, logs it to the screen.
         *
         * @param uri The uri for the document whose metadata should be printed.
         */
        fun dumpImageMetaData(uri: Uri?) {
            // The query, since it only applies to a single document, will only return one row.
            // no need to filter, sort, or select fields, since we want all fields for one
            // document.
            val cursor = requireActivity().contentResolver
                .query(uri!!, null, null, null, null, null)
            cursor?.use { cursorIt: Cursor ->
                // moveToFirst() returns false if the cursorIt has 0 rows.  Very handy for
                // "if there's anything to look at, look at it" conditionals.
                if (cursorIt.moveToFirst()) {

                    // Note it's called "Display Name".  This is provider-specific, and
                    // might not necessarily be the file name.
                    val displayName = cursorIt.getString(
                        cursorIt.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    i(TAG, "Display Name: $displayName")
                    val sizeIndex = cursorIt.getColumnIndex(OpenableColumns.SIZE)
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
                    i(TAG, "Size: $size")
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