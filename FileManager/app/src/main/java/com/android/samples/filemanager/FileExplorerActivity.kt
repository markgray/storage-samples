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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStorageDirectory
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.android.samples.filemanager.databinding.ActivityFileExplorerBinding
import java.io.File

/**
 * This used to be used as the request code for the call to `startActivityForResult` that was used
 * to request the permission `Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION` but startActivity
 * is all that is needed since `onResume` handles the return instead of `onActivityResult`
 */
@Suppress("unused")
const val MANAGE_EXTERNAL_STORAGE_PERMISSION_REQUEST = 1
/**
 * This is the request code that is used in the call to [ActivityCompat.requestPermissions] that is
 * used on devices running Android versions older than R.
 */
const val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST = 2

/**
 * This sample demonstrates how to create a simple File Manager application working from
 * API 19 (KitKat) to API 30 (Android 11).
 */
class FileExplorerActivity : AppCompatActivity() {
    /**
     * Flag used to indicate whether our app has the MANAGE_EXTERNAL_STORAGE_PERMISSION permission:
     * [Manifest.permission.READ_EXTERNAL_STORAGE] for Api19 or "android:manage_external_storage"
     * (the hidden `AppOpsManager.OPSTR_MANAGE_EXTERNAL_STORAGE`) for Api30 and above.
     */
    private var hasPermission: Boolean = false

    /**
     * The `ViewBinding` which is inflated from the layout file layout/activity_file_explorer.xml
     * which we use as our UI. It consists of a `ConstraintLayout` root view holding at its top a
     * `MaterialToolbar`, with two `LinearLayout` and a `ListView` occupying the same space below
     * the `MaterialToolbar` whose visisbility is switched between "visible" and "gone" depending
     * on the state of the app at the moment. The `LinearLayout` with ID [R.id.rationaleView] is
     * visible when the app does not have permission to access the file system and it holds a
     * `TextView` explaining why it needs the user's permission to access the file system and a
     * `Button` (labled "Give Permission") which when clicked will call our [requestStoragePermission]
     * method to allow the user to grant us that permission. The `LinearLayout` with ID
     * [R.id.legacyStorageView] is visible when the app is running on an Android "Q" device and
     * tells the user that he needs to enable the `requestLegacyExternalStorage` flag in the app's
     * AndroidManifest.xml file. The `ListView` with ID [R.id.filesTreeView] is visible when the app
     * has a directory listing it can display in it.
     */
    private lateinit var binding: ActivityFileExplorerBinding

    /**
     * The [File] object of the directory that the user has chosen to have displayed in the `ListView`
     * with ID [R.id.filesTreeView].
     */
    private lateinit var currentDirectory: File

    /**
     * [List] of all of the [File] objects in the directory [currentDirectory].
     */
    private lateinit var filesList: List<File>

    /**
     * The [ArrayAdapter] that feeds views to the `ListView` with ID [R.id.filesTreeView]. Its
     * dataset is an array of [String] which holds the value of [File.getName] (kotlin `name`
     * property) of all of the [File] objects in [filesList].
     */
    private lateinit var adapter: ArrayAdapter<String>

    /**
     * Called when the activity is starting. First we call our super's implementaton of `onCreate`.
     * We then initialize our [ActivityFileExplorerBinding] field [binding] by having the
     * [ActivityFileExplorerBinding.inflate] method use the [LayoutInflater] instance that this
     * Window retrieved from its [Context] to inflate and bind to its associated layout file
     * (layout/activity_file_explorer.xml). We then have the `MaterialToolbar` in [binding] with
     * ID [R.id.toolbar] inflate the menu resource with ID [R.menu.file_manager_menu] into itself
     * (it consists of a single `item` with the title "Settings"). Next we set our content view to
     * the outermost View in the associated layout file of [binding] (its `root` property). Finally
     * we call our [setupUi] method to have it finish the initialization and configuration of the
     * views in [binding].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use this.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFileExplorerBinding.inflate(layoutInflater)
        binding.toolbar.inflateMenu(R.menu.file_manager_menu)
        setContentView(binding.root)

        setupUi()
    }

    /**
     * Called after [onRestoreInstanceState], [onRestart], or [onPause], for your activity to start
     * interacting with the user. This is an indicator that the activity became active and ready to
     * receive input. It is on top of an activity stack and visible to user. First we call our super's
     * implementation of [onResume], then we set our [Boolean] field [hasPermission] to the value that
     * is returned from our [checkStoragePermission] method when it checks whether we have permission
     * to access shared storage on the device. If [hasPermission] is `true` we check whether the
     * device is running Android Q and if so we check whether [Environment.isExternalStorageLegacy]
     * is `false` which indicates that the shared/external storage media is NOT a legacy view that
     * includes files not owned by the app which needs to be enabled in the AndroidManifest file for
     * Android Q using the `requestLegacyExternalStorage` flag, so we change the visibility of the
     * [ActivityFileExplorerBinding.rationaleView] `LinearLayout` of [binding] to GONE, and change
     * the visibility of the [ActivityFileExplorerBinding.legacyStorageView] `LinearLayout` of [binding]
     * to VISIBLE to inform the user that he needs to enable the `requestLegacyExternalStorage` flag
     * in order for the app to work on Q and then we return. If we are not running on Q we set the
     * visibility of the [ActivityFileExplorerBinding.rationaleView] `LinearLayout` of [binding] to
     * GONE, and the  visibility of the [ActivityFileExplorerBinding.filesTreeView] `ListView` of
     * [binding] to VISIBLE then call our [open] method with the [File] returned by the method
     * [getExternalStorageDirectory] (the primary shared/external storage directory) to have [open]
     * read the [File] entries in that directory into our [List] of [File] field [filesList] and then
     * display their names in the [ActivityFileExplorerBinding.filesTreeView] `ListView` of [binding].
     *
     * If [hasPermission] is `false` we set the visibility of the [ActivityFileExplorerBinding.rationaleView]
     * `LinerLayout` to VISIBLE, and the visibility of the [ActivityFileExplorerBinding.filesTreeView]
     * to GONE in order to tell the user that they need to give us permission to access shared storage
     * by clicking the "Give Permission" button in the `LinearLayout`.
     */
    override fun onResume() {
        super.onResume()

        hasPermission = checkStoragePermission(this)
        if (hasPermission) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                if (!Environment.isExternalStorageLegacy()) {
                    binding.rationaleView.visibility = View.GONE
                    binding.legacyStorageView.visibility = View.VISIBLE
                    return
                }
            }

