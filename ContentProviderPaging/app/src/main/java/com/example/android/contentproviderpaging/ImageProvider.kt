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
 * ContentProvider that demonstrates how the paging support works introduced in Android O.
 * This class fetches the images from the local storage but the storage could be
 * other locations such as a remote server.
 */
class ImageProvider : ContentProvider() {
    companion object {
        private const val TAG = "ImageDocumentsProvider"
        private const val IMAGES = 1
        private const val IMAGE_ID = 2
        private val sUriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        // Indicated how many same images are going to be written as dummy images
        private const val REPEAT_COUNT_WRITE_FILES = 10
        private fun resolveDocumentProjection(projection: Array<String>?): Array<String> {
            return projection ?: ImageContract.PROJECTION_ALL
        }

        init {
            sUriMatcher.addURI(ImageContract.AUTHORITY, "images", IMAGES)
            sUriMatcher.addURI(ImageContract.AUTHORITY, "images/#", IMAGE_ID)
        }
    }

    private var mBaseDir: File? = null
    override fun onCreate(): Boolean {
        Log.d(TAG, "onCreate")
        val context = context ?: return false
        mBaseDir = context.filesDir
        writeDummyFilesToStorage(context)
        return true
    }

    override fun query(
        uri: Uri, strings: Array<String>?, s: String?,
        strings1: Array<String>?, s1: String?
    ): Cursor? {
        throw UnsupportedOperationException()
    }

    override fun query(
        uri: Uri, projection: Array<String>?, queryArgs: Bundle?,
        cancellationSignal: CancellationSignal?
    ): Cursor? {
        when (sUriMatcher.match(uri)) {
            IMAGES -> {
            }
            else -> return null
        }
        val result = MatrixCursor(resolveDocumentProjection(projection))
        val files = mBaseDir!!.listFiles()
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
        if (mBaseDir!!.list()!!.isNotEmpty()) {
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