/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.samples.filemanager

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

/**
 * The authority of the [FileProvider] we use to share files with other apps. A `provider` element
 * in the `application` element of our AndroidManifest declares a content provider component using
 * this authority, and we use [AUTHORITY] to build the [Uri] for the [File] we are to share.
 */
private const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.provider"

/**
 * Returns the mime type of its [String] parameter [url], where [url] is the string value of the
 * content [Uri] for a [File]. We use the [MimeTypeMap.getFileExtensionFromUrl] method to retrieve
 * the file extension (or an empty string if there is no extension) of our [String] parameter [url]
 * and initialize our [String] variable `val ext` to it. We retrieve the singleton instance of
 * [MimeTypeMap] and use its [MimeTypeMap.getMimeTypeFromExtension] to discern the MIME type for
 * the extension `ext` (defaulting to "text/plain" if it returns `null`) and return the MIME type
 * to the caller.
 *
 * @param url the string value of the content [Uri] for a [File] whose mime type we need to know.
 * @return the [String] representing the MIME type deduced from the extension of the [File].
 */
fun getMimeType(url: String): String {
    val ext = MimeTypeMap.getFileExtensionFromUrl(url)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "text/plain"
}

/**
 * Returns a [List] of [File] objects denoting the files in the directory that is denoted by our
 * [File] parameter [selectedItem]. We initialize our [List] of [File] variable `val rawFilesList`
 * to the [List] of [File] objects we create by filtering our all the hidden files from the [Array]
 * of [File] that is returned by the [File.listFiles] method of our [File] parameter [selectedItem].
 * If [selectedItem] is the primary shared/external storage directory we return `rawFilesList` if
 * it is not `null` or an empty read-only list if it is `null`. If [selectedItem] is NOT the primary
 * shared/external storage directory we prepend the abstract pathname ([File]) of [selectedItem]'s
 * parent directory to `rawFilesList` and return it (after again making sure the returned value is
 * an empty [List] instead of `null`).
 *
 * @param selectedItem the directory whose [File] entries we should return in a [List].
 * @return a [List] of the [File] entries of our directory parameter [selectedItem].
 */
fun getFilesList(selectedItem: File): List<File> {
    val rawFilesList: List<File>? = selectedItem.listFiles()?.filter { !it.isHidden }

    return if (selectedItem == Environment.getExternalStorageDirectory()) {
        rawFilesList?.toList() ?: listOf()
    } else {
        listOf(selectedItem.parentFile!!) + (rawFilesList?.toList() ?: listOf())
    }
}

/**
 * Returns the [String] which should be used to represent the parent directory of a directory. We
 * return the [String] with resource ID `R.string.go_parent_label` ("⬆️ Parent folder").
 *
 * @param activity the [AppCompatActivity] we should use to access resource strings.
 * @return the [String] which should be used to represent the parent directory.
 */
fun renderParentLink(activity: AppCompatActivity): String {
    return activity.getString(R.string.go_parent_label)
}

/**
 * Returns the [String] which should be used to represent the [File] parameter [file]. If our [File]
 * parameter [file] is a directory we return the formatted string produced by applying the format
 * in the resource [String] whose ID is `R.string.folder_item` ("🗂 %1$s") to the name property of
 * [file], otherwise we return the formatted string produced by applying the format in the resource
 * [String] whose ID is `R.string.file_item` ("📄 %1$s") to the name property of [file]
 *
 * @param activity the [AppCompatActivity] to use to access [String] resources.
 * @param file the [File] we want to represent with a [String].
 * @return the [String] which should be used to represent the [File] parameter [file].
 */
fun renderItem(activity: AppCompatActivity, file: File): String {
    return if (file.isDirectory) {
        activity.getString(R.string.folder_item, file.name)
    } else {
        activity.getString(R.string.file_item, file.name)
    }
}

/**
 * Creates and launches an [Intent] whose action is [Intent.ACTION_VIEW] to allow the user to "view"
 * our [File] parameter [selectedItem] in an activity that is appropriate for the MIME type of the
 * [File]. First we initialize our [Uri] variable `val uri` to the content uri returned by the method
 * [FileProvider.getUriForFile] for the [AUTHORITY] authority and our [File] parameter [selectedItem].
 * Then we initialize our [String] variable `val mime` to the MIME type returned by our [getMimeType]
 * method when given the string value or `uri`. We initialize our [Intent] variable `val intent` to
 * a new instance whose intent action is [Intent.ACTION_VIEW], add the [Intent.FLAG_GRANT_READ_URI_PERMISSION]
 * flag to `intent`, and set its data to `uri` and its type to `mime`. Then to save some typing we
 * use our return statement to call the  [AppCompatActivity.startActivity] method of our parameter
 * [activity] (why this is better than just falling off the end of the method is beyond me).
 *
 * @param activity the [AppCompatActivity] we use to retrieve the context of our `Application`
 * and also use for its [AppCompatActivity.startActivity] method.
 * @param selectedItem the [File] object whose [Uri] we pass to the activity we start.
 */
fun openFile(activity: AppCompatActivity, selectedItem: File) {
    // Get URI and MIME type of file
    val uri: Uri = FileProvider.getUriForFile(activity.applicationContext, AUTHORITY, selectedItem)
    val mime: String = getMimeType(uri.toString())

    // Open file with user selected app
    val intent = Intent(Intent.ACTION_VIEW)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.setDataAndType(uri, mime)
    return activity.startActivity(intent)
}
