/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.samples.mediastore

import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import java.util.Date

/**
 * Simple data class to hold information about an image included in the device's MediaStore.
 *
 * @param id this is the value of the `MediaStore.Images.Media._ID` column of the [Cursor] retrieved
 * from the [MediaStore] for the image whose information we hold (a unique [Long] for each image).
 * @param displayName this is the value of the `MediaStore.Images.Media.DISPLAY_NAME` column of the
 * [Cursor] retrieved from the [MediaStore] for the image whose information we hold (the display
 * name of the media item ie. the file name).
 * @param dateAdded this is the value of the `MediaStore.Images.Media.DATE_ADDED` column of the
 * [Cursor] retrieved from the [MediaStore] for the image whose information we hold (time the media
 * item was first added).
 * @param contentUri a content [Uri] for the image formed by appending the [id] to the value of
 * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] (content://media/external/images/media) example:
 * content://media/external/images/media/597
 */
data class MediaStoreImage(
    val id: Long,
    val displayName: String,
    val dateAdded: Date,
    val contentUri: Uri
) {
    companion object {
        /**
         * This is the [DiffUtil.ItemCallback] implementation used by the `GalleryAdapter` custom
         * [ListAdapter] to calculate the diff between two non-null [MediaStoreImage] objects in its
         * dataset when a new [List] is submitted.
         */
        val DiffCallback: DiffUtil.ItemCallback<MediaStoreImage> = object : DiffUtil.ItemCallback<MediaStoreImage>() {
            /**
             * Called to check whether two objects represent the same item. We return the [Boolean]
             * result of comparing the [MediaStoreImage.id] property or our two parameters for
             * equality (the [MediaStoreImage.id] is unique for each image).
             *
             * @param oldItem The item in the old list.
             * @param newItem The item in the new list.
             * @return `true` if the two items represent the same object or `false` if they are
             * different.
             */
            override fun areItemsTheSame(oldItem: MediaStoreImage, newItem: MediaStoreImage) =
                oldItem.id == newItem.id

            /**
             * Called to check whether two items have the same data. We return the [Boolean]
             * result of comparing our two parameters for equality.
             *
             * @param oldItem The item in the old list.
             * @param newItem The item in the new list.
             * @return True if the contents of the items are the same or false if they are different.
             */
            override fun areContentsTheSame(oldItem: MediaStoreImage, newItem: MediaStoreImage) =
                oldItem == newItem
        }
    }
}