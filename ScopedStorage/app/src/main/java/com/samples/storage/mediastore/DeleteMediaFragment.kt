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
package com.samples.storage.mediastore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.samples.storage.databinding.FragmentDemoBinding

// TODO(yrezgui): Finish this demo
/**
 * Just a skeleton of an unimplemented demo.
 */
class DeleteMediaFragment : Fragment() {
    private var _binding: FragmentDemoBinding? = null
    private val binding get() = _binding!!

    /**
     * Called to have the fragment instantiate its user interface view. First we initialize our
     * [FragmentDemoBinding] field [_binding] by having the [FragmentDemoBinding.inflate] method
     * use our [LayoutInflater] parameter [inflater] to inflate the layout file `fragment_demo.xml`
     * with our [ViewGroup] parameter [container] supplying the `LayoutParams`. We initialize our
     * [View] variable `val view` to the outermost View in the layout file associated with [binding],
     * and then return `view` to the caller to have it serve as our fragment's UI.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the [View] for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDemoBinding.inflate(inflater, container, false)
        @Suppress("UnnecessaryVariable") // Easier to breakpoint this way
        val view = binding.root
        return view
    }

    /**
     * Called when the [View] previously created by [onCreateView] has been detached from the
     * fragment. The next time the fragment needs to be displayed, a new view will be created.
     * This is called after [onStop] and before [onDestroy]. First we call our super's implementation
     * of `onDestroyView`, then we set our [FragmentDemoBinding] field [_binding] to `null`.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
