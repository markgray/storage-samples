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

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.android.contentproviderpaging.common.R

/**
 * [ViewHolder] that represents an image.
 *
 * @param itemView the [View] that we are holding, inflated from the `R.layout.viewholder_image`
 * layout file.
 */
internal class ImageViewHolder(itemView: View) : ViewHolder(itemView) {
    /**
     * The [ImageView] in our [itemView] that displays the jpeg.
     */
    var mImageView: ImageView = itemView.findViewById(R.id.imageview)

    /**
     * The [TextView] in our [itemView] that displays the image number.
     */
    var mTextView: TextView = itemView.findViewById(R.id.textview_image_label)

}
