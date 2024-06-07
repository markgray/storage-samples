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

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
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
        @Suppress("DEPRECATION") // TODO: Use MenuProvider
        setHasOptionsMenu(true)
    }

    /**
     * Prepare the Fragment host's standard options menu to be displayed. This is called right
     * before the menu is shown, every time it is shown. First we call our super's implementation
     * of `onPrepareOptionsMenu`, then we initialize our [MenuItem] variable `val item` to the
     * item in our [Menu] parameter [menu] with the ID [R.id.sample_action]. If our [mLoggedIn]
     * field is `true` we set the title of `item` to "Log out", otherwise we set it to "Log in".
     *
     * @param menu The options [Menu] as last shown or first initialized by [onCreateOptionsMenu].
     */
    @Deprecated("Deprecated in Java") // TODO: Use MenuProvider
    override fun onPrepareOptionsMenu(menu: Menu) {
        @Suppress("DEPRECATION") // TODO: Use MenuProvider
        super.onPrepareOptionsMenu(menu)
        val item: MenuItem = menu.findItem(R.id.sample_action)
        item.setTitle(if (mLoggedIn) R.string.log_out else R.string.log_in)
    }

    /**
     * This hook is called whenever an item in your options menu is selected. If the value returned
     * by [MenuItem.getItemId] (aka kotlin `itemId` property) of our parameter [item] is
     * [R.id.sample_action] we call our [toggleLogin] method to toggle the value of our [Boolean]
     * field [mLoggedIn], then set the title of [item] to "Log out" if [mLoggedIn] is now `true` or
     * to "Log in" if it is now `false`. If the SDK version of the software currently running on
     * this hardware device is greater than equal to [Build.VERSION_CODES.N] we fetch a
     * [ContentResolver] instance for our application's package and call its method
     * [ContentResolver.notifyChange] with a [Uri] representing the roots of our document provider
     * [AUTHORITY], `null` for the observer, and 0 for the `flags` argument. Other wise we fetch a
     * [ContentResolver] instance for our application's package and call its method
     * [ContentResolver.notifyChange] with a [Uri] representing the roots of our document provider
     * [AUTHORITY], `null` for the observer, and `false` for its `syncToNetwork` argument.
     *
     * Whether the `itemId` of [item] was ours or not we then return `true` to consume the
     * event here.
     *
     * @param item The [MenuItem] that was selected.
     * @return Return `false` to allow normal menu processing to proceed, `true` to consume it here.
     */
    @Deprecated("Deprecated in Java")
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
                @Suppress("DEPRECATION") // TODO: migrate to notifyChange(Uri, ContentObserver, int)
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
     * Convenience method to toggle the user's authorization status. First we toggle the value of
     * our [Boolean] field [mLoggedIn] then we call our [writeLoginValue] method to write the new
     * value of [mLoggedIn] to our shared preferences file.
     */
    private fun toggleLogin() {
        // Replace this with your standard method of authentication to determine if your app
        // should make the user's documents available.
        mLoggedIn = !mLoggedIn
        writeLoginValue(mLoggedIn)
        Log.i(TAG, getString(if (mLoggedIn) R.string.logged_in_info else R.string.logged_out_info))
    }

    /**
     * Convenience method to save the user's logged in status to our shared preferences file. We
     * initialize our [SharedPreferences] variable `val sharedPreferences` by retrieving the contents
     * of the preferences file with the name [R.string.app_name] ("StorageProvider") specifying the
     * mode [Context.MODE_PRIVATE] (the created file can only be accessed by the calling application
     * or all applications sharing the same user ID). Then we call the [SharedPreferences.edit] method
     * of `sharedPreferences` to create a new [SharedPreferences.Editor] for it and call the
     * [SharedPreferences.Editor.putBoolean] method of the editor to store the value of our [loggedIn]
     * parameter under the key [R.string.key_logged_in] ("key_logged_in"), then commit the change back
     * to the [SharedPreferences] object `sharedPreferences` that we just edited.
     *
     * @param loggedIn the [Boolean] value we are to store in our shared preferences file.
     */
    private fun writeLoginValue(loggedIn: Boolean) {
        val sharedPreferences: SharedPreferences = requireActivity().getSharedPreferences(
            getString(R.string.app_name),
            Context.MODE_PRIVATE
        )
        sharedPreferences.edit().putBoolean(getString(R.string.key_logged_in), loggedIn).apply()
    }

    /**
     * Convenience method to determine whether the user is logged in. We initialize our
     * [SharedPreferences] variable `val sharedPreferences` by retrieving the contents of the
     * preferences file with the name [R.string.app_name] ("StorageProvider") specifying the
     * mode [Context.MODE_PRIVATE] (the created file can only be accessed by the calling application
     * or all applications sharing the same user ID). Then we return the [Boolean] stored in
     * `sharedPreferences` under the key [R.string.key_logged_in] ("key_logged_in") defaulting to
     * `false` if a preference with that key does not exist.
     *
     * @return `true` if the user is logged in according to the value stored in our shared preferences
     * file, `false` it they are not logged in.
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