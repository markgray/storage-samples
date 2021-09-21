/*
 * Copyright 2021 The Android Open Source Project
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

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.samples.storage.data.SampleFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.CoroutineContext

/**
 * TAG used for logging.
 */
private const val TAG = "AddDocumentViewModel"

/**
 * This is the [AndroidViewModel] that is used by the [AddDocumentFragment] fragment.
 *
 * @param application the [Application] class used for maintaining global application state.
 * @param savedStateHandle handle to saved state passed down to our view model, values can be
 * stored in it using a [String] key, and the value of an entry can observed via the [LiveData]
 * returned by [SavedStateHandle.getLiveData] method for that [String] key. We save the current
 * file's [FileEntry] under the key "current_file", and our [MutableLiveData] wrapped [FileEntry]
 * field [currentFileEntry] is initialized to the [LiveData] that the [SavedStateHandle.getLiveData]
 * method returns for the key "current_file". An observer added to [currentFileEntry] in the
 * `onCreateView` override of [AddDocumentFragment] updates the contents of the "File Details" section
 * of its UI with its new information whenever [currentFileEntry] changes to a non-`null` value.
 */
class AddDocumentViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    /**
     * The [Context] of our [Application].
     */
    private val context: Context
        get() = getApplication()

    /**
     * Check ability to add document in the Downloads folder or not. Convenience property for
     * calling our [canAddDocumentPermission] method.
     */
    val canAddDocument: Boolean
        get() = canAddDocumentPermission(context)

    /**
     * Using lazy to instantiate the [OkHttpClient] only when accessing it, not when the viewmodel
     * is created. Used by our [downloadFileFromInternet] method to download a file from a random
     * URL.
     */
    private val httpClient by lazy { OkHttpClient() }

    /**
     * Flag indicating whether we are currently downloading a file from the Internet or not. It is
     * set to `true` at the beginning of our [addRandomFile] method, and set to `false` when the
     * download completes (successfully or not). Private to prevent other classes from modifying it,
     * public read-only access is provided by our [isDownloading] property.
     */
    private var _isDownloading: MutableLiveData<Boolean> = MutableLiveData(false)
    /**
     * Public read-only access to our [_isDownloading] field. An observer is added to it in the
     * `onCreateView` override of [AddDocumentFragment] whose lambda sets the enabled state of the
     * "Download Random File" button in its UI to disabled when this transitions to `true` or to
     * enabled when it transitions to `false`.
     */
    val isDownloading: LiveData<Boolean> = _isDownloading

    /**
     * We keep the current [FileEntry] in the savedStateHandle to re-render it if there is a
     * configuration change and we expose it as a [LiveData] to the UI via this property.
     */
    val currentFileEntry: MutableLiveData<FileEntry> =
        savedStateHandle.getLiveData("current_file")

    /**
     * Generate random filename when saving a new file. Appends its [String] parameter [extension]
     * to the end of the string value of the current time in milliseconds (separated by a period).
     */
    private fun generateFilename(extension: String) = "${System.currentTimeMillis()}.$extension"

    /**
     * Check if the app can write on the shared storage. On Android 10 (API 29), we can add files to
     * the Downloads folder without having to request the [WRITE_EXTERNAL_STORAGE] permission, so we
     * only check on pre-API 29 devices by calling the [ContextCompat.checkSelfPermission] method
     * with the permission string [WRITE_EXTERNAL_STORAGE] (it returns [PackageManager.PERMISSION_GRANTED]
     * if we have the permission).
     *
     * @param context the [Context] of the [Application].
     * @return `true` if we can write to the Downloads folder, `false` if cannot.
     */
    private fun canAddDocumentPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                context,
                WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Downloads a file from a random URL and writes it to the shared "Downloads" folder of the
     * device. First we post a task to the main thread to set the value of our [MutableLiveData]
     * wrapped [Boolean] field [_isDownloading] to `true` (an observer added to its [isDownloading]
     * accessor field in the `onCreateView` override of [AddDocumentFragment] will set the enabled
     * state of the "Download Random File" button in its UI to disabled when this transitions to
     * `true`). Next we initialize our [String] variable `val randomRemoteUrl` to a random [String]
     * from the [SampleFiles.nonMedia] list of strings, and initialize our [String] variable
     * `val extension` to the string following the last "." character in `randomRemoteUrl` (the file
     * extension). Then we initialize our [String] variable `val filename` to the value returned by
     * our [generateFilename] method when it appends a "." character followed by `extension` to the
     * string value of the current time in milliseconds.
     *
     * Next we call a suspending block in the [CoroutineContext] of [Dispatchers.IO] with the code
     * in the block wrapped in a `try` block intended to catch and log any [Exception] (also posting
     * a task to the main thread to set the value of our [MutableLiveData] wrapped [Boolean] field
     * [_isDownloading] to `false` thereby causing the observer of [isDownloading] to enable the
     * "Download Random File" button in its UI). Within the `try` block we branch on the SDK version
     * of the software currently running on this hardware device (the `SystemProperties` "constant"
     * [Build.VERSION.SDK_INT]):
     *  - [Build.VERSION_CODES.Q] or greater: We initialize our [Uri] variable `val newFileUri` to
     *  the value returned by our [addFileToDownloadsApi29] method when it uses the MediaStore API
     *  to create a file with the name `filename` inside the `Downloads` folder. We initialize our
     *  [OutputStream] variable `val outputStream` to the [OutputStream] that a [ContentResolver]
     *  for our application's package returns when we call its [ContentResolver.openOutputStream] to
     *  open the [File] whose [Uri] is `newFileUri` with the mode "w" (if this is `null` the provider
     *  recently crashed and we throw the [Exception] "ContentResolver couldn't open $newFileUri
     *  outputStream"). We next initialize our [ResponseBody] variable `val responseBody` to the
     *  [ResponseBody] that our [downloadFileFromInternet] method returns when [OkHttpClient] field
     *  [httpClient] executes a [Request] built from our `randomRemoteUrl` variable and returns the
     *  [ResponseBody] of the [Response] that the website returns (it is a one-shot stream from the
     *  origin server to the client application with the raw bytes of the response body). If the
     *  [ResponseBody] returned is `null` we post a task to the main thread to set the value of our
     *  [MutableLiveData] wrapped [Boolean] field [_isDownloading] to `false` and return to the
     *  caller. We use the [use] extension method on `responseBody` to execute a block which uses
     *  the [use] extension method on `outputStream` and in that inner block we use the extension
     *  function [InputStream.copyTo] on the [InputStream] returned by the [ResponseBody.byteStream]
     *  method of `responseBody` to copy that stream to the [OutputStream] of `outputStream`. The
     *  [use] extension function will then close both streams it is used on whether an exception is
     *  thrown or not. When done copying to the file we log the fact that we downloaded to the
     *  `newFileUri` [Uri]. Next we initialize our [String] variable `val path` to the file system
     *  path to the [File] whose content [Uri] is `newFileUri` that our [getMediaStoreEntryPathApi29]
     *  method returns (for example the path to "content://media/external_primary/downloads/605" is
     *  /storage/emulated/0/Download/1632216015520.md when I download a random file on my Pixel 3).
     *  Next we scan the newly added file to make sure [MediaStore.Downloads] is up to date by calling
     *  our suspend function [scanFilePath] to have it use the [MediaScannerConnection.scanFile]
     *  method to have it scan the file system path `path` for the mime type that is found in the
     *  [ResponseBody.contentType] property of `responseBody` passing in a lambda for the callback
     *  which initializes its [FileEntry] variable `val fileDetails` to the file details that our
     *  [getFileDetails] method extracts using the MediaStore API on the scanned [Uri] that the
     *  [MediaScannerConnection.scanFile] method passes the lambda. We log `fileDetails` then set
     *  the "current_file" entry of our [SavedStateHandle] field [savedStateHandle] to `fileDetails`
     *  and finally post a task to the main thread to set the value of [_isDownloading] to `false`
     *  (causing the observer of its accessor field [isDownloading] to enable the "Download Random
     *  File" button in the [AddDocumentFragment] UI).
     *  - Older than [Build.VERSION_CODES.Q]: We initialize our [File] variable `val file` to the
     *  path to the [File] that our [addFileToDownloadsApi21] method creates in the Downloads folder
     *  for the file name `filename` using the java.io API, and initialize our [FileOutputStream]
     *  variable `val outputStream` to a new instance for `file`.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun addRandomFile() {
        _isDownloading.postValue(true)

        val randomRemoteUrl: String = SampleFiles.nonMedia.random()
        val extension: String =
            randomRemoteUrl.substring(randomRemoteUrl.lastIndexOf(".") + 1)
        val filename: String = generateFilename(extension)

        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val newFileUri: Uri = addFileToDownloadsApi29(filename)
                    val outputStream: OutputStream = context.contentResolver
                        .openOutputStream(newFileUri, "w")
                        ?: throw Exception("ContentResolver couldn't open $newFileUri outputStream")

                    val responseBody: ResponseBody? = downloadFileFromInternet(randomRemoteUrl)

                    if (responseBody == null) {
                        _isDownloading.postValue(false)
                        return@withContext
                    }

                    // .use is an extension function that closes the output stream where we're
                    // saving the file content once its lambda is finished being executed
                    responseBody.use {
                        outputStream.use {
                            responseBody.byteStream().copyTo(it)
                        }
                    }

                    Log.d(TAG, "File downloaded ($newFileUri)")

                    val path: String = getMediaStoreEntryPathApi29(newFileUri)
                        ?: throw Exception("ContentResolver couldn't find $newFileUri")
                    Log.d(TAG, "Path to $newFileUri is $path")

                    // We scan the newly added file to make sure MediaStore.Downloads is always up
                    // to date
                    scanFilePath(path, responseBody.contentType().toString()) { uri ->
                        Log.d(TAG, "MediaStore updated ($path, $uri)")

                        viewModelScope.launch {
                            val fileDetails: FileEntry? = getFileDetails(uri)
                            Log.d(TAG, "New file: $fileDetails")

                            savedStateHandle["current_file"] = fileDetails
                            _isDownloading.postValue(false)
                        }
                    }
                } else {
                    val file: File = addFileToDownloadsApi21(filename)
                    val outputStream: FileOutputStream = file.outputStream()

                    val responseBody: ResponseBody? = downloadFileFromInternet(randomRemoteUrl)

                    if (responseBody == null) {
                        _isDownloading.postValue(false)
                        return@withContext
                    }

                    // .use is an extension function that closes the output stream where we're
                    // saving the file content once its lambda is finished being executed
                    responseBody.use {
                        outputStream.use {
                            responseBody.byteStream().copyTo(it)
                        }
                    }

                    Log.d(TAG, "File downloaded (${file.absolutePath})")

                    // We scan the newly added file to make sure MediaStore.Files is always up to
                    // date
                    scanFilePath(file.path, responseBody.contentType().toString()) { uri ->
                        Log.d(TAG, "MediaStore updated ($file.path, $uri)")

                        viewModelScope.launch {
                            val fileDetails = getFileDetails(uri)
                            Log.d(TAG, "New file: $fileDetails")

                            savedStateHandle["current_file"] = fileDetails
                            _isDownloading.postValue(false)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                _isDownloading.postValue(false)
            }
        }
    }

    /**
     * Downloads a random file from internet and saves its content to the specified outputStream
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun downloadFileFromInternet(url: String): ResponseBody? {
        // We use OkHttp to create HTTP request
        val request: Request = Request.Builder().url(url).build()

        return withContext(Dispatchers.IO) {
            val response: Response = httpClient.newCall(request).execute()
            return@withContext response.body
        }
    }

    /**
     * Create a file inside the Downloads folder using java.io API
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun addFileToDownloadsApi21(filename: String): File {
        @Suppress("DEPRECATION")
        val downloadsFolder = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)

        // Get path of the destination where the file will be saved
        val newNonMediaFile = File(downloadsFolder, filename)

        return withContext(Dispatchers.IO) {
            // Create new file if it does not exist, throw exception otherwise
            if (!newNonMediaFile.createNewFile()) {
                throw Exception("File ${newNonMediaFile.name} already exists")
            }

            return@withContext newNonMediaFile
        }
    }

    /**
     * Create a file inside the Downloads folder using MediaStore API
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun addFileToDownloadsApi29(filename: String): Uri {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        return withContext(Dispatchers.IO) {
            val newFile = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
            }

            // This method will perform a binder transaction which is better to execute off the main
            // thread
            return@withContext context.contentResolver.insert(collection, newFile)
                ?: throw Exception("MediaStore Uri couldn't be created")
        }
    }

    /**
     * When adding a file (using java.io or ContentResolver APIs), MediaStore might not be aware of
     * the new entry or doesn't have an updated version of it. That's why some entries have 0 bytes
     * size, even though the file is definitely not empty. MediaStore will eventually scan the file
     * but it's better to do it ourselves to have a fresher state whenever we can
     */
    private suspend fun scanFilePath(path: String, mimeType: String, callback: (uri: Uri) -> Unit) {
        withContext(Dispatchers.IO) {
            MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(mimeType)) { _, uri ->
                callback(uri)
            }
        }
    }

    /**
     * Get a path for a MediaStore entry as it's needed when calling MediaScanner
     */
    private suspend fun getMediaStoreEntryPathApi29(uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            val cursor = context.contentResolver.query(
                uri,
                @Suppress("DEPRECATION")
                arrayOf(FileColumns.DATA),
                null,
                null,
                null
            ) ?: return@withContext null

            cursor.use {
                if (!cursor.moveToFirst()) {
                    return@withContext null
                }
                @Suppress("DEPRECATION")
                return@withContext cursor.getString(cursor.getColumnIndexOrThrow(FileColumns.DATA))
            }
        }
    }

    /**
     * Get file details using the MediaStore API
     */
    private suspend fun getFileDetails(uri: Uri): FileEntry? {
        return withContext(Dispatchers.IO) {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    FileColumns.DISPLAY_NAME,
                    FileColumns.SIZE,
                    FileColumns.MIME_TYPE,
                    FileColumns.DATE_ADDED,
                    @Suppress("DEPRECATION")
                    FileColumns.DATA
                ),
                null,
                null,
                null
            ) ?: return@withContext null

            cursor.use {
                if (!cursor.moveToFirst()) {
                    return@withContext null
                }

                val displayNameColumn = cursor.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(FileColumns.SIZE)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(FileColumns.MIME_TYPE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(FileColumns.DATE_ADDED)

                @Suppress("DEPRECATION")
                val dataColumn = cursor.getColumnIndexOrThrow(FileColumns.DATA)

                return@withContext FileEntry(
                    filename = cursor.getString(displayNameColumn),
                    size = cursor.getLong(sizeColumn),
                    mimeType = cursor.getString(mimeTypeColumn),
                    // FileColumns.DATE_ADDED is in seconds, not milliseconds
                    addedAt = cursor.getLong(dateAddedColumn) * 1000,
                    path = cursor.getString(dataColumn),
                )
            }
        }
    }
}

@Parcelize
data class FileEntry(
    val filename: String,
    val size: Long,
    val mimeType: String,
    val addedAt: Long,
    val path: String
) : Parcelable
