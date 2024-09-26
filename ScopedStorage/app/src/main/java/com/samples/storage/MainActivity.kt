/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samples.storage

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController

/**
 * This class just sets the content view to the layout file [R.layout.main_activity] and configures
 * the [NavController] of the [NavHostFragment] in that file (resource ID [R.id.nav_host_fragment])
 * to be used by the `ActionBar` for "UP" button behavior. The layout file consists of a root
 * `ConstraintLayout` holding only a `fragment` container whose contents is the [NavHostFragment].
 * The "app:navGraph" element specifies the file navigation/nav_graph.xml to be the navigation graph
 * for the [NavHostFragment] and [MainFragment] is the start fragment of the graph.
 */
class MainActivity : AppCompatActivity() {
    /**
     * Configuration options for the action bar `NavigationUI` methods (such as "UP"). We construct
     * this instance in our [onCreate] override to use the navigation graph associated with the
     * [NavController] for the [NavHostFragment] in the [R.id.nav_host_fragment] `fragment` of our
     * layout file. Its start destination ([MainFragment]) will be considered the only top level
     * destination. The Up button will not be displayed when on the start destination of the graph.
     */
    private lateinit var appBarConfiguration: AppBarConfiguration

    /**
     * Called then the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file [R.layout.main_activity]. It consists of a
     * `ConstraintLayout` root view holding a single `fragment` container whose "android:name"
     * attribute specifies that a [NavHostFragment] will be instantiated in it. We initialize our
     * [NavController] variable `val navController` by finding the [NavHostFragment] with the
     * resource ID [R.id.nav_host_fragment] in our UI. We initialize our [AppBarConfiguration] field
     * [appBarConfiguration] to a new instance constructed to use the navigation graph of
     * `navController` (the file navigation/nav_graph.xml) for its "UP" behavior (its start
     * destination ([MainFragment]) will be considered the only top level destination, and the "Up"
     * button will not be displayed when on the start destination of the graph). Finally we call the
     * [setupActionBarWithNavController] method with `navController` as the [NavController] whose
     * navigation actions will be reflected in the title of the action bar, and [appBarConfiguration]
     * for additional configuration options for customizing the behavior of the `ActionBar`.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val rootView = findViewById<ConstraintLayout>(R.id.root_view)
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

        val navController: NavController = findNavController(R.id.nav_host_fragment)

        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    /**
     * This method is called whenever the user chooses to navigate Up within your application's
     * activity hierarchy from the action bar. We initialize our [NavController] variable
     * `val navController` by finding the [NavHostFragment] with the resource ID [R.id.nav_host_fragment]
     * in our UI. If the [NavController.navigateUp] method of `navController` when passed our
     * [AppBarConfiguration] field [appBarConfiguration] (the [NavController] was able to navigate up)
     * we return `true` to the callers, otherwise we return the value returned by our super's
     * implementation of `onSupportNavigateUp`.
     *
     * @return `true` if Up navigation completed successfully and this Activity was finished,
     * `false` otherwise.
     */
    override fun onSupportNavigateUp(): Boolean {
        val navController: NavController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) ||
            super.onSupportNavigateUp()
    }
}
