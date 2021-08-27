/*
 * Copyright 2020 The Android Open Source Project
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

package com.example.graygallery.ui

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.graygallery.R
import com.example.graygallery.utils.Source
import com.example.graygallery.utils.copyImageFromStream
import com.example.graygallery.utils.generateFilename
import com.example.graygallery.utils.applyGrayscaleFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * This is the element name in our xml/filepaths.xml file that contains the "path" attribute that
 * specifies the path of the folder (in the directory on the filesystem where files created with
 * `openFileOutput` are stored) that we use to store the image files we wish to share. In our case
 * the "path" attribute value of the "files-path" element is "images/".
 */
private const val FILEPATH_XML_KEY = "files-path"

/**
 * The URL that we use to fetch random images from the Internet.
 */
private const val RANDOM_IMAGE_URL = "https://source.unsplash.com/random/500x500"

/**
 * This is used as the "input" [Array] of [String] when launching the [ActivityResultLauncher]
 * field `selectPicture` of [DashboardFragment] when the [R.id.selectPicture] button (labeled
 * "Select picture") is clicked in the UI of [DashboardFragment].
 */
val ACCEPTED_MIMETYPES = arrayOf("image/jpeg", "image/png")

/**
 * This [AndroidViewModel] is used by both [DashboardFragment] and [GalleryFragment].
 */
@Suppress("BlockingMethodInNonBlockingContext")
class AppViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * This is the [OkHttpClient] instance that is used by our [saveRandomImageFromInternet] method
     * to download a random image from the [RANDOM_IMAGE_URL] website.
     */
    private val httpClient by lazy { OkHttpClient() }

    /**
     * The application [Context]. We use it whenever we require a [Context]. In our case our
     * [copyImageFromUri] method uses it to retrieve a [ContentResolver] instance for our
     * application's package, and our [File] field [imagesFolder] uses it when it calls our
     * [getImagesFolder] method to get the path to our "images/" folder.
     */
    private val context: Context
        get() = getApplication()

    /**
     * The [MutableLiveData] wrapped [String] that is displayed in a `Snackbar` by [DashboardFragment]
     * or [GalleryFragment] whenever it changes value. `private` to prevent modification by other
     * classes, `public` read only access is provided by our [notification] field.
     */
    private val _notification = MutableLiveData<String>()
    /**
     * Public read only access to our [_notification] field. Observers are added to it in the
     * `onCreateView` overrides of both [DashboardFragment] and [GalleryFragment] which will
     * show a `Snackbar` whenever its [String] contents changes value.
     */
    val notification: LiveData<String>
        get() = _notification

    /**
     * This is the path to our "images/" folder in the directory on the filesystem where private
     * files associated with this application are stored. We use it to store our images in.
     */
    private val imagesFolder: File by lazy { getImagesFolder(context) }

    /**
     * The [MutableLiveData] wrapped [List] of [File] abstract pathnames denoting the files in our
     * "images/" folder. Private to prevent modification by other classes, public read only access
     * is provided by our [images] field.
     */
    private val _images = MutableLiveData(emptyList<File>())
    /**
     * Public read only access to our [images] field. An observer is added to it in the `onCreateView`
     * override of [GalleryFragment] whose lambda will submit the [List] to the adapter of the
     * `RecyclerView` in the UI of [GalleryFragment] to be diffed, and displayed whenever it changes
     * value.
     */
    val images: LiveData<List<File>>
        get() = _images

    /**
     * Reads the directory contents of our [File] field [imagesFolder] (the path to our "images/"
     * folder) and posts a task to a main thread to set the value of our [_images] field to the
     * [List] of [File] pathnames it reads from the directory.
     */
    fun loadImages() {
        viewModelScope.launch {
            val images: List<File> = withContext(Dispatchers.IO) {
                imagesFolder.listFiles()!!.toList()
            }

            _images.postValue(images)
        }
    }

    fun saveImageFromCamera(bitmap: Bitmap) {
        val imageFile = File(imagesFolder, generateFilename(Source.CAMERA))
        val imageStream = FileOutputStream(imageFile)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val grayscaleBitmap = withContext(Dispatchers.Default) {
                        applyGrayscaleFilter(bitmap)
                    }
                    grayscaleBitmap.compress(Bitmap.CompressFormat.JPEG, 100, imageStream)
                    imageStream.flush()
                    imageStream.close()

                    _notification.postValue("Camera image saved")

                } catch (e: Exception) {
                    Log.e(javaClass.simpleName, "Error writing bitmap", e)
                }
            }
        }
    }

    fun copyImageFromUri(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.let {
                    // TODO: Apply grayscale filter before saving image
                    copyImageFromStream(it, imagesFolder)
                    _notification.postValue("Image copied")
                }
            }
        }
    }

    fun saveRandomImageFromInternet() {
        viewModelScope.launch {
            val request = Request.Builder().url(RANDOM_IMAGE_URL).build()

            withContext(Dispatchers.IO) {
                val response = httpClient.newCall(request).execute()

                response.body?.let { responseBody ->
                    val imageFile = File(imagesFolder, generateFilename(Source.INTERNET))
                    // TODO: Apply grayscale filter before saving image
                    imageFile.writeBytes(responseBody.bytes())
                    _notification.postValue("Image downloaded")
                }

                if (!response.isSuccessful) {
                    _notification.postValue("Failed to download image")
                }
            }
        }
    }

    fun clearFiles() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                imagesFolder.deleteRecursively()
                _images.postValue(emptyList())
                _notification.postValue("Images cleared")
            }
        }
    }
}

private fun getImagesFolder(context: Context): File {
    val xml: XmlResourceParser = context.resources.getXml(R.xml.filepaths)

    val attributes: Map<String, String> = getAttributesFromXmlNode(xml, FILEPATH_XML_KEY)

    val folderPath: String = attributes["path"]
        ?: error("You have to specify the sharable directory in res/xml/filepaths.xml")

    return File(context.filesDir, folderPath).also {
        if (!it.exists()) {
            it.mkdir()
        }
    }
}

// TODO: Make the function suspend
@Suppress("SameParameterValue")
private fun getAttributesFromXmlNode(
    xml: XmlResourceParser,
    nodeName: String
): Map<String, String> {
    while (xml.eventType != XmlResourceParser.END_DOCUMENT) {
        if (xml.eventType == XmlResourceParser.START_TAG) {
            if (xml.name == nodeName) {
                if (xml.attributeCount == 0) {
                    return emptyMap()
                }

                val attributes = mutableMapOf<String, String>()

                for (index in 0 until xml.attributeCount) {
                    attributes[xml.getAttributeName(index)] = xml.getAttributeValue(index)
                }

                return attributes
            }
        }

        xml.next()
    }

    return emptyMap()
}
