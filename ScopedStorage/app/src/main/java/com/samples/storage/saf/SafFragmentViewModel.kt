/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samples.storage.saf

import android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultLauncher
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

/**
 * Number of bytes to read at a time from an open stream
 */
private const val FILE_BUFFER_SIZE_BYTES = 1024

/**
 * [ViewModel] contains various examples for how to work with the contents of documents
 * opened with the Storage Access Framework.
 */
class SafFragmentViewModel : ViewModel() {

    /**
     * Writes some random text to its [OutputStream] parameter [outputStream].
     *
     * It's easiest to work with documents selected with the [Intent.ACTION_CREATE_DOCUMENT] action
     * by simply opening an [OutputStream], which is what the [ActivityResultLauncher] field
     * [SafFragment.actionCreateDocument] does with the [Uri] that is returned from the activity it
     * launches and that [OutputStream] is then used as the argument to this method.
     *
     * We return the result of the execution of a suspending block created using the [withContext]
     * method for the [CoroutineContext] of [Dispatchers.IO]. The code in that suspending block:
     *  - initializes its [MutableList] of [String] variable `val lines` to a new instance
     *  - loops over `lineNumber` from 1 to a random [Int] between 1 (inclusive) and 5 (exclusive).
     *  - in that loop it initializes its [String] variable `val line` to the [String] "hello world "
     *  repeated from 1 (inclusive) until 5 (exclusive).
     *  - replaces the first character of `line` with its `titlecase` character and adds `line` to
     *  the `lines` [MutableList].
     *
     * When done with the loop we initialize our [String] variable `val contents` to a [String] with
     * all of the elements in `lines` joined together using the [System.lineSeparator] character as
     * the separator. We then create a [BufferedWriter] on the [OutputStream] parameter [outputStream]
     * using the charset [StandardCharsets.UTF_8] and use the [use] extension function on that
     * [BufferedWriter] to call its [BufferedWriter.write] method to write `contents` to it and
     * close it down correctly whether an exception is thrown or not. The final line of the suspend
     * block is just `contents` which is returned as the result of the block and the
     * [createDocumentExample] method returns this to its caller.
     *
     * @param outputStream the [OutputStream] we are supposed to write to.
     * @return the [String] that we wrote to our [OutputStream] parameter [outputStream].
     */
    suspend fun createDocumentExample(outputStream: OutputStream): String {

        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            val lines = mutableListOf<String>()

            for (lineNumber in 1..Random.nextInt(1, 5)) {
                val line = "hello world ".repeat(Random.nextInt(1, 5))
                lines += line.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                }
            }

            val contents: String = lines.joinToString(separator = System.lineSeparator())

            outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer: BufferedWriter ->
                writer.write(contents)
            }
            contents
        }
    }

    /**
     * Reads the contents of its [InputStream] parameter [inputStream] and generates "SHA-256"
     * message digest hash from it. It then creates a string from the hex value of all the bytes
     * of the digest separated using the separator ":" and returns it to the caller.
     *
     * Similar to [Intent.ACTION_CREATE_DOCUMENT], it's easiest to work with documents selected
     * with the [Intent.ACTION_OPEN_DOCUMENT] action by simply opening an [InputStream] or
     * [OutputStream], depending on the need. In this example, since we don't want to disturb the
     * contents of the file, we're just going to use an [InputStream] to generate a hash of
     * the file's contents. Which is what the [ActivityResultLauncher] field
     * [SafFragment.actionOpenDocument] does with the [Uri] that is returned from the activity it
     * launches and that [InputStream] is then used as the argument to this method.
     *
     * Since hashing the contents of a large file may take some time, this is done in a
     * suspend function with the [Dispatchers.IO] coroutine context.
     *
     * We return the result of the execution of a suspending block created using the [withContext]
     * method for the [CoroutineContext] of [Dispatchers.IO]. The code in that suspending block uses
     * the [use] extension function on our [InputStream] parameter [inputStream] to execute a
     * lambda using `stream` as the explicit argument name for the [InputStream] (the [InputStream]
     * will be closed down correctly whether an exception is thrown or not when the lambda finishes).
     * In the lambda of the [use] extension function we:
     *  - initialize our [MessageDigest] variable `val messageDigest` to a new instance that
     *  implements the "SHA-256" digest algorithm.
     *  - initialize our [ByteArray] variable `val buffer` to a new instance whose size is
     *  [FILE_BUFFER_SIZE_BYTES].
     *  - initialize our [Int] variable `var bytesRead` to the number of bytes read from `stream`
     *  using its [InputStream.read] method into `buffer`.
     *  - we then loop while `bytesRead` is greater than 0, calling the [MessageDigest.update]
     *  method of `messageDigest` to update it to include the `bytesRead` bytes in `buffer` starting
     *  from index 0, and then we again set `bytesRead` to the number of bytes read from `stream`
     *  using its [InputStream.read] method into `buffer`.
     *
     * When done with the loop we initialize our [ByteArray] variable `val hashResult` to the
     * [ByteArray] returned from the [MessageDigest.digest] method of `messageDigest` when it
     * completes the hash computation by performing final operations such as padding and returns
     * the resulting hash value. The last line of the suspending block (which is returned as the
     * value of the call to [withContext] and thus the call to [openDocumentExample]) is a call
     * to the [ByteArray.joinToString] method of `hashResult` which returns the result of
     * formatting each byte as a two character hex string, and joining them together into a [String]
     * using the character ":" as the separator.
     *
     * @param inputStream the [InputStream] that we are supposed to read from.
     * @return a "SHA-256" message digest hash of the contents of our [InputStream] parameter
     * [inputStream] formatted as two character hex strings separated by the ":" character.
     */
    suspend fun openDocumentExample(inputStream: InputStream): String {
        @Suppress("BlockingMethodInNonBlockingContext")
        return withContext(Dispatchers.IO) {
            inputStream.use { stream: InputStream ->
                val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-256")

                val buffer = ByteArray(FILE_BUFFER_SIZE_BYTES)
                var bytesRead: Int = stream.read(buffer)
                while (bytesRead > 0) {
                    messageDigest.update(buffer, 0, bytesRead)
                    bytesRead = stream.read(buffer)
                }
                val hashResult: ByteArray = messageDigest.digest()
                hashResult.joinToString(separator = ":") { "%02x".format(it) }
            }
        }
    }

    /**
     * Simple example of using [DocumentFile] to get all the documents in a folder (by using
     * [Intent.ACTION_OPEN_DOCUMENT_TREE]).
     * It's possible to use [DocumentsContract] and [ContentResolver] directly, but using
     * [DocumentFile] allows us to access an easier to use API.
     *
     * While it's _possible_ to search across multiple directories and recursively work with files
     * via SAF, there can be significant performance penalties to this type of usage. If your
     * use case requires this, consider looking into the permission [MANAGE_EXTERNAL_STORAGE].
     *
     * Accessing any field in the [DocumentFile] object, aside from [DocumentFile.getUri],
     * ultimately performs a lookup with the system's [ContentResolver], and should thus be
     * performed off the main thread, which is why we're doing this transformation from
     * [DocumentFile] to file name and [Uri] in a coroutine.
     *
     * We return the value returned by the suspending lambda block of a call to the method [withContext]
     * which calls the block using the [CoroutineContext] of [Dispatchers.IO]. In that block we branch
     * on whether our [DocumentFile] parameter [folder] is a directory:
     *  - [folder] is a directory: we call the [DocumentFile.listFiles] of [folder] to retrieve an
     *  array of [DocumentFile] of the files contained in the directory represented by [folder] or
     *  `null` if there are none, and apply the [mapNotNull] extension function to that array to
     *  convert it to a list containing only the non-`null` results of constructing a [Pair] with
     *  the [Pair.first] field the `name` property of the [DocumentFile] file and the [Pair.second]
     *  field the `uri` property of the [DocumentFile] file for each element with a non-`null` `name`
     *  field in the original array. This is the list that will be returned by [withContext] and by
     *  the [listFiles] method when [folder] is a directory.
     *  - [folder] is NOT a directory we return an empty list to [withContext] which [listFiles] then
     *  returns to its caller.
     *
     * @param folder the [DocumentFile] representation of the folder whose contents we are to read
     * and use to create a [List] of [Pair] where the [Pair.first] entry is taken from the `name`
     * of each [DocumentFile] and whose [Pair.second] is a [Uri] for the underlying document
     * represented by that [DocumentFile] entry in [folder].
     * @return a [List] of [Pair] where the [Pair.first] entry is taken from the `name` of each
     * [DocumentFile] contained in the [DocumentFile] parameter [folder] folder and whose [Pair.second]
     * is a [Uri] for the underlying document represented by that [DocumentFile] entry in [folder] or
     * we return an empty list if [folder] is not a directory.
     */
    suspend fun listFiles(folder: DocumentFile): List<Pair<String, Uri>> {
        return withContext(Dispatchers.IO) {
            if (folder.isDirectory) {
                folder.listFiles().mapNotNull { file: DocumentFile ->
                    if (file.name != null) Pair(file.name!!, file.uri) else null
                }
            } else {
                emptyList()
            }
        }
    }
}
