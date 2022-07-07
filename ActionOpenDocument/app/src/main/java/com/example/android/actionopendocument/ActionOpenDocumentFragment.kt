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

package com.example.android.actionopendocument

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import java.io.FileDescriptor
import java.io.IOException

/**
 * This fragment has a big [ImageView] that shows PDF pages, and 2 [Button]s to move between pages.
 * We use a [PdfRenderer] to render PDF pages as [Bitmap]s.
 */
class ActionOpenDocumentFragment : Fragment() {

    /**
     * [PdfRenderer] we use to render the PDF.
     */
    private lateinit var pdfRenderer: PdfRenderer

    /**
     * The current [PdfRenderer.Page] PDF document page we are rendering.
     */
    private lateinit var currentPage: PdfRenderer.Page

    /**
     * This is the page number of the PDF page that we were displaying before being stopped for an
     * orientation change which is saved in [onSaveInstanceState] and restored in [onViewCreated],
     * but a logic error somewhere causes the [currentPage] page being displayed to start again at 0
     * TODO: fix it so that the correct page is displayed on restart?
     */
    private var currentPageNumber: Int = INITIAL_PAGE_INDEX

    /**
     * The [ImageView] in our layout file with ID [R.id.image] that we use to display the [Bitmap]
     * of the PDF page we are currently rendering.
     */
    private lateinit var pdfPageView: ImageView

    /**
     * The [Button] in our layout file with ID [R.id.previous] (labeled "Previous") that when clicked
     * moves back one page in the PDF document.
     */
    private lateinit var previousButton: Button

    /**
     * The [Button] in our layout file with ID [R.id.next] (labeled "Next") that when clicked moves
     * forward one page in the PDF document.
     */
    private lateinit var nextButton: Button

    /**
     * The number of pages in the document.
     */
    val pageCount: Int get() = pdfRenderer.pageCount

    companion object {
        /**
         * The key under which the [String] value of the document URI we are supposed to display is
         * stored in the argument [Bundle] this [ActionOpenDocumentFragment] is passed.
         */
        private const val DOCUMENT_URI_ARGUMENT =
            "com.example.android.actionopendocument.args.DOCUMENT_URI_ARGUMENT"

        /**
         * Creates a new instance of [ActionOpenDocumentFragment] whose arguments [Bundle] contains
         * the [String] value of our [Uri] parameter [documentUri]. We contruct a new instance of
         * [ActionOpenDocumentFragment] and immediately use the [apply] extension function to set
         * its construction arguments to a [Bundle] holding the [String] value of our [Uri] parameter
         * [documentUri] stored under the key [DOCUMENT_URI_ARGUMENT].
         *
         * @param documentUri the [Uri] we should use to access the PDF file we are to display.
         * @return a new instance of [ActionOpenDocumentFragment] whose arguments [Bundle] contains
         * the [String] value of our [Uri] parameter [documentUri] stored under the key
         * [DOCUMENT_URI_ARGUMENT]
         */
        fun newInstance(documentUri: Uri): ActionOpenDocumentFragment {
            return ActionOpenDocumentFragment().apply {
                arguments = Bundle().apply {
                    putString(DOCUMENT_URI_ARGUMENT, documentUri.toString())
                }
            }
        }
    }

