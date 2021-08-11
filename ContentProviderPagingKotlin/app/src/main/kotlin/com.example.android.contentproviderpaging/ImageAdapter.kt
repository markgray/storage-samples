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

import android.content.Context
import android.content.res.Resources
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import java.util.*

/**
 * Adapter for [RecyclerView], which manages the image documents.
 */
internal class ImageAdapter(private val mContext: Context) :
    RecyclerView.Adapter<ImageViewHolder>() {

    /**
     * Holds the information for already retrieved images.
     */
    private val mImageDocuments = ArrayList<ImageDocument>()

    /**
     * The total number of all images. This number should be the number of all images even if
     * they are not fetched from the ContentProvider.
     */
    private var mTotalSize: Int = 0

    /**
     * Called when [RecyclerView] needs a new [ImageViewHolder] of the given type to represent an
     * item. This new ViewHolder should be constructed with a new [View] that can represent items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file. The new [ImageViewHolder] will be used to display items of the adapter using
     * [onBindViewHolder]. Since it will be re-used to display different items in the data set, it
     * is a good idea to cache references to sub views of the View to avoid unnecessary
     * [View.findViewById] calls.
     *
     * We initialize our [View] variable `val view` to the [View] that the [LayoutInflater] from
     * the context of our [ViewGroup] parameter [parent] returns when its [LayoutInflater.inflate]
     * method inflates the layout file with resource ID [R.layout.viewholder_image] using [parent]
     * for its LayoutParams without attaching to it. We then return a new instance of [ImageViewHolder]
     * constructed to hold `view`.
     *
     * @param parent   The [ViewGroup] into which the new View will be added after it is bound to
     * an adapter position.
     * @param viewType The view type of the new View.
     * @return A new [ImageViewHolder] that holds a [View] of the given view type.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.viewholder_image, parent, false)
        return ImageViewHolder(view)
    }

    /**
     * Called by [RecyclerView] to display the data at the specified position. This method should
     * update the contents of the [ImageViewHolder.itemView] to reflect the item at the given
     * position. We initialize our [Resources] variable `val resources` to a [Resources] instance
     * for our application's package. Then we branch on whether the size of our [MutableList] of
     * [ImageDocument] dataset field [mImageDocuments] is greater than our [position] parameter
     *  1. The size of [mImageDocuments] is greater (the image has already been fetched from our
     *  provider):
     *      - We begin a load with [Glide] passing in [mContext] as the [Context]
     *      - Request that it load the file whose [ImageDocument.mAbsolutePath] path is given by the
     *      [ImageDocument] in the [position] element of [mImageDocuments].
     *      - Set the Android resource id for a Drawable resource to display while a resource is
     *      loading to [R.drawable.cat_placeholder].
     *      - And sets the [ImageView] the resource will be loaded into to the [ImageViewHolder.mImageView]
     *      field of [holder]
     *      - We then set the text of the [ImageViewHolder.mTextView] field of [holder] to the string
     *      value of [position] plus 1.
     *  2. The size of [mImageDocuments] is less than or equal (the image has not yet been fetched
     *  from our provider):
     *      - We set the [ImageView] that the [ImageViewHolder.mImageView] field of [holder] will
     *      display to the drawable with resource ID [R.drawable.cat_placeholder]
     *
     * @param holder   The [ImageViewHolder] which should be updated to represent the contents of
     * the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val resources: Resources = mContext.resources
        if (mImageDocuments.size > position) {
            Glide.with(mContext)
                .load(mImageDocuments[position].absolutePath)
                .placeholder(R.drawable.cat_placeholder)
                .into(holder.mImageView)
            holder.mTextView.text = (position + 1).toString()
        } else {
            holder.mImageView.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.cat_placeholder, null)
            )
        }
    }

    /**
     * Add an [ImageDocument] to the dataset of the adapter which is held in our [MutableList] of
     * [ImageDocument] field [mImageDocuments].
     *
     * @param imageDocument the image information to be added
     */
    fun add(imageDocument: ImageDocument) {
        mImageDocuments.add(imageDocument)
    }

    /**
     * Set the total number of items in the data set held by the adapter. This is the value returned
     * by our override of the [getItemCount] method and represents the total number of images which
     * our provider can eventually have supplied us with, not the number of images already fetched
     * and added to the adapter which is returned by our [fetchedItemCount] property. We just set
     * our [mTotalSize] field to our [Int] parameter [totalSize].
     *
     * @param totalSize the total number of items in the data set
     */
    fun setTotalSize(totalSize: Int) {
        mTotalSize = totalSize
    }

    /**
     * Returns the number of images already fetched and added to this adapter, which is the size of
     * our [MutableList] of [ImageDocument] field [mImageDocuments].
     *
     * @return the number of images already fetched and added to this adapter.
     */
    val fetchedItemCount: Int
        get() = mImageDocuments.size

    /**
     * Returns the total number of items in the data set held by the adapter. We just return the
     * [Int] value stored in our [mTotalSize] field by our [setTotalSize] method.
     *
     * @return The total number of items in this adapter.
     */
    override fun getItemCount(): Int {
        return mTotalSize
    }

    /**
     * Represents information for an image.
     *
     * @param absolutePath The absolute path in our file system for the jpeg image.
     * @param displayName The file name of the jpeg image (apparently unused?).
     */
    internal data class ImageDocument(val absolutePath: String, val displayName: String)
}
