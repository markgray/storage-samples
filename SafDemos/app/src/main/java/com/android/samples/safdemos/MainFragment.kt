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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.samples.safdemos.imagepicker.ImagePickerFragment
import com.android.samples.safdemos.databinding.FragmentMainBinding
import com.android.samples.safdemos.databinding.ListItemDemoBinding

/**
 * This [Fragment] is the `startDestination` of our navigation graph navigation/nav_graph.xml, and
 * consists of a [RecyclerView] holding [DemoViewHolder] objects (a [RecyclerView.ViewHolder] for
 * [Demo] items), and when one of the entries in the [RecyclerView] is clicked the [NavController]
 * will be told to navigate to the destination of the [Demo.action] action resource ID of the [Demo]
 * held in the [DemoViewHolder]. At present there is only a [Demo] entry for the [ImagePickerFragment]
 * fragment.
 */
class MainFragment : Fragment() {
    /**
     * Called to have the fragment instantiate its user interface view. It is recommended to only
     * inflate the layout in this method and move logic that operates on the returned View to
     * [onViewCreated]. We initialize our [FragmentMainBinding] variable `val binding` to the
     * instance that the [FragmentMainBinding.inflate] method returns when it uses the cached
     * [LayoutInflater] used to inflate Views of this Fragment to inflate and bind to its associated
     * layout file layout/fragment_main.xml (resource ID [R.layout.fragment_main]). We initialize
     * our [Array] of [Demo] variable `val demoItems` to a new instance holding a single [Demo]
     * object for the [ImagePickerFragment] demo whose [Demo.title] is the [String] "Image Picker",
     * whose [Demo.text] is the [String] "Demo of using ACTION_GET_CONTENT to allow a user to pick
     * an image, for example, for a profile picture", whose [Demo.icon] is the drawable with resource
     * ID [R.drawable.ic_image_black_24dp] (an idealized icon of two white mountains with a blue
     * background), and the [Demo.action] resource ID [R.id.action_mainFragment_to_imagePickerFragment]
     * (which is the navigation action for navigating to the [ImagePickerFragment]).
     *
     * We initialize our [DemoAdapter] variable `val adapter` to an instance constructed to use
     * `demoItems` as its dataset and a lambda for its `itemClickListener` which has the [NavController]
     * navigate to the [Demo.action] action resource ID of the [Demo] it is called with. The
     * `itemClickListener` is called from the [View.OnClickListener] of the `root` view of the
     * [DemoViewHolder] clicked with the [Demo] in the position in the dataset corresponding to that
     * [DemoViewHolder]. We then set the adapter of the [FragmentMainBinding.demosList] `RecyclerView`
     * in `binding` to `adapter`. Finally we return the outermost [View] in the associated layout
     * file of `binding` to the caller to serve as our fragment's UI.
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
    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentMainBinding.inflate(layoutInflater)

        val demoItems: Array<Demo> = arrayOf(
            // TODO: Add other SAF demos
            Demo(
                getString(R.string.image_picker_demo_title),
                getString(R.string.image_picker_demo_text),
                R.drawable.ic_image_black_24dp,
                R.id.action_mainFragment_to_imagePickerFragment
            )
        )

        val adapter = DemoAdapter(demoItems) { clickedDemo ->
            findNavController().navigate(clickedDemo.action)
        }
        binding.demosList.adapter = adapter

        return binding.root
    }
}

/**
 * Data class holding information relating to a demo that the user can navigate to.
 *
 * @param title the text which should be displayed in the "demo_title" [TextView] for this demo item.
 * @param text the text which should be displayed in the "demo_text" [TextView] for this demo item.
 * @param icon a drawable to display in the "demo_icon" [ImageView] for this demo item.
 * @param action the resource ID for a navigation action that will navigate to the demo.
 */
data class Demo(
    val title: String,
    val text: String,
    @DrawableRes val icon: Int,
    @IdRes val action: Int
)

private class DemoViewHolder(
    val binding: ListItemDemoBinding
    ) : RecyclerView.ViewHolder(binding.root)

private class DemoAdapter(
    private val demos: Array<Demo>,
    private val itemClickListener: (Demo) -> Unit
) : RecyclerView.Adapter<DemoViewHolder>() {
    @Suppress("ComplexRedundantLet")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DemoViewHolder =
        LayoutInflater.from(parent.context).let { layoutInflater ->
            DemoViewHolder(ListItemDemoBinding.inflate(layoutInflater, parent, false))
        }

    override fun getItemCount() = demos.size

    override fun onBindViewHolder(holder: DemoViewHolder, position: Int) {
        val demo = demos[position]
        holder.binding.demoIcon.setImageResource(demo.icon)
        holder.binding.demoTitle.text = demo.title
        holder.binding.demoText.text = demo.text
        holder.binding.root.setOnClickListener { itemClickListener(demos[position]) }
    }
}