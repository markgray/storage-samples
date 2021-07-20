/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.actionopendocument

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.commitNow

/**
 * TAG used for our fragment [ActionOpenDocumentFragment]
 */
const val DOCUMENT_FRAGMENT_TAG = "com.example.android.actionopendocument.tags.DOCUMENT_FRAGMENT"

/**
 * Simple activity to host [ActionOpenDocumentFragment].
 */
class MainActivity : AppCompatActivity() {

    /**
     * This is the `ConstraintLayout` with ID [R.id.no_document_view] in our UI which holds the
     * "Open File" [Button] which when clicked calls our [openDocumentPicker] method to allow the
     * user to pick a file to view, as well as a `ImageView` holding an icon drawable and a
     * `TextView` with the text: "Click "open" to view the contents of a PDF." Its visibility is set
     * to GONE and its `FrameLayout` parent [ViewGroup] is used to hold [ActionOpenDocumentFragment]
     * once a file is chosen to be displayed.
     */
    private lateinit var noDocumentView: ViewGroup

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file [R.layout.activity_main_real]. This layout
     * consists of a `FrameLayout` root view holding a `ConstraintLayout` displaying our startup
     * UI, which is replaced by an [ActionOpenDocumentFragment] once the user has selected a PDF
     * file to view.
     *
     * Next we initialize our [ViewGroup] field [noDocumentView] by finding the view with the ID
     * [R.id.no_document_view] (the `ConstraintLayout` mentioned above), then find the [Button] with
     * ID [R.id.open_file] (labeled "Open File") and set its [View.OnClickListener] to a lambda
     * which call our method [openDocumentPicker] to have it launch an [Intent.ACTION_OPEN_DOCUMENT]
     * activity to allow the user to pick a PDF to display.
     *
     * If our [Bundle] parameter [savedInstanceState] is not `null` we are being restarted after a
     * configuration change and the system will take care of restoring [ActionOpenDocumentFragment]
     * so we just return. Otherwise we call the [getSharedPreferences] method to retrieve a handle
     * to a [SharedPreferences] with the name [TAG] and apply the [let] extension function to it
     * to have it check the [SharedPreferences] for data stored under the key [LAST_OPENED_URI_KEY]
     * and if data is found use the [String] stored under that key to create an [Uri] to initialize
     * our variable `val documentUri` which we then pass to our method [openDocument] to have it
     * construct an instance of [ActionOpenDocumentFragment] to display that file in place of the
     * `ConstraintLayout` with ID [R.id.no_document_view] that our [ViewGroup] field [noDocumentView]
     * points to. If our [SharedPreferences] does not contain data under the key [LAST_OPENED_URI_KEY]
     * we leave things as they are.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     * down then this [Bundle] contains the data it most recently supplied in [onSaveInstanceState].
     * Note: Otherwise it is `null`.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_real)

        noDocumentView = findViewById(R.id.no_document_view)
        findViewById<Button>(R.id.open_file).setOnClickListener {
            openDocumentPicker()
        }

        if (savedInstanceState != null) return
        getSharedPreferences(TAG, Context.MODE_PRIVATE).let { sharedPreferences ->
            if (sharedPreferences.contains(LAST_OPENED_URI_KEY)) {
                val documentUri: Uri =
                    sharedPreferences.getString(LAST_OPENED_URI_KEY, null)?.toUri() ?: return@let
                openDocument(documentUri)
            }
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. You should place your menu
     * items in to the [Menu] parameter [menu]. This is only called once, the first time the options
     * menu is displayed. To update the menu every time it is displayed, see [onPrepareOptionsMenu].
     * We fetch a [MenuInflater] for our context and use it to inflate the menu layout file with ID
     * [R.menu.main] into our [Menu] parameter [menu]. It holds two [MenuItem]s:
     *  - [R.id.action_open] "Open..." calls our method [openDocumentPicker] to allow the user to
     *  choose a PDF file to display.
     *  - [R.id.action_info] "Info" displays an [AlertDialog] describing what this demo does.
     *
     * Finally we return `true` so that the [Menu] will be displayed.
     *
     * @param menu The options [Menu] in which you place your items.
     * @return You must return `true` for the menu to be displayed, if you return `false` it will
     * not be shown.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    /**
     * This hook is called whenever an item in your options menu is selected. We branch on the
     * `itemId` of our [MenuItem] parameter [item]:
     *  - [R.id.action_info] labeled "Info": we display an [AlertDialog] describing what this demo
     *  does, then return `true` to consume the event here.
     *  - [R.id.action_open] labeled "Open...": we call our method [openDocumentPicker] to allow the
     *  user to choose a PDF file to display, then return `true` to consume the event here.
     *  - all other `itemId`s we return the value returned by our super's implementation of
     *  `onOptionsItemSelected`.
     *
     * @param item The [MenuItem] that was selected.
     * @return Return `false` to allow normal menu processing to proceed, `true` to consume it here.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_info -> {
                AlertDialog.Builder(this)
                    .setMessage(R.string.intro_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return true
            }
            R.id.action_open -> {
                openDocumentPicker()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun openDocument(documentUri: Uri) {
        /**
         * Save the document to [SharedPreferences]. We're able to do this, and use the
         * uri saved indefinitely, because we called [ContentResolver.takePersistableUriPermission]
         * up in [onActivityResult].
         */
        getSharedPreferences(TAG, Context.MODE_PRIVATE).edit {
            putString(LAST_OPENED_URI_KEY, documentUri.toString())
        }

        val fragment = ActionOpenDocumentFragment.newInstance(documentUri)
        supportFragmentManager.commitNow {
            replace(R.id.container, fragment, DOCUMENT_FRAGMENT_TAG)
        }

        // Document is open, so get rid of the call to action view.
        noDocumentView.visibility = View.GONE
    }

    private var resultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                data?.data.also { documentUri ->

                    contentResolver.takePersistableUriPermission(
                        documentUri!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    openDocument(documentUri)
                }
            }
        }

    private fun openDocumentPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            /**
             * It's possible to limit the types of files by mime-type. Since this
             * app displays pages from a PDF file, we'll specify `application/pdf`
             * in `type`.
             * See [Intent.setType] for more details.
             */
            type = "application/pdf"

            /**
             * Because we'll want to use [ContentResolver.openFileDescriptor] to read
             * the data of whatever file is picked, we set [Intent.CATEGORY_OPENABLE]
             * to ensure this will succeed.
             */
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        resultLauncher.launch(intent)
    }
}

private const val TAG = "MainActivity"
private const val LAST_OPENED_URI_KEY =
    "com.example.android.actionopendocument.pref.LAST_OPENED_URI_KEY"

