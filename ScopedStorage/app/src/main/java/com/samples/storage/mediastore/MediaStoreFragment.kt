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

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samples.storage.Action
import com.samples.storage.ActionListAdapter
import com.samples.storage.MainFragment
import com.samples.storage.R
import com.samples.storage.databinding.FragmentListBinding

/**
 * The dataset of [Action] objects that we use to construct an [ActionListAdapter] that will feed
 * views to our [RecyclerView]. Each [Action] object holds a resource ID of a [String] to be
 * displayed in the [TextView] of its view in the [RecyclerView], and a resource ID for a navigation
 * `action` that can be found in the navigation graph of the app (the file navigation/nav_graph.xml).
 * The [View.OnClickListener] of the [TextView] of each [Action] object is a lambda which will navigate
 * to the destination fragment of the navigation `action`. Thus the [Action] object whose
 * [Action.nameRes] field is the resource ID [R.string.mediastore_add] ("Add Media File") will navigate
 * to the [AddMediaFragment] when clicked, the [Action] object whose [Action.nameRes] field is the
 * resource ID [R.string.mediastore_edit] ("Edit Media File") will navigate to the [EditMediaFragment]
 * when clicked, the [Action] object whose [Action.nameRes] field is the resource ID
 * [R.string.mediastore_delete] ("Delete Media File") will navigate to the [DeleteMediaFragment]
 * when clicked, and the [Action] object whose [Action.nameRes] field is the resource ID
 * [R.string.mediastore_downloads] ("Add Document to Downloads") will navigate to the
 * [AddDocumentFragment] when clicked.
 */
private val demoList = arrayOf(
    Action(R.string.mediastore_add, R.id.action_mediaStoreFragment_to_addMediaFragment),
    Action(R.string.mediastore_edit, R.id.action_mediaStoreFragment_to_editMediaFragment),
    Action(R.string.mediastore_delete, R.id.action_mediaStoreFragment_to_deleteMediaFragment),
    Action(R.string.mediastore_downloads, R.id.action_mediaStoreFragment_to_addDocumentFragment),
)

/**
 * This is the start destination of the navigation graph for demos of the `MediaStore` API, and just
 * consists of a [RecyclerView] whose item views when clicked will cause us to navigate to one of
 * four different fragments: [AddMediaFragment], [EditMediaFragment], [DeleteMediaFragment], or
 * [AddDocumentFragment]. It is navigated to from the [MainFragment] when the item in the [RecyclerView]
 * with the text "MediaStore" is clicked.
 */
class MediaStoreFragment : Fragment() {
    /**
     * The view binding generated from our layout file layout/fragment_list.xml (resource ID
     * [R.layout.fragment_list]). It consists of a `FrameLayout` root view holding a single
     * [RecyclerView] (resource ID [R.id.recyclerView]). It is private to prevent other classes
     * from modifying it, but its read-only accessor [binding] is private too so what the hay.
     */
    private var _binding: FragmentListBinding? = null
    /**
     * Read-only access to our [FragmentListBinding] field [_binding].
     */
    private val binding get() = _binding!!

    /**
     * Called to have the fragment instantiate its user interface view. This will be called between
     * [onCreate] and [onViewCreated]. It is recommended to only inflate the layout in this method
     * and move logic that operates on the returned [View] to [onViewCreated]. If you return a [View]
     * from here, you will later be called in [onDestroyView] when the view is being released. First
     * we initialize our [FragmentListBinding] field [_binding] by having the [FragmentListBinding.inflate]
     * method use our [LayoutInflater] parameter [inflater] to inflate and bind to its associated layout
     * file layout/fragment_list.xml with our [ViewGroup] parameter [container] supplying the
     * `LayoutParams`. Next we set the [RecyclerView.LayoutManager] of the [RecyclerView] field
     * [FragmentListBinding.recyclerView] of [binding] to a new instance or [LinearLayoutManager]
     * with its resources supplied by our current [Context], and set its [RecyclerView.Adapter] to
     * a new instance of [ActionListAdapter] constructed to use our [Array] of [Action] field [demoList]
     * as its dataset. The `binding.recyclerView` do nothing statement is a bit puzzling, and removing
     * the line does not change behavior but I guess I'll just leave it in (why not?). Finally we
     * return the outermost [View] in the layout file associated with [binding] to serve as our UI.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment.
     * @param container If non-`null`, this is the parent view that the fragment's UI will be
     * attached to. The fragment should not add the view itself, but this can be used to generate
     * the LayoutParams of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = ActionListAdapter(demoList)

        binding.recyclerView

        return binding.root
    }

    /**
     * Called when the view previously created by [onCreateView] has been detached from the fragment.
     * The next time the fragment needs to be displayed, a new view will be created. This is called
     * after [onStop] and before [onDestroy].  It is called regardless of whether [onCreateView]
     * returned a non-`null` view. Internally it is called after the view's state has been saved but
     * before it has been removed from its parent. First we call our super's implementation of
     * `onDestroyView`, then we set our [FragmentListBinding] field [_binding] to `null`.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
