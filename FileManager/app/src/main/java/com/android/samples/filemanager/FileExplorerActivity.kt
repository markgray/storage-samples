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
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStorageDirectory
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
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
    private var hasPermission = false

    /**
     * The `ViewBinding` which is inflated from the layout file layout/activity_file_explorer.xml
     * which we use as our UI.
     */
    private lateinit var binding: ActivityFileExplorerBinding
    private lateinit var currentDirectory: File
    private lateinit var filesList: List<File>
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFileExplorerBinding.inflate(layoutInflater)
        binding.toolbar.inflateMenu(R.menu.file_manager_menu)
        setContentView(binding.root)

        setupUi()
    }

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
            val selectedItem = filesList[position]
            open(selectedItem)
        }
    }

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
