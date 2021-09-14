/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.samples.safdemos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.RecyclerView
import com.android.samples.safdemos.databinding.ActivityMainBinding
import com.android.samples.safdemos.imagepicker.ImagePickerFragment

/**
 * This class merely sets up the content view, all of the functionality is provided by the
 * [NavHostFragment] in our layout file and the two fragments [MainFragment] and [ImagePickerFragment].
 * [MainFragment] is the `startDestination` fragment and its UI contains a [RecyclerView] designed to
 * hold entries for different demos but only holds an entry for [ImagePickerFragment] at present which
 * will when clicked navigate to the [ImagePickerFragment] demo.
 */
class MainActivity : AppCompatActivity() {

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`.
     * Then we set our [ActivityMainBinding] variable `val binding` to the instance that the
     * [ActivityMainBinding.inflate] method inflates from its associated layout file activity_main.xml
     * (resource ID [R.layout.activity_main]). This layout consists of a vertical `LinearLayout` root
     * view holding an `AppBarLayout` (which holds a `Toolbar`) above a `FragmentContainerView` whose
     * fragment is the `NavHostFragment` for the navigation graph navigation/nav_graph.xml of the app.
     *
     * Next we set our content view to the outermost View in the associated layout file of `binding`,
     * and set the [ActivityMainBinding.toolbar] in `binding` to act as the ActionBar for this
     * Activity window.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use this.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
    }
}
