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
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.view.MenuItem
import android.view.Window
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.example.android.common.logger.Log.e
import com.example.android.common.logger.Log.i
import java.io.IOException

class StorageClientFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sample_action) {
            performFileSearch()
        }
        return true
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    fun performFileSearch() {

        // BEGIN_INCLUDE (use_open_document_intent)
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
        startActivityForResult(intent, READ_REQUEST_CODE)
        // END_INCLUDE (use_open_document_intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        i(TAG, "Received an \"Activity Result\"")
        // BEGIN_INCLUDE (parse_open_document_response)
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"
            val uri: Uri?
            if (resultData != null) {
                uri = resultData.data
                i(TAG, "Uri: " + uri.toString())
                showImage(uri)
            }
            // END_INCLUDE (parse_open_document_response)
        }
    }

    /**
     * Given the URI of an image, shows it on the screen using a DialogFragment.
     *
     * @param uri the Uri of the image to display.
     */
    fun showImage(uri: Uri?) {
        // BEGIN_INCLUDE (create_show_image_dialog)
        if (uri != null) {
            // Since the URI is to an image, create and show a DialogFragment to display the
            // image to the user.
            val fm = requireActivity().supportFragmentManager
            val imageDialog = ImageDialogFragment()
            val fragmentArguments = Bundle()
            fragmentArguments.putParcelable("URI", uri)
            imageDialog.arguments = fragmentArguments
            imageDialog.show(fm, "image_dialog")
        }
        // END_INCLUDE (create_show_image_dialog)
    }

    /**
     * DialogFragment which displays an image, given a URI.
     */
    class ImageDialogFragment : DialogFragment() {
        private var mDialog: Dialog? = null
        private var mUri: Uri? = null
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            mUri = requireArguments().getParcelable("URI")
        }

        /** Create a Bitmap from the URI for that image and return it.
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

            // BEGIN_INCLUDE (show_image)
            // Loading the image is going to require some sort of I/O, which must occur off the UI
            // thread.  Changing the ImageView to display the image must occur ON the UI thread.
            // The easiest way to divide up this labor is with an AsyncTask.  The doInBackground
            // method will run in a separate thread, but onPostExecute will run in the main
            // UI thread.
            val imageLoadAsyncTask: AsyncTask<Uri?, Void?, Bitmap?> =
                object : AsyncTask<Uri?, Void?, Bitmap?>() {
                    protected override fun doInBackground(vararg uris: Uri?): Bitmap? {
                        dumpImageMetaData(uris[0])
                        return getBitmapFromUri(uris[0]!!)
                    }

                    override fun onPostExecute(bitmap: Bitmap?) {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            imageLoadAsyncTask.execute(mUri)
            // END_INCLUDE (show_image)
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
            // BEGIN_INCLUDE (dump_metadata)

            // The query, since it only applies to a single document, will only return one row.
            // no need to filter, sort, or select fields, since we want all fields for one
            // document.
            val cursor = requireActivity().contentResolver
                .query(uri!!, null, null, null, null, null)
            try {
                // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
                // "if there's anything to look at, look at it" conditionals.
                if (cursor != null && cursor.moveToFirst()) {

                    // Note it's called "Display Name".  This is provider-specific, and
                    // might not necessarily be the file name.
                    val displayName = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    i(TAG, "Display Name: $displayName")
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    // If the size is unknown, the value stored is null.  But since an int can't be
                    // null in java, the behavior is implementation-specific, which is just a fancy
                    // term for "unpredictable".  So as a rule, check if it's null before assigning
                    // to an int.  This will happen often:  The storage API allows for remote
                    // files, whose size might not be locally known.
                    val size: String?
                    size = if (!cursor.isNull(sizeIndex)) {
                        // Technically the column stores an int, but cursor.getString will do the
                        // conversion automatically.
                        cursor.getString(sizeIndex)
                    } else {
                        "Unknown"
                    }
                    i(TAG, "Size: $size")
                }
            } finally {
                cursor?.close()
            }
            // END_INCLUDE (dump_metadata)
        }
    }

    companion object {
        // A request code's purpose is to match the result of a "startActivityForResult" with
        // the type of the original request.  Choose any value.
        private const val READ_REQUEST_CODE = 1337
        const val TAG = "StorageClientFragment"
    }
}