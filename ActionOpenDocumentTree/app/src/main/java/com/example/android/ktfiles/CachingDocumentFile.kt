/*
 * Copyright 2019 The Android Open Source Project
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

package com.example.android.ktfiles

import android.content.ContentResolver
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Caching version of a [DocumentFile].
 *
 * A [DocumentFile] will perform a lookup (via the system [ContentResolver]), whenever a
 * property is referenced. This means that a request for [DocumentFile.getName] is a *lot*
 * slower than one would expect.
 *
 * To improve performance in the app, where we want to be able to sort a list of [DocumentFile]s
 * by name, we wrap it like this so the value is only looked up once.
 */
data class CachingDocumentFile(private val documentFile: DocumentFile) {
    /**
     * The display name of this [DocumentFile], used as the primary title displayed to a user.
     */
    val name: String? by lazy { documentFile.name }

    /**
     * The MIME type of this [DocumentFile], ie. concrete MIME type of a document. For example,
     * "image/png" or "application/pdf" for openable files. A document can also be a directory
     * containing additional documents, which is represented with the `MIME_TYPE_DIR` MIME type.
     */
    val type: String? by lazy { documentFile.type }

    /**
     * Indicates if this [DocumentFile] represents a directory: `true` if this file is a directory,
     * `false` otherwise.
     */
    val isDirectory: Boolean by lazy { documentFile.isDirectory }

    /**
     * Return a [Uri] for the underlying document represented by this [DocumentFile]. This can be
     * used with other platform APIs to manipulate or share the underlying content.
     */
    val uri: Uri get() = documentFile.uri

    /**
     * Called to rename our [DocumentFile] field [documentFile] to our [String] parameter [newName].
     * First we call the [DocumentFile.renameTo] method of [documentFile] to have it rename its file
     * to [newName]. Finally we return a new instance of [CachingDocumentFile] constructed to cache
     * the renamed [documentFile].
     *
     * @param newName the name that the user wishes this [DocumentFile] to be renamed as.
     */
    fun rename(newName: String): CachingDocumentFile {
        documentFile.renameTo(newName)
        return CachingDocumentFile(documentFile)
    }
}

/**
 * An extension function which creates and returns a [List] of [CachingDocumentFile] objects from
 * the [DocumentFile] elements in its receiver [Array] of [DocumentFile]s. First we initialize our
 * [MutableList] of [CachingDocumentFile] variable `val list` with a new instance. Then we loop over
 * the [DocumentFile] variable `document` for all of the entries in the `this` of our receiver adding
 * a new instance of [CachingDocumentFile] constructed to cache `document`. Finally we return list to
 * the caller.
 *
 * @return a [MutableList] containing [CachingDocumentFile]s constructed from all or the [DocumentFile]
 * objects in its [Array] of [DocumentFile] receiver.
 */
fun Array<DocumentFile>.toCachingList(): List<CachingDocumentFile> {
    val list = mutableListOf<CachingDocumentFile>()
    for (document in this) {
        list += CachingDocumentFile(document)
    }
    return list
}