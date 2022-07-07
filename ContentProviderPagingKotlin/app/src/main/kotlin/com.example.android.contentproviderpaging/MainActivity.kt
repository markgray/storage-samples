/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.contentproviderpaging

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

/**
 * The launcher Activity.
 */
class MainActivity : AppCompatActivity() {
    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file [R.layout.activity_main] which consists of a
     * single `FrameLayout` root view with the resource ID [R.id.container]. If our [Bundle] parameter
     * [savedInstanceState] is `null` this is the first time we are being run so we fetch the
     * [FragmentManager] for interacting with fragments associated with this activity and use it to
     * begin a [FragmentTransaction] which we use to add a new instance of [ImageClientFragment] to
     * the container with resource ID [R.id.container] and then we commit the [FragmentTransaction].
     * Note: if [savedInstanceState] is not `null` we are being restarted after previously being
     * shut down and the system will take care of restoring our [ImageClientFragment] for us.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     * down then this [Bundle] contains the data it most recently supplied in [onSaveInstanceState].
     * Note: Otherwise it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, ImageClientFragment.newInstance())
                .commit()
        }
    }
}
