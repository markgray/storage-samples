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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
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
    val pageCount get() = pdfRenderer.pageCount

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
     * of `onViewCreated`
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

    override fun onStart() {
        super.onStart()

        val documentUri = arguments?.getString(DOCUMENT_URI_ARGUMENT)?.toUri() ?: return
        try {
            openRenderer(activity, documentUri)
            showPage(currentPageNumber)
        } catch (ioException: IOException) {
            Log.d(TAG, "Exception opening document", ioException)
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            closeRenderer()
        } catch (ioException: IOException) {
            Log.d(TAG, "Exception closing document", ioException)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(CURRENT_PAGE_INDEX_KEY, currentPage.index)
        super.onSaveInstanceState(outState)
    }

    /**
     * Sets up a [PdfRenderer] and related resources.
     */
    @Throws(IOException::class)
    private fun openRenderer(context: Context?, documentUri: Uri) {
        if (context == null) return

        /**
         * It may be tempting to use `use` here, but [PdfRenderer] expects to take ownership
         * of the [FileDescriptor], and, if we did use `use`, it would be auto-closed at the
         * end of the block, preventing us from rendering additional pages.
         */
        val fileDescriptor = context.contentResolver.openFileDescriptor(documentUri, "r") ?: return

        // This is the PdfRenderer we use to render the PDF.
        pdfRenderer = PdfRenderer(fileDescriptor)
        currentPage = pdfRenderer.openPage(currentPageNumber)
    }

    /**
     * Closes the [PdfRenderer] and related resources.
     *
     * @throws IOException When the PDF file cannot be closed.
     */
    @Throws(IOException::class)
    private fun closeRenderer() {
        currentPage.close()
        pdfRenderer.close()
    }

    /**
     * Shows the specified page of PDF to the screen.
     *
     * The way [PdfRenderer] works is that it allows for "opening" a page with the method
     * [PdfRenderer.openPage], which takes a (0 based) page number to open. This returns
     * a [PdfRenderer.Page] object, which represents the content of this page.
     *
     * There are two ways to render the content of a [PdfRenderer.Page].
     * [PdfRenderer.Page.RENDER_MODE_FOR_PRINT] and [PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY].
     * Since we're displaying the data on the screen of the device, we'll use the later.
     *
     * @param index The page index.
     */
    private fun showPage(index: Int) {
        if (index < 0 || index >= pdfRenderer.pageCount) return

        currentPage.close()
        currentPage = pdfRenderer.openPage(index)

        // Important: the destination bitmap must be ARGB (not RGB).
        val bitmap = createBitmap(currentPage.width, currentPage.height, Bitmap.Config.ARGB_8888)

        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        pdfPageView.setImageBitmap(bitmap)

        val pageCount = pdfRenderer.pageCount
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

private const val TAG = "ActionOpenDocumentFragment"
private const val INITIAL_PAGE_INDEX = 0

