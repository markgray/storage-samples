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

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.BaseColumns
import androidx.loader.content.Loader
import java.io.File

/**
 * The contract for the [ImageProvider].
 */
internal object ImageContract {
    /**
     * The authority to use to access our [ImageProvider], it is the same as the android:authorities
     * attribute of the `provider` element in our AndroidManifest.xml file which specifies the
     * authorities under which this content provider can be found.
     */
    const val AUTHORITY = "com.example.android.contentproviderpaging.documents"

    /**
     * The [Uri] that is used to send a query to [ImageProvider], used in the `loadInBackground`
     * override of the `LoaderCallback` used to create a new [Loader] instance that is ready to
     * start loading in [ImageClientFragment].
     */
    val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/images")

    /**
     * The projection for all of its columns which [ImageProvider] defaults to if the projection
     * that its `query` override is passed is `null` (as it always is in our case). This projection
     * is used in the construction of the [MatrixCursor] which is used to build the [Cursor] for
     * each row that the `query` override of [ImageProvider] returns.
     */
    val PROJECTION_ALL = arrayOf(
        BaseColumns._ID,
        Columns.DISPLAY_NAME,
        Columns.ABSOLUTE_PATH,
        Columns.SIZE
    )

    /**
     * The column names used by [ImageProvider].
     */
    internal interface Columns : BaseColumns {
        companion object {
            /**
             * The file name of the file or directory, which is just the last name in the
             * [ABSOLUTE_PATH] to the file. Comes from the [File.getName] method (aka kotlin
             * `name` property)
             */
            const val DISPLAY_NAME = "display_name"

            /**
             * The absolute path of the file or directory, comes from the [File.getAbsolutePath]
             * method (aka kotlin `absolutePath` property)
             */
            const val ABSOLUTE_PATH = "absolute_path"

            /**
             * The length of the file (the return value is unspecified if the pathname denotes a
             * directory), comes from the [File.length] method.
             */
            const val SIZE = "size"
        }
    }
}