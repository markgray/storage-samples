/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.contentproviderpaging

import android.content.ContentProvider
import android.content.UriMatcher
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * [ContentProvider] that demonstrates how the paging support introduced in Android O works.
 * This class fetches the images from the local storage but the storage could be other locations
 * such as a remote server.
 */
class ImageProvider : ContentProvider() {
    /**
     * The absolute path to the directory on the filesystem where files created with
     * `openFileOutput` are stored.
     */
    private lateinit var mBaseDir: File

    /**
     * Implement this to initialize your content provider on startup. This method is called for all
     * registered content providers on the application main thread at application launch time.
     *
     * We initialize our [Context] variable `val context` to the [Context] this provider is running
     * in, returning `false` if this is `null`. We then initialize our [File] field [mBaseDir] to
     * the absolute path to the directory on the filesystem where files created with `openFileOutput`
     * are stored. Next we call our [writeDummyFilesToStorage] method with `context` to have it
     * write a bunch of copies of the cat jpegs in our raw resources to the internal storage
     * directory. Finally we return `true` to the caller to report that we were successfully loaded.
     *
     * @return `true` if the provider was successfully loaded, `false` otherwise.
     */
    override fun onCreate(): Boolean {
        Log.d(TAG, "onCreate")

        val context: Context = context ?: return false
        mBaseDir = context.filesDir
        writeDummyFilesToStorage(context)

        return true
    }