    /**
     * Called to have the fragment instantiate its user interface view. This will be called between
     * [onCreate] and [onViewCreated]. It is recommended to _only_ inflate the layout in this method
     * and move logic that operates on the returned [View] to [onViewCreated]. We return the [View]
     * that our [LayoutInflater] parameter [inflater] inflates from the layout file with resource ID
     * [R.layout.fragment_pdf_renderer_basic] using our [ViewGroup] parameter [container] for its
     * `LayoutParams` without attaching to it. This [View] consists of a vertical `LinearLayout`
     * holding the [ImageView] we use to display pages from our PDF document above a horizontal
     * `LinearLayout` holding two [Button]s the user can use to move the page number displayed up
     * and down.
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate any views in the
     * fragment.
     * @param container If non-`null`, this is the parent [ViewGroup] that the fragment's UI will be
     * attached to. The fragment should not add the view itself, but this can be used to generate
     * the `LayoutParams` of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the [View] for the fragment's UI, or `null`.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pdf_renderer_basic, container, false)
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been
     * restored in to the [View]. This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created. The fragment's view hierarchy
     * is not however attached to its parent at this point. First we call our super's implementation
     * of `onViewCreated`. We initialize our [Button] field [previousButton] by finding the view
     * with ID [R.id.previous] and immediately use the [apply] extension function to set its
     * [View.OnClickListener] to a lambda which calls our [showPage] method to have it display the
     * page before the page index of our [PdfRenderer.Page] field [currentPage]. We initialize our
     * [Button] field [nextButton] by finding the view with ID [R.id.next] and immediately use the
     * [apply] extension function to set its [View.OnClickListener] to a lambda which calls our
     * [showPage] method to have it display the page after the page index of our [PdfRenderer.Page]
     * field [currentPage]. Finally, if our [Bundle] parameter [savedInstanceState] is not `null`
     * we retrieve the [Int] stored under the key [CURRENT_PAGE_INDEX_KEY] and set our field
     * [currentPageNumber] to it, defaulting to [INITIAL_PAGE_INDEX] if this is the first time we
     * are running.
     *
     * @param view The [View] returned by [onCreateView].
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed from a
     * previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pdfPageView = view.findViewById(R.id.image)
        previousButton = view.findViewById<Button>(R.id.previous).apply {
            setOnClickListener {
                showPage(currentPage.index - 1)
            }
        }
        nextButton = view.findViewById<Button>(R.id.next).apply {
            setOnClickListener {
                showPage(currentPage.index + 1)
            }
        }

        // If there is a savedInstanceState (screen orientations, etc.), we restore the page index.
        currentPageNumber = savedInstanceState?.getInt(CURRENT_PAGE_INDEX_KEY, INITIAL_PAGE_INDEX)
            ?: INITIAL_PAGE_INDEX
    }

    /**
     * Called when the [Fragment] is visible to the user. This is generally tied to Activity.onStart
     * of the containing Activity's lifecycle. First we call our super's implementation of `onStart`.
     * We try to initialize our [Uri] variable `val documentUri` by tring to retrieve the [String]
     * stored under the key [DOCUMENT_URI_ARGUMENT] in the arguments [Bundle] supplied when the
     * fragment was instantiated (if any) and converting that [String] to a [Uri] returning to the
     * caller if the any result in our chain of commands is `null`. If we succeed in initializing
     * `documentUri` to a [Uri] we execute a `try` block intended to catch and log [IOException]
     * wherein we call our [openRenderer] method with the `FragmentActivity` this fragment is
     * currently associated with as the [Context], and `documentUri` as the [Uri] of the PDF file
     * to open. Then we call our [showPage] method to have it render the [currentPageNumber] page
     * in the PDF file to our display.
     */
    override fun onStart() {
        super.onStart()

        val documentUri: Uri = arguments?.getString(DOCUMENT_URI_ARGUMENT)?.toUri() ?: return
        try {
            openRenderer(activity, documentUri)
            showPage(currentPageNumber)
        } catch (ioException: IOException) {
            Log.d(TAG, "Exception opening document", ioException)
        }
    }

    /**
     * Called when the `Fragment` is no longer started. This is generally tied to Activity.onStop
     * of the containing Activity's lifecycle. First we call our super's implementation of `onStop`,
     * then wrapped in a `try` block intended to catch and log [IOException] we call our method
     * [closeRenderer] to have it close the [PdfRenderer] field [pdfRenderer] and related resources.
     */
    override fun onStop() {
        super.onStop()
        try {
            closeRenderer()
        } catch (ioException: IOException) {
            Log.d(TAG, "Exception closing document", ioException)
        }
    }

    /**
     * Called to ask the fragment to save its current dynamic state, so it can later be reconstructed
     * in a new instance if its process is restarted. If a new instance of the fragment later needs
     * to be created, the data you place in the [Bundle] here will be available in the Bundle given
     * to [onCreate], [onCreateView], and [onViewCreated]. We store the page index of the page that
     * our [PdfRenderer.Page] field [currentPage] is rendering under the key [CURRENT_PAGE_INDEX_KEY]
     * in our [Bundle] parameter [outState] then call our super's implementation of
     * `onSaveInstanceState`.
     *
     * @param outState [Bundle] in which to place your saved state.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_PAGE_INDEX_KEY, currentPage.index)
        super.onSaveInstanceState(outState)
    }

    /**
     * Sets up a [PdfRenderer] and related resources. If our [Context] parameter [context] is `null`
     * we return having done nothing. Otherwise we use [context] to retrieve a [ContentResolver]
     * instance for our application's package and call its [ContentResolver.openFileDescriptor]
     * method to open a raw file descriptor to access data under our [Uri] parameter [documentUri]
     * for read mode, returning if this is `null` or using it to initialize our [ParcelFileDescriptor]
     * variable `val fileDescriptor` if it is not. We then initialize our [PdfRenderer] field
     * [pdfRenderer] to a new instance using `fileDescriptor` as the Seekable file descriptor for it
     * to read from. Finally we initialize our [PdfRenderer.Page] field [currentPage] to the page
     * that the [PdfRenderer.openPage] method returns for the index [currentPageNumber].
     *
     * @param context the [Context] of the `FragmentActivity` this fragment is currently associated
     * with that we use to retrieve a [ContentResolver] instance for our application's package.
     * @param documentUri the [Uri] pointing to a PDF document for us to render.
     */
    @Throws(IOException::class)
    private fun openRenderer(context: Context?, documentUri: Uri) {
        if (context == null) return

        /**
         * It may be tempting to use `use` here, but [PdfRenderer] expects to take ownership
         * of the [FileDescriptor], and, if we did use `use`, it would be auto-closed at the
         * end of the block, preventing us from rendering additional pages.
         */
        val fileDescriptor: ParcelFileDescriptor =
            context.contentResolver.openFileDescriptor(documentUri, "r") ?: return

        // This is the PdfRenderer we use to render the PDF.
        pdfRenderer = PdfRenderer(fileDescriptor)
        currentPage = pdfRenderer.openPage(currentPageNumber)
    }

