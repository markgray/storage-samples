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

import android.app.Activity
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android.contentproviderpaging.ImageAdapter.ImageDocument
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fragment that works as a client for accessing the DocumentsProvider
 * ([ImageProvider].
 */
class ImageClientFragment : Fragment() {
    /**
     * The [ImageAdapter] (custom [RecyclerView.Adapter] of [ImageViewHolder] objects) which feeds
     * views to our [RecyclerView].
     */
    private var mAdapter: ImageAdapter? = null
    /**
     * The [LinearLayoutManager] used by our [RecyclerView] as its [RecyclerView.LayoutManager].
     */
    private var mLayoutManager: LinearLayoutManager? = null
    /**
     * The [LoaderManager.LoaderCallbacks] callback interface we use to interact with the manager.
     */
    private val mLoaderCallback = LoaderCallback()

    /**
     * The offset position for the [ContentProvider] to use as the starting position to fetch
     * the images from.
     */
    private val mOffset = AtomicInteger(0)

    /**
     * Called to have the fragment instantiate its user interface view. This will be called between
     * [onCreate] and [onViewCreated]. It is recommended to only inflate the layout in this method
     * and move logic that operates on the returned [View] to [onViewCreated]. We return the [View]
     * that our [LayoutInflater] parameter [inflater] inflates from the layout file with resource ID
     * [R.layout.fragment_image_client] using our [ViewGroup] parameter [container] for `LayoutParams`
     * without attaching to it. This layout file consists of a vertical `LinearLayout` holding a
     * [Button] with the label "Show images" above a [RecyclerView].
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the `LayoutParams` of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_client, container, false)
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been
     * restored in to the view. First we call our super's implementation of `onViewCreated`. We
     * initialize our [Activity] variable `val activity` to the [FragmentActivity] this fragment
     * is currently associated with, and we initialize our [RecyclerView] variable `val recyclerView`
     * to the [View] with ID [R.id.recyclerview].
     *
     * If our [LinearLayoutManager] field [mLayoutManager] is `null` we first initialize it to a
     * new instance of [LinearLayoutManager] before setting the [RecyclerView.LayoutManager] that
     * `recyclerView` will use to [mLayoutManager]. If our [ImageAdapter] field [mAdapter] is `null`
     * we first initialize it to a new instance of [ImageAdapter] before setting the
     * [RecyclerView.Adapter] of `recyclerView` that will provide child views on demand to
     * [mAdapter].
     *
     * Next we add an anonymous [RecyclerView.OnScrollListener] to `recyclerView` whose `onScrolled`
     * override will:
     *  - Initialize its [Int] variable `val lastVisiblePosition` to the adapter position of the
     *  last visible view that the [LinearLayoutManager.findLastVisibleItemPosition] method of
     *  [mLayoutManager] returns.
     *  - If `lastVisiblePosition` is greater than or equal to the number of images already fetched
     *  and added to the adapter [mAdapter] that its [ImageAdapter.getFetchedItemCount] returns we:
     *    - Initialize our [Int] variable `val pageId` to `lastVisiblePosition` divided by [LIMIT] (10)
     *    - Use an instance of [LoaderManager] to start or restart a new [Loader] whose ID is `pageId`
     *    with `null` for its args, and [mLoaderCallback] for its [LoaderManager.LoaderCallbacks]
     *
     * Next we initialize our [Button] variable `val showButton` to the view in our [View] parameter
     * [rootView] with resource ID [R.id.button_show] (labeled "Show images") and then set its
     * [View.OnClickListener] to a lambda which uses an instance of [LoaderManager] to start or
     * restart a new [Loader] whose ID is 0, with `null` for its args, and [mLoaderCallback] for its
     * [LoaderManager.LoaderCallbacks], and then sets the visibility of `showButton` to GONE.
     *
     * @param rootView The [View] returned by [onCreateView].
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(rootView, savedInstanceState)
        val activity: Activity? = activity
        val recyclerView = activity!!.findViewById<View>(R.id.recyclerview) as RecyclerView
        if (mLayoutManager == null) {
            mLayoutManager = LinearLayoutManager(activity)
        }
        recyclerView.layoutManager = mLayoutManager
        if (mAdapter == null) {
            mAdapter = ImageAdapter(activity)
        }
        recyclerView.adapter = mAdapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lastVisiblePosition = mLayoutManager!!.findLastVisibleItemPosition()
                if (lastVisiblePosition >= mAdapter!!.fetchedItemCount) {
                    Log.d(
                        TAG,
                        "Fetch new images. LastVisiblePosition: " + lastVisiblePosition
                            + ", NonEmptyItemCount: " + mAdapter!!.fetchedItemCount
                    )
                    val pageId = lastVisiblePosition / LIMIT
                    // Fetch new images once the last fetched item becomes visible
                    LoaderManager.getInstance(this@ImageClientFragment)
                        .restartLoader(pageId, null, mLoaderCallback)
                }
            }
        })
        val showButton = rootView.findViewById<Button>(R.id.button_show)
        showButton.setOnClickListener {
            LoaderManager.getInstance(this).restartLoader(0, null, mLoaderCallback)
            showButton.visibility = View.GONE
        }
    }

    /**
     * The custom [LoaderManager.LoaderCallbacks] class we use for our field [mLoaderCallback]. It
     * is used as the callback of the [Loader] used to read images from our [ImageProvider].
     */
    private inner class LoaderCallback : LoaderManager.LoaderCallbacks<Cursor> {
        /**
         * Instantiate and return a new [Loader] for the given ID. This will always be called from
         * the process's main thread. We initialize our [Activity] variable `val activity` to the
         * [FragmentActivity] this [ImageClientFragment] fragment is currently associated with. Then
         * we return an anonymous [CursorLoader] whose [CursorLoader.loadInBackground] override:
         *  - Initializes its [Bundle] variable `val bundle` to a new instance.
         *  - Stores our [mOffset] field under the key [ContentResolver.QUERY_ARG_OFFSET] in `bundle`
         *  - Stores our [LIMIT] constant (10) under the key [ContentResolver.QUERY_ARG_LIMIT] in `bundle`
         *  - Retrieves a [ContentResolver] instance for our application's package and calls its
         *  [ContentResolver.query] method with [ImageContract.CONTENT_URI] as the [Uri] to retrieve
         *  ("content://com.example.android.contentproviderpaging.documents/images" which our provider
         *  [ImageProvider] is configured to handle in our AndroidManifest.xml), with `null` as the
         *  projection (will return all columns), `bundle` as the query arguments, and `null` as the
         *  signal to cancel the operation in progress. Finally we return the [Cursor] returned by
         *  the [ContentResolver.query] method to the caller.
         *
         * @param id The ID whose loader is to be created.
         * @param args Any arguments supplied by the caller.
         * @return Return a new [Loader] instance that is ready to start loading.
         */
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
            val activity: Activity? = this@ImageClientFragment.activity
            return object : CursorLoader(activity as Context) {
                /**
                 * Called on a worker thread to perform the actual load and to return the result of
                 * the load operation. Implementations should not deliver the result directly, but
                 * should return them from this method, which will eventually end up calling
                 * [Loader.deliverResult] on the UI thread. If implementations need to process the
                 * results on the UI thread they may override [Loader.deliverResult] and do so
                 * there. To support cancellation, this method should periodically check the value
                 * of `isLoadInBackgroundCanceled` and terminate when it returns true. Subclasses
                 * may also override [CursorLoader.cancelLoadInBackground] to interrupt the load
                 * directly instead of polling `isLoadInBackgroundCanceled`. When the load is
                 * canceled, this method may either return normally or throw `OperationCanceledException`.
                 * In either case, the Loader will call `onCanceled` to perform post-cancellation
                 * cleanup and to dispose of the result object, if any.
                 *
                 * We initialize our [Bundle] variable `val bundle` to a new instance, store our
                 * [mOffset] field under the key [ContentResolver.QUERY_ARG_OFFSET] in `bundle`,
                 * and our [LIMIT] constant (10) under the key [ContentResolver.QUERY_ARG_LIMIT] in
                 * `bundle`. Finally we retrieve a [ContentResolver] instance for our application's
                 * package and calls its [ContentResolver.query] method with [ImageContract.CONTENT_URI]
                 * as the [Uri] to retrieve ("content://com.example.android.contentproviderpaging.documents/images"
                 * which our provider [ImageProvider] is configured to handle in our AndroidManifest.xml),
                 * with `null` as the projection (will return all columns), `bundle` as the query
                 * arguments, and `null` as the signal to cancel the operation in progress, and we
                 * return the [Cursor] returned by the [ContentResolver.query] method to the caller.
                 *
                 * @return The result of the load operation.
                 */
                override fun loadInBackground(): Cursor {
                    val bundle = Bundle()
                    bundle.putInt(ContentResolver.QUERY_ARG_OFFSET, mOffset.toInt())
                    bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, LIMIT)
                    return activity!!.contentResolver
                        .query(ImageContract.CONTENT_URI, null, bundle, null)!!
                }
            }
        }

        /**
         * Called when a previously created loader has finished its load. Note that normally an
         * application is not allowed to commit fragment transactions while in this call, since it
         * can happen after an activity's state is saved.
         *
         * This function is guaranteed to be called prior to the release of the last data that was
         * supplied for this Loader. At this point you should remove all use of the old data (since
         * it will be released soon), but should not do your own release of the data since its Loader
         * owns it and will take care of that. The [Loader] will take care of management of its data
         * so you don't have to. In particular:
         *  - The Loader will monitor for changes to the data, and report them to you through new
         *  calls here. You should not monitor the data yourself.
         *  - The Loader will release the data once it knows the application is no longer using it.
         *  For example, if the data is [Cursor] from a [CursorLoader], you should not call
         *  [Cursor.close] on it yourself. If the [Cursor] is being placed in a `CursorAdapter`,
         *  you should use the `CursorAdapter.swapCursor` method so that the old [Cursor] is not
         *  closed.
         *
         * This will always be called from the process's main thread.
         *
         * First we initialize our [Bundle] variable `val extras` to the contents of the `extras`
         * property of our [Cursor] parameter [cursor] (extra values are an optional way for cursors
         * to provide out-of-band metadata to their users). Next we initialize our [Int] variable
         * `val totalSize` to the value stored under the key [ContentResolver.EXTRA_SIZE] in `extras`.
         * Our [ImageProvider] uses this extra to store the total number of dummy files that it has
         * created from the 13 jpegs in our raw resources and written to the internal storage (in our
         * case 130). Note: There are a maximum of [LIMIT] (10) rows in the [Cursor] parameter [cursor]
         * as we specified in the value stored under the key [ContentResolver.QUERY_ARG_LIMIT] in the
         * args [Bundle] passed to [ContentResolver.query] from which we can construct [ImageDocument]
         * objects. We then call the [ImageAdapter.setTotalSize] method of our [mAdapter] field to
         * have it set the total number of images that can be displayed (the value returned by its
         * [ImageAdapter.getItemCount] override).
         *
         * @param loader The Loader that has finished.
         * @param cursor The data generated by the Loader.
         */
        override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
            val extras = cursor.extras
            val totalSize: Int = extras.getInt(ContentResolver.EXTRA_SIZE)
            mAdapter!!.setTotalSize(totalSize)
            val beforeCount = mAdapter!!.fetchedItemCount
            while (cursor.moveToNext()) {
                val displayName = cursor.getString(
                    cursor.getColumnIndex(
                        ImageContract.Columns.DISPLAY_NAME
                    )
                )
                val absolutePath = cursor.getString(
                    cursor.getColumnIndex(
                        ImageContract.Columns.ABSOLUTE_PATH
                    )
                )
                val imageDocument = ImageDocument()
                imageDocument.mAbsolutePath = absolutePath
                imageDocument.mDisplayName = displayName
                mAdapter!!.add(imageDocument)
            }
            val cursorCount = cursor.count
            if (cursorCount == 0) {
                return
            }
            val activity: Activity? = this@ImageClientFragment.activity
            mAdapter!!.notifyItemRangeChanged(beforeCount, cursorCount)
            val offsetSnapShot = mOffset.get()
            val message = activity!!.resources
                .getString(
                    R.string.fetched_images_out_of, offsetSnapShot + 1,
                    offsetSnapShot + cursorCount, totalSize
                )
            mOffset.addAndGet(cursorCount)
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }

        override fun onLoaderReset(loader: Loader<Cursor>) {}
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "ImageClientFragment"

        /**
         * The number of fetched images in a single query to the DocumentsProvider.
         */
        private const val LIMIT = 10
        fun newInstance(): ImageClientFragment {
            val args = Bundle()
            val fragment = ImageClientFragment()
            fragment.arguments = args
            return fragment
        }
    }
}