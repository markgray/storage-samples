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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.navigation.NavController
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
        /**
         * Cached reference to the [TextView] in our [itemView] with resource ID [R.id.textView]
         * that the [ActionListAdapter.onBindViewHolder] override uses to display the [String] with
         * the resource ID of the [Action.nameRes] field of the [Action] that it is binding to our
         * [ViewHolder].
         */
        val textView: TextView = view.findViewById(R.id.textView)

        init {
            // Define click listener for the ViewHolder's View.
        }
    }

    /**
     * Called when [RecyclerView] needs a new [ViewHolder] of the given type to represent an item.
     * We initialize our [View] variable `val view` by having the [LayoutInflater] from the [Context]
     * of our [ViewGroup] parameter [parent] inflate the layout file with ID [R.layout.list_row_item]
     * (the file layout/list_row_item.xml) using [parent] for the layout params without attaching to
     * it. Finally we return a new instance of [ViewHolder] constructed to use `view` as its itemView.
     * Invoked by the layout manager.
     *
     * @param parent The [ViewGroup] into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new [View].
     * @return A new [ViewHolder] that holds a [View] of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_row_item, parent, false)

        return ViewHolder(view)
    }

    /**
     * Called by [RecyclerView] to display the data at the specified position. This method should
     * update the contents of the [ViewHolder.itemView] to reflect the item at the given position.
     * We initialize our [Context] variable `val context` to the [Context] of the [TextView] field
     * [ViewHolder.textView] in [holder]. Next we set the text of that [TextView] to the localized
     * string from the application's package's default string table whose resource ID is given by
     * the [Action.nameRes] field of the [Action] object at position [position] in our [Array] of
     * [Action] dataset field [dataSet], and set the [View.OnClickListener] of that [TextView] to a
     * lambda which finds a [NavController] associated with the [TextView] and calls the
     * [NavController.navigate] method of that [NavController] to have it navigate to the action ID
     * which is in the [Action.actionRes] field of the [Action] object at position [position] in our
     * [dataSet] dataset. Invoked by the layout manager.
     *
     * @param holder The [ViewHolder] which should be updated to represent the contents of the item
     * at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context: Context = holder.textView.context

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        holder.textView.text = context.getString(dataSet[position].nameRes)
        holder.textView.setOnClickListener {
            it.findNavController().navigate(dataSet[position].actionRes)
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter. We just return the size
     * of our [Array] of [Action] field [dataSet] to the caller. Invoked by the layout manager.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int = dataSet.size
}
