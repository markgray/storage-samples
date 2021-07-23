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
import androidx.fragment.app.commit
import com.example.android.ktfiles.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    /**
     * This is the [ActivityMainBinding] `ViewBinding` that is inflated from our layout file
     * layout/activity_main.xml which allows us to access views within it as kotlin properties.
     */
    private lateinit var binding: ActivityMainBinding

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val openDirectoryButton = binding.fabOpenDirectory
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

    fun showDirectoryContents(directoryUri: Uri) {
        supportFragmentManager.commit {
            val directoryTag = directoryUri.toString()
            val directoryFragment = DirectoryFragment.newInstance(directoryUri)
            replace(R.id.fragment_container, directoryFragment, directoryTag)
            addToBackStack(directoryTag)
        }
    }

    private fun openDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        resultLauncher.launch(intent)
    }
}
