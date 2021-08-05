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
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.ArrayList

/**
 * Adapter for RecyclerView, which manages the image documents.
 */
internal class ImageAdapter(private val mContext: Context) :
    RecyclerView.Adapter<ImageViewHolder>() {
    /** Holds the information for already retrieved images.  */
    private val mImageDocuments: MutableList<ImageDocument> = ArrayList()

    /**
     * The total size of the all images. This number should be the size for all images even if
     * they are not fetched from the ContentProvider.
     */
    private var mTotalSize = 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.viewholder_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val resources = mContext.resources
        if (mImageDocuments.size > position) {
            Glide.with(mContext)
                .load(mImageDocuments[position].mAbsolutePath)
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
     * Add an image as part of the adapter.
     *
     * @param imageDocument the image information to be added
     */
    fun add(imageDocument: ImageDocument) {
        mImageDocuments.add(imageDocument)
    }

    /**
     * Set the total size of all images.
     *
     * @param totalSize the total size
     */
    fun setTotalSize(totalSize: Int) {
        mTotalSize = totalSize
    }

    /**
     * @return the number of images already fetched and added to this adapter.
     */
    val fetchedItemCount: Int
        get() = mImageDocuments.size

    override fun getItemCount(): Int {
        return mTotalSize
    }

    /**
     * Represents information for an image.
     */
    internal class ImageDocument {
        var mAbsolutePath: String? = null
        var mDisplayName: String? = null
    }
}