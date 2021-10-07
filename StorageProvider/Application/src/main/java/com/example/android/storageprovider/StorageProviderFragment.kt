/*
 * Copyright 2013 The Android Open Source Project
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
package com.example.android.storageprovider

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.example.android.common.logger.Log

/**
 * Toggles the user's login status via a login menu option, and enables/disables the cloud storage
 * content provider.
 */
class StorageProviderFragment : Fragment() {
    /**
     * Flag we use to determine if we are "Logged in" or not.
     */
    private var mLoggedIn: Boolean = false

    /**
     * Called to do initial creation of a fragment. This is called after [onAttach] and before
     * [onCreateView]. First we call our super's implementation of `onCreate`, then we set our
     * [Boolean] field [mLoggedIn] to the value read from our shared preferences file by our
     * [readLoginValue] method. Finally we call the [setHasOptionsMenu] method with `true` to
     * report that this fragment would like to participate in populating the options menu by
     * receiving a call to [onCreateOptionsMenu] and related methods. Note that [MainActivity]
     * overrides [onCreateOptionsMenu] to create the options menu, but we override the method
     * [onPrepareOptionsMenu] (to change the title of the [MenuItem] in the options menu from
     * "Log in" to "Log out" and vice versa depending on the value of our [mLoggedIn] field) and
     * [onOptionsItemSelected] to do what needs doing when the [MenuItem] with ID [R.id.sample_action]
     * is clicked.
     *
     * @param savedInstanceState We do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLoggedIn = readLoginValue()
        setHasOptionsMenu(true)
    }

    /**
     * Prepare the Fragment host's standard options menu to be displayed. This is called right
     * before the menu is shown, every time it is shown.
     *
     * @param menu The options [Menu] as last shown or first initialized by [onCreateOptionsMenu].
     */
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val item: MenuItem = menu.findItem(R.id.sample_action)
        item.setTitle(if (mLoggedIn) R.string.log_out else R.string.log_in)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sample_action) {
            toggleLogin()
            item.setTitle(if (mLoggedIn) R.string.log_out else R.string.log_in)

            // BEGIN_INCLUDE(notify_change)
            // Notify the system that the status of our roots has changed.  This will trigger
            // a call to MyCloudProvider.queryRoots() and force a refresh of the system
            // picker UI.  It's important to call this or stale results may persist.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                requireActivity().contentResolver.notifyChange(
                    DocumentsContract.buildRootsUri(AUTHORITY),
                    null,
                    0
                )
            } else {
                @Suppress("DEPRECATION")
                requireActivity().contentResolver.notifyChange(
                    DocumentsContract.buildRootsUri(AUTHORITY),
                    null,
                    false
                )
            }
            // END_INCLUDE(notify_change)
        }
        return true
    }

    /**
     * Dummy function to change the user's authorization status.
     */
    private fun toggleLogin() {
        // Replace this with your standard method of authentication to determine if your app
        // should make the user's documents available.
        mLoggedIn = !mLoggedIn
        writeLoginValue(mLoggedIn)
        Log.i(TAG, getString(if (mLoggedIn) R.string.logged_in_info else R.string.logged_out_info))
    }

    /**
     * Dummy function to save whether the user is logged in.
     */
    private fun writeLoginValue(loggedIn: Boolean) {
        val sharedPreferences = requireActivity().getSharedPreferences(
            getString(R.string.app_name),
            Context.MODE_PRIVATE
        )
        sharedPreferences.edit().putBoolean(getString(R.string.key_logged_in), loggedIn).apply()
    }

    /**
     * Dummy function to determine whether the user is logged in.
     */
    private fun readLoginValue(): Boolean {
        val sharedPreferences = requireActivity().getSharedPreferences(
            getString(R.string.app_name),
            Context.MODE_PRIVATE
        )
        return sharedPreferences.getBoolean(getString(R.string.key_logged_in), false)
    }

    companion object {
        /**
         * TAG we use for logging.
         */
        private const val TAG = "StorageProviderFragment"

        /**
         * The authority used for our [MyCloudProvider], it is the value of the "android:authorities"
         * attribute of the `<provider>` element in our AndroidManifest.xml file whose "android:name"
         * attribute specifies [MyCloudProvider] as our provider.
         */
        private const val AUTHORITY = "com.example.android.storageprovider.documents"
    }
}