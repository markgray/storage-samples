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
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.InputStream
import java.io.OutputStream

/**
 * URL returning random picture provided by Unsplash. Read more here: https://source.unsplash.com
 */
private const val RANDOM_IMAGE_URL = "https://source.unsplash.com/random/500x500"

/**
 * This is the [AndroidViewModel] that is used by the [AddDocumentFragment] fragment.
 *
 * @param application the [Application] class used for maintaining global application state.
 * @param savedStateHandle handle to saved state passed down to our view model, values can be
 * stored in it using a [String] key, and the value of an entry can observed via the [LiveData]
 * returned by [SavedStateHandle.getLiveData] method for that [String] key. We store the current
 * media [Uri] in it under the key "currentMediaUri", and we expose it as a [LiveData] to the UI
 * using our [currentMediaUri] property. We also store the "temporary photo Uri" under the key
 * "temporaryPhotoUri" using our [saveTemporarilyPhotoUri] method. The `OnClickListener` of the
 * "Take Picture" button of the [AddDocumentFragment] UI calls this with the content [Uri] that
 * will hold the captured image before launching the camera activity. It is retrieved by our
 * [temporaryPhotoUri] property on return from executing the camera activity and used to replace
 * the contents of the [currentMediaUri] entry in this [SavedStateHandle].
 */
class AddMediaViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    /**
     * The [Context] of the [Application].
     */
    private val context: Context
        get() = getApplication()

    /**
     * `true` if this app has permission to write to shared storage. We return the result returned
     * by our [checkMediaStorePermission] method when passed our application context field [context].
     * It always returns `true` for Android versions greater than or equal to Q because we use
     * [MediaStore] for them, otherwise it calls the [checkSelfPermission] method to check whether
     * the user has granted us the [WRITE_EXTERNAL_STORAGE] permission yet and returns `true` if
     * the result is [PackageManager.PERMISSION_GRANTED]. It is used by [AddDocumentFragment] in
     * four places to decide whether it needs to show the "Permission Section" of its UI in order
     * to allow the user to click the "Request Permission" button in order to grant us the
     * permissions we need.
     */
    val canWriteInMediaStore: Boolean
        get() = checkMediaStorePermission(context)

    /**
     * The [OkHttpClient] that our [saveRandomImageFromInternet] method uses to download a random
     * image from the Internet. We use lazy to instantiate the [OkHttpClient] only when accessing
     * it, not when the viewmodel is created.
     */
    private val httpClient by lazy { OkHttpClient() }

    /**
     * We keep the current media [Uri] in the savedStateHandle to re-render it if there is a
     * configuration change and we expose it here as a [LiveData] to the UI. An observer added
     * to it in the `onCreateView` override of [AddDocumentFragment] uses [Glide] to load this
     * content [Uri] into the `ImageView` section of its UI whenever this changes value.
     */
    val currentMediaUri: LiveData<Uri?> = savedStateHandle.getLiveData("currentMediaUri")

    /**
     * TakePicture activityResult action doesn't return the [Uri] once the image has been taken, so
     * we need to save the temporarily created URI in [savedStateHandle] until we handle its result.
     * The `OnClickListener` added to the "Take Picture" button of the UI calls this with the [Uri]
     * that the camera activity will store the Image to, and the lambda of the [ActivityResultLauncher]
     * that is executed on return from the camera activity calls this with `null` to clear it again.
     * Our [temporaryPhotoUri] method is used to retrieve the value from [savedStateHandle] by the
     * [ActivityResultLauncher] lambda in order to call our [loadCameraMedia] method with it to set
     * the "currentMediaUri" entry to it on return from the camera activity.
     */
    fun saveTemporarilyPhotoUri(uri: Uri?) {
        savedStateHandle["temporaryPhotoUri"] = uri
    }

    /**
     * Getter for the "temporaryPhotoUri" entry in our [SavedStateHandle] field [savedStateHandle].
     * Read by the lambda of the "Take Picture" [ActivityResultLauncher] on return from the camera
     * activity in order to order to call our [loadCameraMedia] method with it to set the
     * "currentMediaUri" entry in [savedStateHandle] to it.
     */
    val temporaryPhotoUri: Uri?
        get() = savedStateHandle["temporaryPhotoUri"]

    /**
     * [loadCameraMedia] is called when the TakePicture or TakeVideo intent returns a successful
     * result with the content [Uri] for the shared storage file that was written to. We store that
     * [Uri] in our [SavedStateHandle] field [savedStateHandle] under the key "currentMediaUri" and
     * an observer of our [currentMediaUri] property will use [Glide] to load the `ImageView`
     * section of its UI with the image whenever that entry changes value.
     *
     * @param uri the content [Uri] pointing to the image file in shared storage.
     */
    fun loadCameraMedia(uri: Uri) {
        Log.d(TAG, "currentMediaUri is set to $uri")
        savedStateHandle["currentMediaUri"] = uri
    }

    /**
     * We create a [Uri] where the image will be stored. For Android Q and above this will be
     * something like "content://media/external_primary/images/media/609", on older versions
     * "content://media/external/images/media/609". First we initialize our [Uri] variable
     * `val imageCollection` depending on the value of [Build.VERSION.SDK_INT] (SDK version of the
     * software currently running on this hardware device):
     *  - For versions Q and newer: we initialize `imageCollection` to the value returned by the
     *  [MediaStore.Images.Media.getContentUri] method for [MediaStore.VOLUME_EXTERNAL_PRIMARY]
     *  ("content://media/external_primary/images/media" on my Pixel 3)
     *  - For versions older than Q: we initialize `imageCollection` to the value of the constant
     *  [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] ("content://media/external/images/media" on
     *  an API 21 emulator).
     *
     * Then we use the [withContext] method to call a suspending block with the coroutine context of
     * [Dispatchers.IO], and in the block we initialize our [ContentValues] variable `val newImage`
     * to a new instance to which we use the [apply] extension function to add to the [ContentValues]
     * under the key `MediaStore.Images.Media.DISPLAY_NAME` the file name generated by our method
     * [generateFilename] with our [Source] parameter [source] supplying the prefix for the file,
     * and the string "jpg" as the extension of the file. Finally we return the [Uri] returned from
     * the method [ContentResolver.insert] of a [ContentResolver] instance for our application's
     * package when we call it with `imageCollection` as the URL of the table to insert into, and
     * `newImage` as the values for the newly inserted row (it creates a `newImage` file in the
     * `imageCollection` folder and returns a content [Uri] that can be used to access it).
     *
     * @param source the [Source] of the image, either [Source.CAMERA] ("camera-" prefix) or
     * [Source.INTERNET] ("internet-" prefix).
     * @return a content [Uri] that can be used to access the new file that has been created in the
     * shared storage.
     */
    suspend fun createPhotoUri(source: Source): Uri? {
        /**
         * For build versions Q and newer this is content://media/external_primary/images/media,
         * for old versions this is content://media/external/images/media
         */
        val imageCollection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        Log.d(TAG, "$imageCollection")

        return withContext(Dispatchers.IO) {
            val newImage = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, generateFilename(source, "jpg"))
            }

            // This method will perform a binder transaction which is better to execute off the main
            // thread
            return@withContext context.contentResolver.insert(imageCollection, newImage)
        }
    }

    /**
     * We create a [Uri] where the camera will store the video. For Android Q and above this will be
     * something like "content://media/external_primary/video/media/610", on older versions
     * "content://media/external/video/media/610". We initialize our [Uri] variable `val videoCollection`
     * based on the value of [Build.VERSION.SDK_INT] (SDK version of the software currently running
     * on this hardware device):
     *  - Greater than or equal to [Build.VERSION_CODES.Q] (API 29) we use the [Uri] returned by
     *  the [MediaStore.Video.Media.getContentUri] for [MediaStore.VOLUME_EXTERNAL_PRIMARY] (the
     *  method creates the [Uri] by appending the path [MediaStore.VOLUME_EXTERNAL_PRIMARY] followed
     *  by the path "video" followed by the path "media" to the [MediaStore] `AUTHORITY_URI`. The
     *  [Uri] `AUTHORITY_URI` is the [Uri] formed by using [Uri.parse] on the [String] formed by
     *  concatenating the [String]s "content://" and the `AUTHORITY` [String] "media". The path
     *  [MediaStore.VOLUME_EXTERNAL_PRIMARY] is the [String] "external_primary")
     *  - Less than [Build.VERSION_CODES.Q] we use the [Uri] in the static final variable
     *  [MediaStore.Video.Media.EXTERNAL_CONTENT_URI] (which is initialized to the [Uri] that
     *  is returned by the [MediaStore.Video.Media.getContentUri] method for the volume path
     *  "external": content://media/external/video/media).
     *
     * Then we use the [withContext] method to call a suspending block with the coroutine context of
     * [Dispatchers.IO], and in the block we initialize our [ContentValues] variable `val newVideo`
     * to a new instance to which we use the [apply] extension function to add to the [ContentValues]
     * under the key `MediaStore.Images.Media.DISPLAY_NAME` the file name generated by our method
     * [generateFilename] with our [Source] parameter [source] supplying the prefix for the file,
     * and the string "mp4" as the extension of the file. Finally we return the [Uri] returned from
     * the method [ContentResolver.insert] of a [ContentResolver] instance for our application's
     * package when we call it with `videoCollection` as the URL of the table to insert into, and
     * `newVideo` as the values for the newly inserted row (it creates a `newVideo` file in the
     * `imageCollection` folder and returns a content [Uri] that can be used to access it).
     *
     * @param source the [Source] of the image, either [Source.CAMERA] ("camera-" prefix) or
     * [Source.INTERNET] ("internet-" prefix). We are only called with [Source.CAMERA] by the
     * `OnClickListener` of the "Take Video" button in the UI of [AddMediaFragment].
     * @return a content [Uri] that can be used to access the new file that has been created in the
     * shared storage.
     */
    suspend fun createVideoUri(source: Source): Uri? {
        /**
         * For build versions Q and newer this is content://media/external_primary/video/media,
         * for old versions this is content://media/external/video/media
         */
        val videoCollection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        Log.d(TAG, "$videoCollection")

        return withContext(Dispatchers.IO) {
            val newVideo = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, generateFilename(source, "mp4"))
            }

            // This method will perform a binder transaction which is better to execute off the main
            // thread
            return@withContext context.contentResolver.insert(videoCollection, newVideo)
        }
    }

    /**
     * [saveRandomImageFromInternet] downloads a random image from unsplash.com and saves its
     * content in shared storage. We launch a new coroutine in the [CoroutineScope] tied to this
     * `ViewModel`. In the coroutine block we initialize our [Uri] variable `val imageUri` to the
     * content [Uri] returned by our [createPhotoUri] for the prefix [Source.INTERNET] ("internet-")
     * and initialize our [Request] variable `val request` to a [Request] whose URL target is
     * [RANDOM_IMAGE_URL] ("https://source.unsplash.com/random/500x500"). Then we use the [withContext]
     * method to call a suspending block with the coroutine context of [Dispatchers.IO], and in the
     * block iff the `imageUri` [Uri] is not `null` we use the [let] extension on that `destinationUri`
     * to execute a block where:
     *  - we initialize our [Response] variable `val response` to the [Response] returned when we use
     *  our [OkHttpClient] field [httpClient] to execute the [Request] it "prepares" from `request`.
     *  - iff the [ResponseBody] of the [Response] variable `response` is not `null` we use the [use]
     *  extension function on this `responseBody` to execute a block on it where we use the method
     *  [ContentResolver.openOutputStream] of a [ContentResolver] instance for our application's
     *  package to open an [OutputStream] to write to the content [Uri] `destinationUri`, and if that
     *  is not `null` we copy the [InputStream] returned by the [ResponseBody.byteStream] method of
     *  `responseBody` to that [OutputStream]. Then since we can't write to [savedStateHandle] within
     *  a background thread we use the [withContext] method to call a suspending block with
     *  the coroutine context of [Dispatchers.Main] and in that block store `destinationUri` under
     *  the key "currentMediaUri" in [savedStateHandle] and call our parameter [callback].
     *
     * @param callback a lambda which will be executed after we finish downloading the random image
     * and copying it to shared storage. The `OnClickListener` of the "Download Picture" button in
     * the UI of [AddMediaFragment] re-enables the button in the lambda it calls us with.
     */
    fun saveRandomImageFromInternet(callback: () -> Unit) {
        viewModelScope.launch {
            val imageUri: Uri? = createPhotoUri(Source.INTERNET)
            // We use OkHttp to create HTTP request
            val request: Request = Request.Builder().url(RANDOM_IMAGE_URL).build()

            withContext(Dispatchers.IO) {

                imageUri?.let { destinationUri: Uri ->
                    val response: Response = httpClient.newCall(request).execute()

                    // .use is an extension function that closes the output stream where we're
                    // saving the image content once its lambda is finished being executed
                    @Suppress("UNNECESSARY_SAFE_CALL") // It looks like java code is missing nullable annotation
                    response.body?.use { responseBody: ResponseBody ->
                        context.contentResolver.openOutputStream(destinationUri, "w")?.use {
                            responseBody.byteStream().copyTo(it)

                            /**
                             * We can't set savedStateHandle within a background thread, so we do it
                             * within the [Dispatchers.Main], which execute its coroutines on the
                             * main thread
                             */
                            withContext(Dispatchers.Main) {
                                savedStateHandle["currentMediaUri"] = destinationUri
                                callback()
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "AddMediaViewModel"
    }
}

/**
 * Check if the app can write to shared storage.  On Android 10 (API 29), we can add media to
 * [MediaStore] without having to request the [WRITE_EXTERNAL_STORAGE] permission, so we only
 * check on pre-API 29 devices by calling the [checkSelfPermission] method to check whether we
 * have been granted the permission [WRITE_EXTERNAL_STORAGE] and returning `true` if the value
 * it returns is [PackageManager.PERMISSION_GRANTED].
 *
 * @param context the application [Context]
 * @return `true` if our app can write to shared storage, `false` if we cannot.
 */
private fun checkMediaStorePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        true
    } else {
        checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * enum that is used to choose the prefix of the file name generated by our [generateFilename]
 * method. [CAMERA] selects the [String] "camera-" for the prefix, and [INTERNET] selects the
 * [String] "internet-" for the prefix.
 */
enum class Source {
    /**
     * Used to select the [String] "camera-" for the file name prefix generated by [generateFilename]
     */
    CAMERA,

    /**
     * Used to select the [String] "internet-" for the file name prefix generated by [generateFilename]
     */
    INTERNET
}

/**
 * Generates the file name to be used for storing an image to shared storage. We branch on the value
 * of our [Source] parameter [source]:
 *  - [Source.CAMERA]: we return the [String] formed by concatenating the [String] "camera-" to
 *  the string value of the current time in milliseconds, followed by a "." and our [String]
 *  parameter [extension].
 *  - [Source.INTERNET]: we return the [String] formed by concatenating the [String] "internet-" to
 *  the string value of the current time in milliseconds, followed by a "." and our [String]
 *  parameter [extension].
 *
 * @param source the source of the image, either [Source.CAMERA] or [Source.INTERNET], used for
 * choosing the prefix of the file name "camera-" or "internet-" respectively.
 * @param extension the extension of the file (aka file type), in our case "jpg" or "mp4".
 * @return a [String] that can be used to name a file.
 */
private fun generateFilename(source: Source, extension: String): String {
    return when (source) {
        Source.CAMERA -> {
            "camera-${System.currentTimeMillis()}.$extension"
        }

        Source.INTERNET -> {
            "internet-${System.currentTimeMillis()}.$extension"
        }
    }
}
