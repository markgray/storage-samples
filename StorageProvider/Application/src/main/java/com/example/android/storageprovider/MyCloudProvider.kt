/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.example.android.storageprovider

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.CancellationSignal
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.example.android.common.logger.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

/**
 * Manages documents and exposes them to the Android system for sharing. It extends [DocumentsProvider]
 * which is the base class for a document provider. A document provider offers read and write access
 * to durable files, such as files stored on a local disk, or files in a cloud storage service. To
 * create a document provider, extend [DocumentsProvider], implement the abstract methods, and add
 * it to your manifest like this:
 *
 *      <manifest>
 *       ...
 *         <application>
 *       ...
 *            <provider
 *                  android:name="com.example.android.storageprovider.MyCloudProvider"
 *                  android:authorities="com.example.android.storageprovider.documents"
 *                  android:exported="true"
 *                  android:grantUriPermissions="true"
 *                  android:permission="android.permission.MANAGE_DOCUMENTS"
 *                 <intent-filter>
 *                      <action android:name="android.content.action.DOCUMENTS_PROVIDER" />
 *                 </intent-filter>
 *              </provider>
 *                 ...
 *         </application>
 *     </manifest>
 *
 * When defining your provider, you must protect it with `Manifest.permission.MANAGE_DOCUMENTS`, which
 * is a permission only the system can obtain. Applications cannot use a documents provider directly;
 * they must go through `Intent.ACTION_OPEN_DOCUMENT` or `Intent.ACTION_CREATE_DOCUMENT` which requires
 * a user to actively navigate and select documents. When a user selects documents through that UI,
 * the system issues narrow URI permission grants to the requesting application.
 *
 * # Documents
 *
 * A document can be either an openable stream (with a specific MIME type), or a directory containing
 * additional documents (with the `DocumentsContract.Document.MIME_TYPE_DIR MIME` type). Each directory
 * represents the top of a subtree containing zero or more documents, which can recursively contain
 * even more documents and directories.
 *
 * Each document can have different capabilities, as described by `DocumentsContract.Document.COLUMN_FLAGS`.
 * For example, if a document can be represented as a thumbnail, your provider can set
 * `DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL` and implement [openDocumentThumbnail] to
 * return that thumbnail.
 *
 * Each document under a provider is uniquely referenced by its `DocumentsContract.Document.COLUMN_DOCUMENT_ID`,
 * which must not change once returned. A single document can be included in multiple directories
 * when responding to [queryChildDocuments]. For example, a provider might surface a single photo in
 * multiple locations: once in a directory of geographic locations, and again in a directory of dates.
 *
 * # Roots
 *
 * All documents are surfaced through one or more "roots." Each root represents the top of a document
 * tree that a user can navigate. For example, a root could represent an account or a physical storage
 * device. Similar to documents, each root can have capabilities expressed through
 * DocumentsContract.Root.COLUMN_FLAGS.
 */
class MyCloudProvider : DocumentsProvider() {
    /**
     * A file object at the root of the file hierarchy. Depending on your implementation, the root
     * does not need to be an existing file system directory. For example, a tag-based document
     * provider might return a directory containing all tags, represented as child directories.
     */
    private var mBaseDir: File? = null

    /**
     * We implement this to initialize our content provider on startup. This method is called for
     * all registered content providers on the application main thread at application launch time.
     * It must not perform lengthy operations, or application startup will be delayed.
     *
     * @return `true` if the provider was successfully loaded, `false` otherwise
     */
    override fun onCreate(): Boolean {
        Log.v(TAG, "onCreate")
        mBaseDir = context!!.filesDir
        writeDummyFilesToStorage()
        return true
    }

