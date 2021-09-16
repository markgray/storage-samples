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
import androidx.appcompat.app.AppCompatActivity
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
     * Called then the activity is starting.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val navController: NavController = findNavController(R.id.nav_host_fragment)

        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController: NavController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) ||
            super.onSupportNavigateUp()
    }
}
