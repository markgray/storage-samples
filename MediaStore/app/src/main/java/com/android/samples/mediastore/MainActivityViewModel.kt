/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.samples.mediastore

import android.app.Application
import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * The [AndroidViewModel] view model used by our application.
 */
class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * The [MutableLiveData] wrapped [List] of [MediaStoreImage] that serves as our dataset. Private
     * to prevent other classes from changing it, read-only access is provided by our [images] field.
     * Our [loadImages] method posts the [List] of [MediaStoreImage] objects that our [queryImages]
     * method builds from the [Cursor] returned from the [ContentResolver.query] method for the [Uri]
     * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] ("content://media/external/images/media").
     */
    private val _images = MutableLiveData<List<MediaStoreImage>>()

    /**
     * Public read-only access to our [_images] field. An observer is added to it in the `onCreate`
     * override of [MainActivity] whose lambda calls the [ListAdapter.submitList] method of the
     * `GalleryAdapter` feeding views to its [RecyclerView] with the [List] value whenever it changes
     * value.
     */
    val images: LiveData<List<MediaStoreImage>> get() = _images

    /**
     * The [ContentObserver] we register in our [loadImages] method to receive call backs for any
     * changes that occur in the content of the `Uri` [MediaStore.Images.Media.EXTERNAL_CONTENT_URI]
     * ("content://media/external/images/media"). The lambda that is executed when this occurs is
     * a call to [loadImages] to reload our dataset.
     */
    private var contentObserver: ContentObserver? = null

    /**
     * When an attempt to delete an image fails because of a [SecurityException] this serves as a
     * signal to the Activity that it needs to request permission and try the delete again if
     * permission is granted. It is set to the [MediaStoreImage] argument to our [performDeleteImage]
     * method when the method catches [SecurityException] when it tries to delete the image using
     * the [ContentResolver.delete] method.
     */
    private var pendingDeleteImage: MediaStoreImage? = null

    /**
     * This [MutableLiveData] wrapped [IntentSender] is built to hold the [PendingIntent] that the
     * [RecoverableSecurityException] suggests as the primary action to involve the end user to
     * recover from the exception. It is set by our [performDeleteImage] method when it catches a
     * [RecoverableSecurityException] to a [IntentSender] object that wraps the existing sender of
     * the [PendingIntent] that is the action intent of the primary action that will initiate the
     * recovery from the [RecoverableSecurityException]. It is private to prevent other classes from
     * modifying it, public read-only access is provided by our [permissionNeededForDelete] field.
     */
    private val _permissionNeededForDelete = MutableLiveData<IntentSender?>()

    /**
     * Public read-only access to our [_permissionNeededForDelete] field. An observer is added to it
     * in the `onCreate` override of [MainActivity] whose lambda launches the activity described by
     * the [IntentSender] for its result whenever it changes to a non-`null` value.
     */
    val permissionNeededForDelete: LiveData<IntentSender?> = _permissionNeededForDelete

    /**
     * Performs a one shot load of images from [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] into
     * our [LiveData] wrapped [List] of [MediaStoreImage] field [_images]. We launch a new coroutine
     * without blocking the current thread using the [CoroutineScope] tied to this `ViewModel`. The
     * coroutine block consists of a lambda which:
     *  - Initializes our [List] of [MediaStoreImage] variable `val imageList` to the [List] returned
     *  by our [queryImages] method when it queries a [ContentResolver] instance for our application's
     *  package for the [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] content [Uri] and builds a
     *  [MediaStoreImage] object for each of the rows of the [Cursor] returned.
     *  - Posts a task to a main thread to set value of our [MutableLiveData] wrapped [List] of
     *  [MediaStoreImage] field [_images] to `imageList`.
     *  - If our [ContentObserver] field [contentObserver] is `null` we set it to an instance that
     *  is registered to observe [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] for changes and to
     *  execute a lambda which calls this [loadImages] method again when the content of the [Uri]
     *  changes.
     *
     * This is called from the `showImages` method of [MainActivity], and by the [ContentObserver]
     * that we register.
     */
    fun loadImages() {
        viewModelScope.launch {
            val imageList: List<MediaStoreImage> = queryImages()
            _images.postValue(imageList)

            if (contentObserver == null) {
                contentObserver = getApplication<Application>().contentResolver.registerObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                ) {
                    loadImages()
                }
            }
        }
    }

    /**
     * Requests the [MediaStore] to delete the image associated with our [MediaStoreImage] parameter
     * [image]. We launch a new coroutine without blocking the current thread using the [CoroutineScope]
     * tied to this `ViewModel`. The coroutine block consists of a lambda which calls our `suspend`
     * function [performDeleteImage] with our parameter [image].
     *
     * It is called from our [deletePendingImage] method and from the [MaterialAlertDialogBuilder]
     * shown by the `deleteImage` method of [MainActivity].
     *
     * @param image the [MediaStoreImage] containing the information needed to delete the image (the
     * URL of the row to delete is in its [MediaStoreImage.contentUri] property, and its `_ID` is in
     * in its [MediaStoreImage.id] property).
     */
    fun deleteImage(image: MediaStoreImage) {
        viewModelScope.launch {
            performDeleteImage(image)
        }
    }

    /**
     * This method is called upon successful return from the activity launched to have the system
     * ask the user for permission to delete the file associated with the [MediaStoreImage] field
     * [pendingDeleteImage]. If [pendingDeleteImage] is not `null` we use the [let] extension
     * function to set it to `null` and to call our [deleteImage] method to delete the image
     * associated with it.
     *
     * It is called in the lambda of the [ActivityResultLauncher] of [IntentSenderRequest] that is
     * registered to initialize the `requestPermissionToDelete` field of [MainActivity], and that
     * [ActivityResultLauncher] is launched by an observer of our [permissionNeededForDelete] field
     * when it changes to a non-`null` value.
     */
    fun deletePendingImage() {
        pendingDeleteImage?.let { image ->
            pendingDeleteImage = null
            deleteImage(image)
        }
    }

    /**
     * Queries a [ContentResolver] instance for our application's package for all of the images that
     * the [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] content [Uri] returns in its [Cursor] and
     * converts that [Cursor] to a [List] of [MediaStoreImage] objects which it returns.
     *
     * @return a [List] of [MediaStoreImage] objects which can be used to reference all of the images
     * that [MediaStore] is aware of.
     */
    private suspend fun queryImages(): List<MediaStoreImage> {
        val images = mutableListOf<MediaStoreImage>()

        /**
         * Working with [ContentResolver]s can be slow, so we'll do this off the main
         * thread inside a coroutine.
         */
        withContext(Dispatchers.IO) {

            /**
             * A key concept when working with Android [ContentProvider]s is something called
             * "projections". A projection is the list of columns to request from the provider,
             * and can be thought of (quite accurately) as the "SELECT ..." clause of a SQL
             * statement.
             *
             * It's not _required_ to provide a projection. In this case, one could pass `null`
             * in place of `projection` in the call to [ContentResolver.query], but requesting
             * more data than is required has a performance impact.
             *
             * For this sample, we only use a few columns of data, and so we'll request just a
             * subset of columns.
             */
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            )

            /**
             * The `selection` is the "WHERE ..." clause of a SQL statement. It's also possible
             * to omit this by passing `null` in its place, and then all rows will be returned.
             * In this case we're using a selection based on the date the image was taken.
             *
             * Note that we've included a `?` in our selection. This stands in for a variable
             * which will be provided by the next variable.
             */
            val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"

            /**
             * The `selectionArgs` is a list of values that will be filled in for each `?`
             * in the `selection`.
             */
            val selectionArgs = arrayOf(
                // Release day of the G1. :)
                dateToTimestamp(day = 22, month = 10, year = 2008).toString()
            )

            /**
             * Sort order to use. This can also be null, which will use the default sort
             * order. For [MediaStore.Images], the default sort order is ascending by date taken.
             */
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            getApplication<Application>().contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor: Cursor ->

                /**
                 * In order to retrieve the data from the [Cursor] that's returned, we need to
                 * find which index matches each column that we're interested in.
                 *
                 * There are two ways to do this. The first is to use the method
                 * [Cursor.getColumnIndex] which returns -1 if the column ID isn't found. This
                 * is useful if the code is programmatically choosing which columns to request,
                 * but would like to use a single method to parse them into objects.
                 *
                 * In our case, since we know exactly which columns we'd like, and we know
                 * that they must be included (since they're all supported from API 1), we'll
                 * use [Cursor.getColumnIndexOrThrow]. This method will throw an
                 * [IllegalArgumentException] if the column named isn't found.
                 *
                 * In either case, while this method isn't slow, we'll want to cache the results
                 * to avoid having to look them up for each row.
                 */
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

                Log.i(TAG, "Found ${cursor.count} images")
                while (cursor.moveToNext()) {

                    // Here we'll use the column indexs that we found above.
                    val id = cursor.getLong(idColumn)
                    val dateModified =
                        Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))
                    val displayName = cursor.getString(displayNameColumn)


                    /**
                     * This is one of the trickiest parts:
                     *
                     * Since we're accessing images (using
                     * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI], we'll use that
                     * as the base URI and append the ID of the image to it.
                     *
                     * This is the exact same way to do it when working with [MediaStore.Video] and
                     * [MediaStore.Audio] as well. Whatever `Media.EXTERNAL_CONTENT_URI` you
                     * query to get the items is the base, and the ID is the document to
                     * request there.
                     */
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val image = MediaStoreImage(id, displayName, dateModified, contentUri)
                    images += image

                    // For debugging, we'll output the image objects we create to logcat.
                    Log.v(TAG, "Added image: $image")
                }
            }
        }

        Log.v(TAG, "Found ${images.size} images")
        return images
    }

    /**
     * Has the [MediaStore] delete the image associated with our [MediaStoreImage] parameter [image].
     * We use the [CoroutineContext] of [Dispatchers.IO] to call a suspending block which consists
     * of a `try` block intended to catch and deal with [SecurityException] and in the `try` block
     * we use a [ContentResolver] instance for our application's package to delete the row whose URL
     * is the [MediaStoreImage.contentUri] property of our [image] parameter, filtering for rows where
     * the [MediaStore.Images.Media._ID] column consists of the [String] value of the [MediaStoreImage.id]
     * property of [image].
     *
     * If we `catch` [SecurityException] we check if [Build.VERSION.SDK_INT] (SDK version of the
     * software currently running on this hardware device) is greater than or equal to
     * [Build.VERSION_CODES.Q] and if so we initialize our [RecoverableSecurityException] variable
     * `val recoverableSecurityException` by casting the [SecurityException] if possible (rethrowing
     * the [SecurityException] if it is not). We then set our [pendingDeleteImage] field to our
     * parameter [image], and post a task to the main thread to set our [IntentSender] field
     * [_permissionNeededForDelete] to a [IntentSender] that wraps the sender of the action
     * [PendingIntent] of the primary action that will initiate recovery from the
     * [RecoverableSecurityException] contained in `recoverableSecurityException`.
     *
     * If on the other hand [Build.VERSION.SDK_INT] is less than [Build.VERSION_CODES.Q] we just
     * rethrow the [SecurityException].
     *
     * @param image the [MediaStoreImage] containing the information needed to delete the image
     * associated with it from the file system.
     */
    private suspend fun performDeleteImage(image: MediaStoreImage) {
        withContext(Dispatchers.IO) {
            try {
                /**
                 * In [Build.VERSION_CODES.Q] and above, it isn't possible to modify
                 * or delete items in MediaStore directly, and explicit permission
                 * must usually be obtained to do this.
                 *
                 * The way it works is the OS will throw a [RecoverableSecurityException],
                 * which we can catch here. Inside there's an [IntentSender] which the
                 * activity can use to prompt the user to grant permission to the item
                 * so it can be either updated or deleted.
                 */
                getApplication<Application>().contentResolver.delete(
                    image.contentUri,
                    "${MediaStore.Images.Media._ID} = ?",
                    arrayOf(image.id.toString())
                )
            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException =
                        securityException as? RecoverableSecurityException
                            ?: throw securityException

                    // Signal to the Activity that it needs to request permission and
                    // try the delete again if it succeeds.
                    pendingDeleteImage = image
                    _permissionNeededForDelete.postValue(
                        recoverableSecurityException.userAction.actionIntent.intentSender
                    )
                } else {
                    throw securityException
                }
            }
        }
    }

    /**
     * Convenience method to convert a day/month/year date into a UNIX timestamp.
     *
     * @param day the day of the month (1-31)
     * @param month the month (1-12)
     * @param year the year
     * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT for the date specified
     * by our parameters.
     */
    @Suppress("SameParameterValue") // Suggested change would make method less reusable
    private fun dateToTimestamp(day: Int, month: Int, year: Int): Long =
        SimpleDateFormat("dd.MM.yyyy", Locale.US).let { formatter ->
            TimeUnit.MICROSECONDS.toSeconds(formatter.parse("$day.$month.$year")?.time ?: 0)
        }

    /**
     * Since we register a [ContentObserver], we want to unregister this when the `ViewModel`
     * is being released. If our [ContentObserver] field [contentObserver] is not `null` we use
     * the [let] extension function to call the [ContentResolver.unregisterContentObserver] method
     * of a [ContentResolver] instance for our application's package with `it` as the argument.
     */
    override fun onCleared() {
        contentObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }
}