    /**
     * Implement this to handle query requests from clients. Apps targeting O or higher should
     * override `query(Uri, String[], Bundle, CancellationSignal)` and provide a stub implementation
     * of this method. We just throw [UnsupportedOperationException] because we do target O.
     *
     * @param uri The [Uri] to query. This will be the full [Uri] sent by the client, if the client
     * is requesting a specific record, the [Uri] will end in a record number that the implementation
     * should parse and add to a WHERE or HAVING clause, specifying that _id value.
     * @param projection The list of columns to put into the cursor. If `null` all columns are included.
     * @param selection A selection criteria to apply when filtering rows. If `null` then all rows
     * are included.
     * @param selectionArgs You may include ?s in selection, which will be replaced by the values
     * from [selectionArgs], in the order that they appear in the selection. The values will be
     * bound as [String]s.
     * @param sortOrder How the rows in the cursor should be sorted. If `null` then the provider is
     * free to define the sort order.
     * @return a [Cursor] or `null`.
     */
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        throw UnsupportedOperationException()
    }

    /**
     * Implement this to handle query requests where the arguments are packed into a [Bundle]. First
     * we use the [UriMatcher.match] method of our [sUriMatcher] field to make sure that our [Uri]
     * parameter [uri] matches for the path "images" ([UriMatcher.match] returns [IMAGES]), and if
     * it does not we return `null` to the caller. Otherwise we initialize our [MatrixCursor] variable
     * `val result` to a new instance constructed to contain the column names returned by our method
     * [resolveDocumentProjection] when it is called with our [Array] of [String] parameter
     * [projection] (in our case this is always [ImageContract.PROJECTION_ALL] which contains the
     * names of all of our columns). Next we initialize our [Array] of [File] variable `val files`
     * to the array of abstract pathnames denoting the files in the directory [mBaseDir]. We initialize
     * our [Int] variable `val offset` to the value stored in our [Bundle] parameter [queryArgs] under
     * the key [ContentResolver.QUERY_ARG_OFFSET], and our [Int] variable `val limit` to the value
     * stored under the key [ContentResolver.QUERY_ARG_LIMIT]. We make sure that `offset` and `limit`
     * are both greater than or equal to 0 throwing a [IllegalArgumentException] if they are not.
     * Then if `offset` is greater than or equal to the size of `files` we return `result` without
     * adding any rows to it.
     *
     * Otherwise we initialize our [Int] variable `val maxIndex` to `offset` plus `limit` coerced to
     * be at most the size of `files`. Then we loop over `i` from `offset` until `maxIndex` calling
     * our [includeFile] method to have it add a new row for the [File] at index `i` in `files` to
     * `result`.
     *
     * We next initialize our [Bundle] variable `val bundle` to the [Bundle] returned from our
     * [constructExtras] method when it is passed our [Bundle] parameter [queryArgs], and our
     * [Array] of [File] variable `files` and then set `bundle` to be the [Bundle] that will be
     * returned by the [Cursor.getExtras] method of `result` and return `result` to the caller.
     *
     * @param uri The [Uri] to query. This will be the full [Uri] sent by the client.
     * @param projection The list of columns to put into the cursor. If `null` provide a default set
     * of columns.
     * @param queryArgs A [Bundle] containing additional information necessary for the operation.
     * Arguments may include SQL style arguments, such as [ContentResolver.QUERY_ARG_SQL_LIMIT], but
     * note that the documentation for each individual provider will indicate which arguments they
     * support.
     * @param cancellationSignal A signal to cancel the operation in progress, or `null`.
     * @return a [Cursor] or `null`.
     */
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        queryArgs: Bundle?,
        cancellationSignal: CancellationSignal?
    ): Cursor? {
        // We only support a query for multiple images, return null for other form of queries
        // including a query for a single image.
        if (sUriMatcher.match(uri) != IMAGES) return null
        val result = MatrixCursor(resolveDocumentProjection(projection))

        val files: Array<File>? = mBaseDir.listFiles()
        val offset = queryArgs!!.getInt(ContentResolver.QUERY_ARG_OFFSET, 0)
        val limit = queryArgs.getInt(ContentResolver.QUERY_ARG_LIMIT, Integer.MAX_VALUE)
        Log.d(TAG, "queryChildDocuments with Bundle, Uri: " +
                uri + ", offset: " + offset + ", limit: " + limit)
        if (offset < 0) {
            throw IllegalArgumentException("Offset must not be less than 0")
        }
        if (limit < 0) {
            throw IllegalArgumentException("Limit must not be less than 0")
        }

        if (offset >= files!!.size) {
            return result
        }

        val maxIndex = (offset + limit).coerceAtMost(files.size)
        for (i in offset until maxIndex) {
            includeFile(result, files[i])
        }

        val bundle = constructExtras(queryArgs, files)
        result.extras = bundle
        return result
    }

    /**
     * Constructs the extras [Bundle] that our [query] override stores in the [Cursor] that it returns
     * to its caller.
     *
     * @param queryArgs the `queryArgs` [Bundle] parameter passed to our [query] override.
     * @param files the [Array] of all of the [File] objects we can provide.
     * @return a [Bundle] which has the total number of [File] objects we can provide stored under
     * the key [ContentResolver.EXTRA_TOTAL_COUNT], as well as an [Array] of [String] which lists
     * the keys of of the arguments [query] has been asked to honor in our [Bundle] parameter
     * [queryArgs] ([ContentResolver.QUERY_ARG_OFFSET] and/or [ContentResolver.QUERY_ARG_LIMIT] or
     * neither) stored under the key [ContentResolver.EXTRA_HONORED_ARGS].
     */
    private fun constructExtras(queryArgs: Bundle, files: Array<File>): Bundle {
        val bundle = Bundle()
        bundle.putInt(ContentResolver.EXTRA_TOTAL_COUNT, files.size)
        var size = 0
        if (queryArgs.containsKey(ContentResolver.QUERY_ARG_OFFSET)) {
            size++
        }
        if (queryArgs.containsKey(ContentResolver.QUERY_ARG_LIMIT)) {
            size++
        }
        if (size > 0) {
            val honoredArgs = arrayOfNulls<String>(size)
            var index = 0
            if (queryArgs.containsKey(ContentResolver.QUERY_ARG_OFFSET)) {
                honoredArgs[index++] = ContentResolver.QUERY_ARG_OFFSET
            }
            if (queryArgs.containsKey(ContentResolver.QUERY_ARG_LIMIT)) {
                honoredArgs[index] = ContentResolver.QUERY_ARG_LIMIT
            }
            bundle.putStringArray(ContentResolver.EXTRA_HONORED_ARGS, honoredArgs)
        }
        return bundle
    }

    @Suppress("RedundantNullableReturnType")
    override fun getType(uri: Uri): String? {
        return when (sUriMatcher.match(uri)) {
            IMAGES -> "vnd.android.cursor.dir/images"
            IMAGE_ID -> "vnd.android.cursor.item/images"
            else -> throw IllegalArgumentException(String.format("Unknown URI: %s", uri))
        }
    }

    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        throw UnsupportedOperationException()
    }

    override fun delete(uri: Uri, s: String?, strings: Array<String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun update(uri: Uri, contentValues: ContentValues?, s: String?,
                        strings: Array<String>?): Int {
        throw UnsupportedOperationException()
    }

    /**
     * Add a representation of a file to a cursor.

     * @param result the cursor to modify
     * *
     * @param file   the File object representing the desired file (may be null if given docID)
     */
    private fun includeFile(result: MatrixCursor, file: File) {
        val row = result.newRow()
        row.add(ImageContract.Columns.DISPLAY_NAME, file.name)
        row.add(ImageContract.Columns.SIZE, file.length())
        row.add(ImageContract.Columns.ABSOLUTE_PATH, file.absolutePath)
    }

    /**
     * Preload sample files packaged in the apk into the internal storage directory.  This is a
     * dummy function specific to this demo.  The MyCloud mock cloud service doesn't actually
     * have a backend, so it simulates by reading content from the device's internal storage.
     */
    private fun writeDummyFilesToStorage(context: Context) {
        if (mBaseDir.list()!!.isNotEmpty()) {
            return
        }

        val imageResIds = getResourceIdArray(context, R.array.image_res_ids)
        for (i in 0 until REPEAT_COUNT_WRITE_FILES) {
            for (resId in imageResIds) {
                writeFileToInternalStorage(context, resId, "-$i.jpeg")
            }
        }
    }

    /**
     * Write a file to internal storage.  Used to set up our dummy "cloud server".

     * @param context   the Context
     * *
     * @param resId     the resource ID of the file to write to internal storage
     * *
     * @param extension the file extension (ex. .png, .mp3)
     */
    private fun writeFileToInternalStorage(context: Context, resId: Int, extension: String) {
        val ins = context.resources.openRawResource(resId)
        val buffer = ByteArray(1024)
        try {
            val filename = context.resources.getResourceEntryName(resId) + extension
            val fos = context.openFileOutput(filename, Context.MODE_PRIVATE)
            while (true) {
                val size = ins.read(buffer, 0, 1024)
                if (size < 0) break
                fos.write(buffer, 0, size)
            }
            ins.close()
            fos.write(buffer)
            fos.close()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    /**
     *  Used to read an array of resource IDs from the resource array whose ID is our [Int] parameter
     * [arrayResId]. We initialize our [TypedArray] variable `val ar` by retrieving a [Resources]
     * instance from our [Context] parameter [context] and using its [Resources.obtainTypedArray]
     * method to fetch a [TypedArray] holding an array of the array values in the resource identifier
     * [arrayResId]. We initialize our [Int] variable `val len` to the number of values in `ar`, and
     * initialize our [IntArray] variable `val resIds` to a new array of size `len`, with all elements
     * initialized to zero. We then loop over [Int] variable `i` from 0 until `len` setting the `i`th
     * entry in `resIds` to the resource identifier for the attribute at index `i` in `ar`. When done
     * looping we call the [TypedArray.recycle] method of `ar` to recycle the [TypedArray] so it can
     * be re-used by a later caller, and return `resIds` to the caller.
     *
     * @param context the [Context] this provider is running in.
     * @param arrayResId the resource ID of the array of resource IDs we are to read.
     * @return an [IntArray] of the resource identifiers for the attributes in the array of resource
     * IDs whose resource ID is [arrayResId].
     */
    private fun getResourceIdArray(context: Context, arrayResId: Int): IntArray {
        val ar: TypedArray = context.resources.obtainTypedArray(arrayResId)
        val len = ar.length()
        val resIds = IntArray(len)
        for (i in 0 until len) {
            resIds[i] = ar.getResourceId(i, 0)
        }
        ar.recycle()
        return resIds
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "ImageDocumentsProvider"

        /**
         * The code that is returned when a [Uri] is matched by [UriMatcher] field [sUriMatcher]
         * against the authority [ImageContract.AUTHORITY] and path "images".
         */
        private const val IMAGES = 1

        /**
         * The code that is returned when a [Uri] is matched by [UriMatcher] field [sUriMatcher]
         * against the authority [ImageContract.AUTHORITY] and path "images/#".
         */
        private const val IMAGE_ID = 2

        /**
         * The [UriMatcher] that we use to match the [Uri] parameter of our [getType] override and
         * our [query] override. Two URIs to match are added to it in our `init` block, both using
         * the authority [ImageContract.AUTHORITY] with the path "images" returning the code [IMAGES]
         * and the path "images/#" returning the code [IMAGE_ID]. [getType] returns the mime type
         * "vnd.android.cursor.dir/images" when the [UriMatcher.match] method of [sUriMatcher] returns
         * [IMAGES], and the mime type "vnd.android.cursor.item/images" when the [UriMatcher.match]
         * method of [sUriMatcher] returns [IMAGE_ID]. [query] returns `null` unless the [UriMatcher.match]
         * method of [sUriMatcher] returns [IMAGES].
         */
        private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {
            sUriMatcher.addURI(ImageContract.AUTHORITY, "images", IMAGES)
            sUriMatcher.addURI(ImageContract.AUTHORITY, "images/#", IMAGE_ID)
        }

        /**
         * How many copies of the same images are going to be written as dummy images.
         */
        private const val REPEAT_COUNT_WRITE_FILES = 10

        /**
         * Convenience method to default to [ImageContract.PROJECTION_ALL] if the projection passed
         * to our [query] override is `null`.
         *
         * @return our [Array] of [String] parameter [projection] if it is not `null` or the constant
         * [Array] of [String] in [ImageContract.PROJECTION_ALL] if it is `null`.
         */
        private fun resolveDocumentProjection(projection: Array<String>?): Array<String> {
            return projection ?: ImageContract.PROJECTION_ALL
        }
    }
}
