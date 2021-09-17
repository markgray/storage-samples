/*
 * Copyright 2021 The Android Open Source Project
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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView

/**
 * This data class holds a string resource ID for a [String] that names a demo fragment, and the
 * resource ID for a navigation action that can be used to navigate to that demo fragment.
 *
 * @param nameRes the resource ID for a [String] that can be used as the text of the [View] that is
 * used by [RecyclerView] to represent this [Action] object.
 * @param actionRes the resource ID for a navigation action that can be used to navigate to the demo
 * fragment that this [Action] object represents.
 */
data class Action(@StringRes val nameRes: Int, @IdRes val actionRes: Int)

/**
 * The custom [RecyclerView.Adapter] that is used to feed views to the [RecyclerView] in the UI of
 * [MainFragment].
 *
 * @param dataSet the [Array] of [Action] objects we should use as our dataset.
 */
class ActionListAdapter(private val dataSet: Array<Action>) :
    RecyclerView.Adapter<ActionListAdapter.ViewHolder>() {

    /**
     * Our custom [RecyclerView.ViewHolder], it just caches a reference to the [TextView] with
     * resource ID [R.id.textView] in the [View] parameter of its constructor (its [itemView]).
     *
     * @param view a [View] that is inflated from the layout file [R.layout.list_row_item].
     */
    @Suppress("RedundantEmptyInitializerBlock")
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.textView)

        init {
            // Define click listener for the ViewHolder's View.
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.list_row_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val context = viewHolder.textView.context

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.textView.text = context.getString(dataSet[position].nameRes)
        viewHolder.textView.setOnClickListener {
            it.findNavController().navigate(dataSet[position].actionRes)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size
}
