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
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody

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
     * The [OkHttpClient] that our [saveRandomImageFromInternet] uses to download a random image
     * from the Internet. We use lazy to instantiate the [OkHttpClient] only when accessing it, not
     * when the viewmodel is created.
     */
    private val httpClient by lazy { OkHttpClient() }

    /**
     * We keep the current media [Uri] in the savedStateHandle to re-render it if there is a
     * configuration change and we expose it here as a [LiveData] to the UI. An observer added
     * to it in the `onCreateView` override of [AddDocumentFragment] uses [Glide] to load this
     * content [Uri] into the `ImageView` section of its UI whenever this changes value.
     */
    val currentMediaUri: LiveData<Uri?> = savedStateHandle.getLiveData<Uri?>("currentMediaUri")

    /**
     * TakePicture activityResult action isn't returning the [Uri] once the image has been taken, so
     * we need to save the temporarily created URI in [savedStateHandle] until we handle its result
     */
    fun saveTemporarilyPhotoUri(uri: Uri?) {
        savedStateHandle["temporaryPhotoUri"] = uri
    }

    val temporaryPhotoUri: Uri?
        get() = savedStateHandle.get<Uri?>("temporaryPhotoUri")

    /**
     * [loadCameraMedia] is called when TakePicture or TakeVideo intent is returning a
     * successful result, that we set to the currentMediaUri property, which will
     * trigger to load the thumbnail in the UI
     */
    fun loadCameraMedia(uri: Uri) {
        savedStateHandle["currentMediaUri"] = uri
    }

    /**
     * We create a [Uri] where the image will be stored
     */
    suspend fun createPhotoUri(source: Source): Uri? {
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

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
     * We create a [Uri] where the camera will store the video
     */
    suspend fun createVideoUri(source: Source): Uri? {
        val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

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
     * content
     */
    @Suppress("BlockingMethodInNonBlockingContext")
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
}

/**
 * Check if the app can writes on the shared storage
 *
 * On Android 10 (API 29), we can add media to MediaStore without having to request the
 * [WRITE_EXTERNAL_STORAGE] permission, so we only check on pre-API 29 devices
 */
private fun checkMediaStorePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        true
    } else {
        checkSelfPermission(context, WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

enum class Source {
    CAMERA, INTERNET
}

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
