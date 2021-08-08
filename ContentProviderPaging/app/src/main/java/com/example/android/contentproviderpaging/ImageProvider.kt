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
@file:Suppress("UnusedImport")

package com.example.android.contentproviderpaging

import android.content.ContentProvider
import android.content.UriMatcher
import com.example.android.contentproviderpaging.ImageContract
import android.os.Bundle
import android.database.MatrixCursor
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.MatrixCursor.RowBuilder
import com.example.android.contentproviderpaging.R
import android.content.res.TypedArray
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import android.util.Log
import java.io.File
import java.io.IOException
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import java.lang.UnsupportedOperationException
import java.util.Arrays

/**
 * [ContentProvider] that demonstrates how the paging support introduced in Android O works.
 * This class fetches the images from the local storage but the storage could be other locations
 * such as a remote server.
 */
class ImageProvider : ContentProvider() {
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

        init {
            // Add a Uri to match the path "images" which returns IMAGES when it matches
            sUriMatcher.addURI(ImageContract.AUTHORITY, "images", IMAGES)
            // Add a Uri to match the path "images/#" which returns IMAGES_ID when it matches
            sUriMatcher.addURI(ImageContract.AUTHORITY, "images/#", IMAGE_ID)
        }
    }

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
     * @return `true` if the provider was successfully loaded, `false` otherwise
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
     * Implement this to handle query requests where the arguments are packed into a [Bundle].
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
        if (sUriMatcher.match(uri) != IMAGES) return null
        val result = MatrixCursor(resolveDocumentProjection(projection))
        val files = mBaseDir.listFiles()
        val offset = queryArgs!!.getInt(ContentResolver.QUERY_ARG_OFFSET, 0)
        val limit = queryArgs.getInt(ContentResolver.QUERY_ARG_LIMIT, Int.MAX_VALUE)
        Log.d(
            TAG, "queryChildDocuments with Bundle, Uri: " +
                uri + ", offset: " + offset + ", limit: " + limit
        )
        require(offset >= 0) { "Offset must not be less than 0" }
        require(limit >= 0) { "Limit must not be less than 0" }
        if (offset >= files!!.size) {
            return result
        }
        var i = offset
        val maxIndex = (offset + limit).coerceAtMost(files.size)
        while (i < maxIndex) {
            includeFile(result, files[i])
            i++
        }
        val bundle = Bundle()
        bundle.putInt(ContentResolver.EXTRA_SIZE, files.size)
        var honoredArgs = arrayOfNulls<String>(2)
        var size = 0
        if (queryArgs.containsKey(ContentResolver.QUERY_ARG_OFFSET)) {
            honoredArgs[size++] = ContentResolver.QUERY_ARG_OFFSET
        }
        if (queryArgs.containsKey(ContentResolver.QUERY_ARG_LIMIT)) {
            honoredArgs[size++] = ContentResolver.QUERY_ARG_LIMIT
        }
        if (size != honoredArgs.size) {
            honoredArgs = honoredArgs.copyOf(size)
        }
        bundle.putStringArray(ContentResolver.EXTRA_HONORED_ARGS, honoredArgs)
        result.extras = bundle
        return result
    }

    @Suppress("RedundantNullableReturnType")
    override fun getType(uri: Uri): String? {
        return when (sUriMatcher.match(uri)) {
            IMAGES -> "vnd.android.cursor.dir/images"
            IMAGE_ID -> "vnd.android.cursor.item/images"
            else -> throw IllegalArgumentException(
                String.format(
                    "Unknown URI: %s",
                    uri
                )
            )
        }
    }

    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        throw UnsupportedOperationException()
    }

    override fun delete(uri: Uri, s: String?, strings: Array<String>?): Int {
        throw UnsupportedOperationException()
    }

    override fun update(
        uri: Uri, contentValues: ContentValues?, s: String?,
        strings: Array<String>?
    ): Int {
        throw UnsupportedOperationException()
    }

    /**
     * Add a representation of a file to a cursor.
     *
     * @param result the cursor to modify
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
     *
     * @param context   the Context
     * @param resId     the resource ID of the file to write to internal storage
     * @param extension the file extension (ex. .png, .mp3)
     */
    private fun writeFileToInternalStorage(context: Context, resId: Int, extension: String) {
        val ins = context.resources.openRawResource(resId)
        var size: Int
        val buffer = ByteArray(1024)
        try {
            val filename = context.resources.getResourceEntryName(resId) + extension
            val fos = context.openFileOutput(filename, Context.MODE_PRIVATE)
            while (ins.read(buffer, 0, 1024).also { size = it } >= 0) {
                fos.write(buffer, 0, size)
            }
            ins.close()
            fos.write(buffer)
            fos.close()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    private fun getResourceIdArray(context: Context, arrayResId: Int): IntArray {
        val ar = context.resources.obtainTypedArray(arrayResId)
        val len = ar.length()
        val resIds = IntArray(len)
        for (i in 0 until len) {
            resIds[i] = ar.getResourceId(i, 0)
        }
        ar.recycle()
        return resIds
    }
}