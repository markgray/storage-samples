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

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the [DirectoryFragment].
 *
 * @param application the global [Application] of our activity, used to supply Application context
 * to our [AndroidViewModel] super class.
 */
class DirectoryFragmentViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * The [MutableLiveData] wrapped [List] of [CachingDocumentFile] objects that is used as the
     * dataset for the [DirectoryEntryAdapter] adapter which feeds views to the [RecyclerView] that
     * [DirectoryFragment] uses to display directory entries. Private to prevent other classes from
     * modifying it, it is set by our [loadDirectory] method.
     */
    private val _documents = MutableLiveData<List<CachingDocumentFile>>()

    /**
     * Public access to our [_documents] field. An observer is added to it in the `onCreateView`
     * override of [DirectoryFragment] which calls the [DirectoryEntryAdapter.setEntries] method
     * of the adapter feeding views to its [RecyclerView] to have it replace its dataset with it
     * whenever it changes value.
     */
    val documents: MutableLiveData<List<CachingDocumentFile>> = _documents

    /**
     * The [MutableLiveData] wrapped [Event] of [CachingDocumentFile] which is used to signal that
     * the user wishes to open the [CachingDocumentFile] subdirectory for viewing. Private to prevent
     * other classes from modifying it, public access is provided by our [openDirectory] field.
     */
    private val _openDirectory = MutableLiveData<Event<CachingDocumentFile>>()

    /**
     * Public access to our [_openDirectory] field. An [Event] of [CachingDocumentFile] is posted to
     * it from our [documentClicked] method if its [CachingDocumentFile] is a directory. An observer
     * is added to it in the `onCreateView` override of [DirectoryFragment] which calls the
     * [MainActivity.showDirectoryContents] method with the [Uri] of our[CachingDocumentFile]
     * to have it replace the current [DirectoryFragment] with one that will display the contents
     * of the directory whenever it changes value.
     */
    val openDirectory: MutableLiveData<Event<CachingDocumentFile>> = _openDirectory

    /**
     * The [MutableLiveData] wrapped [Event] of [CachingDocumentFile] which is used to signal that
     * the user wishes to open the [CachingDocumentFile] file for viewing. Private to prevent
     * other classes from modifying it, public access is provided by our [openDocument] field.
     */
    private val _openDocument = MutableLiveData<Event<CachingDocumentFile>>()

    /**
     * Public access to our [_openDocument] field. An [Event] of [CachingDocumentFile] is posted to
     * it from our [documentClicked] method if its [CachingDocumentFile] is NOT a directory. An
     * observer is added to it in the `onCreateView` override of [DirectoryFragment] which calls its
     * [DirectoryFragment.openDocument] method with our [CachingDocumentFile] to have it launch an
     * [Intent] with the action [Intent.ACTION_VIEW] to have some activity handle the [Uri] of our
     * [CachingDocumentFile] whenever it changes value.
     */
    val openDocument: MutableLiveData<Event<CachingDocumentFile>> = _openDocument

    /**
     * Loads the [DocumentFile] entries of the directory whose [Uri] is our parameter [directoryUri]
     * into our data, converts them to [CachingDocumentFile] objects for efficiency, sorts them
     * alphabetically, and posts them to our [_documents] field. We initialize our [DocumentFile]
     * variable `val documentsTree` to the [DocumentFile] representing the document tree rooted at
     * our [Uri] parameter [directoryUri] that the [DocumentFile.fromTreeUri] method returns
     * (returning to our caller without doing anything more if this is `null`).
     *
     * We use our `toCachingList` extension function on the [Array] of [DocumentFile] objects that
     * the [DocumentFile.listFiles] method of `documentsTree` returns to create a [List] of
     * [CachingDocumentFile] objects to initialize our variable `val childDocuments`.
     *
     * We launch a new coroutine in the [CoroutineScope] tied to this [ViewModel] whose lambda uses
     * the [Dispatchers.IO] coroutine context to call a suspending block to sort `childDocuments` by
     * the [CachingDocumentFile.name] field saving the new [MutableList] of [CachingDocumentFile]
     * objects it returns when it completes in the variable `val sortedDocuments`. Then back in the
     * [CoroutineScope] tied to this [ViewModel] we post a task to the main thread that sets our
     * [_documents] field to `sortedDocuments`.
     *
     * @param directoryUri the [Uri] of the directory whose [DocumentFile] entries we are supposed
     * to read, convert into a [List] of [CachingDocumentFile] objects, sort alphabetically, and
     * post to our [_documents] data set field.
     */
    fun loadDirectory(directoryUri: Uri) {
        val documentsTree = DocumentFile.fromTreeUri(getApplication(), directoryUri) ?: return
        val childDocuments: List<CachingDocumentFile> = documentsTree.listFiles().toCachingList()

        // It's much nicer when the documents are sorted by something, so we'll sort the documents
        // we got by name. Unfortunate there may be quite a few documents, and sorting can take
        // some time, so we'll take advantage of coroutines to take this work off the main thread.
        viewModelScope.launch {
            val sortedDocuments: MutableList<CachingDocumentFile> = withContext(Dispatchers.IO) {
                childDocuments.toMutableList().apply {
                    sortBy { it.name }
                }
            }
            _documents.postValue(sortedDocuments)
        }
    }

    /**
     * Method to dispatch between clicking on a document (which should be opened), and a directory
     * (which the user wants to navigate into). We branch on the [CachingDocumentFile.isDirectory]
     * property of our parameter [clickedDocument]:
     *  - `true` - the underlying [DocumentFile] is a directory - we post a task to the main thread
     *  to set our [openDirectory] field to a new [Event] constructed to hold [clickedDocument].
     *  - `false` - the underlying [DocumentFile] is NOT a directory - we post a task to the main
     *  thread to set our [openDocument] field to a new [Event] constructed to hold [clickedDocument].
     *
     * @param clickedDocument the [CachingDocumentFile] whose view was clicked.
     */
    fun documentClicked(clickedDocument: CachingDocumentFile) {
        if (clickedDocument.isDirectory) {
            openDirectory.postValue(Event(clickedDocument))
        } else {
            openDocument.postValue(Event(clickedDocument))
        }
    }
}