    override fun queryRoots(projection: Array<String>?): Cursor {
        Log.v(TAG, "queryRoots")

        // Create a cursor with either the requested fields, or the default projection.  This
        // cursor is returned to the Android system picker UI and used to display all roots from
        // this provider.
        val result = MatrixCursor(resolveRootProjection(projection))

        // If user is not logged in, return an empty root cursor.  This removes our provider from
        // the list entirely.
        if (!isUserLoggedIn) {
            return result
        }

        // It's possible to have multiple roots (e.g. for multiple accounts in the same app) -
        // just add multiple cursor rows.
        // Construct one row for a root called "MyCloud".
        val row = result.newRow()
        row.add(Root.COLUMN_ROOT_ID, ROOT)
        row.add(Root.COLUMN_SUMMARY, context!!.getString(R.string.root_summary))

        // FLAG_SUPPORTS_CREATE means at least one directory under the root supports creating
        // documents.  FLAG_SUPPORTS_RECENTS means your application's most recently used
        // documents will show up in the "Recents" category.  FLAG_SUPPORTS_SEARCH allows users
        // to search all documents the application shares.
        row.add(
            Root.COLUMN_FLAGS,
            Root.FLAG_SUPPORTS_CREATE or Root.FLAG_SUPPORTS_RECENTS or Root.FLAG_SUPPORTS_SEARCH
        )

        // COLUMN_TITLE is the root title (e.g. what will be displayed to identify your provider).
        row.add(
            Root.COLUMN_TITLE,
            context!!.getString(R.string.app_name)
        )

        // This document id must be unique within this provider and consistent across time.  The
        // system picker UI may save it and refer to it later.
        row.add(
            Root.COLUMN_DOCUMENT_ID,
            getDocIdForFile(mBaseDir)
        )

        // The child MIME types are used to filter the roots and only present to the user roots
        // that contain the desired type somewhere in their file hierarchy.
        row.add(Root.COLUMN_MIME_TYPES, getChildMimeTypes(mBaseDir))
        row.add(Root.COLUMN_AVAILABLE_BYTES, mBaseDir!!.freeSpace)
        row.add(Root.COLUMN_ICON, R.drawable.ic_launcher)
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun queryRecentDocuments(
        rootId: String,
        projection: Array<String>
    ): Cursor {
        Log.v(TAG, "queryRecentDocuments")

        // This example implementation walks a local file structure to find the most recently
        // modified files.  Other implementations might include making a network call to query a
        // server.

        // Create a cursor with the requested projection, or the default projection.
        val result = MatrixCursor(resolveDocumentProjection(projection))
        val parent = getFileForDocId(rootId)

        // Create a queue to store the most recent documents, which orders by last modified.
        val lastModifiedFiles = PriorityQueue(
            5
        ) { i: File?, j: File? -> i!!.lastModified().compareTo(j!!.lastModified()) }

        // Iterate through all files and directories in the file structure under the root.  If
        // the file is more recent than the least recently modified, add it to the queue,
        // limiting the number of results.
        val pending = LinkedList<File?>()

        // Start by adding the parent to the list of files to be processed
        pending.add(parent)

        // Do while we still have unexamined files
        while (!pending.isEmpty()) {
            // Take a file from the list of unprocessed files
            val file = pending.removeFirst()
            if (file!!.isDirectory) {
                // If it's a directory, add all its children to the unprocessed list
                val listOfFiles = file.listFiles()
                if (listOfFiles != null) {
                    Collections.addAll(pending, *listOfFiles)
                } else {
                    throw RuntimeException("file.listFiles() is null")
                }
            } else {
                // If it's a file, add it to the ordered queue.
                lastModifiedFiles.add(file)
            }
        }

        // Add the most recent files to the cursor, not exceeding the max number of results.
        var includedCount = 0
        while (includedCount < MAX_LAST_MODIFIED + 1 && !lastModifiedFiles.isEmpty()) {
            val file = lastModifiedFiles.remove()
            includeFile(result, null, file)
            includedCount++
        }
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun querySearchDocuments(
        rootId: String,
        query: String,
        projection: Array<String>
    ): Cursor {
        Log.v(TAG, "querySearchDocuments")

        // Create a cursor with the requested projection, or the default projection.
        val result = MatrixCursor(resolveDocumentProjection(projection))
        val parent = getFileForDocId(rootId)

        // This example implementation searches file names for the query and doesn't rank search
        // results, so we can stop as soon as we find a sufficient number of matches.  Other
        // implementations might use other data about files, rather than the file name, to
        // produce a match; it might also require a network call to query a remote server.

        // Iterate through all files in the file structure under the root until we reach the
        // desired number of matches.
        val pending = LinkedList<File?>()

        // Start by adding the parent to the list of files to be processed
        pending.add(parent)

        // Do while we still have unexamined files, and fewer than the max search results
        while (!pending.isEmpty() && result.count < MAX_SEARCH_RESULTS) {
            // Take a file from the list of unprocessed files
            val file = pending.removeFirst()
            if (file!!.isDirectory) {
                // If it's a directory, add all its children to the unprocessed list
                val listOfFiles = file.listFiles()
                if (listOfFiles != null) {
                    Collections.addAll(pending, *listOfFiles)
                } else {
                    throw RuntimeException("file.listFiles() is null")
                }
            } else {
                // If it's a file and it matches, add it to the result cursor.
                if (file.name.lowercase(Locale.getDefault()).contains(query)) {
                    includeFile(result, null, file)
                }
            }
        }
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point,
        signal: CancellationSignal
    ): AssetFileDescriptor {
        Log.v(TAG, "openDocumentThumbnail")
        val file = getFileForDocId(documentId)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }

    @Throws(FileNotFoundException::class)
    override fun queryDocument(
        documentId: String,
        projection: Array<String>?
    ): Cursor {
        Log.v(TAG, "queryDocument")

        // Create a cursor with the requested projection, or the default projection.
        val result = MatrixCursor(resolveDocumentProjection(projection))
        includeFile(result, documentId, null)
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String
    ): Cursor {
        Log.v(TAG, "queryChildDocuments, parentDocumentId: " +
            parentDocumentId +
            " sortOrder: " +
            sortOrder)
        val result = MatrixCursor(resolveDocumentProjection(projection))
        val parent = getFileForDocId(parentDocumentId)
        val parentListOfFiles = parent!!.listFiles()
        if (parentListOfFiles != null) {
            for (file in parentListOfFiles) {
                includeFile(result, null, file)
            }
        } else {
            throw RuntimeException("parent.listFiles() is null")
        }
        return result
    }

    @Throws(FileNotFoundException::class)
    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        Log.v(TAG, "openDocument, mode: $mode")
        // It's OK to do network operations in this method to download the document, as long as you
        // periodically check the CancellationSignal.  If you have an extremely large file to
        // transfer from the network, a better solution may be pipes or sockets
        // (see ParcelFileDescriptor for helper methods).
        val file = getFileForDocId(documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        val isWrite = mode.indexOf('w') != -1
        return if (isWrite) {
            // Attach a close listener if the document is opened in write mode.
            try {
                val handler = Handler(context!!.mainLooper)
                ParcelFileDescriptor.open(file, accessMode, handler
                ) {
                    // Update the file with the cloud server.  The client is done writing.
                    Log.i(TAG, "A file with id " + documentId + " has been closed!  Time to " +
                        "update the server.")
                }
            } catch (e: IOException) {
                throw FileNotFoundException("Failed to open document with id " + documentId +
                    " and mode " + mode)
            }
        } else {
            ParcelFileDescriptor.open(file, accessMode)
        }
    }

    @Throws(FileNotFoundException::class)
    override fun createDocument(
        documentId: String,
        mimeType: String,
        displayName: String
    ): String {
        Log.v(TAG, "createDocument")
        val parent = getFileForDocId(documentId)
        val file = File(parent!!.path, displayName)
        try {
            file.createNewFile()
            file.setWritable(true)
            file.setReadable(true)
        } catch (e: IOException) {
            throw FileNotFoundException("Failed to create document with name " +
                displayName + " and documentId " + documentId)
        }
        return getDocIdForFile(file)
    }

    @Throws(FileNotFoundException::class)
    override fun deleteDocument(documentId: String) {
        Log.v(TAG, "deleteDocument")
        val file = getFileForDocId(documentId)
        if (file!!.delete()) {
            Log.i(TAG, "Deleted file with id $documentId")
        } else {
            throw FileNotFoundException("Failed to delete document with id $documentId")
        }
    }

    @Throws(FileNotFoundException::class)
    override fun getDocumentType(documentId: String): String {
        val file = getFileForDocId(documentId)
        return getTypeForFile(file)
    }

    /**
     * Gets a string of unique MIME data types a directory supports, separated by newlines.  This
     * should not change.
     *
     * @param parent the File for the parent directory
     * @return a string of the unique MIME data types the parent directory supports
     */
    @Suppress("UNUSED_PARAMETER")
    private fun getChildMimeTypes(parent: File?): String {
        val mimeTypes: MutableSet<String> = HashSet()
        mimeTypes.add("image/*")
        mimeTypes.add("text/*")
        mimeTypes.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document")

        // Flatten the list into a string and insert newlines between the MIME type strings.
        val mimeTypesString = StringBuilder()
        for (mimeType in mimeTypes) {
            mimeTypesString.append(mimeType).append("\n")
        }
        return mimeTypesString.toString()
    }

    /**
     * Get the document ID given a File.  The document id must be consistent across time.  Other
     * applications may save the ID and use it to reference documents later.
     *
     *
     * This implementation is specific to this demo.  It assumes only one root and is built
     * directly from the file structure.  However, it is possible for a document to be a child of
     * multiple directories (for example "android" and "images"), in which case the file must have
     * the same consistent, unique document ID in both cases.
     *
     * @param file the File whose document ID you want
     * @return the corresponding document ID
     */
    private fun getDocIdForFile(file: File?): String {
        var path = file!!.absolutePath

        // Start at first char of path under root
        val rootPath = mBaseDir!!.path
        path = when {
            rootPath == path -> {
                ""
            }
            rootPath.endsWith("/") -> {
                path.substring(rootPath.length)
            }
            else -> {
                path.substring(rootPath.length + 1)
            }
        }
        return "root:$path"
    }

    /**
     * Add a representation of a file to a cursor.
     *
     * @param result the cursor to modify
     * @param docId  the document ID representing the desired file (may be null if given file)
     * @param file   the File object representing the desired file (may be null if given docID)
     * @throws java.io.FileNotFoundException if the file is not found
     */
    @Throws(FileNotFoundException::class)
    private fun includeFile(result: MatrixCursor, docId: String?, file: File?) {
        var docIdLocal = docId
        var fileLocal = file
        if (docIdLocal == null) {
            docIdLocal = getDocIdForFile(fileLocal)
        } else {
            fileLocal = getFileForDocId(docIdLocal)
        }
        var flags = 0
        if (fileLocal!!.isDirectory) {
            // Request the folder to lay out as a grid rather than a list. This also allows a larger
            // thumbnail to be displayed for each image.
            //            flags |= Document.FLAG_DIR_PREFERS_GRID;

            // Add FLAG_DIR_SUPPORTS_CREATE if the file is a writable directory.
            if (fileLocal.isDirectory && fileLocal.canWrite()) {
                flags = flags or DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
            }
        } else if (fileLocal.canWrite()) {
            // If the file is writable set FLAG_SUPPORTS_WRITE and
            // FLAG_SUPPORTS_DELETE
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_WRITE
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_DELETE
        }
        val displayName = fileLocal.name
        val mimeType = getTypeForFile(fileLocal)
        if (mimeType.startsWith("image/")) {
            // Allow the image to be represented by a thumbnail rather than an icon
            flags = flags or DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
        }
        val row = result.newRow()
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, docIdLocal)
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName)
        row.add(DocumentsContract.Document.COLUMN_SIZE, fileLocal.length())
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType)
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, fileLocal.lastModified())
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags)

        // Add a custom icon
        row.add(DocumentsContract.Document.COLUMN_ICON, R.drawable.ic_launcher)
    }

    /**
     * Translate your custom URI scheme into a File object.
     *
     * @param docId the document ID representing the desired file
     * @return a File represented by the given document ID
     * @throws java.io.FileNotFoundException if the file is not found
     */
    @Throws(FileNotFoundException::class)
    private fun getFileForDocId(docId: String): File? {
        var target = mBaseDir
        if (docId == ROOT) {
            return target
        }
        val splitIndex = docId.indexOf(':', 1)
        return if (splitIndex < 0) {
            throw FileNotFoundException("Missing root for $docId")
        } else {
            val path = docId.substring(splitIndex + 1)
            target = File(target, path)
            if (!target.exists()) {
                throw FileNotFoundException("Missing file for $docId at $target")
            }
            target
        }
    }

    /**
     * Preload sample files packaged in the apk into the internal storage directory.  This is a
     * dummy function specific to this demo.  The MyCloud mock cloud service doesn't actually
     * have a backend, so it simulates by reading content from the device's internal storage.
     */
    private fun writeDummyFilesToStorage() {
        val listOfmBaseDir = mBaseDir!!.list()
        if (listOfmBaseDir != null) {
            if (listOfmBaseDir.isNotEmpty()) {
                return
            }
        } else {
            throw RuntimeException("mBaseDir.list() is null")
        }
        val imageResIds = getResourceIdArray(R.array.image_res_ids)
        for (resId in imageResIds) {
            writeFileToInternalStorage(resId, ".jpeg")
        }
        val textResIds = getResourceIdArray(R.array.text_res_ids)
        for (resId in textResIds) {
            writeFileToInternalStorage(resId, ".txt")
        }
        val docxResIds = getResourceIdArray(R.array.docx_res_ids)
        for (resId in docxResIds) {
            writeFileToInternalStorage(resId, ".docx")
        }
    }

    /**
     * Write a file to internal storage.  Used to set up our dummy "cloud server".
     *
     * @param resId     the resource ID of the file to write to internal storage
     * @param extension the file extension (ex. .png, .mp3)
     */
    private fun writeFileToInternalStorage(resId: Int, extension: String) {
        val ins = context!!.resources.openRawResource(resId)
        val outputStream = ByteArrayOutputStream()
        var size: Int
        var buffer: ByteArray? = ByteArray(1024)
        try {
            while (ins.read(buffer, 0, 1024).also { size = it } >= 0) {
                outputStream.write(buffer!!, 0, size)
            }
            ins.close()
            buffer = outputStream.toByteArray()
            val filename = context!!.resources.getResourceEntryName(resId) + extension
            val fos = context!!.openFileOutput(filename, Context.MODE_PRIVATE)
            fos.write(buffer)
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getResourceIdArray(arrayResId: Int): IntArray {
        val ar = context!!.resources.obtainTypedArray(arrayResId)
        val len = ar.length()
        val resIds = IntArray(len)
        for (i in 0 until len) {
            resIds[i] = ar.getResourceId(i, 0)
        }
        ar.recycle()
        return resIds
    }

    /**
     * Dummy function to determine whether the user is logged in.
     */
    private val isUserLoggedIn: Boolean
        get() {
            val sharedPreferences = context!!.getSharedPreferences(
                context!!.getString(R.string.app_name),
                Context.MODE_PRIVATE
            )
            val isTheUserLoggedIn = sharedPreferences.getBoolean(
                context!!.getString(R.string.key_logged_in),
                false
            )
            if (!isTheUserLoggedIn) {
                Log.i(TAG, "The user is NOT logged in")
            } else {
                Log.i(TAG, "The user IS logged in")
            }
            return isTheUserLoggedIn
        }

    companion object {
        private const val TAG = "MyCloudProvider"

        /**
         * Use these as the default columns to return information about a root if no specific
         * columns are requested in a query.
         */
        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_MIME_TYPES,
            Root.COLUMN_FLAGS,
            Root.COLUMN_ICON,
            Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_AVAILABLE_BYTES
        )

        /**
         * Use these as the default columns to return information about a document if no specific
         * columns are requested in a query.
         */
        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE
        )

        /**
         * No official policy on how many to return, but make sure you do limit the number of recent
         * and search results.
         */
        private const val MAX_SEARCH_RESULTS = 20
        private const val MAX_LAST_MODIFIED = 5
        private const val ROOT = "root"

        /**
         * @param projection the requested root column projection
         * @return either the requested root column projection, or the default projection if the
         * requested projection is null.
         */
        private fun resolveRootProjection(projection: Array<String>?): Array<String> {
            return projection ?: DEFAULT_ROOT_PROJECTION
        }

        private fun resolveDocumentProjection(projection: Array<String>?): Array<String> {
            return projection ?: DEFAULT_DOCUMENT_PROJECTION
        }

        /**
         * Get a file's MIME type
         *
         * @param file the File object whose type we want
         * @return the MIME type of the file
         */
        private fun getTypeForFile(file: File?): String {
            return if (file!!.isDirectory) {
                DocumentsContract.Document.MIME_TYPE_DIR
            } else {
                getTypeForName(file.name)
            }
        }

        /**
         * Get the MIME data type of a document, given its filename.
         *
         * @param name the filename of the document
         * @return the MIME data type of a document
         */
        private fun getTypeForName(name: String): String {
            val lastDot = name.lastIndexOf('.')
            if (lastDot >= 0) {
                val extension = name.substring(lastDot + 1)
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (mime != null) {
                    return mime
                }
            }
            return "application/octet-stream"
        }
    }
}