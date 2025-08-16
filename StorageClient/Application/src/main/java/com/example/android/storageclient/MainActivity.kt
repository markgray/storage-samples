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
package com.example.android.storageclient

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
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
 * is in [StorageClientFragment].
 */
class MainActivity : SampleActivityBase() {
    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file `R.layout.activity_main`, which consists of
     * a vertical `LinearLayout` root view that holds a `ScrollView` holding a `TextView` displaying
     * our into message, with a 1dp "darker_gray" `View` seperating it from a `FragmentContainerView`
     * that holds a `LogFragment` that displays any messages our app logs.
     *
     * If the [FragmentManager] for interacting with fragments associated with this activity cannot
     * find a [Fragment] whose tag is [FRAGTAG] (the [String] "StorageClientFragment") we initialize
     * our [FragmentTransaction] variable `val transaction` to the instance that the
     * [FragmentManager.beginTransaction] method of the [FragmentManager] for interacting with
     * fragments associated with this activity returns, initialize our [StorageClientFragment]
     * variable `val fragment` to a new instance, use `transaction` to add `fragment` with the tag
     * [FRAGTAG] and then commit `transaction`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val rootView = findViewById<LinearLayout>(R.id.sample_main_layout)
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
        val menuHost: MenuHost = this
        menuHost.addMenuProvider(menuProvider, this)
        if (supportFragmentManager.findFragmentByTag(FRAGTAG) == null) {
            val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
            val fragment = StorageClientFragment()
            transaction.add(fragment, FRAGTAG)
            transaction.commit()
        }
    }

    /**
     * Our  [MenuProvider]
     */
    private val menuProvider: MenuProvider = object : MenuProvider {
        /**
         * Called by the [MenuHost] to allow the [MenuProvider] to inflate [MenuItem]s into the menu.
         * We leave this up to our [StorageClientFragment] fragment.
         *
         * @param menu The options menu in which you place your items.
         * @param menuInflater a [MenuInflater] you can use to inflate an XML menu file with.
         */
        @Suppress("EmptyMethod") // No real need to remove it
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            return
        }

        /**
         * Called by the [MenuHost] when a [MenuItem] is selected from the menu. We do not implement
         * anything yet, this will be done in `MarsRealEstateFinal`. We leave this up to our
         * [StorageClientFragment] fragment.
         *
         * @param menuItem the menu item that was selected
         * @return `true` if the given menu item is handled by this menu provider, `false` otherwise.
         */
        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return false
        }
    }

    /**
     * Creates a chain of targets that will receive log data
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
            @Suppress("KotlinConstantConditions")
            @SuppressLint("ObsoleteSdkInt")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                (logFragment.logView ?: return).setTextAppearance(R.style.Log)
            } else {
                (logFragment.logView ?: return).setTextAppearance(this, R.style.Log)
            }
        }
        Log.i(TAG, "Ready")
    }

    companion object {
        /**
         * TAG used for logging.
         */
        const val TAG: String = "MainActivity"

        /**
         * The fragment tag we use when adding the [StorageClientFragment] to the activity state.
         */
        const val FRAGTAG: String = "StorageClientFragment"
    }
}
