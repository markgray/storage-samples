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
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.graygallery.R
import com.example.graygallery.utils.Source
import com.example.graygallery.utils.applyGrayscaleFilter
import com.example.graygallery.utils.copyImageFromStream
import com.example.graygallery.utils.generateFilename
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
val ACCEPTED_MIMETYPES: Array<String> = arrayOf("image/jpeg", "image/png")

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
     * folder) and posts a task to the main thread to set the value of our [_images] field to the
     * [List] of [File] pathnames it reads from the directory.
     *
     * We launch a new coroutine without blocking the current thread using the [CoroutineScope] tied
     * to this [AppViewModel], and in its lambda we launch a suspending block using the coroutine
     * context of [Dispatchers.IO] (suspending until it completes). The lambda of this block returns
     * the result of calling the [File.listFiles] method of [imagesFolder] and converting the [Array]
     * of [File] that it returns to a [List] of [File] and this value is used to initialize the [List]
     * of [File] variable `val images`. When the outer `viewModelScope` [CoroutineScope] resumes it
     * posts a task to the main thread to set the value of our [_images] field to `images`.
     */
    fun loadImages() {
        viewModelScope.launch {
            val images: List<File> = withContext(Dispatchers.IO) {
                imagesFolder.listFiles()!!.toList()
            }

            _images.postValue(images)
        }
    }

    /**
     * Saves its [Bitmap] parameter [bitmap] as a JPEG image in our [imagesFolder] directory. It is
     * used to save the [Bitmap] returned by the activity launched by the [TakePicturePreview]
     * `ActivityResultContract` of the [DashboardFragment.takePicture] field, which is executed
     * by the `OnClickListener` of the "Take picture" button of the UI of [DashboardFragment].
     *
     * We initialize our [File] variable `val imageFile` to a [File] created in our [imagesFolder]
     * directory whose name is the [String] returned by our [generateFilename] method for the enum
     * [Source.CAMERA] (it returns the [String] "camera-" concatenated with the string value of
     * the current time in milliseconds concatenated to the string ".jpg"). Then we initialize our
     * [FileOutputStream] variable `val imageStream` to a file output stream for writing to the file
     * `imageFile`.
     *
     * We launch a new coroutine without blocking the current thread using the [CoroutineScope] tied
     * to this [AppViewModel], and in its lambda we launch a suspending block using the coroutine
     * context of [Dispatchers.IO] (suspending until it completes). In this block we use a `try`
     * block intended to catch and log any [Exception] to initialize our [Bitmap] variable
     * `val grayscaleBitmap` to the [Bitmap] created by our `suspend` method [applyGrayscaleFilter]
     * from our [bitmap] parameter using the [Dispatchers.Default] `CoroutineDispatcher` and when
     * our block resumes we use the [Bitmap.compress] method of `grayscaleBitmap` to write a
     * compressed JPEG version of the bitmap to the outputstream `imageStream`. We then flush and
     * close `imageStream`. Finally we post a task to the main thread to set the value of our
     * [String] field [_notification] to the string "Camera image saved" (observers added to its
     * public [notification] "accessor" property in both [DashboardFragment] and [GalleryFragment]
     * will show a `Snackbar` whenever its [String] contents changes value).
     *
     * @param bitmap the [Bitmap] we are supposed to convert to a JPEG and then store in our
     * [imagesFolder] directory.
     */
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

    /**
     * Downloads an image from its [Uri] parameter [uri] and saves it in our [imagesFolder] directory.
     * We launch a new coroutine without blocking the current thread using the [CoroutineScope] tied
     * to this [AppViewModel], and in its lambda we launch a suspending block using the coroutine
     * context of [Dispatchers.IO] (suspending until it completes). In this block we fetch a
     * [ContentResolver] instance for our application's package and call its method
     * [ContentResolver.openInputStream] to open an [InputStream] to the content associated with our
     * content [Uri] parameter [uri] and if that is not `null` we use the [let] extension function
     * on that [InputStream] to call a function block with the [InputStream] as its value which calls
     * our [copyImageFromStream] method to copy the contents of the [InputStream] to our [imagesFolder]
     * directory. We then post a task to the main thread to set the value of our [String] field
     * [_notification] to the string "Image copied" (observers added to its public [notification]
     * "accessor" property in both [DashboardFragment] and [GalleryFragment] will show the [String]
     * in a `Snackbar` whenever its contents changes value).
     *
     * @param uri the [Uri] for the image we are to download and save.
     */
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

    /**
     * Downloads a random image from the URL [RANDOM_IMAGE_URL] ("https://source.unsplash.com/random/500x500")
     * and saves it in our [imagesFolder] directory. We launch a new coroutine without blocking the
     * current thread using the [CoroutineScope] tied to this [AppViewModel], and in its lambda we
     * initialize our [Request] variable `val request` to an instance whose URL target is our constant
     * [RANDOM_IMAGE_URL]. Then we launch a suspending block using the coroutine context of
     * [Dispatchers.IO] (suspending until it completes). In this block we initialize our [Response]
     * variable `val response` to the [Response] returned when we `execute` the [Call] returned by the
     * [OkHttpClient.newCall] method for `request`. If the [Response.body] `ResponseBody` of `response`
     * is not `null` we use the [let] extension function on it to call a function block with it as its
     * argument and in that block we:
     *  - Initialize our [File] variable `val imageFile` to a new [File] in our [imagesFolder] directory
     *  whose name is created by our [generateFilename] method for the argument [Source.INTERNET]
     *  (this name will be of the form: "internet-" concatenated to the string value of the current
     *  time in milliseconds, concatenated to the string ".jpg")
     *  - We set the content `imageFile` to the array of bytes in the [ByteArray] returned by the
     *  `bytes` method of `responseBody`
     *  - We then post a task to the main thread to set the value of our [String] field [_notification]
     *  to the string "Image downloaded" (observers added to its public [notification] "accessor"
     *  property in both [DashboardFragment] and [GalleryFragment] will show the [String] in a
     *  `Snackbar` whenever its contents changes value).
     *
     * If the [Response.isSuccessful] method of `response` returns `false` we post a task to the main
     * thread to set the value of our [String] field [_notification] to the string "Failed to download
     * image" to have [DashboardFragment] and [GalleryFragment] show that string in a `Snackbar`.
     */
    fun saveRandomImageFromInternet() {
        viewModelScope.launch {
            val request: Request = Request.Builder().url(RANDOM_IMAGE_URL).build()

            withContext(Dispatchers.IO) {
                val response: Response = httpClient.newCall(request).execute()

                @Suppress("UNNECESSARY_SAFE_CALL")
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

    /**
     * Deletes our [imagesFolder] directory and all of its children. We launch a new coroutine without
     * blocking the current thread using the [CoroutineScope] tied to this [AppViewModel], and in its
     * lambda we launch a suspending block using the coroutine context of [Dispatchers.IO] (suspending
     * until it completes). In this block we call the [File.deleteRecursively] method of [imagesFolder]
     * to delete the directory and all of its children, post a task to the main thread to set the value
     * of our [MutableLiveData] wrapped [List] of [File] abstract pathnames field [_images] to an empty
     * list, and post a task to the main thread to set the value of our [String] field [_notification]
     * to the string "Images cleared" (observers added to its public [notification] "accessor" property
     * in both [DashboardFragment] and [GalleryFragment] will show the [String] in a `Snackbar` whenever
     * its contents changes value).
     */
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

/**
 * Returns the [File] pathname for the folder in the directory holding application files which we
 * use to store our images (in our case: "/data/user/0/com.example.graygallery/files/images"),
 * creating that folder if it does not already exist.
 *
 * We initialize our [XmlResourceParser] variable `val xml` by using the [Resources.getXml] method of
 * a [Resources] instance for the application's package to retrieve an [XmlResourceParser] which we
 * can use to read the generic XML resource file whose ID is [R.xml.filepaths]. We initialize our
 * [Map] of [String] to [String] variable `val attributes` by using our [getAttributesFromXmlNode]
 * method to parse `xml` looking for the element name [FILEPATH_XML_KEY] ("files-path") and then to
 * extract the attribute name and value of all of its attributes to use as their key and value in the
 * [Map] it returns. We then initialize our [String] variable `val folderPath` to the value stored in
 * `attributes` under the key "path" (throwing an [IllegalStateException] if this is `null`). Finally
 * we return a [File] whose parent directory is the absolute path to the directory on the filesystem
 * where private files associated with this Context's application package are stored and whose child
 * path is `folderPath` using the [also] extension function to create the `folderPath` directory if
 * it does not already exist.
 *
 * This method is used to "lazily" initialize the [File] field [AppViewModel.imagesFolder].
 */
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
/**
 * Parses its [XmlResourceParser] parameter [xml] looking for the element whose name is our [String]
 * parameter [nodeName], and returns all of the attributes of that element in a [Map] of [String] to
 * [String] with the name of each attribute as the key and the value of that attribute as the value.
 *
 * While the event type of [xml] is not equal to [XmlResourceParser.END_DOCUMENT] (logical end of the
 * xml document) we check whether the event type of [xml] is [XmlResourceParser.START_TAG] (start tag
 * was read) and if so we check if the name of the current element is our [String] parameter [nodeName]
 * and if so we check if the number of attributes of the current start tag is equal to 0 and return
 * an empty [Map] if it is. Otherwise:
 *  - We initialize our [Map] of [String] to [String] variable `val attributes` to a new instance.
 *  - We loop from `index` 0 to the number of attributes of the current start tag setting the value
 *  of the `attributes` whose key is the attribute name of the local name of the specified attribute
 *  at index `index` in [xml] to the value of the attribute at index `index` in [xml].
 *  - When done with all of the attributes in [xml] we return `attributes` to the caller.
 *
 * If the event type of [xml] is NOT [XmlResourceParser.START_TAG], or the name of the current element
 * is NOT [nodeName] we call the [XmlResourceParser.next] method of [xml] to get the next parsing
 * event of [xml].
 *
 * @param xml the [XmlResourceParser] we are to parse for the attributes of the element [nodeName].
 * @param nodeName the name of the element whose attributes we are to extract and return.
 * @return a [Map] of [String] to [String] of the attribute names and their values for the element
 * in [xml] whose name is [nodeName].
 */
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
