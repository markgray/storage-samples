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
import java.util.*

/**
 * Simple data class to hold information about an image included in the device's MediaStore.
 *
 * @param id this is the value of the [MediaStore.Images.Media._ID] column of the [Cursor] retrieved
 * from the [MediaStore] for the image whose information we hold (a unique [Long] for each image).
 * @param displayName this is the value of the [MediaStore.Images.Media.DISPLAY_NAME] column of the
 * [Cursor] retrieved from the [MediaStore] for the image whose information we hold (the display
 * name of the media item ie. the file name).
 * @param dateAdded this is the value of the [MediaStore.Images.Media.DATE_ADDED] column of the
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
        val DiffCallback = object : DiffUtil.ItemCallback<MediaStoreImage>() {
            override fun areItemsTheSame(oldItem: MediaStoreImage, newItem: MediaStoreImage) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MediaStoreImage, newItem: MediaStoreImage) =
                oldItem == newItem
        }
    }
}