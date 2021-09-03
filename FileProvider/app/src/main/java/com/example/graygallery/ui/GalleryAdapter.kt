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

package com.example.graygallery.ui

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.api.load
import com.example.graygallery.R
import java.io.File

/**
 * This [ListAdapter] supplies views to the [RecyclerView] that [GalleryFragment] uses to display
 * all of the images that the user has added to the app's private "images/" folder. The type of
 * lists that this adapter will receive hold [File] objects, and our [RecyclerView.ViewHolder] is
 * an [ImageViewHolder].
 *
 * @param onClick the [OnClickListener] of the [ImageViewHolder.imageView] view calls this method
 * with the [File] that is stored as the `tag` property of the [ImageViewHolder.rootView] view.
 */
class GalleryAdapter(private val onClick: (File) -> Unit) :
    ListAdapter<File, ImageViewHolder>(
        ListItemCallback()
    ) {

    /**
     * Called when [RecyclerView] needs a new [ImageViewHolder] of the given type to represent an
     * item. We initialize our [LayoutInflater] variable `val layoutInflater` to the [LayoutInflater]
     * from the context of our [ViewGroup] parameter [parent]. Then we initialize our [View] variable
     * `val view` to the [View] that `layoutInflater` inflates from the layout file with the resource
     * ID [R.layout.gallery_item_layout] using [parent] for its `LayoutParams` without attaching to
     * it. Finally we return a new instance of [ImageViewHolder] constructed to use `view` as its
     * [ImageViewHolder.rootView] and [onClick] as the method that the [OnClickListener] of the
     * [ImageView] in `view` calls when the user clicks on the [ImageView].
     *
     * @param parent The [ViewGroup] into which the new View will be added after it is bound to an
     * adapter position.
     * @param viewType The view type of the new View.
     * @return A new [ImageViewHolder] that holds a [View] of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.gallery_item_layout, parent, false)
        return ImageViewHolder(view, onClick)
    }

    /**
     * Called by [RecyclerView] to display the data at the specified position. This method should
     * update the contents of the [ImageViewHolder.itemView] to reflect the item at the given
     * position. We initialize our [File] variable `val file` to the [File] at position `position`
     * in our dataset, then set the tag associated with the [ImageViewHolder.rootView] view of our
     * [ImageViewHolder] parameter [holder] to `file`. Then we call the [ImageView.load] extension
     * function on the [ImageViewHolder.imageView] view of [holder] to have it load the image in
     * `file` into itself, enabling a crossfade animation when the load completes successfully.
     *
     * @param holder The [ImageViewHolder] which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val file: File = getItem(position)
        holder.rootView.tag = file

        holder.imageView.load(file) {
            crossfade(true)
        }
    }
}

/**
 * This is the [DiffUtil.ItemCallback] implementation we have our [ListAdapter] super use for
 * calculating the diff between two non-`null` items in our dataset.
 */
class ListItemCallback : DiffUtil.ItemCallback<File>() {
    /**
     * Called to check whether two objects represent the same item. We return the [Boolean] result
     * of comparing the [String] `name` property (value returned by [File.getName]) of our two
     * parameters for equality.
     *
     * @param oldItem The item in the old list.
     * @param newItem The item in the new list.
     * @return `true` if the two items represent the same object or `false` if they are different.
     */
    override fun areItemsTheSame(oldItem: File, newItem: File) =
        oldItem.name == newItem.name

    /**
     * Called to check whether two items have the same data. We return the [Boolean] result of
     * comparing our two parameters for structural equality.
     *
     * @param oldItem The item in the old list.
     * @param newItem The item in the new list.
     * @return `true` if the contents of the items are the same or `false` if they are different.
     */
    override fun areContentsTheSame(oldItem: File, newItem: File) =
        oldItem == newItem
}

/**
 * Basic [RecyclerView.ViewHolder] for our gallery.
 *
 * @param view the [View] we should use as our [rootView] (aka [itemView]).
 * @param onClick the function that the [OnClickListener] of the [ImageView] in [rootView] should
 * call with the [File] that is stored in the `tag` of [rootView].
 */
class ImageViewHolder(view: View, onClick: (File) -> Unit) : RecyclerView.ViewHolder(view) {
    val rootView = view
    val imageView: ImageView = view.findViewById(R.id.image)

    init {
        imageView.setOnClickListener {
            val image = rootView.tag as? File ?: return@setOnClickListener
            onClick(image)
        }
    }
}