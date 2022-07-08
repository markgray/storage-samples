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

package com.example.graygallery.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.*
import com.example.graygallery.ui.AppViewModel
import java.io.FileOutputStream
import kotlin.coroutines.CoroutineContext

/**
 * The string value of this `enum` is used in calls to our [generateFilename] method as the prefix
 * for a file name. [CAMERA] is used by the [AppViewModel.saveImageFromCamera] method, [INTERNET] by
 * [AppViewModel.saveRandomImageFromInternet] and [FILEPICKER] by our [copyImageFromStream] method.
 */
enum class Source {
    /**
     * Prefix of files created by the [AppViewModel.saveImageFromCamera] method.
     */
    CAMERA,

    /**
     * Prefix of files created by the [copyImageFromStream] method.
     */
    FILEPICKER,

    /**
     * Prefix of files created by the [AppViewModel.saveRandomImageFromInternet] method.
     */
    INTERNET;

    /**
     * Returns the `name` of `this` [Source] converted to lowercase.
     *
     * @return the [Enum.name] of `this` [Source] converted to lowercase.
     */
    override fun toString(): String {
        return name.lowercase(Locale.getDefault())
    }
}

/**
 * Returns a filename formed by concatenating the [String] value of its [Source] parameter [source]
 * followed by the string value of the current time in milliseconds, followed by the ".jpg" string.
 *
 * @param source the [Source] whose string value we are to use as the prefix of the filename.
 * @return a filename formed by concatenating the [String] value of its [Source] parameter [source]
 * followed by the string value of the current time in milliseconds, followed by the ".jpg" string.
 */
fun generateFilename(source: Source): String = "$source-${System.currentTimeMillis()}.jpg"

/**
 * Copies the [InputStream] parameter [input] to a file in the directory [directory] whose name is
 * created by our [generateFilename] method from the enum [Source.FILEPICKER]. We call a suspending
 * block with the [CoroutineContext] of [Dispatchers.IO] (the CoroutineDispatcher that is designed
 * for offloading blocking IO tasks to a shared pool of threads) whose lambda calls the
 * [InputStream.copyTo] method of our [InputStream] parameter [input] to have it copy itself into
 * the directory whose directory is [directory] and whose file is the [FileOutputStream] created
 * from the filename generated by our [generateFilename] method for the [Source.FILEPICKER] enum
 * by the [File.outputStream] extension function (this last bit relies on a rather dubious bit of
 * magic which replaces a string with a call to the [File] constructor that takes a string when a
 * [File] is required instead of a [String]).
 *
 * @param input the [InputStream] we are to copy to the file.
 * @param directory the [File] path to the directory we are to write the file into.
 */
suspend fun copyImageFromStream(input: InputStream, directory: File) {
    withContext(Dispatchers.IO) {
        input.copyTo(File(directory, generateFilename(Source.FILEPICKER)).outputStream())
    }
}