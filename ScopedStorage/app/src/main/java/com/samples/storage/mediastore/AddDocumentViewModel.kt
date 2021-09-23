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
import android.database.Cursor
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
     *  variable `val outputStream` to a new instance for writing to `file`. We next initialize our
     *  [ResponseBody] variable `val responseBody` to the [ResponseBody] that our [downloadFileFromInternet]
     *  method returns when [OkHttpClient] field [httpClient] executes a [Request] built from our
     *  `randomRemoteUrl` variable and returns the [ResponseBody] of the [Response] that the website
     *  returns (it is a one-shot stream from the origin server to the client application with the
     *  raw bytes of the response body). If the [ResponseBody] returned is `null` we post a task to
     *  the main thread to set the value of our [MutableLiveData] wrapped [Boolean] field
     *  [_isDownloading] to `false` and return to the caller. We use the [use] extension method on
     *  `responseBody` to execute a block which uses the [use] extension method on `outputStream`
     *  and in that inner block we use the extension function [InputStream.copyTo] on the [InputStream]
     *  returned by the [ResponseBody.byteStream] method of `responseBody` to copy that stream to the
     *  [OutputStream] of `outputStream`. The [use] extension function will then close both streams
     *  it is used on whether an exception is thrown or not. When done copying to the file we log
     *  the fact that we downloaded to the absolute pathname of the [File] `file`. Next we scan the
     *  newly added file to make sure [MediaStore.Downloads] is up to date by calling our suspend
     *  function [scanFilePath] to have it use the [MediaScannerConnection.scanFile] method to have
     *  it scan the file system path of `file` for the mime type that is found in the
     *  [ResponseBody.contentType] property of `responseBody` passing in a lambda for the callback
     *  which initializes its [FileEntry] variable `val fileDetails` to the file details that our
     *  [getFileDetails] method extracts using the MediaStore API on the scanned [Uri] that the
     *  [MediaScannerConnection.scanFile] method passes the lambda. We log `fileDetails` then set
     *  the "current_file" entry of our [SavedStateHandle] field [savedStateHandle] to `fileDetails`
     *  and finally post a task to the main thread to set the value of [_isDownloading] to `false`
     *  (causing the observer of its accessor field [isDownloading] to enable the "Download Random
     *  File" button in the [AddDocumentFragment] UI).
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
                    Log.d(TAG, "MimeType of $newFileUri is ${responseBody.contentType()}")

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
     * Starts the download of the file whose URL is our [String] parameter [url] by our [OkHttpClient]
     * field [httpClient] and returns the [ResponseBody] of the [Response] received. We initialize our
     * [Request] variable `val request` by using a [Request.Builder] to build a [Request] whose URL
     * target is constructed from our [String] parameter [url]. Then we use the [withContext] method
     * to call a suspending block with the coroutine context of [Dispatchers.IO], and in the block
     * we initialize our [Response] variable `val response` to the [Response] returned when we use
     * our [OkHttpClient] field [httpClient] to execute the [Request] it "prepares" from `request`.
     * When that blocking call returns the block returns the [ResponseBody] of the [Response] variable
     * `response` as its result and [downloadFileFromInternet] returns that to its caller.
     *
     * @param url the URL address of the file on the Internet.
     * @return the [ResponseBody] of the [Response] received from the website. A [ResponseBody] is a
     * one-shot stream from the origin server to the client application with the raw bytes of the
     * response body. Each response body is supported by an active connection to the webserver.
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
     * Create a file inside the Downloads folder using java.io API. First we initialize our variable
     * `val downloadsFolder` to a [File] for the [DIRECTORY_DOWNLOADS] shared storage directory that
     * the [Environment.getExternalStoragePublicDirectory] returns. Then we initialize our [File]
     * variable `val newNonMediaFile` to a [File] in `downloadsFolder` with the name [filename].
     * Then we use the [withContext] method to call a suspending block with the coroutine context
     * of [Dispatchers.IO], and in the block if the [File.createNewFile] method of `newNonMediaFile`
     * returns `false` we throw an [Exception] stating that the file already exists, otherwise the
     * block returns `newNonMediaFile` as its result and [addFileToDownloadsApi21] returns this
     * [File] to the caller.
     *
     * @param filename the name of the file we are to create in the Downloads folder.
     * @return the [File] instance created from the [File] abstract pathname of the Downloads folder
     * and the pathname string in our [String] parameter [filename] (ie. the [File] points to the
     * file we created in the Downloads folder).
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
     * Create a file inside the Downloads folder using MediaStore API. We initialize our [Uri]
     * variable `val collection` to the content uri for the [MediaStore] collection of downloaded
     * items on its [MediaStore.VOLUME_EXTERNAL_PRIMARY] volume (aka the `Downloads` folder) that
     * the [MediaStore.Downloads.getContentUri] method returns. Then we use the [withContext] method
     * to call a suspending block with the coroutine context of [Dispatchers.IO], and in the block
     * we initialize our [ContentValues] variable `val newFile` to a new instance to which we use
     * the [apply] method to store our [String] parameter [filename] in the [ContentValues] set
     * under the key [MediaStore.Downloads.DISPLAY_NAME] ("_display_name"). The block then calls
     * the [ContentResolver.insert] method of a [ContentResolver] instance for our application's
     * package to insert a new row whose values are those of the [ContentValues] variable `newFile`
     * in the table whose URL is `collection` (ie. tries to create a [File] whose name is [filename]
     * in the Downloads folder) and if the [Uri] that [ContentResolver.insert] returns is non-`null`
     * returns this as its value which [addFileToDownloadsApi29] then returns to its caller. If the
     * call to [ContentResolver.insert] returns `null` we throw the [Exception] "MediaStore Uri
     * couldn't be created" (a non-`null` [Uri] is a content [Uri] which can be opened to write to
     * the newly created [File]).
     *
     * @param filename the name of the file we are to create in the Downloads folder.
     * @return a [Uri] which can be used to open an [OutputStream] to write to the file with the
     * name of our [String] parameter [filename] in the Downloads folder.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun addFileToDownloadsApi29(filename: String): Uri {
        val collection: Uri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        return withContext(Dispatchers.IO) {
            val newFile: ContentValues = ContentValues().apply {
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
     * but it's better to do it ourselves to have a fresher state whenever we can.
     *
     * We use the [withContext] method to call a suspending block with the coroutine context of
     * [Dispatchers.IO] and in that block we call the [MediaScannerConnection.scanFile] convenience
     * function to construct a [MediaScannerConnection], call `connect` on it and then when the
     * connection is established call its `scanFile` method with the path in our [path] parameter
     * and mime type in our [mimeType] parameter to request the media scanner to scan the file.
     *
     * The [MediaScannerConnection.OnScanCompletedListener] lambda we pass to
     * [MediaScannerConnection.scanFile] will then call our [callback] parameter with the
     * [MediaStore] content [Uri] pointing to the scanned file.
     *
     * @param path the file system path to the newly added file. On my Pixel 3 this will be something
     * like: /storage/emulated/0/Download/1632216015520.md
     * @param mimeType the mime type [String] that is found in the [ResponseBody.contentType]
     * property of the [ResponseBody], for the above file this was "text/plain"
     * @param callback a callback which will be called with the the scanned [Uri] that the
     * [MediaScannerConnection.OnScanCompletedListener] callback of our call to the the method
     * [MediaScannerConnection.scanFile] is called with (this is the [MediaStore] content [Uri]
     * pointing to our newly created file).
     */
    private suspend fun scanFilePath(path: String, mimeType: String, callback: (uri: Uri) -> Unit) {
        withContext(Dispatchers.IO) {
            MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(mimeType)) { _, uri ->
                callback(uri)
            }
        }
    }

    /**
     * Get a path for a MediaStore entry as it's needed when calling MediaScanner. We use the
     * [withContext] method to call a suspending block with the coroutine context of [Dispatchers.IO]
     * and in that block:
     *  - We initialize our [Cursor] variable `val cursor` to the [Cursor] that a [ContentResolver]
     *  instance for our application's package returns when we call its [ContentResolver.query]
     *  method for our content [Uri] parameter [uri] to retrieve its [FileColumns.DATA] column.
     *  - Then we use the [use] extension function on `cursor` returning `null` if its method
     *  [Cursor.moveToFirst] returns `false` (indicates that the [Cursor] is empty) or return the
     *  [String] in the [FileColumns.DATA] column of `cursor` if it returns `true`.
     *
     * [getMediaStoreEntryPathApi29] then returns the value returned by the [withContext] block
     * to its caller.
     *
     * @param uri the [MediaStore] content [Uri] pointing to the file entry.
     * @return the file system path to the file, ie. /storage/emulated/0/Download/1632216015520.md
     * where the "1632216015520.md" file name differs for the different files downloaded of course.
     */
    private suspend fun getMediaStoreEntryPathApi29(uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            val cursor: Cursor = context.contentResolver.query(
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
     * Get file details using the MediaStore API. We use the [withContext] method to call a suspending
     * block with the coroutine context of [Dispatchers.IO] and in that block we initialize our
     * [Cursor] variable `val cursor` to the [Cursor] that a [ContentResolver] instance for our
     * application's package returns when we call its [ContentResolver.query] method for our content
     * [Uri] parameter [uri] to retrieve the columns: [FileColumns.DISPLAY_NAME] (the file name),
     * [FileColumns.SIZE] (the file length), [FileColumns.MIME_TYPE] (MIME type of the media item),
     * [FileColumns.DATE_ADDED] (time the media item was first added), and [FileColumns.DATA] (the
     * file system path to the file). If the call to [ContentResolver.query] returns `null` the block
     * returns `null`. Then we use the [use] extension method on `cursor` to:
     *  - call its [Cursor.moveToFirst] method to move `cursor` to the first row (returning `null`
     *  from the [withContext] block if the method returns `false` indicating the cursor is empty).
     *  - initialize our [Int] variable `val displayNameColumn` to the zero-based index for the
     *  column name [FileColumns.DISPLAY_NAME].
     *  - initialize our [Int] variable `val sizeColumn` to the zero-based index for the
     *  column name [FileColumns.SIZE].
     *  - initialize our [Int] variable `val mimeTypeColumn` to the zero-based index for the
     *  column name [FileColumns.MIME_TYPE].
     *  - initialize our [Int] variable `val dateAddedColumn` to the zero-based index for the
     *  column name [FileColumns.DATE_ADDED].
     *  - initialize our [Int] variable `val dataColumn` to the zero-based index for the
     *  column name [FileColumns.DATA].
     *  - then the [withContext] block returns a new instance of [FileEntry] constructed to use the
     *  [String] in column `displayNameColumn` for its [FileEntry.filename] property, the [Long] in
     *  column `sizeColumn` for its [FileEntry.size] property, the [String] in column `mimeTypeColumn`
     *  for its [FileEntry.mimeType] property, the [Long] in column `dateAddedColumn` multiplied by
     *  1000 (to convert seconds to milliseconds) for its [FileEntry.addedAt] property, and the
     *  [String] in column `dataColumn` for its [FileEntry.path] property.
     *
     * [getFileDetails] then returns the [FileEntry] returned from the [withContext] block to its
     * caller.
     *
     * @param uri the [MediaStore] content [Uri] pointing to the file whose details we want.
     * @return a [FileEntry] instance constructed from the relevant columns of the [Cursor] that the
     * [ContentResolver] returns when we query for our [Uri] parameter [uri].
     */
    private suspend fun getFileDetails(uri: Uri): FileEntry? {
        return withContext(Dispatchers.IO) {
            val cursor: Cursor = context.contentResolver.query(
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

/**
 * This is used to hold the file details of the last random file downloaded from the Internet which
 * is stored in the [SavedStateHandle] field [AddDocumentViewModel.savedStateHandle] of the
 * [AddDocumentViewModel] being used by the app under the key "current_file".
 * The [AddDocumentViewModel.currentFileEntry] field is a [MutableLiveData] that can be used to
 * access the current [FileEntry] contents stored under that key, and an observer added to it in
 * the `onCreateView` override of [AddDocumentFragment] updates the contents of the file details
 * section of its UI whenever the [FileEntry] changes value.
 *
 * @param filename the file name of the file, for example "1632216015520.md"
 * @param size the length of the file in bytes
 * @param mimeType the MIME type of the file, for example "text/plain"
 * @param addedAt the time the file was first added in millseconds since January 1, 1970
 * @param path the file system path of the file: /storage/emulated/0/Download/1632216015520.md
 */
@Parcelize
data class FileEntry(
    val filename: String,
    val size: Long,
    val mimeType: String,
    val addedAt: Long,
    val path: String
) : Parcelable
