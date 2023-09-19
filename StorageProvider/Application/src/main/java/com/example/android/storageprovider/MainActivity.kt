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
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.android.common.activities.SampleActivityBase
import com.example.android.common.logger.Log
import com.example.android.common.logger.LogFragment
import com.example.android.common.logger.LogWrapper
import com.example.android.common.logger.MessageOnlyLogFilter

/**
 * A simple launcher activity containing a summary sample description
 * and an options menu with one menu item. All of the actual sample code
 * is in [StorageProviderFragment] and [MyCloudProvider]
 */
class MainActivity : SampleActivityBase() {
    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file [R.layout.activity_main], which consists of
     * a vertical `LinearLayout` root view that holds a `TextView` displaying our info message, with
     * a 1dp "darker_gray" `View` seperating it from a `FragmentContainerView` that holds a
     * `LogFragment` that displays any messages our app logs.
     *
     * If the [FragmentManager] for interacting with fragments associated with this activity cannot
     * find a [Fragment] whose tag is [FRAGTAG] (the [String] "StorageProviderFragment") we initialize
     * our [FragmentTransaction] variable `val transaction` to the instance that the
     * [FragmentManager.beginTransaction] method of the [FragmentManager] for interacting with
     * fragments associated with this activity returns, initialize our [StorageProviderFragment]
     * variable `val fragment` to a new instance, use `transaction` to add `fragment` with the tag
     * [FRAGTAG] and then commit `transaction`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (supportFragmentManager.findFragmentByTag(FRAGTAG) == null) {
            val transaction = supportFragmentManager.beginTransaction()
            val fragment = StorageProviderFragment()
            transaction.add(fragment, FRAGTAG)
            transaction.commit()
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu. We use a [MenuInflater] for
     * this [Context] to inflate our options menu file [R.menu.main] into our [Menu] parameter [menu]
     * (it consists of a single [MenuItem] with the ID [R.id.sample_action] and the android:title
     * "Log in"). Then we return `true` so that the menu will be displayed. Note that
     * [StorageProviderFragment] overrides [onOptionsItemSelected] and will handle any clicks on the
     * [MenuItem] in our options [Menu].
     *
     * @param menu The options [Menu] in which you place your items.
     * @return You must return `true` for the menu to be displayed;
     * if you return `false` it will not be shown.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    /**
     * Create a chain of targets that will receive log data
     */
    override fun initializeLogging() {
        // Wraps Android's native log framework.
        val logWrapper = LogWrapper()
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.logNode = logWrapper

        // Filter strips out everything except the message text.
        val msgFilter = MessageOnlyLogFilter()
        logWrapper.next = msgFilter

        // On screen logging via a fragment with a TextView.
        val logFragment = supportFragmentManager
            .findFragmentById(R.id.log_fragment) as LogFragment?
        msgFilter.next = (logFragment ?: return).logView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (logFragment.logView ?: return).setTextAppearance(R.style.Log)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                (logFragment.logView ?: return).setTextAppearance(R.style.Log)
            } else {
                (logFragment.logView ?: return).setTextAppearance(this, R.style.Log)
            }
        }
        (logFragment.logView ?: return).setBackgroundColor(Color.WHITE)
        Log.i(TAG, "Ready")
    }

    companion object {
        /**
         * TAG used for logging.
         */
        const val TAG: String = "MainActivity"

        /**
         * TAG for our [Fragment]
         */
        const val FRAGTAG: String = "StorageProviderFragment"
    }
}