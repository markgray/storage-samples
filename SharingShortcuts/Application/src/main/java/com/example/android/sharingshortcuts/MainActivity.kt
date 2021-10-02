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
package com.example.android.sharingshortcuts

import android.app.Activity
import android.content.ClipData
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Provides the landing screen of this sample. There is nothing particularly interesting here. All
 * the codes related to the Direct Share feature are in [SharingShortcutsManager].
 */
class MainActivity : Activity() {
    /**
     * The [EditText] in our UI with resource ID [R.id.body] which the user uses to type the message
     * he wants to share.
     */
    private lateinit var mEditBody: EditText

    /**
     * The instance of [SharingShortcutsManager] that provides the "Sharing Shortcuts" items to the
     * system using [ShortcutManagerCompat].
     */
    private lateinit var mSharingShortcutsManager: SharingShortcutsManager

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file [R.layout.activity_main]. It consists of a
     * vertical `LinearLayout` holding a [TextView] with the message: "This app demonstrates how to
     * implement Direct Share. Use some other app and share a text. For your convenience, you can
     * also use the input below to share the text" above an [EditText] where the user can enter their
     * message, with a "Share" [Button] at the bottom of the UI. Next we initialize our [EditText]
     * field [mEditBody] by finding the view with ID [R.id.body], and set the [View.OnClickListener]
     * of the view with ID [R.id.share] to our [View.OnClickListener] field [mOnClickListener].
     *
     * Finally we initialize our [SharingShortcutsManager] field [mSharingShortcutsManager] to a new
     * instance and call its [SharingShortcutsManager.pushDirectShareTargets] method to have it
     * build a [List] of [ShortcutInfoCompat] shortcuts and use the [ShortcutManagerCompat.addDynamicShortcuts]
     * method to publish that list of dynamic shortcuts.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mEditBody = findViewById(R.id.body)
        findViewById<View>(R.id.share).setOnClickListener(mOnClickListener)
        mSharingShortcutsManager = SharingShortcutsManager()
        mSharingShortcutsManager.pushDirectShareTargets(this)
    }

    /**
     * This is the [View.OnClickListener] that is used by the "Share" [Button] in our UI (resource
     * ID [R.id.share]). When the ID of the of [View] that was clicked is [R.id.share] we call our
     * [share] method to have it construct a "share" [Intent] and use it to create an [Intent] with
     * the action [Intent.ACTION_CHOOSER] whose activity it then launches so that the user can select
     * an activity to perform the "share" [Intent].
     */
    private val mOnClickListener = View.OnClickListener { v: View ->
        when (v.id) {
            R.id.share -> share()
        }
    }

    /**
     * Emits a sample share [Intent]. We initialize our [Intent] variable `val sharingIntent` to a
     * new instance whose action is [Intent.ACTION_SEND], set its MIME data type to "text/plain",
     * add the text from our [EditText] field [mEditBody] as an extra with the name [Intent.EXTRA_TEXT],
     * and add the string "Send message" as an extra with the name [Intent.EXTRA_TITLE]. Next we
     * initialize our [ClipData] variable `val thumbnail` to the value or our [clipDataThumbnail]
     * property. Its getter uses the [ContentResolver] to create and return a new ClipData holding a
     * content [Uri] pointing to a [Bitmap] copy of our launcher ICON that our [saveImageThumbnail]
     * method creates and writes to our application specific cache directory on the filesystem. If
     * `thumbnail` is not `null` we set the [ClipData] associated with the `sharingIntent` [Intent]
     * to `thumbnail` and set its flags to [Intent.FLAG_GRANT_READ_URI_PERMISSION].
     *
     * Finally we call the [startActivity] method to have it launch an activity using an [Intent]
     * whose action is [Intent.ACTION_CHOOSER] for an activity that will allow the user to select
     * an activity to execute the "sharing" [Intent] `sharingIntent`.
     */
    private fun share() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_TEXT, mEditBody.text.toString())
        // (Optional) If you want a preview title, set it with Intent.EXTRA_TITLE
        sharingIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.send_intent_title))

        // (Optional) if you want a preview thumbnail, create a content URI and add it
        // The system only supports content URIs
        val thumbnail: ClipData? = clipDataThumbnail
        if (thumbnail != null) {
            sharingIntent.clipData = thumbnail
            sharingIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(Intent.createChooser(sharingIntent, null))
    }

    /**
     * Get ClipData thumbnail object that needs to be passed in the Intent.
     * It stores the launcher icon in the cache and retrieves in a content URI.
     * The ClipData object is created with the URI we get from the FileProvider.
     *
     * For this to work, you need to configure a FileProvider in the project. We added it to the
     * AndroidManifest.xml file where we can configure it. We added the images path where we
     * save the image to the @xml/file_paths file which tells the [FileProvider] where we intend to
     * request content URIs.
     *
     * In our getter we initialize our [Uri] variable `val contentUri` to the content [Uri] that
     * is returned by our [saveImageThumbnail] method, then use the [ClipData.newUri] method to
     * create a new ClipData holding `contentUri` which we return as our value. This is wrapped
     * in a `try` block which catches [FileNotFoundException] and [IOException] in order to print
     * a stack trace to the standard error stream and return `null` as our value.
     *
     * @return thumbnail [ClipData] object to set in the sharing Intent.
     */
    private val clipDataThumbnail: ClipData?
        get() = try {
            val contentUri: Uri = saveImageThumbnail()
            ClipData.newUri(contentResolver, null, contentUri)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }

    /**
     * Save our Launcher image to the cache and return it as a content URI.
     *
     * IMPORTANT: This could trigger StrictMode. Do not do this in your app.
     * For the simplicity of the code sample, this is running on the Main thread
     * but these tasks should be done in a background thread.
     *
     * We initialize our [Bitmap] variable `val bm` to the [Bitmap] that the method
     * [BitmapFactory.decodeResource] decodes from our app's launcher image (resource ID
     * [R.mipmap.ic_launcher]). We initialize our [File] variable `val cachePath` to the
     * path to the [IMAGE_CACHE_DIR] ("images") directory in our application specific cache
     * directory on the filesystem. We call the [File.mkdirs] method of `cachePath` to create
     * that directory, including any necessary but nonexistent parent directories. We initialize
     * our [FileOutputStream] variable `val stream` to a file output stream to write to the file
     * with the name [IMAGE_FILE] ("image.png") in the `cachePath` directory. We call the
     * [Bitmap.compress] method of `bm` to have it write a [Bitmap.CompressFormat.PNG] compressed
     * version of itself to the [OutputStream] `stream`. We then close `stream`. Next we initialize
     * our [File] variable `val imagePath` to a new instance pointing to the [IMAGE_CACHE_DIR]
     * directory in our application specific cache directory, and our [File] variable `val newFile`
     * to a new instance pointing to our [IMAGE_FILE] file in the `imagePath` directory. Finally
     * we return the content [Uri] that the [FileProvider.getUriForFile] method produces for the
     * [File] `newFile` using [FILE_PROVIDER_AUTHORITY] as the authority of the [FileProvider]
     * defined in the `<provider>` element in our app's manifest.
     *
     * @throws IOException if image couldn't be saved to the cache.
     * @return image content Uri
     */
    @Throws(IOException::class)
    private fun saveImageThumbnail(): Uri {
        val bm: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        val cachePath = File(cacheDir, IMAGE_CACHE_DIR)
        cachePath.mkdirs()
        val stream = FileOutputStream("$cachePath/$IMAGE_FILE")
        bm.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        val imagePath = File(cacheDir, IMAGE_CACHE_DIR)
        val newFile = File(imagePath, IMAGE_FILE)
        return FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, newFile)
    }

    companion object {
        /**
         * Domain authority for our app FileProvider
         */
        private const val FILE_PROVIDER_AUTHORITY = "com.example.android.sharingshortcuts.fileprovider"

        /**
         * Cache directory to store images This is the same path specified in the @xml/file_paths
         * and accessed from the AndroidManifest
         */
        private const val IMAGE_CACHE_DIR = "images"

        /**
         * Name of the file to use for the thumbnail image
         */
        private const val IMAGE_FILE = "image.png"
    }
}