            binding.rationaleView.visibility = View.GONE
            binding.filesTreeView.visibility = View.VISIBLE

            // TODO: Use getStorageDirectory instead https://developer.android.com/reference/android/os/Environment.html#getStorageDirectory()
            @Suppress("DEPRECATION") // See ActionOpenDocumentTree for modern way to do this.
            open(getExternalStorageDirectory())
        } else {
            binding.rationaleView.visibility = View.VISIBLE
            binding.filesTreeView.visibility = View.GONE
        }
    }

    /**
     * Finishes the setup and configuration of our UI. First we set the [Toolbar.OnMenuItemClickListener]
     * of the [ActivityFileExplorerBinding.toolbar] `MaterialToolbar` of [binding] to a lambda which
     * starts the activity [SettingsActivity] to inform the user of the "SDK codename" of the device,
     * the "SDK version" of the device, that status of the "Legacy External Storage" flag, the name
     * of the permission we need, and whether the permission has been granted. It also has buttons to
     * "Open Settings", and to "Request Permission" that the user can click to perform these tasks.
     * The lambda then returns `true` to consume the event.
     *
     * Next we set the [View.OnClickListener] of the [ActivityFileExplorerBinding.permissionButton]
     * `Button` of [binding] to a lambda which calls our [requestStoragePermission] method to allow
     * the user to grant us permission to access the shared/external file directory of the device.
     *
     * We initialize our [ArrayAdapter] of [String] field [adapter] to a new instance which uses the
     * system layout file with ID [android.R.layout.simple_list_item_1] to instantiate views and a
     * new [mutableListOf] instance of [String] as the objects to represent in the [ListView]. We
     * set the adapter of the [ActivityFileExplorerBinding.filesTreeView] `ListView` to [adapter],
     * and set its [AdapterView.OnItemClickListener] to a lambda which initializes its [File] variable
     * `val selectedItem` to the [File] in the position of the clicked item in our [List] of [File]
     * field [filesList] and then calls our [open] method with `selectedItem` to have it do what needs
     * to be done to open the `selectedItem` file or directory.
     */
    private fun setupUi() {
        binding.toolbar.setOnMenuItemClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }

        binding.permissionButton.setOnClickListener {
            requestStoragePermission(this)
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        binding.filesTreeView.adapter = adapter
        binding.filesTreeView.setOnItemClickListener { _, _, position, _ ->
            val selectedItem: File = filesList[position]
            open(selectedItem)
        }
    }

    /**
     * Called to "open" its [File] parameter [selectedItem], either by creating and launching an
     * [Intent] with the action [Intent.ACTION_VIEW] to allow the user to view the contents if the
     * [File] parameter [selectedItem] is a file, or by replacing the [File] entries in our [List]
     * of [File] field [filesList] with the list of [File] entries of [selectedItem] if it is a
     * directory (the name of the [File] entries will then be used to replace the dataset of our
     * [ArrayAdapter] of [String] field [adapter] which will be displayed in the [ListView] that
     * it is providing views to).
     *
     * @param selectedItem the [File] of the directory or file that needs to be opened.
     */
    private fun open(selectedItem: File) {
        if (selectedItem.isFile) {
            return openFile(this, selectedItem)
        }

        currentDirectory = selectedItem
        filesList = getFilesList(currentDirectory)

        adapter.clear()
        adapter.addAll(filesList.map {
            if (it.path == selectedItem.parentFile!!.path) {
                renderParentLink(this)
            } else {
                renderItem(this, it)
            }
        })

        adapter.notifyDataSetChanged()
    }
}