/**
 * Convenience extension method to register a [ContentObserver] given a lambda. We initialize our
 * [ContentObserver] variable `val contentObserver` to a new instance which uses a [Handler] which
 * uses the application's main [Looper] to run its [ContentObserver.onChange] method on. We override
 * [ContentObserver.onChange] to call our [observer] lambda with the [Boolean] parameter of `onChange`
 * (which is ignored by the lambda in our case). Next we call the [ContentResolver.registerContentObserver]
 * method with our [Uri] parameter [uri] as the [Uri] to watch for changes, `true` for its [Boolean]
 * `notifyForDescendants` parameter (when `true`, the observer will also be notified whenever a change
 * occurs to the URI's descendants in the path hierarchy), and `contentObserver` as the object that
 * receives callbacks when changes occur. Finally we return `contentObserver` to the caller.
 *
 * @param uri the [Uri] to watch for changes.
 * @param observer a lambda which the [ContentObserver] should execute when changes occur to the
 * [Uri] parameter [uri]
 * @return a [ContentObserver] configured to watch the [Uri] parameter [uri] for changes and to
 * execute the [observer] parameter lambda when they occur.
 */
@Suppress("SameParameterValue")
private fun ContentResolver.registerObserver(
    uri: Uri,
    observer: (selfChange: Boolean) -> Unit
): ContentObserver {
    val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            observer(selfChange)
        }
    }
    registerContentObserver(uri, true, contentObserver)
    return contentObserver
}


/**
 * TAG used for logging
 */
private const val TAG = "MainActivityVM"