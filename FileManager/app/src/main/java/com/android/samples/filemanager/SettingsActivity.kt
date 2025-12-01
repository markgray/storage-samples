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
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
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
     * Called when the activity is starting. First we call [enableEdgeToEdge] to enable edge to
     * edge display, then we call our super's implementation of `onCreate`, and initialize our
     * [ActivitySettingsBinding] field [binding] by having the
     * [ActivitySettingsBinding.inflate] method use the [LayoutInflater] instance that this
     * Window retrieved from its [Context] to inflate and bind to its associated layout file
     * (layout/activity_settings.xml), and set our content view to the outermost [View] in the
     * associated layout file of [binding] (its `root` property).
     *
     * We then call [ViewCompat.setOnApplyWindowInsetsListener] to take over the policy for
     * applying window insets to the [ActivitySettingsBinding.getRoot] root [View] of [binding]
     * with the `listener` argument a lambda that accepts the [View] passed the lambda in variable
     * `v` and the [WindowInsetsCompat] passed the lambda in variable `windowInsets`. It initializes
     * its [Insets] variable `insets` to the [WindowInsetsCompat.getInsets] of `windowInsets` with
     * [WindowInsetsCompat.Type.systemBars] as the argument, then it updates the layout parameters
     * of `v` to be a [ViewGroup.MarginLayoutParams] with the left margin set to `insets.left`,
     * the right margin set to `insets.right`, the top margin set to `insets.top`, and the bottom
     * margin set to `insets.bottom`. Finally it returns [WindowInsetsCompat.CONSUMED] to the caller
     * (so that the window insets will not keep passing down to descendant views).
     *
     * We initialize our [ArrayAdapter] field [adapter] to a new instance constructed to use the
     * system layout file with resource ID [android.R.layout.simple_list_item_1] for instantiating
     * views, and to use the [List] of [String] returned by our [getInfoList] method as the objects
     * to represent in its views. We set the adapter of the [ListView] bound to
     * [ActivitySettingsBinding.infoList] in [binding] to [adapter]. Next we set the
     * [OnClickListener] of the [ActivitySettingsBinding.openSettingsButton] button of [binding]
     * (labeled "Open settings") to a lambda which calls our method [openPermissionSettings], and
     * the [OnClickListener] of the [ActivitySettingsBinding.requestPermissionButton] button of
     * [binding] (labeled "Request permission") to a lambda which calls our method
     * [requestStoragePermission].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v: View, windowInsets: WindowInsetsCompat ->
            val insets: Insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
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

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, getInfoList())
        binding.infoList.adapter = adapter

        binding.openSettingsButton.setOnClickListener {
            openPermissionSettings(this)
        }
        binding.requestPermissionButton.setOnClickListener {
            requestStoragePermission(this)
        }
    }

    /**
     * Returns a [List] of [String] containing information about the software running on the device
     * we are on, and external storage permission information related to this app. This [List] is
     * used as the data set for the [ListView] displayed in our UI. The information consists of:
     *  1. "SDK codename:" displays the value of [Build.VERSION.CODENAME] which is the current
     *  development codename, or the string "REL" if this is a release build.
     *  2. "SDK version:" displays the string value of [Build.VERSION.SDK_INT] which is the SDK
     *  version of the software currently running on this hardware device.
     *  3. "Legacy External Storage:" displays the value returned by [getLegacyStorageStatus] which
     *  indicates whether "Legacy View" is used to access external storage. It is `true` only on "Q",
     *  "N/A" on older SDKs, and `false` on "R".
     *  4. "Permission used:" displays the value returned by [getStoragePermissionName] which is
     *  the name of the permission used to request access to external storage, either our hard coded
     *  [MANAGE_EXTERNAL_STORAGE_PERMISSION] on SDK on "R" and above, or the value of the system
     *  constant [Manifest.permission.READ_EXTERNAL_STORAGE] on all other SDKs.
     *  5. "Permission granted:" displays the value returned by [getPermissionStatus] which is `true`
     *  or `false` depending on whether we have been granted permission to access external storage.
     *
     * @return a [List] of [String] displaying information about the device we are running on and
     * external storage permission information related to this app.
     */
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