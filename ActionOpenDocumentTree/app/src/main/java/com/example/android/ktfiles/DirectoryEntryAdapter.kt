/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.ktfiles

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * The [RecyclerView.Adapter] used to create, hold, and supply views of [CachingDocumentFile] objects
 * for the [RecyclerView] displayed by [DirectoryFragment].
 *
 * @param clickListeners holds the [View.OnClickListener] and [View.OnLongClickListener] callbacks
 * that should be called when one of our views is clicked or long-clicked.
 */
class DirectoryEntryAdapter(
    private val clickListeners: ClickListeners
) : RecyclerView.Adapter<DirectoryEntryAdapter.ViewHolder>() {

    /**
     * Our dataset of [CachingDocumentFile] objects.
     */
    private val directoryEntries = mutableListOf<CachingDocumentFile>()

    /**
     * Called when RecyclerView needs a new [ViewHolder] of the given type to represent an item.
     * This new [ViewHolder] should be constructed with a new [View] that can represent the items
     * of the given type. You can either create a new [View] manually or inflate it from an XML
     * layout file.
     *
     * The new [ViewHolder] will be used to display items of the adapter using [onBindViewHolder].
     * Since it will be re-used to display different items in the data set, it is a good idea to
     * cache references to sub views of the View to avoid unnecessary [View.findViewById] calls.
     *
     * We initialize our [View] variable `val view` by retrieving the [LayoutInflater] from the
     * [Context] of our [ViewGroup] parameter [parent] and using it to inflate the layout file with
     * ID [R.layout.directory_item] using [parent] for its LayoutParams without attaching to it.
     * Finally we return a new instance of [ViewHolder] constructed to hold `view`.
     *
     * @param parent   The [ViewGroup] into which the new [View] will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new [View].
     * @return A new [ViewHolder] that holds a [View] of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.directory_item, parent, false)
        return ViewHolder(view)
    }

    /**
     * Called by [RecyclerView] to display the data at the specified position. This method should
     * update the contents of the [ViewHolder.itemView] to reflect the item at the given position.
     *
     * We use the kotlin [with] method to call a lambda function block using our [ViewHolder] parameter
     * [viewHolder] as the receiver and in this lambda we:
     *  - Initialize our [CachingDocumentFile] variable `val item` to the object in position [position]
     *  in our [MutableList] of [CachingDocumentFile] dataset field [directoryEntries].
     *  - Initialize our [Int] variable `val itemDrawableRes` to the drawable resource ID
     *  [R.drawable.ic_folder_black_24dp] if `item` is a directory, or to [R.drawable.ic_file_black_24dp]
     *  if it is not a directory.
     *  - Set the text of the [TextView] field [ViewHolder.fileName] of our receiver to the
     *  [CachingDocumentFile.name] field of `item`.
     *  - Set the text of the [TextView] field [ViewHolder.mimeType] of our receiver to the
     *  [CachingDocumentFile.type] field of `item` or to the empty [String] if it is `null`.
     *  - Set the content of the [ImageView] field [ViewHolder.imageView] of our receiver to the
     *  drawable with resource ID `itemDrawableRes`.
     *  - Set the [View.OnClickListener] of the [View] field [ViewHolder.root] of our receiver to
     *  to a lambda that calls the [ClickListeners.onDocumentClicked] override of our [clickListeners]
     *  field with `item` as its parameter.
     *  - Set the [View.OnLongClickListener] of the [View] field [ViewHolder.root] of our receiver
     *  to a lambda that calls the [ClickListeners.onDocumentLongClicked] override of our
     *  [clickListeners] field with `item` as its parameter, and returns `true` to the caller to
     *  consume the long click.
     *
     * @param viewHolder The [ViewHolder] which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        with(viewHolder) {
            val item: CachingDocumentFile = directoryEntries[position]
            val itemDrawableRes: Int = if (item.isDirectory) {
                R.drawable.ic_folder_black_24dp
            } else {
                R.drawable.ic_file_black_24dp
            }

            fileName.text = item.name
            mimeType.text = item.type ?: ""
            imageView.setImageResource(itemDrawableRes)

            root.setOnClickListener {
                clickListeners.onDocumentClicked(item)
            }
            root.setOnLongClickListener {
                clickListeners.onDocumentLongClicked(item)
                true
            }
        }
    }

    /**
     * Returns the total number of items in the data set held by the adapter. We just return the
     * size of our [MutableList] of [CachingDocumentFile] dataset field [directoryEntries].
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount() = directoryEntries.size

    /**
     * Replaces our [MutableList] of [CachingDocumentFile] dataset field [directoryEntries] with the
     * entries in its [List] of [CachingDocumentFile] parameter [newList]. In a block synchronized
     * on [directoryEntries] we remove all elements from [directoryEntries], add all of the elements
     * of [newList] to [directoryEntries], and then call the [notifyDataSetChanged] method to notify
     * any registered observers of this [DirectoryEntryAdapter] that the data set has changed.
     *
     * @param newList the new [List] of [CachingDocumentFile] elements to use as our dataset.
     */
    @SuppressLint("NotifyDataSetChanged")  // TODO: Move to DiffUtil
    fun setEntries(newList: List<CachingDocumentFile>) {
        synchronized(directoryEntries) {
            directoryEntries.clear()
            directoryEntries.addAll(newList)
            notifyDataSetChanged()
        }
    }

    /**
     * Provide a reference to the type of views that you are using (custom [ViewHolder]).
     *
     * @param view the [itemView] we are to hold.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val root = view
        val fileName: TextView = view.findViewById(R.id.file_name)
        val mimeType: TextView = view.findViewById(R.id.mime_type)
        val imageView: ImageView = view.findViewById(R.id.entry_image)
    }
}

interface ClickListeners {
    fun onDocumentClicked(clickedDocument: CachingDocumentFile)
    fun onDocumentLongClicked(clickedDocument: CachingDocumentFile)
}