    /**
     * Closes the [PdfRenderer] and related resources. First we call the [PdfRenderer.Page.close]
     * method of our field [currentPage] to close the page we are displaying, then we call the
     * [PdfRenderer.close] method of our field [pdfRenderer] to close the renderer.
     *
     * @throws IOException When the PDF file cannot be closed.
     */
    @Throws(IOException::class)
    private fun closeRenderer() {
        currentPage.close()
        pdfRenderer.close()
    }

    /**
     * Shows the specified page of PDF to the screen. The way [PdfRenderer] works is that it allows
     * for "opening" a page with the method [PdfRenderer.openPage], which takes a (0 based) page
     * number to open. This returns a [PdfRenderer.Page] object, which represents the content of
     * this page. There are two ways to render the content of a [PdfRenderer.Page]:
     *  - [PdfRenderer.Page.RENDER_MODE_FOR_PRINT] Mode to render the content for printing.
     *  - [PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY] Mode to render the content for display on a screen.
     *
     * Since we're displaying the data on the screen of the device, we'll use the later. If our
     * [Int] parameter [index] is less than 0, or greater than or equal to the number of pages in
     * the document that our [PdfRenderer] field [pdfRenderer] is rendering we return having done
     * nothing. Otherwise we call the [PdfRenderer.Page.close] method of our field [currentPage] to
     * close the page we are displaying, then we set [currentPage] to the [PdfRenderer.Page] that
     * the [PdfRenderer.openPage] method of [pdfRenderer] returns when it opens the page whose index
     * is our [Int] parameter [index]. Next we initialize our [Bitmap] variable `val bitmap` to a
     * to an `ARGB_8888` [Bitmap] whose width is the width of [currentPage] and whose height is the
     * height of [currentPage].
     *
     * We call the [PdfRenderer.Page.render] method of [currentPage] to have it render itself to our
     * [Bitmap] variable `bitmap` using the render mode [PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY].
     * We then call the [ImageView.setImageBitmap] method of our field [pdfPageView] to have it set
     * `bitmap` as the content of the [ImageView].
     *
     * We initialize our [Int] variable `val pageCount` to the number of pages in the document that
     * [PdfRenderer] field [pdfRenderer] is holding then proceed to enable the [Button] field
     * [previousButton] if [index] is not equal to 0, and enable the [Button] field [nextButton] if
     * [index] plus 1 is less than `pageCount`. Finally we set the title associated with this activity
     * to the string formed by using the format [String] with ID [R.string.app_name_with_index] to
     * format [index] plus 1 and `pageCount` into its decimal placeholders (the result is like so:
     *
     *     "aodViewer  1/42"
     *
     * where 1 is [index] plus 1 and 42 is `pageCount`.
     *
     * @param index The page index.
     */
    private fun showPage(index: Int) {
        if (index < 0 || index >= pdfRenderer.pageCount) return

        currentPage.close()
        currentPage = pdfRenderer.openPage(index)

        // Important: the destination bitmap must be ARGB (not RGB).
        val bitmap: Bitmap =
            createBitmap(currentPage.width, currentPage.height, Bitmap.Config.ARGB_8888)

        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        pdfPageView.setImageBitmap(bitmap)

        val pageCount: Int = pdfRenderer.pageCount
        previousButton.isEnabled = (0 != index)
        nextButton.isEnabled = (index + 1 < pageCount)
        activity?.title = getString(R.string.app_name_with_index, index + 1, pageCount)
    }
}

/**
 * Key string for saving the state of current page index.
 */
private const val CURRENT_PAGE_INDEX_KEY =
    "com.example.android.actionopendocument.state.CURRENT_PAGE_INDEX_KEY"

/**
 * TAG used for logging
 */
private const val TAG = "ActionOpenDocumentFragment"

/**
 * Default starting page index to display.
 */
private const val INITIAL_PAGE_INDEX = 0

