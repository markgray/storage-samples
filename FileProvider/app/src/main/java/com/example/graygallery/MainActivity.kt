/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.graygallery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.graygallery.ui.DashboardFragment
import com.example.graygallery.ui.GalleryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * This is a very simple [AppCompatActivity] which only sets the content view and configures the
 * action bar and [NavController].
 */
class MainActivity : AppCompatActivity() {
    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to the layout file [R.layout.activity_main]. This consists of a
     * `ConstraintLayout` root view holding a [BottomNavigationView] at its bottom (resource ID
     * [R.id.nav_view]) with a `fragment` container at its top (resource ID [R.id.nav_host_fragment]).
     *
     * Next we initialize our [BottomNavigationView] variable `val navView` by finding the view with
     * ID [R.id.nav_view], and our [NavController] variable `val navController` by finding the view
     * with ID [R.id.nav_host_fragment] (this `fragment` container uses the "android:name" attribute
     * to specify a [NavHostFragment] as the fragment class to instantiate). We initialize our
     * [AppBarConfiguration] variable `val appBarConfiguration` to a new instance whose set of top
     * level destinations consists of [R.id.navigation_dashboard] (our [DashboardFragment] fragment)
     * and [R.id.navigation_gallery] (our [GalleryFragment]). Note: The Up button will not be
     * displayed when on these destinations.
     *
     * We then call the [setupActionBarWithNavController] method to set up the `ActionBar` for use
     * with the [NavController] `navController`, with `appBarConfiguration` as additional
     * configuration options for customizing the behavior of the `ActionBar`. Finally we call the
     * [BottomNavigationView.setupWithNavController] method of `navView` to set it up for use with
     * our [NavController] variable `navController` (the selected item in the `NavigationView` will
     * automatically be updated when the destination changes).
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val navController: NavController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_gallery
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}