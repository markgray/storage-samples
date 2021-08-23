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

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.widget.Toolbar
import com.android.samples.filemanager.databinding.ActivitySettingsBinding

/**
 * The activity is launched by the lambda which is set as the [Toolbar.OnMenuItemClickListener] of
 * the [Toolbar] of our [FileExplorerActivity] which is called whenever the user clicks on the
 * options menu (which holds only a "Settings" icon). Its UI consists of a [ListView] which displays
 * the "SDK codename" of the device, the "SDK version" of the device, that status of the "Legacy
 * External Storage" flag, the name of the permission we need, and whether the permission has been
 * granted. It also had buttons to "Open Settings", and to "Request Permission" that the user can
 * click to perform these tasks.
 */
class SettingsActivity : AppCompatActivity() {
    /**
     * The `ViewBinding` which is inflated from the layout file layout/activity_settings.xml
     * which we use as our UI. It consists of a `ConstraintLayout` root view holding at its top a
     * `MaterialToolbar`, with a [ListView] and two [Button] widgets below the `MaterialToolbar`.
     */
    private lateinit var binding: ActivitySettingsBinding
    /**
     * The [ArrayAdapter] which feeds views holding [String]s to the [ListView] in our UI.
     */
    private lateinit var adapter: ArrayAdapter<String>

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, getInfoList())
        binding.infoList.adapter = adapter

        binding.openSettingsButton.setOnClickListener {
            openPermissionSettings(this)
        }
        binding.requestPermissionButton.setOnClickListener {
            requestStoragePermission(this)
        }
    }

    private fun getInfoList(): List<String> {
        return listOf(
            getString(R.string.sdk_codename_info, Build.VERSION.CODENAME),
            getString(R.string.sdk_version_info, Build.VERSION.SDK_INT.toString()),
            getString(R.string.legacy_storage_info, getLegacyStorageStatus()),
            getString(R.string.permission_used_info, getStoragePermissionName()),
            getString(R.string.permission_granted_info, getPermissionStatus(this))
        )
    }
}