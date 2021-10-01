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

    private val mOnClickListener = View.OnClickListener { v ->
        when (v.id) {
            R.id.share -> share()
        }
    }

    /**
     * Emits a sample share [Intent].
     */
    private fun share() {
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = "text/plain"
        sharingIntent.putExtra(Intent.EXTRA_TEXT, mEditBody.text.toString())
        // (Optional) If you want a preview title, set it with Intent.EXTRA_TITLE
        sharingIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.send_intent_title))

        // (Optional) if you want a preview thumbnail, create a content URI and add it
        // The system only supports content URIs
        val thumbnail = clipDataThumbnail
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
     *
     * For this to work, you need to configure a FileProvider in the project. We added it to the
     * AndroidManifest.xml file where we can configure it. We added the images path where we
     * save the image to the @xml/file_paths file which tells the FileProvider where we intend to
     * request content URIs.
     *
     * @return thumbnail ClipData object to set in the sharing Intent.
     */
    private val clipDataThumbnail: ClipData?
        get() = try {
            val contentUri = saveImageThumbnail()
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
     * @throws IOException if image couldn't be saved to the cache.
     * @return image content Uri
     */
    @Throws(IOException::class)
    private fun saveImageThumbnail(): Uri {
        val bm = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
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
        // Domain authority for our app FileProvider
        private const val FILE_PROVIDER_AUTHORITY = "com.example.android.sharingshortcuts.fileprovider"

        // Cache directory to store images
        // This is the same path specified in the @xml/file_paths and accessed from the AndroidManifest
        private const val IMAGE_CACHE_DIR = "images"

        // Name of the file to use for the thumbnail image
        private const val IMAGE_FILE = "image.png"
    }
}