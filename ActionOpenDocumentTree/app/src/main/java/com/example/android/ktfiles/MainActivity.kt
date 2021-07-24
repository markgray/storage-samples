/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.ktfiles

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.example.android.ktfiles.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * This is the starting point for our directory display demo. Its [FloatingActionButton] allows the
 * user to launch a directory choser activity and then display the contents of the directory returned
 * from that activity.
 */
class MainActivity : AppCompatActivity() {

    /**
     * This is the [ActivityMainBinding] `ViewBinding` that is inflated from our layout file
     * layout/activity_main.xml which allows us to access views within it as kotlin properties.
     */
    private lateinit var binding: ActivityMainBinding

    /**
     * Called when the activity is starting.First we call our super's implementation of `onCreate`.
     * Then we use the [ActivityMainBinding.inflate] method with the LayoutInflater instance that
     * this Window retrieved from its Context to inflate our layout file [R.layout.activity_main]
     * into an [ActivityMainBinding] instance which we use to initialize our field [binding], and
     * set our content view to the outermost View in the associated layout file associated with
     * [ActivityMainBinding]. Next we call the [setSupportActionBar] method with the
     * [ActivityMainBinding.toolbar] property of [binding] to set that [Toolbar] to act as the
     * ActionBar for this Activity window. We initialize our [FloatingActionButton] variable
     * `val openDirectoryButton` to the [ActivityMainBinding.fabOpenDirectory] property of
     * [binding] then set its [View.OnClickListener] to a lambda that calls our [openDirectory]
     * method to have it launch an [Intent.ACTION_OPEN_DOCUMENT_TREE] activity to allow the user
     * to choose a directory to work with.
     *
     * Finally we have the FragmentManager for interacting with fragments associated with this
     * activity add a new lambda listener for changes to the fragment back stack which:
     *  - Initializes its [Boolean] variable `val directoryOpen` to `true` if the number of entries
     *  currently in the back stack of the FragmentManager is greater than 0.
     *  - has this activity's ActionBar display home as an "up" affordance if `directoryOpen` is `true`
     *  - has this activity's ActionBar include the application home affordance in the action bar if
     *  `directoryOpen` is `true`
     *  - if `directoryOpen` is `true` sets the visibility of `openDirectoryButton` to [View.GONE]
     *  (the contents of a directory chosen by the user is being displayed).
     *  - if `directoryOpen` is `false` sets the visibility of `openDirectoryButton` to [View.VISIBLE]
     *  (no directory is being displayed so allow the user to click `openDirectoryButton` so they can
     *  choose one).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val openDirectoryButton: FloatingActionButton = binding.fabOpenDirectory
        openDirectoryButton.setOnClickListener {
            openDirectory()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val directoryOpen = supportFragmentManager.backStackEntryCount > 0
            supportActionBar?.let { actionBar ->
                actionBar.setDisplayHomeAsUpEnabled(directoryOpen)
                actionBar.setDisplayShowHomeEnabled(directoryOpen)
            }

            if (directoryOpen) {
                openDirectoryButton.visibility = View.GONE
            } else {
                openDirectoryButton.visibility = View.VISIBLE
            }
        }
    }

    /**
     * This method is called whenever the user chooses to navigate Up within your application's
     * activity hierarchy from the action bar. We call the [FragmentManager.popBackStack] method
     * of the [FragmentManager] for interacting with fragments associated with this activity to
     * have it pop the top state off the back stack, and return `false` to indicate that this
     * Activity was not finished.
     *
     * @return `true` if Up navigation completed successfully and this Activity was finished,
     * `false` otherwise.
     */
    override fun onSupportNavigateUp(): Boolean {
        supportFragmentManager.popBackStack()
        return false
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
     * method, then it calls our [showDirectoryContents] method to have it open and display the
     * directory pointed to by the [Uri] in a new instance of [DirectoryFragment].
     */
    private val resultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                data?.data.also { directoryUri ->

                    contentResolver.takePersistableUriPermission(
                        directoryUri!!,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    showDirectoryContents(directoryUri)
                }
            }
        }

    /**
     * Called to have a new instance of [DirectoryFragment] constructed and added to our UI which
     * will display the directory pointed to by our [Uri] parameter [directoryUri]. We use the
     * [FragmentManager.commit] extension method on the [FragmentManager] for interacting with
     * fragments associated with this activity to have it execute a lambda whose receiver is a
     * [FragmentTransaction] then commit the [FragmentTransaction]. In the lambda we:
     *  - Initialize our [String] variable `val directoryTag` to the string value of [directoryUri].
     *  - Initialize our variable `val directoryFragment` to a new instance of [DirectoryFragment]
     *  that is constructed to display [directoryUri].
     *  - Execute the [FragmentTransaction.replace] method of our receiver to have it replace the
     *  contents of the container with ID [R.id.fragment_container] with `directoryFragment` using
     *  `directoryTag` as the fragment TAG.
     *  - Execute the [FragmentTransaction.addToBackStack] method of our receiver to have it add
     *  itself to the backstack with the name `directoryTag`.
     *
     * @param directoryUri the [Uri] returned from the [Intent.ACTION_OPEN_DOCUMENT_TREE] activity
     * that our [openDirectory] method starts which points to the directory that the user chose to
     * be displayed.
     */
    fun showDirectoryContents(directoryUri: Uri) {
        supportFragmentManager.commit {
            val directoryTag: String = directoryUri.toString()
            val directoryFragment = DirectoryFragment.newInstance(directoryUri)
            replace(R.id.fragment_container, directoryFragment, directoryTag)
            addToBackStack(directoryTag)
        }
    }

    /**
     * Called to launch an activity which has registered an intent filter in its AndroidManifest for
     * the action [Intent.ACTION_OPEN_DOCUMENT_TREE] in order to have the user select a directory to
     * be displayed. We initialize our [Intent] variable `val intent` with a new instance whose
     * action is [Intent.ACTION_OPEN_DOCUMENT_TREE] (Allows the user to pick a directory subtree.
     * When invoked, the system will display the various DocumentsProvider instances installed on
     * the device, letting the user navigate through them. Apps can fully manage documents within
     * the returned directory.) We then call the [ActivityResultLauncher.launch] method of our
     * [resultLauncher] field to have it launch the activity requested by `intent` for its result,
     * which is then handled in the lambda argument of the [registerForActivityResult] method used
     * to create [resultLauncher].
     */
    private fun openDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        resultLauncher.launch(intent)
    }
}
