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
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commitNow

/**
 * TAG used for our fragment [ActionOpenDocumentFragment]
 */
const val DOCUMENT_FRAGMENT_TAG: String = "com.example.android.actionopendocument.tags.DOCUMENT_FRAGMENT"

/**
 * Simple activity to host [ActionOpenDocumentFragment].
 */
class MainActivity : AppCompatActivity() {

    /**
     * This is the `ConstraintLayout` with ID `R.id.no_document_view` in our UI which holds the
     * "Open File" [Button] which when clicked calls our [openDocumentPicker] method to allow the
     * user to pick a file to view, as well as a `ImageView` holding an icon drawable and a
     * `TextView` with the text: "Click "open" to view the contents of a PDF." Its visibility is set
     * to GONE and its `FrameLayout` parent [ViewGroup] is used to hold [ActionOpenDocumentFragment]
     * once a file is chosen to be displayed.
     */
    private lateinit var noDocumentView: ViewGroup

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file `R.layout.activity_main_real`. This layout
     * consists of a `FrameLayout` root view holding a `ConstraintLayout` displaying our startup
     * UI, which is replaced by an [ActionOpenDocumentFragment] once the user has selected a PDF
     * file to view.
     *
     * Next we initialize our [ViewGroup] field [noDocumentView] by finding the view with the ID
     * `R.id.no_document_view` (the `ConstraintLayout` mentioned above), then find the [Button] with
     * ID `R.id.open_file` (labeled "Open File") and set its [View.OnClickListener] to a lambda
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
     * `ConstraintLayout` with ID `R.id.no_document_view` that our [ViewGroup] field [noDocumentView]
     * points to. If our [SharedPreferences] does not contain data under the key [LAST_OPENED_URI_KEY]
     * we leave things as they are.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     * down then this [Bundle] contains the data it most recently supplied in [onSaveInstanceState].
     * Note: Otherwise it is `null`.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_real)
        val rootView = findViewById<FrameLayout>(R.id.container)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Apply the insets as a margin to the view.
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = insets.left
                rightMargin = insets.right
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            // Return CONSUMED if you don't want want the window insets to keep passing
            // down to descendant views.
            WindowInsetsCompat.CONSUMED
        }

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
     * `R.menu.main` into our [Menu] parameter [menu]. It holds two [MenuItem]s:
     *  - `R.id.action_open` "Open..." calls our method [openDocumentPicker] to allow the user to
     *  choose a PDF file to display.
     *  - `R.id.action_info` "Info" displays an [AlertDialog] describing what this demo does.
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
     *  - `R.id.action_info` labeled "Info": we display an [AlertDialog] describing what this demo
     *  does, then return `true` to consume the event here.
     *  - `R.id.action_open` labeled "Open...": we call our method [openDocumentPicker] to allow the
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

    /**
     * Constructs a new instance of [ActionOpenDocumentFragment] to display the PDF document that is
     * accessible using our [Uri] parameter [documentUri] and adds it to our UI in the `FrameLayout`
     * with resource ID `R.id.container`. First we save the [String] version of [documentUri] in the
     * [SharedPreferences] field named [TAG] under the key [LAST_OPENED_URI_KEY]. Then we use the
     * method [ActionOpenDocumentFragment.newInstance] to construct a new instance of
     * [ActionOpenDocumentFragment] to retrieve and display the PDF file specified by our [Uri]
     * parameter [documentUri] and use it to initialize our variable `val fragment`. We use the
     * `FragmentManager.commitNow` extension method to have it run a lambda in a [FragmentTransaction]
     * which is automatically committed, with the lambda consisting of a [FragmentTransaction.replace]
     * command which replaces any existing fragment in the container with ID `R.id.container` with
     * `fragment`, using [DOCUMENT_FRAGMENT_TAG] as the tag name for the fragment.
     *
     * Finally now that the document is open we set the visibility of our [ViewGroup] field
     * [noDocumentView] which displays before we have a document to dispaly to GONE (just in
     * case we have not already done so).
     *
     * @param documentUri the [Uri] that points to the PDF document we are supposed to display.
     */
    private fun openDocument(documentUri: Uri) {
        /**
         * Save the document to [SharedPreferences]. We're able to do this, and use the
         * uri saved indefinitely, because we called [ContentResolver.takePersistableUriPermission]
         * up in [onActivityResult].
         */
        getSharedPreferences(TAG, Context.MODE_PRIVATE).edit {
            putString(LAST_OPENED_URI_KEY, documentUri.toString())
        }

        val fragment: ActionOpenDocumentFragment = ActionOpenDocumentFragment.newInstance(documentUri)
        supportFragmentManager.commitNow {
            replace(R.id.container, fragment, DOCUMENT_FRAGMENT_TAG)
        }

        // Document is open, so get rid of the call to action view.
        noDocumentView.visibility = View.GONE
    }

    /**
     * The [ActivityResultLauncher] launcher used to start the activity specified by the [Intent]
     * passed to its [ActivityResultLauncher.launch] method for the result returned by that activity,
     * with the [ActivityResult] result returned by that activity handed to the lambda argument of
     * [registerForActivityResult] for use by our activity. Our lamda callback argument checks to
     * make sure that the `resultCode` of the [ActivityResult] `result` is [Activity.RESULT_OK] and
     * if so initializes its [Intent] variable `val data` to the [Intent] in the the `data` property
     * of `result`. It then accesses the [Uri] stored in the `data` property of that `data` [Intent]
     * and uses the [also] extension function of that [Uri] to first take persistable URI permission
     * grant that has been offered for that [Uri] using the [ContentResolver.takePersistableUriPermission]
     * method, then it calls our [openDocument] method to have it open and display the PDF file pointed
     * to by the [Uri] in a new instance of [ActionOpenDocumentFragment].
     */
    private val resultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                data?.data.also { documentUri ->

                    contentResolver.takePersistableUriPermission(
                        documentUri ?: return@also,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    openDocument(documentUri)
                }
            }
        }

    /**
     * Launches a [Intent.ACTION_OPEN_DOCUMENT] activity to allow the user to pick a PDF file to
     * display. First we initialize our [Intent] variable `val intent` with a new instance whose
     * action is [Intent.ACTION_OPEN_DOCUMENT] (Allows the user to select and return one or more
     * existing documents, the system will display the various `DocumentsProvider` instances
     * installed on the device, letting the user interactively navigate through them). We execute
     * the [apply] extension function on this [Intent] in order to set its `type` to "application/pdf"
     * and to add the category [Intent.CATEGORY_OPENABLE] (indicates that the intent only wants URIs
     * that can be opened with [ContentResolver.openFileDescriptor]). Finally we call the method
     * [ActivityResultLauncher.launch] of our field [resultLauncher] to have it launch the activity
     * specified by `intent` and then to handle the [ActivityResult] returned to its callback in
     * order to display the PDF file chosen by the user.
     */
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

/**
 * File name of the [SharedPreferences] file that we use to store the [Uri] of that PDF file chosen.
 */
private const val TAG = "MainActivity"

/**
 * Key under which we store the [String] value of the [Uri] of the last PDF file chosen for display.
 */
private const val LAST_OPENED_URI_KEY =
    "com.example.android.actionopendocument.pref.LAST_OPENED_URI_KEY"

